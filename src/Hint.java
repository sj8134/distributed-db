/**
 * Hint class stores the information to be replicated back on a server that has
 * temporarily failed.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 *
 */

public class Hint {
	private String replica;
	private String fileName;
	private byte[] data;
	private long time;

	public Hint(String replica, String fileName, byte[] fileData, long time) {
		this.replica = replica;
		this.fileName = fileName;
		this.data = fileData;
		this.time = time;
	}

	public String getReplica() {
		return replica;
	}

	public void setReplica(String replica) {
		this.replica = replica;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public long getTime() {
		return time;
	}
}
