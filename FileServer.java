
import java.io.File;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Prasanna Parthasarathy
 *
 */
public class FileServer {
	int port = 8000;
	FileServer(int p, String command){
		System.setProperty("user.dir",".");
		this.port  = p;
		if(command != null && !command.isEmpty())
			if(command.equalsIgnoreCase("start"))
				this.start();
	}

	public void start() {
		ServerSocket serverSocket = null;
		try {
			
			// Create Welcoming Socket at port 8000
			serverSocket = new ServerSocket(this.port);
			createSupportingFiles();
			System.out.println("Server running on port: "+this.port);
			// Wait for contact-request by clients
			while (true) {

				// Once request arrives allocate new socket
				Socket socketConnection = serverSocket.accept();
				System.out.println(
						"Client with connected from " + socketConnection.getInetAddress().getHostName() + "...");
				Thread serverThread = new ServerThread(socketConnection, serverSocket);
				serverThread.start();

			}
		} catch (Exception e) {
			System.out.println("Socket Disconnected!");
			System.out.println(e.getMessage());
		}

	}
	
	public static void createSupportingFiles() throws Exception {
		File upFile = new File("part_uploads.ser");
		File downFile = new File("part_uploads.ser");
		
		if(!upFile.exists()) {
			FileOutputStream fileOut = new FileOutputStream(upFile);
			fileOut.flush();
			fileOut.close();
		}
		
		if(!downFile.exists()) {
			FileOutputStream fileOut = new FileOutputStream(upFile);
			fileOut.flush();
			fileOut.close();
		}
		
	}
	
	public static void main(String[] args) {
		FileServer fs = new FileServer(9999, "start");
	}
}
