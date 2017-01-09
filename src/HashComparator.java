import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * This class provides a multi-threaded implementation of hash comparator
 * reponsible for comparing two merkel trees.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class HashComparator extends Thread {

	ServerImpl server;
	String replica;

	public HashComparator(ServerImpl server, String replica) {
		this.server = server;
		this.replica = replica;
	}

	/**
	 * Overridden run method implementation for getting merkel tree from the
	 * co-ordinator server and its replica. It then compared the trees and
	 * return back a list of files to be fixed.
	 */
	public void run() {
		try {
			ArrayList<ArrayList<HashNode>> replicaTree = RMI.getRemoteConnection(replica)
					.getMerkelTree(RMI.getHostName(), true);
			ArrayList<ArrayList<HashNode>> serverTree = server.getMerkelTree(null, false);
			ArrayList<Integer> fileList = MerkelHash.compareTrees(serverTree, replicaTree);
			if (fileList != null)
				server.fixReplicaFiles(replica, fileList);

		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

}
