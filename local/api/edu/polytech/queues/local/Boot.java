package api.edu.polytech.queues.local;

import api.edu.polytech.queues.Bootstrap;
import api.edu.polytech.queues.Task;

public class Boot implements Bootstrap {
	@Override
	public Task newTask(Runnable r, String name) {
		return null;
	}
}
