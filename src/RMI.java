
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;

/**
 * This class provides all the RMI connection and server details methods.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 *
 */
public class RMI {
	private static int portNumber;

	public static void setPort(int port) {
		portNumber = port;
	}

	/**
	 * @return
	 * @throws UnknownHostException
	 */
	public static String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName() + ".cs.rit.edu";
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static ServerInterface getRemoteConnection(String serverName) {
		String server = "rmi://" + serverName + ":" + portNumber + "/FileServer";
		ServerInterface Interface = null;
		try {
			Interface = (ServerInterface) Naming.lookup(server);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Interface;
	}

	public static boolean ping(String serverName) {
		String server = "rmi://" + serverName + ":" + portNumber + "/FileServer";
		try {
			Naming.lookup(server);
			System.out.println(ColorCodes.GREEN + serverName + " : Ping successful" + ColorCodes.RESET);
			return true;
		} catch (Exception e) {
			System.out.println(ColorCodes.RED + serverName + " : Ping failed" + ColorCodes.RESET);
			return false;
		}
	}

}
