package api.edu.polytech.queues.local;

import api.edu.polytech.queues.MessageQueue;
import api.edu.polytech.queues.QueueBroker;

public class CMessageQueue implements MessageQueue {
	@Override
	public QueueBroker broker() {
		return null;
	}

	@Override
	public void setListener(Listener l) {

	}

	@Override
	public boolean send(byte[] bytes, int offset, int length, SendListener l) {
		return false;
	}

	@Override
	public void close() {

	}

	@Override
	public boolean closed() {
		return false;
	}
}
