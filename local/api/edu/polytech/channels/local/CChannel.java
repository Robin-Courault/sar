package api.edu.polytech.channels.local;

import api.edu.polytech.channels.Channel;
import utils.edu.polytech.utils.CircularBuffer;

public class CChannel implements Channel {
	private final CircularBuffer in;
	private final CircularBuffer out;
	private boolean disconnected = false;
	protected final CChannel otherSide;
	private final Object inLock;
	private final Object outLock;

	private CChannel(CChannel otherSide) {
		this.in = otherSide.out;
		this.out = otherSide.in;
		this.inLock = otherSide.outLock;
		this.outLock = otherSide.inLock;
		this.otherSide = otherSide;
	}

	protected CChannel(int capacity) {
		this.in = new CircularBuffer(capacity);
		this.out = new CircularBuffer(capacity);
		this.inLock = new Object();
		this.outLock = new Object();
		this.otherSide = new CChannel(this);
	}

	private void checkIllegalArguments(byte[] bytes, int offset, int length) {
		// check illegal arguments
		if (offset < 0 || bytes.length <= offset) {
			throw new IllegalArgumentException("offset out of bounds (offset < 0 or offset >= bytes.length)");
		} else if (length < 0 || length > bytes.length) {
			throw new IllegalArgumentException("length out of bounds (length < 0 or length > bytes.length)");
		} else if (offset + length > bytes.length) {
			throw new IllegalArgumentException("offset + length out of bounds (offset + length > bytes.length)");
		}
	}

	@Override
	public int read(byte[] bytes, int offset, int length) {
		checkIllegalArguments(bytes, offset, length);

		synchronized (inLock) {
			while (in.empty() && !disconnected()) {
				try {
					inLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			}

			if (in.empty() && disconnected()) {
				return 0;
			}

			int nBytesRead = 0;
			while (!in.empty() && nBytesRead < length) {
				bytes[offset + nBytesRead] = in.pull();
				nBytesRead++;
			}

			inLock.notifyAll();

			return nBytesRead;
		}
	}

	@Override
	public int write(byte[] bytes, int offset, int length) {
		checkIllegalArguments(bytes, offset, length);

		synchronized (outLock) {
			if (disconnected) {
				// drop bytes
				return length;
			}

			while (out.full() && !disconnected) {
				try {
					outLock.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			}

			if (disconnected) {
				return length;
			}

			int nBytesWritten = 0;
			while (!out.full() && nBytesWritten < length) {
				out.push(bytes[offset + nBytesWritten]);
				nBytesWritten++;
			}

			outLock.notifyAll();

			return nBytesWritten;
		}
	}

	@Override
	public boolean disconnected() {
		return disconnected && in.empty();
	}

	@Override
	public synchronized void disconnect() {
		if (!disconnected) {
			disconnected = true;

			// unlock all blocked write & read
			synchronized (inLock) {
				inLock.notifyAll();
			}
			synchronized (outLock) {
				outLock.notifyAll();
			}
		}
	}
}
