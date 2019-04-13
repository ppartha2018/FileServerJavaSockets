import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * @author Prasanna Parthasarathy
 *
 */
public class ServerThread extends Thread {

	ServerSocket serverSocket = null;
	Socket socketConnection;
	String cwd;

	public ServerThread(Socket s, ServerSocket ss) {
		socketConnection = s;
		serverSocket = ss;
		System.setProperty("user.dir", ".");
		cwd = System.getProperty("user.dir");
	}
	public synchronized void uploadFile(String hostname, String filePath, long fileLength, InputStream inFromClient,
			PrintWriter outToClient) throws Exception {
		boolean complete = true;
		// read 64 kb at a time
		int size = 64 * 1024;
		byte[] data = new byte[size];
		long bytesReceived = 0;
		FileOutputStream fileOut = null;
		DataOutputStream dataOut = null;
		//System.out.println("Inside Upload Function: " + hostname + filePath);
		try {
			File uploadingFile = new File(filePath);
			if(!uploadingFile.exists())
				uploadingFile.getParentFile().mkdirs();
			// need to retrieve this info from a stored hashtable - due to partial uploads
			long startOffset = readPartialData(hostname, filePath);
			//System.out.println("Read offset: " + startOffset);
			if (startOffset > -1 && uploadingFile.exists()) {
				// means partial upload
				bytesReceived = startOffset;
				// startOffset = startOffset;
				fileOut = new FileOutputStream(uploadingFile, true); // append mode
				System.out.println("Resuming upload...");
			} else {
				// clean slate
				startOffset = 0;
				bytesReceived = 0;
				fileOut = new FileOutputStream(uploadingFile);
				System.out.println("Uploading new file...");
			}

			//System.out.println("Adjusted offset: " + startOffset);
			// Inform client where to start the file upload from
			outToClient.println(startOffset);
			dataOut = new DataOutputStream(fileOut);
			int curr = 0;

			while (complete) {
				curr = inFromClient.read(data, 0, data.length);
				if (curr == -1) {
					complete = false;
					storeUploadData(hostname, filePath, bytesReceived, true);
				} else {
					dataOut.write(data, 0, curr);
					dataOut.flush();
					bytesReceived += curr;
					storeUploadData(hostname, filePath, bytesReceived, false);
				}
				int percent = (int) (((double) bytesReceived / fileLength) * 100);
				if (percent == 100)
					// delete one more digit, since 3 digits
					System.out.print("\b");
				System.out.print("\b\b\b" + percent + "%");
			}
			System.out.println("\nFile Uploaded!");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			fileOut.flush();
			fileOut.close();
			socketConnection.close();
		}
	}

	public synchronized void storeUploadData(String hostname, String filePath, long bytesUploaded, boolean remove) {
		ObjectOutputStream out = null;
		ObjectInputStream inp = null;
		String serFile = "part_uploads.ser";
		HashMap<String, Long> partialUploadsMap = null;

		try {
			File objFile = new File(serFile);
			if (!objFile.exists())
				partialUploadsMap = new HashMap<String, Long>();
			else {
				inp = new ObjectInputStream(new FileInputStream(objFile));
				try {
					partialUploadsMap = (HashMap<String, Long>) inp.readObject();
				} catch (Exception e) {
					partialUploadsMap = new HashMap<String, Long>();
				}

				if (partialUploadsMap == null) {
					partialUploadsMap = new HashMap<String, Long>();
				}
			}

			out = new ObjectOutputStream(new FileOutputStream(objFile));
			// format: clientIP::filename , bytesUploaded
			String key = hostname + "::" + filePath;
			partialUploadsMap.put(key, bytesUploaded);
			if (remove)
				partialUploadsMap.remove(key);
			out.writeObject(partialUploadsMap);
			out.flush();
			out.close();
			inp.close();
		} catch (Exception e) {
			// do nothing
		}
	}

	public synchronized long readPartialData(String hostname, String filePath) {
		ObjectInputStream inp = null;
		long partBytesOffset = -1;
		try {
			String key = hostname + "::" + filePath;
			inp = new ObjectInputStream(new FileInputStream("part_uploads.ser"));
			HashMap<String, Long> partialUploadsMap = (HashMap<String, Long>) inp.readObject();
			if (partialUploadsMap != null && partialUploadsMap.size() > 0) {
				if (partialUploadsMap.get(key) != null) {
					partBytesOffset = partialUploadsMap.get(key);
				}
			}
			inp.close();
		} catch (Exception e) {
		} finally {
			try {
				inp.close();
			} catch (Exception e) {

			}
		}

		return partBytesOffset;
	}

	public void listFiles(InputStream inFromClient, PrintWriter outToClient, String filePath) throws Exception {
		try {
			System.out.println("Server: processing dir request for : "+filePath);
			ArrayList<File> files = new ArrayList<File>();
			StringBuilder sbr = new StringBuilder();
			File f = new File(filePath);
			if(!f.exists()) {
				sbr.append("ERR404: Given Directory does not exist in Server.");
			}else {
				files = new ArrayList<File>(Arrays.asList(f.listFiles()));
				for(File ff: files) {
					sbr.append(ff.getName());
					sbr.append("\t");
				}
			}
			outToClient.println(sbr.toString());
			
		} catch (Exception e) {
				
		} finally
		{
			inFromClient.close();
			outToClient.close();
			socketConnection.close();
		}
	}
	
