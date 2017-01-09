
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * This class represents a replica handler instance with methods to replicate
 * data from one server to its replicas.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class ReplicaHandler implements Runnable {
	ServerImpl server;
	int replicationFactor;
	boolean hintsOn = false;
	ArrayList<Hint> hints;
	ArrayList<String> failBuffer;
	ArrayList<Hint> hintsBuffer;
	static Object hintLock = new Object();
	static Object failLock = new Object();
	private long MAX_SAVE_TIME = 15000;
	private int PING_TIMEGAP = 5000;
	private int MAX_HINTS_LIMIT = 5;

	public ReplicaHandler(ServerImpl server, int replicationFactor, boolean hintsOn) {
		this.server = server;
		this.replicationFactor = replicationFactor;
		hints = new ArrayList<>();
		failBuffer = new ArrayList<>();
		this.hintsOn = hintsOn;
		Thread dispatcher = new Thread(this, "hintHandler");
		Thread failureHandler = new Thread(this, "failureHandler");
		dispatcher.start();
		failureHandler.start();
	}

	public void switchOnHints() {
		hintsOn = true;
	}

	public void switchOffHints() {
		hintsOn = false;
	}

	public void replicate(byte[] fileData, String fileName) {
		try {
			ArrayList<String> replicas = server.getNeighbors();
			// for all replica servers
			for (String replica : replicas) {

				// try pinging the replica
				if (RMI.ping(replica)) {
					// if available, copy file to it
					System.out.println("Replicating '" + fileName + "'");
					ServerInterface replicaServer = RMI.getRemoteConnection(replica);
					replicaServer.copyFile(fileData, fileName, RMI.getHostName());
				} else if (hintsOn) {
					if (hints.size() >= MAX_HINTS_LIMIT) {
						synchronized (failLock) {
							System.out.println(ColorCodes.RED + "HINTS BUFFER full." + ColorCodes.RESET);
							failBuffer.add(replica);
							failLock.notify();
						}
						return;
					}
					System.out.println("### NEW HINT {" + replica + " : " + fileName + "} SAVED ###");
					synchronized (hintLock) {
						hints.add(new Hint(replica, fileName, fileData, System.currentTimeMillis()));
						hintLock.notify();
					}
				} else {
					while (!RMI.ping(replica)) {
						Thread.sleep(2000);
					}
					ServerInterface replicaServer = RMI.getRemoteConnection(replica);
					replicaServer.copyFile(fileData, fileName, RMI.getHostName());
				}

			}
		} catch (RemoteException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * private void hintListener() { while (true) { try { synchronized
	 * (hintLock) { hintLock.wait();
	 * 
	 * for (Hint hint : hintsBuffer) { hints.add(hint); }
	 * 
	 * } } catch (InterruptedException e) { e.printStackTrace(); } } }
	 */

	@Override
	public void run() {
		if (Thread.currentThread().getName().equals("hintHandler")) {
			hintThread();
		} else
			failureThread();
	}

	private void failureThread() {
		try {
			while (true) {
				synchronized (failLock) {
					failLock.wait();

					while (failBuffer.size() > 0) {
						for (int index = 0; index < failBuffer.size(); index++) {
							String replica = failBuffer.get(index);
							if (RMI.ping(replica)) {
								new HashComparator(this.server, replica).start();
								failBuffer.remove(replica);
							}
						}
						failLock.wait(PING_TIMEGAP);
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 */
	private void hintThread() {
		while (true) {
			try {
				synchronized (hintLock) {
					if (hints.size() == 0)
						hintLock.wait();

					while (hints.size() > 0) {
						for (int index = 0; index < hints.size(); index++) {
							Hint hint = hints.get(index);
							if (RMI.ping(hint.getReplica())) {
								writeHint(hint);
								hints.remove(hint);
							} else {
								if (System.currentTimeMillis() - hint.getTime() > MAX_SAVE_TIME) {
									synchronized (failLock) {
										System.out.println(hint.getReplica() + ": " + ColorCodes.RED + "HINT TIMEOUT."
												+ ColorCodes.RESET);
										failBuffer.add(hint.getReplica());
										failLock.notify();
									}
									hints.remove(hint);
								}
							}
						}
						hintLock.wait(PING_TIMEGAP);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param hint
	 */
	private void writeHint(Hint hint) {
		new Thread() {
			public void run() {
				try {
					RMI.getRemoteConnection(hint.getReplica()).copyFile(hint.getData(), hint.getFileName(),
							RMI.getHostName());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
