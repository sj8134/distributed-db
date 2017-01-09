
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This is a server Program that registers itself using RMI 
 * 
 * @author Sahil Jasrotia, Anurag Malik
 *
 */

public class Server {

	public static void main(String args[]){
		Thread server;
		try {
			int tokens = 0;
			int portNumber = 1099;
			// Check if user intended to make this server a master node
			ServerImpl serverImpl;
			portNumber = Integer.parseInt(args[0]);
			if (args.length > 1){				
				tokens = Integer.parseInt(args[1]);
				serverImpl = new ServerImpl(tokens,portNumber);
			} 
			else{
				serverImpl = new ServerImpl(portNumber);
			}
			
			
			// Create the server instance and bind it with the registry
						
			Registry registry = LocateRegistry.createRegistry(portNumber);
			registry.rebind("FileServer", serverImpl);
			// Create and start the server thread.
			server = new Thread(serverImpl);
			server.start();
			server.join();
			registry.unbind("FileServer");
			System.out.println("Server Left the Chord");
		} 
		catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
}
