import java.io.Serializable;
import java.util.HashSet;

/**
 * This class represents a token instance, with methods to read and write
 * details about tokens.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class Token implements Serializable {

	private static final long serialVersionUID = 1L;
	private int id;
	private int start;
	private int end;
	private HashSet<Integer> keyList;

	public Token(int id, int start, int end) {
		this.id = id;
		this.start = start;
		this.end = end;
		keyList = new HashSet<>();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public void addKey(Integer key) {
		keyList.add(key);
	}

	public HashSet<Integer> getKeys() {
		return keyList;
	}

	public boolean inRange(int dest) {
		return dest >= start && dest <= end;
	}
}
