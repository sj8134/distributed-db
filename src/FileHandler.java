
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;

/**
 * This class contains all the file handling methods.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 *
 */
public class FileHandler {
	static String homeDirectory;

	FileHandler() {
		try {
			homeDirectory = System.getProperty("user.home") + "/Courses/CSCI652/"
					+ InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * fileSave: Saves the file in the local disc
	 * 
	 * @param fileData:
	 *            Contains the file data fileName: Contains the file name
	 * 
	 * @return None
	 */
	public void writeToDisk(byte[] fileData, String fileName) {
		try {
			BufferedOutputStream bufferStream = new BufferedOutputStream(
					new FileOutputStream(homeDirectory + "/" + fileName));
			bufferStream.write(fileData, 0, fileData.length);
			// System.out.println("NEW FILE RECEIVED : " + ColorCodes.YELLOW +
			// "'" + fileName + "'" + ColorCodes.RESET);
			bufferStream.flush();
			bufferStream.close();
		} catch (IOException e) {
			System.out.println(ColorCodes.RED + "ERROR : File WRITE operation failed" + ColorCodes.RESET);
			e.printStackTrace();
		}
	}

	/**
	 * fileRead: Performs the file copy operation
	 * 
	 * @param fileName:
	 *            Contains the file name that needs to be copied
	 * 
	 * @return byte[]: the file data
	 */
	public byte[] fileRead(String fileName) {
		byte buffer[] = null;
		try {
			File file = new File(homeDirectory + "/" + fileName);
			buffer = new byte[(int) file.length()];
			BufferedInputStream bf = new BufferedInputStream(new FileInputStream(homeDirectory + "/" + file.getName()));
			bf.read(buffer, 0, buffer.length);
			bf.close();
		} catch (Exception e) {
			System.out.println(ColorCodes.RED + "ERROR : No such file found." + ColorCodes.RESET);
			return null;
		}
		return buffer;
	}

	/**
	 * This method is used to delete a file from the local directory of current
	 * server
	 * 
	 * @param file
	 */
	public void deleteFile(String fileName) {
		File file = new File(homeDirectory + "/" + fileName);
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			System.out.println("Error : File to be deleted not found.");
			e.printStackTrace();
		}
	}

}
