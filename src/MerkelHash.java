
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * This class represents a Merkel hash tree for data integrity checking on
 * co-ordinator and replica server.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class MerkelHash {

	public static ArrayList<ArrayList<HashNode>> createTree(TreeMap<Integer, BigInteger> hashList) {

		ArrayList<ArrayList<HashNode>> tree = new ArrayList<>();
		ArrayList<HashNode> fileList = new ArrayList<>();

		for (Entry<Integer, BigInteger> file : hashList.entrySet()) {
			fileList.add(new HashNode(file.getKey(), file.getValue()));
		}

		tree.add(fileList);
		boolean run = true;
		while (run) {
			ArrayList<HashNode> tempList = new ArrayList<>();
			int lastIndex = fileList.size() - 1;
			for (int i = 0; i <= lastIndex; i += 2) {
				BigInteger parent;
				if (i == lastIndex)
					parent = fileList.get(i).dataHash;
				else
					parent = Encryptor.encryptSHA("" + fileList.get(i).dataHash + fileList.get(i + 1).dataHash);
				tempList.add(new HashNode(-1, parent));
			}
			// tree.add(tempList);
			fileList = tempList;
			run = false;
			if (fileList.size() == 1)
				break;
		}
		return tree;
	}

	/**
	 * Method to compare two Merkel trees and return back a list of non-matching nodes.
	 * 
	 * @param coordinator
	 * @param replica
	 * @return
	 */
	public static ArrayList<Integer> compareTrees(ArrayList<ArrayList<HashNode>> coordinator,
			ArrayList<ArrayList<HashNode>> replica) {

		ArrayList<Integer> changeList = new ArrayList<Integer>();
		// int lastCord = coordinator.size() - 1;
		int lastReplica = replica.size() - 1;
		/*
		 * if (coordinator.get(lastCord).get(0).dataHash.equals(replica.get(
		 * lastReplica).get(0).dataHash)) { return null; }
		 */

		ArrayList<HashNode> temp1, temp2;
		temp1 = coordinator.get(0);
		temp2 = replica.get(0);
		for (int index = 0; index < temp1.size(); index++) {
			// System.out.println((temp1.get(index).dataHash)+" - "
			// +(temp2.get(index).dataHash));
			if (index >= temp2.size() || !temp1.get(index).dataHash.equals(temp2.get(index).dataHash)) {
				changeList.add(temp1.get(index).fileHash);
			}
		}

		int jumpIndex = 0;
		lastReplica = -1;

		for (int level = lastReplica; level >= 0; level--) {
			temp1 = coordinator.get(level);
			temp2 = replica.get(level);
			for (int index = jumpIndex; index < temp1.size(); index++) {
				if (index >= temp2.size() || !temp1.get(index).dataHash.equals(temp2.get(index).dataHash)) {
					if (level == 0) {
						changeList.add(temp1.get(index).fileHash);
					} else {
						jumpIndex = 2 * index;
						break;
					}
				}
			}
		}

		return changeList;
	}

}
