import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author Prasanna Parthasarathy
 *
 */
public class FSSClient {

	public String hostname = "localhost";
	public int serverPort = 9999;
	Socket clientSocket = null;

	FSSClient(String args[]) {
		if (System.getProperty("PA1_SERVER") != null) {
			try {
				String[] hostProps = System.getProperty("PA1_SERVER").split(":");
				this.hostname = hostProps[0];
				this.serverPort = Integer.parseInt(hostProps[1]);
			} catch (Exception e) {
				System.out.println("Error finding hostname and port from System Environment variable PA1_SERVER.");
			}
		}
		try {
			clientSocket = new Socket(hostname, serverPort);
		} catch (Exception e) {
			System.out.println("Error connecting to Server!");
		}

		this.processCommands(args);
	}

	private static void sendBytes(BufferedInputStream in, OutputStream out, BufferedReader inFromServer)
			throws Exception {
		int size = 10000;
		byte[] buffer = new byte[size];
		int len = 0;
		long bytesUploaded = 0;
		long startOffset = 0;
		try {
			startOffset = Long.parseLong(inFromServer.readLine());
		} catch (Exception e) {
			// if some parse exception occurs,ignore, it will be 0 by default
			//System.out.println("Parse exception for startoffset");
		}
		in.skip(startOffset);
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
			out.flush();
			bytesUploaded += len;
			//System.out.println(bytesUploaded);
		}
	}

	public void uploadFile(String localPath, String remotePath) {
		InputStream localFileStream = null;
		OutputStream outToServer = null;
		BufferedReader inFromServer = null;
		BufferedInputStream bis = null;
		PrintWriter pw = null;
		File localFile = null;

		try {

			localFile = new File(localPath);
			localFileStream = new FileInputStream(localFile);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			pw = new PrintWriter(clientSocket.getOutputStream(), true);
			bis = new BufferedInputStream(localFileStream);
			pw.println("upload");
			pw.println(remotePath);
			pw.println(localFile.length());
			sendBytes(bis, outToServer, inFromServer);
			System.out.println("File Upload successful!");

		} catch (FileNotFoundException fe) {
			System.out.println("The local file: " + localPath + " does not exist!");
		} catch (UnknownHostException uh) {
			//System.out.println("Unknown host!");
			//uh.printStackTrace();
		} catch (IOException ie) {
			//System.out.println("IoException connecting to server!");
			//ie.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bis != null)
					bis.close();
				if (outToServer != null)
					outToServer.close();
				if (pw != null)
					pw.close();
				if (clientSocket != null)
					clientSocket.close();
			} catch (Exception e) {
				// System.out.println("Error closing resources!");
			}
		}
	}

	public void downloadFile(String remotePath) {
		BufferedReader inFromServer = null;
		BufferedInputStream bis = null;
		PrintWriter pw = null;
		File localFile = null;
		boolean complete = true;
		InputStream insServer = null;
		// read 64 kb at a time
		int size = 10000;
		byte[] data = new byte[size];
		long bytesReceived = 0;
		FileOutputStream fileOut = null;
		DataOutputStream dataOut = null;

		try {
			insServer = clientSocket.getInputStream();
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			pw = new PrintWriter(clientSocket.getOutputStream(), true);
			pw.println("download");
			pw.println(remotePath);
			// denote if the file is present in the server
			boolean available = Boolean.parseBoolean(inFromServer.readLine());
			if (available) {
				String fileName = inFromServer.readLine();
				long fileLength = Long.parseLong(inFromServer.readLine());
				localFile = new File(fileName);
				int curr = 0;
				fileOut = new FileOutputStream(localFile);
				dataOut = new DataOutputStream(fileOut);
				System.out.println("Downloading the file...");

				while (complete) {
					curr = insServer.read(data, 0, data.length);
					if (curr == -1) {
						complete = false;
					} else {
						dataOut.write(data, 0, curr);
						dataOut.flush();
						bytesReceived += curr;
					}
					int percent = (int) (((double) bytesReceived / fileLength) * 100);
					if (percent == 100)
						// delete one more digit, since 3 digits
						System.out.print("\b");
					System.out.print("\b\b\b" + percent + "%");
				}
				System.out.println("File Download successful!");
				dataOut.flush();
				dataOut.close();
				fileOut.flush();
				fileOut.close();
			} else {
				System.out.println("The requested file does not exist in server.");
			}

		} catch (UnknownHostException uh) {
			//System.out.println("Unknown host!");
			//uh.printStackTrace();
		} catch (IOException ie) {
			//System.out.println("IoException connecting to server!");
			//ie.printStackTrace();
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			try {
				if (dataOut != null)
					dataOut.close();
				if (fileOut != null)
					fileOut.close();
				if (bis != null)
					bis.close();
				if (bis != null)
					bis.close();
				if (inFromServer != null)
					inFromServer.close();
				if (pw != null)
					pw.close();
				if (clientSocket != null)
					clientSocket.close();
			} catch (Exception e) {
				//System.out.println("Error closing resources!");
			}
		}
	}

	public void listDir(String remotePath) {

		dirActions(remotePath, "dir");
	}

	public void mkdir(String remotePath) {

		dirActions(remotePath, "mkdir");
	}

	public void rmDir(String remotePath) {

		dirActions(remotePath, "rmdir");
	}

	public void dirActions(String remotePath, String command) {

		PrintWriter pw = null;
		BufferedReader fromServer = null;
		try {
			System.out.println("Processing " + command + " request for: " + remotePath);
			pw = new PrintWriter(clientSocket.getOutputStream(), true);
			fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			pw.println(command);
			pw.println(remotePath);
			pw.flush();
			System.out.println(fromServer.readLine());
			pw.close();
			fromServer.close();

		} catch (Exception e) {
			System.out.println("Error processing" + command + " request.");
		}
	}

	public void processCommands(String[] args) {
		String command = args[1];
		try {
			if (command.equalsIgnoreCase("upload")) {
				String pathOnclient = args[2];
				String pathOnServer = args[3];
				uploadFile(pathOnclient, pathOnServer);
			} else if (command.equalsIgnoreCase("download")) {
				String pathOnServer = args[2];
				downloadFile(pathOnServer);
			} else if (command.equalsIgnoreCase("dir")) {
				String remotePath = args[2];
				listDir(remotePath);

			} else if (command.equalsIgnoreCase("mkdir")) {
				String remotePath = args[2];
				mkdir(remotePath);
			} else if (command.equalsIgnoreCase("rmdir")) {
				String remotePath = args[2];
				rmDir(remotePath);
			} else if (command.equalsIgnoreCase("rm")) {
				String remotePath = args[2];
				rmDir(remotePath);
			} else if (command.equalsIgnoreCase("shutdown")) {
				dirActions("server", "shutdown");
				System.out.println("Server shutdown successful!");
			} else {
				System.out.println("Invalid arguments.");
			}
		} catch (Exception e) {
			// e.printStackTrace();
		} finally {
			try {
				clientSocket.close();
			} catch (Exception e2) {
			}
		}

	}

}
