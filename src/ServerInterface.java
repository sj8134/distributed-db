
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * File server interface used by the different servers to join cord and
 * perform different operations in chord  
 * 
 * @author Sahil Jasrotia, Anurag Malik
 *
 */
public interface ServerInterface extends Remote{	
	public void joinChord(int idSpace, String hostName) throws RemoteException;
	public void updateNeighbours(String predecessor, String successor,int predecessorId,int successorId) throws RemoteException;	
	public void updatePredecessor(String predecessor, int predecessorId) throws RemoteException;
	public void updateSuccessor(String successor, int successorId) throws RemoteException;
	public void leaveChord() throws RemoteException;
	public void fileInsert(byte fileData[], String fileName) throws RemoteException;	
	public void fileFoundOrErrorMessage(String message) throws RemoteException;
	public void sendTokens(ArrayList<Token> tokenList) throws RemoteException;
	public ArrayList<Token> getTokens(String hostName) throws RemoteException;
	public void checkIfTokensAvailable(String callbackServer) throws RemoteException;
	public void askTokens(String askingServer) throws RemoteException;
	public void distributeTokens(ArrayList<Token> tokens, int tokenCount, String callbackServer) throws RemoteException;
	public int getNumServers() throws RemoteException;
	public void setNumServers(int numNodes) throws RemoteException;
	//TODO
	public void copyFile(byte[] fileData, String fileName, String server) throws RemoteException;
	public void fileInsertRequest(byte[] data, String fileName, int dest) throws RemoteException;
	public void fileTransferRequest(String server) throws RemoteException;
	public void deleteReplica(String serverName, ArrayList<String> fileList) throws RemoteException;
	//public ArrayList<Token> getTokens(String hostName) throws RemoteException;
	public int getTokenDistributionCount() throws RemoteException;
	public ArrayList<ArrayList<HashNode>> getMerkelTree(String reqServer, boolean getReplica) throws RemoteException;
	public Record retrieveFile(int fieldId) throws RemoteException;
}
