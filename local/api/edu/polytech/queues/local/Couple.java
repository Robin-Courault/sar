package api.edu.polytech.queues.local;

public class Couple<L, R> {
	private L left;
	private R right;

	public Couple(L left, R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}
}
