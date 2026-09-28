package api.edu.polytech.queues.local;

import api.edu.polytech.queues.MessageQueue;
import api.edu.polytech.queues.QueueBroker;
import api.edu.polytech.queues.Task;
import utils.edu.polytech.utils.channels.CircularBuffer;

import java.util.LinkedList;
import java.util.List;

public class CMessageQueue implements MessageQueue {
	private final CircularBuffer in;
	private final CircularBuffer out;
	protected final CMessageQueue otherSide;
	private final QueueBroker broker;
	private ClosingState state = ClosingState.OPEN;
	private final CInternalListener internalListener;
	private Listener listener;
	private final List<SendRequest> toSend = new LinkedList<>();
	private boolean pendingSend = false;

	private CMessageQueue(CMessageQueue otherSide) {
		this.in = otherSide.out;
		this.out = otherSide.in;
		this.broker = otherSide.broker;
		this.internalListener = new CInternalListener();
		this.otherSide = otherSide;
	}

	protected CMessageQueue(int capacity, QueueBroker broker) {
		this.in = new CircularBuffer(capacity);
		this.out = new CircularBuffer(capacity);
		this.broker = broker;
		this.internalListener = new CInternalListener();
		this.otherSide = new CMessageQueue(this);
	}

	@Override
	public QueueBroker broker() {
		return broker;
	}

	@Override
	public void setListener(Listener l) {
		listener = l;
	}

	@Override
	public boolean send(byte[] bytes, int offset, int length, SendListener l) {
		if (offset < 0 && offset + length > bytes.length) {
			throw new IllegalArgumentException("Offset or length out of range");
		}

		if (state != ClosingState.OPEN || otherSide.state != ClosingState.OPEN) {
			broker().getTask().post(new Runnable() {
				@Override
				public void run() {
					l.sent(bytes, offset, length);
				}
			});
			return false;
		} else {
			toSend.add(new SendRequest(bytes, offset, length, l));
			if (!pendingSend && !out.full()) {
				pendingSend = true;
				broker.getTask().post(new Runnable() {
					@Override
					public void run() {
						send();
					}
				});
			}
			return true;
		}
	}

	private void send() {
		pendingSend = false;
		Task task = broker().getTask();

		while (!toSend.isEmpty()) {
			SendRequest req = toSend.removeFirst();

			if (state == ClosingState.OPEN) {
				int bytesWrote = req.send();

				otherSide.broker.getTask().post(new Runnable() {
					@Override
					public void run() {
						otherSide.internalListener.messageSent(bytesWrote);
					}
				});

				if (req.restToSend() != 0) {
					// bytes to sent later
					toSend.addFirst(req);
					break;
				}
			}

			task.post(new Runnable() {
				@Override
				public void run() {
					req.sent();
				}
			});
		}
	}

	@Override
	public void close() {
		switch (state) {
			case OPEN:
				state = ClosingState.CLOSING;
			case CLOSING:
				if (in.empty() && otherSide.state != ClosingState.OPEN) {
					state = ClosingState.CLOSED;

					broker.getTask().post(new Runnable() {
						@Override
						public void run() {
							listener.closed();
						}
					});
				}

				if (otherSide.state == ClosingState.CLOSING) {
					otherSide.broker.getTask().post(new Runnable() {
						@Override
						public void run() {
							otherSide.close();
						}
					});
				}

				if (otherSide.toSend.isEmpty()) {
					otherSide.broker.getTask().post(new Runnable() {
						@Override
						public void run() {
							otherSide.close();
						}
					});
				}
			case CLOSED:
				break;
		}
	}

	@Override
	public boolean closed() {
		return state == ClosingState.CLOSED;
	}

	private enum ClosingState {
		CLOSED,
		CLOSING,
		OPEN
	}

	private class SendRequest {
		private final byte[] bytes;
		private final int offset;
		private final int length;
		private final SendListener listener;
		private int lastSended = 0;

		public SendRequest(byte[] bytes, int offset, int length, SendListener l) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
			this.listener = l;
		}

		public void sent() {
			listener.sent(bytes, offset, length);
		}

		public int send() {
			int lastIndexSended = lastSended;

			while (!out.full() && lastSended < length) {
				out.push(bytes[offset + lastSended++]);
			}

			if (lastIndexSended == lastSended) {
				throw new IllegalStateException("Same byte to send after sent");
			}

			return lastSended - lastIndexSended;
		}

		public int restToSend() {
			return lastSended - length;
		}
	}

	private interface InternalListener {
		public void messageSent(int size);
		public void messageReaded();
	}

	protected class CInternalListener implements InternalListener {
		@Override
		public void messageSent(int size) {
			byte[] bytes = new byte[size];
			int index = 0;

			while (!in.empty() && index < size) {
				bytes[index] = in.pull();
				index++;
			}

			broker.getTask().post(new Runnable() {
				@Override
				public void run() {
					listener.received(bytes);
				}
			});

			otherSide.broker.getTask().post(new Runnable() {
				@Override
				public void run() {
					otherSide.internalListener.messageReaded();
				}
			});

			if (state == ClosingState.CLOSING || otherSide.state != ClosingState.OPEN) {
				close();
			}
		}

		@Override
		public void messageReaded() {
			if (!pendingSend) {
				pendingSend = true;
				broker.getTask().post(new Runnable() {
					@Override
					public void run() {
						send();
					}
				});
			}
		}
	}
}
