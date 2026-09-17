package api.edu.polytech.channels.local;

import api.edu.polytech.channels.Bootstrap;
import api.edu.polytech.channels.Broker;
import api.edu.polytech.channels.Task;

public class Boot implements Bootstrap {

  public Boot() {
    new BrokerManager();
  }
  
  @Override
  public Broker newBroker(String name) {
    throw new RuntimeException("NYI");
  }

  @Override
  public Task newTask(Broker b, Runnable r, String name) {
    throw new RuntimeException("NYI");
  }

}
