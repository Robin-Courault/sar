package api.edu.polytech.channels.local;

import api.edu.polytech.channels.Channel;

import java.util.ArrayList;
import java.util.List;

public class Rdv {
	static final int DEFAULT_CAPACITY = 256;

	private int nConnections = 0;
	private boolean hasAccept = false;
	private Channel channelToShare = null;

	public Rdv() {}

	public synchronized Channel accept() {
		if (hasAccept) {
			throw new RuntimeException("An accept is already in waiting state.");
		} else {
			hasAccept = true;

			// create channels for future connection
			CChannel channelForAccept = new CChannel(DEFAULT_CAPACITY);
			channelToShare = channelForAccept.otherSide;

			if (nConnections <= 0) {
				try {
					wait();
				} catch (InterruptedException e) {
					return null;
				}
			} else {
				nConnections--;
				notify();
			}

			return channelForAccept;
		}
	}

	public synchronized Channel connect() {
		// if hasAccept is true here, then nConnections == 1 (for this connection)
		if (hasAccept) {
			notify();
		}

		while (!hasAccept) {
			nConnections++;
			try {
				wait();
			} catch (InterruptedException e) {
				return null;
			}
		}

		hasAccept = false;
		Channel channelToShare = this.channelToShare;
		this.channelToShare = null;
		return channelToShare;
	}
}
