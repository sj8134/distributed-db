
/**
 * This class stores the information about the nodes successor and predecessor 
 * 
 * @author Sahil Jasrotia, Anurag Malik
 *
 */
public class Neighbors {

	private String predecessor;
	private String successor;
	private int predecessorId;
	private int successorId;
	
	public Neighbors(String predecessor, String successor, int predecessorId, int successorId){
		this.setPredecessor(predecessor);
		this.setSuccessor(successor);
		this.predecessorId = predecessorId;
		this.successorId = successorId;
	}

	public String getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(String predecessor) {
		this.predecessor = predecessor;
	}

	public String getSuccessor() {
		return successor;
	}

	public void setSuccessor(String successor) {
		this.successor = successor;
	}

	public int getPredecessorId() {
		return predecessorId;
	}

	public void setPredecessorId(int predecessorId) {
		this.predecessorId = predecessorId;
	}

	public int getSuccessorId() {
		return successorId;
	}

	public void setSuccessorId(int successorId) {
		this.successorId = successorId;
	}
}
