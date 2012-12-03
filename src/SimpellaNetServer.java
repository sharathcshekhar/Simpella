import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class SimpellaNetServer {
	int port;
	boolean status;
	SimpellaStats stats;
	
	class TCPServer implements Runnable {
		private int portNumber;

		public TCPServer(int portNumber) {
			this.portNumber = portNumber;
		}

		public void run() {
			try {
				TCPServerThread(portNumber);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public int start() {
		status = true;
		Thread tcp_t = new Thread(new TCPServer(port));
		tcp_t.start();
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
	private void TCPServerThread(int tcpServerPort) {
		// create client socket
		class TCPserverResponseThread implements Runnable {
			private Socket clientSocket;

			public TCPserverResponseThread(Socket clientSocket) {
				this.clientSocket = clientSocket;
			}

			public void run() {
				try {
					TCPserverResponse(clientSocket);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		;
		Socket clientSocket = new Socket();
		if(Simpella.debug){
			System.out.println("Starting TCP Server at port " + tcpServerPort);
		}
		ServerSocket SimpellaTCP = null;
		try {// create server socket
			SimpellaTCP = new ServerSocket(tcpServerPort);
		} catch (SocketException e) {
			System.out.println("TCP Socket already in use.");
			System.exit(1);
		} catch (IOException e) {
			System.out
					.println("Failed to start TCP server at " + tcpServerPort);
			System.exit(1);
		}
		while (status) {
			try {
				clientSocket = SimpellaTCP.accept();	
			} catch (IOException e) {
				System.out.println("Accept failed at " + tcpServerPort);
				continue;
			}
			
			if(Simpella.debug) {
				String inComingIP = clientSocket.getInetAddress().getHostAddress();
				int inComingPort = clientSocket.getPort();
				System.out.println("TCP connection Accepted from IP " + inComingIP +
						":" + inComingPort);
			}
			/* No point in checking this! DEAD code! 
			if (SimpellaConnectionStatus.isInConnectionPresent(inComingIP,
					inComingPort)
					|| SimpellaConnectionStatus.isOutConnectionPresent(
							inComingIP, inComingPort)) {
				System.out.println("Duplicate connection");
				clientSocket.close();
			} else { */
			if (SimpellaConnectionStatus.incomingConnectionCount < 3) {
				/* maintain a List of threads spawned */
				Thread tcp_serverResp_t = new Thread(
						new TCPserverResponseThread(clientSocket));
				tcp_serverResp_t.start();
			} else {
				String error = "SIMPELLA/0.6 503 Maximum number of connections reached. Sorry!";
				System.out.println("Server replies with " + error);
				DataOutputStream outToClient;
				try {
					outToClient = new DataOutputStream(
							clientSocket.getOutputStream());
				
					outToClient.write(error.getBytes());
					clientSocket.close();
				} catch (IOException e) {
					System.out.println("Client session terminated abruptly. Exiting");
					return;
				}
			}

		}
	}

	/**
	 * TCP server response.
	 * 
	 * @param clientSocket
	 *            the client socket
	 */
	public void TCPserverResponse(Socket clientSocket) {
		// New connection, check for the connection headers
		String replyToConnect = "SIMPELLA/0.6 200 OK\r\n";
		byte[] cmd = new byte[128];
		int len = 0;
		DataOutputStream outToClient;
		DataInputStream inFromClient;
		try {
			outToClient = new DataOutputStream(clientSocket.getOutputStream());

			inFromClient = new DataInputStream(
					clientSocket.getInputStream());

			len = inFromClient.read(cmd);
			if (len == -1) {
				System.out.println("Client closed the connection unexpectedly");
			}
			String S = new String(cmd, 0, len);
			if (S.substring(0, len).equals("SIMPELLA CONNECT/0.6\r\n")) {
				if (Simpella.debug) {
					System.out.println("Server replies with " + replyToConnect);
				}
				outToClient.write(replyToConnect.getBytes());
				// wait for 3 way handshake from the client, read as many bytes
				// as sent. Client should reply with the exact same message
				len = inFromClient.read(cmd, 0, replyToConnect.length());
				String threeWayHandshakedReply = new String(cmd, 0, len);
				if (threeWayHandshakedReply.substring(0, len).equals(
						replyToConnect)) {
					System.out.println(threeWayHandshakedReply.substring(17,
							len - 2));
					SimpellaConnectionStatus
							.addIncomingConnection(clientSocket);
				} else {
					System.out
							.println("Client did not respond with handshake signal, closing connection");
					clientSocket.close();
					return;
				}

			} else {
				System.out.println("Unknown connection request, ignoring");
				return;
			}
		} catch (IOException e) {
			System.out.println("Error while establishing connection. Session ended abruptly");
			return;
		}
		while (true) {
			byte[] header = new byte[23];
			try {
				len = inFromClient.read(header, 0, 23);
				// set bytes and packs read
				stats = SimpellaConnectionStatus.getBySocket(clientSocket);
				if (null != stats) {
					stats.setRecvdBytes(len);
					stats.setRecvdPacks();
					SimpellaConnectionStatus.setTotalBytesRecvd(len);
					SimpellaConnectionStatus.setTotalPacketsRecvd();
				}
				if (len == -1) {
					System.out.println("Client has terminated the connection");
					SimpellaConnectionStatus
							.delIncomingConnection(clientSocket);
					clientSocket.close();
					break;
				}
			} catch (IOException e) {
				System.out.println("Session ended by client. Closing connection");
				return;
			}
			SimpellaHandleMsg msgHandler = new SimpellaHandleMsg();
			msgHandler.handleMsg(header, clientSocket);
		}
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
