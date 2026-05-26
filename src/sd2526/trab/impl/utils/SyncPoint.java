package sd2526.trab.impl.utils;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {

	private final ConcurrentHashMap<Long, String> result;
	private long version;

	private SyncPoint() {
		this.result = new ConcurrentHashMap<Long, String>();

		// Had to change to -1, because when starting at 0 I was getting errors because
		// waitForResult(0) wouldn't actully wait, and therefore would instantly return
		// before the consumer could apply the operation
		this.version = -1;
	}

	private static SyncPoint instance = null;

	public static synchronized SyncPoint getSyncPoint() {
		if (SyncPoint.instance == null)
			SyncPoint.instance = new SyncPoint();
		return SyncPoint.instance;
	}

	public synchronized String waitForResult(long n) {
		while (version < n) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nothing to be done here
			}
		}
		return result.remove(n);
	}

	public synchronized void waitForVersion(long n) {
		while (version < n) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nothing to be done here
			}
		}
	}

	public synchronized void setResult(long n, String res) {
		if (n < version)
			throw new RuntimeException("Version " + n + " is already set");
		if (res != null) {
			result.put(n, res);
		}
		version = n;
		notifyAll();
	}

	/**
	 * @return this replica's current version, (-1 if no operation happened)
	 */
	public synchronized long currentVersion() {
		return version;
	}
}