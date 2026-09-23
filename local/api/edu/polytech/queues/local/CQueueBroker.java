package api.edu.polytech.queues.local;

import api.edu.polytech.queues.QueueBroker;
import api.edu.polytech.queues.Task;

public class CQueueBroker implements QueueBroker {
	@Override
	public String getName() {
		return "";
	}

	@Override
	public Task getTask() {
		return null;
	}

	@Override
	public boolean bind(int port, BindListener listener) {
		return false;
	}

	@Override
	public boolean unbind(int port) {
		return false;
	}

	@Override
	public boolean connect(String name, int port, ConnectListener listener) {
		return false;
	}
}
