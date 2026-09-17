package api.edu.polytech.channels.local;

import java.util.HashMap;
import java.util.Map;

public class BrokerManager {
	private static BrokerManager singleton = null;

	public Map<String, CBroker> brokers = new HashMap<>();

	BrokerManager() {
		if (singleton == null) {
			singleton = this;
		} else {
			throw new RuntimeException("BrokerManager already initialized");
		}
	}

	public static BrokerManager getBrokerManager() {
		if (singleton == null) {
			throw new RuntimeException("BrokerManager not initialized");
		}

		return singleton;
	}

	public void add(CBroker broker) {
		brokers.put(broker.getName(), broker);
	}

	public void remove(CBroker broker) {
		brokers.remove(broker.getName());
	}

	public CBroker get(String name) {
		return brokers.get(name);
	}
}
