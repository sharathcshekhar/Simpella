import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SimpellaFileClient {
	//TODO use SimpellaQueryResults object instead.
	private int fileIndex;
	private String fileName;
	private String serverIP;
	private int serverPort;
	
	public int getFileIndex() {
		return fileIndex;
	}

	public void setFileIndex(int fileIndex) {
		this.fileIndex = fileIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getServerIP() {
		return serverIP;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void downloadFile() {
		Socket clientSocket = null;
		String request = "GET /get/" + fileIndex + "/" + fileName
				+ " HTTP/1.1\r\n";
		String userAgent = "User-Agent: Simpella\r\n";

		// TODO set IP and port from lookup
		String host = "Host: 127.0.0.1:5050\r\n";
		String connRange = "Connection: Keep-Alive\r\n" + "Range: bytes=0-\r\n"
				+ "\r\n";
		StringBuffer download_cmd = new StringBuffer(request + userAgent + host
				+ connRange);
			
		try {
			clientSocket = new Socket(serverIP, serverPort);
		} catch (UnknownHostException e) {
			System.out.println("Socket error during download");
		} catch (IOException e) {
			System.out.println("Socket error during download");
		}

		try {
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());

			outToServer.write(download_cmd.toString().getBytes());
		} catch (IOException e) {
			System.out.println("Error during sending request to server");
		}

		byte[] readResp = new byte[1000];
		try {
			clientSocket.getInputStream().read(readResp);
			System.out.println("Response received : " + new String(readResp));
		} catch (IOException e) {
			System.out.println("Server Response Error");
		}
		FileOutputStream fo = null;
		File tmpFile = new File("." + fileName);
		File newFile = new File(fileName);
		try {
			fo = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e1) {
			System.out.println("Error while downloading file");
		}
		DataOutputStream out = new DataOutputStream(fo);
		byte[] readData = new byte[1024];
		int i = 0;
		try {
			while (!((i = clientSocket.getInputStream().read(readData, 0, 1024)) == -1)) {
				out.write(readData, 0, i);
			}
		} catch (IOException e) {
			System.out.println("Error while downloading file");
		}
		// rename file
		if (!tmpFile.renameTo(newFile)) {
			System.out.println("File already exists");
			return;
		}
		try {
			clientSocket.close();
			out.close();
		} catch (IOException e) {
			System.out.println("Sockets closed abruplty");
		}
	}

}
