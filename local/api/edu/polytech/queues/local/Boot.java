package api.edu.polytech.queues.local;

import api.edu.polytech.queues.Bootstrap;
import api.edu.polytech.queues.Task;
import utils.edu.polytech.utils.queues.Executor;

public class Boot implements Bootstrap {
	@Override
	public Task newTask(Runnable r, String name) {
		Task newTask = Executor.self().newTask(name);
		newTask.post(r);
		return newTask;
	}
}
