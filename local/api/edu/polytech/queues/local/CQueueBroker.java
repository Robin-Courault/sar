package api.edu.polytech.queues.local;

import api.edu.polytech.queues.QueueBroker;
import api.edu.polytech.queues.Task;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CQueueBroker implements QueueBroker {
	private QueueBrokerManager brokerManager;
	private final String name;
	private final Task task;
	private final Map<Integer, Rdv> rdvs = new HashMap<>();

	public CQueueBroker(String name) {
		this.name = name;
		this.brokerManager = QueueBrokerManager.getBrokerManager();
		this.task = Task.task(); // creator task is the current Task

		if (this.brokerManager == null) {
			throw new RuntimeException("BrokerManager is undefined, please to create BrokerManager.");
		} else {
			this.brokerManager.add(this);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Task getTask() {
		return task;
	}

	@Override
	public boolean bind(int port, BindListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener is null");
		}

		if (this.rdvs.containsKey(port)) {
			return false;
		} else {
			Rdv rdv = new Rdv();
			rdv.setAccepter(listener);
			this.rdvs.put(port, rdv);
			return true;
		}
	}

	@Override
	public boolean unbind(int port) {
		if (this.rdvs.containsKey(port)) {
			Rdv removedRdv = this.rdvs.remove(port);
			removedRdv.unbind();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		return connect(name, port, listener, getTask());
	}

	private boolean connect(String name, int port, ConnectListener listener, Task task) {
		if (listener == null) {
			throw new IllegalArgumentException("listener is null");
		}

		if (this.getName().equals(name)) {
			Rdv rdv = getOrCreateRdv(port);
			rdv.addConnection(listener, task);
			return true;
		} else {
			CQueueBroker remoteBroker = brokerManager.get(name);

			if (remoteBroker == null) {
				return false;
			} else {
				remoteBroker.task.post(new Runnable() {
					@Override
					public void run() {
						remoteBroker.connect(name, port, listener, task);
					}
				});
				return true;
			}
		}
	}

	private Rdv getOrCreateRdv(int port) {
		if (this.rdvs.containsKey(port)) {
			return this.rdvs.get(port);
		} else {
			Rdv rdv = new Rdv();
			this.rdvs.put(port, rdv);
			return rdv;
		}
	}

	private class Rdv {
		static final int DEFAULT_CAPACITY = 256;

		private BindListener bindListener;
		private List<Couple<ConnectListener, Task>> connectRequests = new LinkedList<>();
		// left = accept side | right = connect side
		private List<Couple<CMessageQueue, CMessageQueue>> createdQueues = new LinkedList<>();

		public void setAccepter(BindListener bindListener) {
			this.bindListener = bindListener;
			checkRdv();
		}

		public void addConnection(ConnectListener connectListener, Task task) {
			this.connectRequests.add(new Couple<>(connectListener, task));
			checkRdv();
		}

		public void checkRdv() {
			if (bindListener != null && !connectRequests.isEmpty()) {
				CMessageQueue messageQueueAcceptSide = new CMessageQueue(DEFAULT_CAPACITY, CQueueBroker.this);
				CMessageQueue messageQueueConnectSide = messageQueueAcceptSide.otherSide;
				this.createdQueues.add(new Couple<>(messageQueueAcceptSide, messageQueueConnectSide));

				CQueueBroker.this.getTask().post(new Runnable() {
					@Override
					public void run() {
						bindListener.accepted(messageQueueAcceptSide);
					}
				});

				Couple<ConnectListener, Task> currentRequest = connectRequests.removeFirst();
				currentRequest.getRight().post(new Runnable() {
					@Override
					public void run() {
						currentRequest.getLeft().connected(messageQueueConnectSide);
					}
				});
			}
		}

		public void unbind() {
			bindListener.unbound();
			// refused all pending connection requests
			for (Couple<ConnectListener, Task> connectRequest : this.connectRequests) {
				connectRequest.getRight().post(new Runnable() {
					@Override
					public void run() {
						connectRequest.getLeft().refused();
					}
				});
			}
		}
	}
}
