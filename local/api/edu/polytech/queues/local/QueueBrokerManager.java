package api.edu.polytech.queues.local;

import java.util.HashMap;
import java.util.Map;

public class QueueBrokerManager {
	private static QueueBrokerManager singleton = null;

	public Map<String, CQueueBroker> brokers = new HashMap<>();

	QueueBrokerManager() {
		if (singleton == null) {
			singleton = this;
		} else {
			throw new RuntimeException("BrokerManager already initialized");
		}
	}

	public static QueueBrokerManager getBrokerManager() {
		if (singleton == null) {
			throw new RuntimeException("BrokerManager not initialized");
		}

		return singleton;
	}

	public void add(CQueueBroker broker) {
		brokers.put(broker.getName(), broker);
	}

	public void remove(CQueueBroker broker) {
		brokers.remove(broker.getName());
	}

	public CQueueBroker get(String name) {
		return brokers.get(name);
	}
}
