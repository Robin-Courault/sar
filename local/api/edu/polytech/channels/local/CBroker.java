package api.edu.polytech.channels.local;

import api.edu.polytech.channels.Broker;
import api.edu.polytech.channels.Channel;

import java.util.HashMap;
import java.util.Map;

public class CBroker implements Broker {
	private BrokerManager brokerManager;
	private String name;
	private Map<Integer, Rdv> rdvs = new HashMap<>();

	CBroker(String name) {
		this.name = name;
		this.brokerManager = BrokerManager.getBrokerManager();

		if (this.brokerManager == null) {
			throw new RuntimeException("BrokerManager is undefined, please to create BrokerManager.");
		} else {
			this.brokerManager.add(this);
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Channel connect(String name, int port) {
		if (getName().compareTo(name) != 0) {
			return brokerManager.brokers.get(name).connect(name, port);
		} else {
			Rdv rdv = rdvs.get(port);
			if (rdv == null) {
				rdv = new Rdv();
				rdvs.put(port, rdv);
			}

			return rdv.connect();
		}
	}

	@Override
	public Channel accept(int port) {
		Rdv rdv = rdvs.get(port);
		if (rdv == null) {
			rdv = new Rdv();
			rdvs.put(port, rdv);
		}

		return rdv.accept();
	}
}
