import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;

public class SimpellaFileServer {

	int port;
	boolean status;

	class FileServer implements Runnable {
		private int portNumber;

		public FileServer(int portNumber) {
			this.portNumber = portNumber;
		}

		public void run() {
			try {
				FileServerThread(portNumber);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public int start() {
		status = true;
		Thread fileSrv_t = new Thread(new FileServer(port));
		fileSrv_t.start();
		return 0;
	}

	public void stop() {
		status = false;
	}

	/**
	 * TCP server thread.
	 * 
	 * @param tcpServerPort
	 *            the tcp server port
	 */
	private void FileServerThread(int fileServerPort) throws Exception {
		// create client socket
		class FileServerServiceThread implements Runnable {
			private Socket clientSocket;

			public FileServerServiceThread(Socket clientSocket) {
				this.clientSocket = clientSocket;
			}

			public void run() {
				try {
					fileServerService(clientSocket);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		;
		Socket clientSocket = new Socket();
		System.out.println("Starting File Server at port " + fileServerPort);
		ServerSocket fileSrvSocket = null;
		try {// create server socket
			fileSrvSocket = new ServerSocket(fileServerPort);
		} catch (SocketException e) {
			System.out.println("TCP Socket already in use.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Failed to start File server at "
					+ fileServerPort);
			System.exit(1);
		}
		while (status) {
			try {
				clientSocket = fileSrvSocket.accept();

			} catch (IOException e) {
				System.out.println("Accept failed at " + fileServerPort);
				continue;
			}
			System.out.println("Download connection Accepted..");
			Thread file_dwResp_t = new Thread(new FileServerServiceThread(
					clientSocket));
			file_dwResp_t.start();
		}
	}

	/**
	 * TCP server response.
	 * 
	 * @param clientSocket
	 *            the client socket
	 */
	public void fileServerService(Socket clientSocket) throws Exception {
		byte[] input = new byte[512];
		int len = clientSocket.getInputStream().read(input);
		System.out.println(len + "bytes of data read from client");
		String request = new String(input);
		StringTokenizer token = new StringTokenizer(request, "\r\n");
		String firstLineInRequest = token.nextToken();
		
		boolean flag = false;
		while (token.hasMoreElements()) {
			String eachToken[] = token.nextToken().split(":");
			for (String each : eachToken) {
				if (each.contains("bytes=0-")) {
					flag = true;
					break;
				}
			}
		}

		if (!flag) {
			System.out.println("Invalid Simpella File request");
			return;
		}
		
		String requestString = firstLineInRequest.substring(4, firstLineInRequest.length() - 9);
		System.out.println("/get/FileIndex/Filename/ = " + requestString);
		String[] parsedStrings = requestString.split("/");
		System.out.println("Parsed Strings:" + parsedStrings[0] + parsedStrings[1] 
				+ parsedStrings[2] + parsedStrings[3]);
		int fileIndex = Integer.parseInt(parsedStrings[2]);
		String filename = parsedStrings[3];
		
		SimpellaFileShareDB db = new SimpellaFileShareDB();
	//	String file_fullPath = db.getFullFilePath(filename, fileIndex);
		// test code
		String file_fullPath = "/home/sharath/simpella_share/share3/01 Yarighelhana.mp3";
		String response = null;
		if(file_fullPath == null){
			System.out.println("File not present");
			response = "HTTP/1.1 503 File not found.\r\n\r\n";
			clientSocket.getOutputStream().write(response.getBytes());
			return;
		}
		File fileStream = new File(file_fullPath);
		
		byte[] fileBuffer = new byte[4096]; //Write through a 4Kb buffer

		response = "HTTP/1.1 200 OK\r\n";
		String server = "Server: Simpella0.6\r\n";
		String contentType = "Content-type: application/binary\r\n";
		String contentLen = "Content-length: " + fileStream.length() + "\r\n\r\n";
		StringBuffer responseBuf = new StringBuffer(response + server
				+ contentType + contentLen);
		clientSocket.getOutputStream().write(responseBuf.toString().getBytes());

		DataInputStream di = new DataInputStream(new FileInputStream(fileStream));

		DataOutputStream dos = new DataOutputStream(
				clientSocket.getOutputStream());
		int i = 0;
		while (!((i = di.read(fileBuffer, 0, 1024)) == -1)) {
			dos.write(fileBuffer, 0, i);
		}
		dos.close();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
