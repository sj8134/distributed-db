
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * This is a Server program that contains the Dynamo chord logic with Virtual
 * Nodes.
 * 
 * @author Sahil Jasrotia , Anurag Malik
 */
public class ServerImpl extends UnicastRemoteObject implements ServerInterface, Runnable {
	private static final long serialVersionUID = 1L;
	private Neighbors neighbors; // To save the neighbors of the current node
	private int portNumber = 1099; // To store the port number
	private boolean masterNode = false;// Flag to check if this is master node
	private int MAX_SERVERS = 1000; // Max number of servers in the chord
	private int numTokens = 0;
	private ArrayList<Token> tokenList;
	private HashMap<Integer, String> fileMap;
	private HashMap<String, HashSet<String>> replicaFileMap;
	private HashMap<String, ArrayList<Token>> sentTokenList;
	private int myId;
	private int numNodes = 1;
	private String ENTRY_SERVER = "kansas.cs.rit.edu";
	private ReplicaHandler replicator;
	private FileHandler fileHandler;
	private boolean inDynamo;

	/**
	 * FileServerImpl constructor to create new instance of the Server
	 * 
	 * @param argument,portNumber
	 * 
	 * @return None
	 */
	public ServerImpl(int portNumber)
			throws RemoteException, UnknownHostException, MalformedURLException, NotBoundException {
		RMI.setPort(portNumber);
		this.myId = getHash(RMI.getHostName());
		this.neighbors = new Neighbors(null, null, 0, 0);
		this.portNumber = portNumber;
		this.tokenList = new ArrayList<Token>();
		this.fileMap = new HashMap<Integer, String>();
		this.replicaFileMap = new HashMap<String, HashSet<String>>();
		this.sentTokenList = new HashMap<String, ArrayList<Token>>();
		this.fileHandler = new FileHandler();
		replicator = new ReplicaHandler(this, 2, true);
		inDynamo = false;
	}

	/**
	 * FileServerImpl Constructor for master server
	 * 
	 * @param argument,portNumber
	 * 
	 * @return None
	 */
	public ServerImpl(int tokens, int portNumber)
			throws RemoteException, UnknownHostException, MalformedURLException, NotBoundException {

		RMI.setPort(portNumber);
		String address = RMI.getHostName();
		this.numTokens = tokens;
		this.masterNode = true;
		this.neighbors = new Neighbors(address, address, 0, 0);
		this.portNumber = portNumber;
		this.tokenList = new ArrayList<Token>();
		replicator = new ReplicaHandler(this, 2, true);
		initTokens();
		this.myId = 0;
		this.fileMap = new HashMap<Integer, String>();
		this.replicaFileMap = new HashMap<String, HashSet<String>>();
		this.sentTokenList = new HashMap<String, ArrayList<Token>>();
		this.fileHandler = new FileHandler();
		inDynamo = true;
	}

	/**
	 * Method to initialize the initial tokens list with the master server
	 */
	public void initTokens() {
		int tokenSize = MAX_SERVERS / this.numTokens;
		int count = 0;
		int start = 0;
		int end = tokenSize - 1;
		tokenList.add(new Token(count++, 0, tokenSize - 1));
		for (int i = 1; i < this.numTokens; i++) {
			start = end + 1;
			end = start + tokenSize - 1;
			tokenList.add(new Token(count++, start, end));
		}
	}

	/**
	 * getMyIdSpace: This method returns the nodes ID
	 * 
	 * @param None
	 * 
	 * @return id: Current nodes id in the chord
	 */
	public int getHash(String input) {
		return Math.abs(input.hashCode() % MAX_SERVERS);
	}

	/**
	 * fileFoundOrErrorMessage: This method displays the file trail message/or
	 * any error message
	 * 
	 * @param message:
	 *            Message that needs to be conveyed to the server
	 * 
	 * @return None
	 */
	@Override
	public void fileFoundOrErrorMessage(String message) {
		System.out.println(message);
	}

