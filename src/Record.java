import java.io.Serializable;
/**
 * This record represents a record in data store.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class Record implements Serializable{
	
	private static final long serialVersionUID = 1L;
	byte[] data;
	String fileName;

	public Record(String fileName, byte[] data) {
		super();
		this.data = data;
		this.fileName = fileName;
	}
	
	public String getfileName() {
		return fileName;
		
	}
	
	public byte[] getData() {
		return data;
	}
}