	public void mkDir(InputStream inFromClient, PrintWriter outToClient, String filePath) throws Exception {
		try {
			System.out.println("Server: processing mkdir request for : "+filePath);
			StringBuilder sbr = new StringBuilder();
			File f = new File(filePath);
			if(f.exists()) {
				sbr.append("ERR501: Given directory already exist in server.");
			}else {
				boolean created = f.mkdir();
				if(created) {
					sbr.append("Directory ");
					sbr.append(filePath);
					sbr.append(" created in server.");
				} else {
					sbr.append("ERR501: ");
					sbr.append("Directory ");
					sbr.append(filePath);
					sbr.append(" creation failed in server.");
				}
			}
			outToClient.println(sbr.toString());
			
		} catch (Exception e) {
				
		} finally
		{
			inFromClient.close();
			outToClient.close();
			socketConnection.close();
		}
	}
	
	public void rmDir(InputStream inFromClient, PrintWriter outToClient, String filePath) throws Exception {
		try {
			System.out.println("Server: processing rmDir request for : "+filePath);
			StringBuilder sbr = new StringBuilder();
			File f = new File(filePath);
			if(!f.exists()) {
				sbr.append("ERR502: Given directory does not exist in server.");
			}else {
				boolean deleted = f.delete();
				if(deleted) {
					sbr.append("Directory/File '");
					sbr.append(filePath);
					sbr.append("' deleted in server.");
				} else {
					sbr.append("ERR502: ");
					sbr.append("Directory/File '");
					sbr.append(filePath);
					sbr.append("' deletion failed in server.");
				}
			}
			outToClient.println(sbr.toString());
			
		} catch (Exception e) {
				
		} finally
		{
			inFromClient.close();
			outToClient.close();
			socketConnection.close();
		}
	}
	
	public synchronized void uploadFileToClient(String localPath) {
		InputStream localFileStream = null;
		OutputStream outToServer = null;
		BufferedReader inFromServer = null;
		BufferedInputStream bis = null;
		PrintWriter pw = null;
		File localFile = null;

		try {

			localFile = new File(localPath);
			pw = new PrintWriter(socketConnection.getOutputStream(), true);
			if(!localFile.exists()) {
				pw.println(false);
			} else {
				pw.println(true);
				localFileStream = new FileInputStream(localFile);
				outToServer = new DataOutputStream(socketConnection.getOutputStream());
				inFromServer = new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));
				bis = new BufferedInputStream(localFileStream);
				pw.println(localFile.getName());
				pw.println(localFile.length());
				sendBytes(bis, outToServer, inFromServer);
			}
			
		} catch (FileNotFoundException fe) {
			System.out.println("The local file: " + localPath + " does not exist!");
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
				if (bis != null)
					bis.close();
				if (outToServer != null)
					outToServer.close();
				if (pw != null)
					pw.close();
				if (socketConnection != null)
					socketConnection.close();
			} catch (Exception e) {
				//System.out.println("Error closing resources!");
			}
		}
	}
	
	private static void sendBytes(BufferedInputStream in, OutputStream out, BufferedReader inFromServer)
			throws Exception {
		int size = 10000;
		byte[] buffer = new byte[size];
		int len = 0;
		long bytesUploaded = 0;
		long startOffset = 0;
		try {
			startOffset = 0;
		} catch (Exception e) {
			// if some parse exception occurs,ignore, it will be 0 by default
			//System.out.println("Parse exception for startoffset");
		}
		//in.skip(startOffset);
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
			out.flush();
			bytesUploaded += len;
			//System.out.println(bytesUploaded);
		}
	}

	public void run() {
		BufferedReader inCommand = null;
		InputStream inFromClient = null;
		String command = null;
		PrintWriter outToClient = null;

		try {
			//System.out.println("Starting a new server thread..");
			// Create & attach input stream to new socket
			inFromClient = socketConnection.getInputStream();

			inCommand = new BufferedReader(new InputStreamReader(socketConnection.getInputStream()));
			// Create & attach output stream to new socket
			outToClient = new PrintWriter(socketConnection.getOutputStream(), true);

			// Read from socket
			command = inCommand.readLine();

			if (command != null) {
				if (command.equalsIgnoreCase("upload")) {
					String hostname = socketConnection.getInetAddress().getHostName();
					String filePath = inCommand.readLine();
					long incomingLength = Long.parseLong(inCommand.readLine());
					System.out.println("Request to upload file " + filePath + " received from "
							+ socketConnection.getInetAddress().getHostName() + "...");
					uploadFile(hostname, filePath, incomingLength, inFromClient, outToClient);
				} else if (command.equalsIgnoreCase("download")) {
					String filePath = inCommand.readLine();
					System.out.println("Request to download file " + filePath + " received from "
							+ socketConnection.getInetAddress().getHostName() + "...");
					uploadFileToClient(filePath);

				} else if (command.equalsIgnoreCase("dir")) {
					String filePath = inCommand.readLine();
					listFiles(inFromClient, outToClient, filePath);

				} else if (command.equalsIgnoreCase("mkdir")) {
					String filePath = inCommand.readLine();
					mkDir(inFromClient, outToClient, filePath);
				} else if (command.equalsIgnoreCase("rmdir")) {
					String filePath = inCommand.readLine();
					rmDir(inFromClient, outToClient, filePath);
				} else if (command.equalsIgnoreCase("shutdown")) {
					System.out.println("Shutting down server!");
					serverSocket.close();
				}
			}

		} catch (EOFException ee) {
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				outToClient.flush();
				outToClient.close();
			} catch (Exception j) {

			}
		}
	}

}