	/**
	 * This method provides a way of sending tokens to another server in the
	 * distributed network.
	 * 
	 */
	@Override
	public void sendTokens(ArrayList<Token> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			this.tokenList.add(tokens.get(i));
		}
	}

	/**
	 * This method provides a way for receiving tokens from another server.
	 */
	@Override
	public ArrayList<Token> getTokens(String hostName) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		int size = this.tokenList.size();
		int tokensToSend = 0;
		try {
			tokensToSend = size - RMI.getRemoteConnection(ENTRY_SERVER).getTokenDistributionCount();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (size % 2 != 0)
			tokensToSend++;
		// Sending half the tokens I have
		for (int i = size - tokensToSend; i < size; i++) {
			tokens.add(tokenList.get(i));
		}
		// Removing the tokens that I am sending from my list
		for (int i = 0; i < tokens.size(); i++) {
			tokenList.remove(tokens.get(i));
		}
		sentTokenList.put(hostName, tokens);
		return tokens;
	}

	/**
	 * This method provides a way for requesting tokens from another server.
	 */
	@Override
	public void askTokens(String callbackServer) {
		try {
			ServerInterface Interface = RMI.getRemoteConnection(callbackServer);
			Interface.sendTokens(this.getTokens(callbackServer));
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.checkIfTokensAvailable(callbackServer);
	}

	/**
	 * Check if any more tokens are available to be picked up from the sent
	 * tokens list.
	 */
	@Override
	public void checkIfTokensAvailable(String callbackServer) {
		// if (RMI.getHostName().equals(callbackServer))
		// return;
		ServerInterface Interface = RMI.getRemoteConnection(neighbors.getSuccessor());
		try {
			if (this.neighbors.getSuccessorId() != 0) {
				Interface.askTokens(callbackServer);
			} else
				RMI.getRemoteConnection(callbackServer).fileTransferRequest(null);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to distribute tokens to the new joining server
	 */
	@Override
	public void distributeTokens(ArrayList<Token> tokens, int tokenCount, String callbackServer) {
		try {
			if ((!tokens.isEmpty()) && (tokens.size() < tokenCount)) {
				this.tokenList.add(tokens.get(0));
				for (Integer fieldId : tokens.remove(0).getKeys()) {
					Record record = RMI.getRemoteConnection(callbackServer).retrieveFile(fieldId);
					fileInsert(record.getData(), record.getfileName());
				}
			} else {
				for (int i = 0; i < tokenCount; i++) {
					this.tokenList.add(tokens.get(0));
					for (Integer fieldId : tokens.remove(0).getKeys()) {
						Record record = RMI.getRemoteConnection(callbackServer).retrieveFile(fieldId);
						fileInsert(record.getData(), record.getfileName());
					}
				}
			}

			if (!tokens.isEmpty()) {
				RMI.getRemoteConnection(this.neighbors.getSuccessor()).distributeTokens(tokens, tokenCount,
						callbackServer);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method returns the number of total servers currently present in the
	 * distributed system.
	 */
	@Override
	public int getNumServers() {
		return this.numNodes;
	}

	/**
	 * This method is used to update the number of servers present in the
	 * distributed systems.
	 */
	@Override
	public void setNumServers(int numNodes) {
		this.numNodes = numNodes;
	}

	/**
	 * This method is called from the co-ordinator method to remove all or
	 * selected files from the replica server.
	 */
	@Override
	public void deleteReplica(String serverName, ArrayList<String> fileList) {
		HashSet<String> fileNames = this.replicaFileMap.get(serverName);
		if (fileNames == null)
			return;

		if (fileList == null) {
			this.replicaFileMap.remove(serverName);
			for (String file : fileNames) {
				fileHandler.deleteFile(file);
			}
		} else {
			for (String file : fileList) {
				fileNames.remove(file);
				fileHandler.deleteFile(file);
			}
			this.replicaFileMap.put(serverName, fileNames);
		}
	}

	/**
	 * joinChord: Performs the join chord operation
	 * 
	 * @param idSpace:
	 *            The IdSpace the chord belongs to hostName: The hostName of the
	 *            server who wants to join the chord
	 * 
	 * @return None
	 */
	@Override
	public void joinChord(int idSpace, String hostName) {
		this.numNodes++;
		try {
			ServerInterface Interface = RMI.getRemoteConnection(hostName);
			// Send my half tokens to this joining chord
			Interface.sendTokens(getTokens(hostName));

			// If the server lies within current server's Id space then add it
			// to the chord and update the successor and predecessor information
			if ((idSpace > this.myId && idSpace < this.neighbors.getSuccessorId())
					|| this.neighbors.getSuccessorId() == 0) {
				Interface = RMI.getRemoteConnection(hostName);

				this.deleteReplica(this.neighbors.getSuccessor(), null);
				RMI.getRemoteConnection(this.neighbors.getSuccessor()).deleteReplica(RMI.getHostName(), null);
				Interface.updateNeighbours(RMI.getHostName(), this.neighbors.getSuccessor(), this.myId,
						this.neighbors.getSuccessorId());

				// String oldSuccessor = neighbors.getSuccessor();
				RMI.getRemoteConnection(this.neighbors.getSuccessor()).updatePredecessor(hostName, getHash(hostName));
				this.updateSuccessor(hostName, idSpace);
				Interface.checkIfTokensAvailable(hostName);
			}
			// Else forward the request clockwise in the chord till the server
			// find its correct place
			else {
				// Don't allow server to join the chord with the same ID space
				// that already existed in the chord
				if (idSpace == this.myId) {
					RMI.getRemoteConnection(hostName).fileFoundOrErrorMessage("You cannot join chord");
				} else {
					// System.out.println("Forwarding ADD Request");
					RMI.getRemoteConnection(neighbors.getSuccessor()).joinChord(idSpace, hostName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * joinChord: Performs the leave chord operation
	 * 
	 * @param None
	 * 
	 * @return None
	 */
	@Override
	public void leaveChord() {
		ServerInterface Interface;
		try {
			inDynamo = false;
			// Before leaving the chord update the predecessor and successor of
			// the affecting servers
			// and transfer all the files to my successor server
			ServerInterface succServer = RMI.getRemoteConnection(this.neighbors.getSuccessor());
			succServer.deleteReplica(RMI.getHostName(), null);

			succServer.updatePredecessor(this.neighbors.getPredecessor(), this.neighbors.getPredecessorId());

			ServerInterface predServer = RMI.getRemoteConnection(this.neighbors.getPredecessor());
			predServer.deleteReplica(RMI.getHostName(), null);
			predServer.updateSuccessor(this.neighbors.getSuccessor(), this.neighbors.getSuccessorId());

			Interface = RMI.getRemoteConnection(ENTRY_SERVER);
			int numNodes = Interface.getNumServers();
			// I am leaving so number of servers will be less 1
			numNodes = numNodes - 1;
			// Also tell the coordinator node that reduce the number of server
			// count
			Interface.setNumServers(numNodes);
			int tokenCount = this.tokenList.size() / numNodes;
			if (tokenCount == 0) {
				tokenCount = 1;
			}
			// Distribute the tokens to all the servers in the chord
			succServer.distributeTokens(this.tokenList, tokenCount, RMI.getHostName());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * updateNeighbours: To update the current server's neighbors
	 * 
	 * @param predecessor:
	 *            Contains the predecessor node's host name successor: Contains
	 *            the successor's node's host name
	 * 
	 * @return None
	 */
	@Override
	public void updateNeighbours(String predecessor, String successor, int predecessorId, int successorId) {
		// System.out.println("** Neighbors updated");
		this.neighbors.setPredecessor(predecessor);
		this.neighbors.setSuccessor(successor);
		this.neighbors.setPredecessorId(predecessorId);
		this.neighbors.setSuccessorId(successorId);
		inDynamo = true;
	}

	/**
	 * updatePredecessor: To update the current server's predecessor
	 * 
	 * @param predecessor:
	 *            Contains the predecessor node's host name
	 * 
	 * @return None
	 */
	@Override
	public void updatePredecessor(String predecessor, int predecessorId) {
		this.neighbors.setPredecessor(predecessor);
		this.neighbors.setPredecessorId(predecessorId);
	}

	/**
	 * updateNeighbours: To update the current server's successor
	 * 
	 * @param successor:
	 *            Contains the successor's node's host name
	 * 
	 * @return None
	 */
	@Override
	public void updateSuccessor(String successor, int successorId) {
		this.neighbors.setSuccessor(successor);
		this.neighbors.setSuccessorId(successorId);
	}

	/**
	 * run: The server thread runs here
	 * 
	 * @param None
	 * 
	 * @return None
	 */
	@Override
	public void run() {
		Scanner sc = new Scanner(System.in);
		boolean regBind = true;
		while (true) {
			System.out.println("============================ Dashboard Options ==============================");
			System.out.print(ColorCodes.GREEN + "\t" + String.format("%-25s", "1. File Upload"));
			System.out.println(ColorCodes.CYAN + "\t" + String.format("%-25s", "2. Status View") + ColorCodes.RESET);
			if (!masterNode) {
				System.out.print(ColorCodes.CYAN + "\t");
				if (!inDynamo)
					System.out.print(String.format("%-25s", "3. Mount"));
				else {
					System.out.print(String.format("%-25s", "3. Unmount"));
				}
				System.out.print(ColorCodes.GREEN + "\t");
				if (regBind) {
					System.out.print(String.format("%-25s", "4. Unbind"));
				} else {
					System.out.print(String.format("%-25s", "4. Rebind"));
				}
			}
			System.out.println(ColorCodes.RESET);
			System.out.print("OPTION --> ");
			int input = sc.nextInt();
			Registry registry;
			try {
				registry = LocateRegistry.getRegistry(portNumber);

				switch (input) {
				case 1: // File Insert option
					System.out.print("Please enter FILE NAME --> ");
					String fileName = sc.next();
					uploadFile(fileName);
					break;

				case 2: // File View option
					viewInfo();
					break;

				case 3: // Node Join option
					// If server is not the master server and has not already
					// joined
					// then allow it to join the chord
					if (!inDynamo) {
						if (!masterNode && this.neighbors.getPredecessor() == null
								&& this.neighbors.getSuccessor() == null) {
							String hostname = RMI.getHostName();
							RMI.getRemoteConnection(ENTRY_SERVER).joinChord(getHash(hostname), hostname);
						} else {
							System.out.println("You are already in the Chord");
						}
					} else {
						if (!masterNode) {
							leaveChord();
							factoryReset();
						}
					}
					break;

				case 4:
					if (regBind) {
						registry.unbind("FileServer");
						regBind = false;
					} else {
						registry.rebind("FileServer", this);
						regBind = true;
					}
					break;
				default:
					System.out.println("INVALID");
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	/*
	 * This method restarts the server and clears any existing details saved at
	 * the server.
	 */
	public void factoryReset() {
		fileMap.clear();
		replicaFileMap.clear();
		tokenList.clear();
		sentTokenList.clear();
		inDynamo = false;
		neighbors = new Neighbors(null, null, 0, 0);
	}

	/**
	 * Get neighbors method returns the details of predecessor and successor
	 * nodes of the servers.
	 * 
	 * @return
	 */
	public ArrayList<String> getNeighbors() {
		ArrayList<String> neighborsList = new ArrayList<>();
		if (!neighbors.getSuccessor().equals(RMI.getHostName())) {
			neighborsList.add(this.neighbors.getSuccessor());
			if (!neighbors.getSuccessor().equals(neighbors.getPredecessor()))
				neighborsList.add(this.neighbors.getPredecessor());
		}
		return neighborsList;
	}

	/**
	 * this method provides the way of inserting new file on this server.
	 */
	@Override
	public void fileInsert(byte[] fileData, String fileName) throws RemoteException {
		replicator.replicate(fileData, fileName);
		fileHandler.writeToDisk(fileData, fileName);
		System.out.println("File Inserted : " + ColorCodes.YELLOW + "'" + fileName + "'" + ColorCodes.RESET);
		fileMap.put(getHash(fileName), fileName);
	}

	/**
	 * this method provides the way of copying a new file on the current replica
	 * server.
	 */
	@Override
	public void copyFile(byte[] fileData, String fileName, String server) throws RemoteException {
		new Thread() {
			public void run() {
				HashSet<String> files = replicaFileMap.get(server);
				if (files == null) {
					files = new HashSet<>();
				}
				files.add(fileName);

				replicaFileMap.put(server, files);
				fileHandler.writeToDisk(fileData, fileName);
				System.out.println("File copied : " + ColorCodes.YELLOW + "'" + fileName + "'" + ColorCodes.RESET);
			}
		}.start();

	}

	/**
	 * On picking up new tokens, this method is used to transfer files from one
	 * server to another.
	 */
	public void fileTransferRequest(String server) throws RemoteException {

		if (server == null) {
			RMI.getRemoteConnection(neighbors.getSuccessor()).fileTransferRequest(RMI.getHostName());
			return;
		}

		if (server.equals(RMI.getHostName()))
			return;

		ArrayList<String> fileList = new ArrayList<>();
		for (Token token : sentTokenList.get(server)) {
			if (token.getKeys().size() == 0)
				continue;
			for (Integer fileKey : token.getKeys()) {
				fileList.add(fileMap.get(fileKey));
				fileMap.remove(fileKey);
			}
			System.out.println("Sending files - " + fileList);
			// deleteReplica(server, fileList);
			removeReplicas(fileList);
			moveFilesToServer(server, fileList, false);
		}

		if (server.equals(neighbors.getPredecessor()) || server.equals(neighbors.getPredecessor())) {
			ServerInterface serverHost = RMI.getRemoteConnection(server);
			for (String file : fileMap.values()) {
				new Thread() {
					public void run() {
						byte[] fileData = fileHandler.fileRead(file);
						if (fileData == null) {
							return;
						}
						try {
							serverHost.copyFile(fileData, file, RMI.getHostName());
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
		}

		RMI.getRemoteConnection(neighbors.getSuccessor()).fileTransferRequest(server);
	}

	/**
	 * This method make calls to both predecessor and successor nodes to remove
	 * any data on replica server of current server.
	 * 
	 * @param fileList
	 */
	private void removeReplicas(ArrayList<String> fileList) {
		try {
			RMI.getRemoteConnection(neighbors.getSuccessor()).deleteReplica(RMI.getHostName(), fileList);
			RMI.getRemoteConnection(neighbors.getPredecessor()).deleteReplica(RMI.getHostName(), fileList);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	/**
	 * While leaving a chord distributed network or while accepting a new server
	 * as its predecessor, the current server moves some or all of its files
	 * onto its adjacent servers in the network.
	 * 
	 * @param serverName
	 * @param allFiles
	 */
	public void moveFilesToServer(String serverName, ArrayList<String> fileList, boolean allFiles) {

		if (fileList == null || serverName == null)
			return;
		ListIterator<String> itr = fileList.listIterator();
		try {
			while (itr.hasNext()) {
				String file = itr.next();
				// fetch all files data and forward it to the required server
				byte[] buffer = fileHandler.fileRead(file);
				if (buffer == null)
					continue;
				// delete this file from the current server
				fileHandler.deleteFile(file);
				fileMap.remove(file);

				RMI.getRemoteConnection(serverName).fileInsert(buffer, file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to display all the information of the current server,
	 * details include the host name, predecessor and successor nodes, and all
	 * the files stored on this server.
	 */
	public void viewInfo() {

		System.out.println(ColorCodes.BLUE + "******************************* RIT DYNAMO "
				+ " **********************************" + ColorCodes.RESET);
		System.out.print("Hostname : " + ColorCodes.GREEN + RMI.getHostName() + ColorCodes.RESET);
		if (inDynamo)
			System.out.println(String.format("%45s", "Status :" + ColorCodes.GREEN + "LIVE") + ColorCodes.RESET);
		else
			System.out.println(String.format("%45s", "Status :" + ColorCodes.RED + "OFF") + ColorCodes.RESET);
		System.out.println("Connected servers:");
		System.out.println("\tPredecessor : " + neighbors.getPredecessor());
		System.out.println("\tSuccessor : " + neighbors.getSuccessor());

		int count = 0;
		System.out.println("\nFiles:");
		Iterator<String> itr = fileMap.values().iterator();
		while (itr.hasNext()) {
			System.out.println("\t" + ++count + ". " + itr.next());
		}

		count = 0;
		System.out.println("\nReplica Files:");
		for (Entry<String, HashSet<String>> record : replicaFileMap.entrySet()) {
			System.out.println(++count + ". " + record.getKey() + " ( " + record.getValue().size() + " files )");
			for (String file : record.getValue()) {
				System.out.println(ColorCodes.CYAN + "\t" + file + ColorCodes.RESET);
			}
		}

		System.out.print("Tokens List : ");
		for (Token token : tokenList) {
			System.out.print(ColorCodes.YELLOW + token.getId() + " " + ColorCodes.RESET);
		}

		System.out.println(ColorCodes.BLUE + "\n**************************************"
				+ "***************************************\n" + ColorCodes.RESET);
	}

	/**
	 * This method is used by a server on chord distributed system to upload a
	 * file from its directory onto the network and it is saved at its right
	 * destination server.
	 * 
	 * @param fileName
	 */
	private void uploadFile(String fileName) {
		int dest = getHash(fileName);
		try {
			if (checkInTokenRange(dest)) {
				byte[] data = fileHandler.fileRead(fileName);
				if (data != null) {
					System.out.println(ColorCodes.GREEN + "File Upload :  SUCCESS" + ColorCodes.RESET);
					fileMap.put(dest, fileName);
					replicator.replicate(data, fileName);
				}
				return;
			}

			byte[] data = fileHandler.fileRead(fileName);
			if (data == null) {
				return;
			}

			RMI.getRemoteConnection(neighbors.getSuccessor()).fileInsertRequest(data, fileName, dest);
			fileHandler.deleteFile(fileName);

		} catch (RemoteException e) {
			System.out.println(
					ColorCodes.RED + "Error : Failed to connect to " + neighbors.getSuccessor() + ColorCodes.RESET);
			e.printStackTrace();
		}
	}

	/**
	 * this method checks if a new file destination lies in the range of any
	 * token at this server.
	 * 
	 * @param dest
	 * @return
	 */
	private boolean checkInTokenRange(int dest) {
		for (Token token : tokenList) {
			if (token.inRange(dest)) {
				token.addKey(dest);
				return true;
			}
		}
		return false;
	}

	/**
	 * This method provides the way to retrieve a new file request at the
	 * current server.
	 */
	@Override
	public Record retrieveFile(int fieldId) {
		byte[] data = fileHandler.fileRead(this.fileMap.get(fieldId));
		if (data == null) {
			return null;
		}
		Record record = new Record(this.fileMap.get(fieldId), data);
		// delete this file from the current server
		fileHandler.deleteFile(this.fileMap.get(fieldId));
		fileMap.remove(this.fileMap.get(fieldId));

		return record;
	}

	/**
	 * This method is called from any other server to insert files at the
	 * current server.
	 */
	@Override
	public void fileInsertRequest(byte[] data, String fileName, int dest) throws RemoteException {

		if (checkInTokenRange(dest)) {
			fileMap.put(dest, fileName);
			fileHandler.writeToDisk(data, fileName);
			replicator.replicate(data, fileName);
		} else {
			// System.out.println(ColorCodes.GREEN + "Routing '"+fileName+"' to
			// " + neighbors.getSuccessor() + ColorCodes.RESET);
			RMI.getRemoteConnection(neighbors.getSuccessor()).fileInsertRequest(data, fileName, dest);
		}
	}

	/**
	 * 
	 */
	@Override
	public int getTokenDistributionCount() throws RemoteException {
		return numTokens / numNodes;
	}

	/**
	 * This method is used to fix replica files on the server.
	 * 
	 * @param replica
	 * @param fileList
	 */
	public void fixReplicaFiles(String replica, ArrayList<Integer> fileList) {
		try {
			int count = 1;
			System.out.println("Fixing inconsistent replica files: ");
			ServerInterface replicaServer = RMI.getRemoteConnection(replica);
			for (Integer file : fileList) {
				String fileName = fileMap.get(file);
				System.out.println("\t" + count++ + ". " + fileName);
				byte[] data = fileHandler.fileRead(fileName);
				if (data == null)
					continue;
				replicaServer.copyFile(data, fileName, RMI.getHostName());
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method reads all the files saved at the current server and return a
	 * merkel tree of the hash codes of files.
	 */
	@Override
	public ArrayList<ArrayList<HashNode>> getMerkelTree(String reqServer, boolean getReplica) throws RemoteException {
		TreeMap<Integer, BigInteger> merkelMap = new TreeMap<>();
		Collection<String> allFiles = new ArrayList<>();
		if (!getReplica)
			allFiles = fileMap.values();
		else {
			for (String files : replicaFileMap.get(reqServer)) {
				allFiles.add(files);
			}
		}
		for (String file : allFiles) {
			byte[] dataFile = fileHandler.fileRead(file);
			if (dataFile == null) {
				continue;
			}
			String data = new String(dataFile);
			Integer fileHash = getHash(file);
			BigInteger dataHash = Encryptor.encryptSHA(data);

			merkelMap.put(fileHash, dataHash);
		}
		return MerkelHash.createTree(merkelMap);
	}
}