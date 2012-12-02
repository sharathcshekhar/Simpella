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
	private void TCPServerThread(int tcpServerPort) throws Exception {
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
		System.out.println("Starting TCP Server at port " + tcpServerPort);
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
				//add if unique ip to global list
				SimpellaConnectionStatus.checkAndAddIpToGlobalTable(clientSocket.getInetAddress().getHostAddress(),
						clientSocket.getPort());
			} catch (IOException e) {
				System.out.println("Accept failed at " + tcpServerPort);
				continue;
			}
			System.out.println("TCP connection Accepted..");
			String inComingIP = clientSocket.getInetAddress().getHostAddress();
			int inComingPort = clientSocket.getPort();
			if (SimpellaConnectionStatus.isInConnectionPresent(inComingIP,
					inComingPort)
					|| SimpellaConnectionStatus.isOutConnectionPresent(
							inComingIP, inComingPort)) {
				System.out.println("Duplicate connection");
				clientSocket.close();
			} else {
				if (SimpellaConnectionStatus.incomingConnectionCount < 3) {
					/* maintain a List of threads spawned */
					Thread tcp_serverResp_t = new Thread(
							new TCPserverResponseThread(clientSocket));
					tcp_serverResp_t.start();
				} else {
					String error = "SIMPELLA/0.6 503 Maximum number of connections reached";
					System.out.println("Server replies with " + error);
					DataOutputStream outToClient = new DataOutputStream(
							clientSocket.getOutputStream());
					outToClient.write(error.getBytes());
					clientSocket.close();
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
	public void TCPserverResponse(Socket clientSocket) throws Exception {
		// New connection, check for the connection headers
		String replyToConnect = "SIMPELLA/0.6 200 OK";
		DataOutputStream outToClient = new DataOutputStream(
				clientSocket.getOutputStream());

		DataInputStream inFromClient = new DataInputStream(
				clientSocket.getInputStream());
		byte[] cmd = new byte[512];
		// TODO read larger messages in chunks
		int len = inFromClient.read(cmd);

		String S = new String(cmd);
		if (S.substring(0, len).equals("SIMPELLA CONNECT/0.6\r\n")) {
			System.out.println("Server replies with " + replyToConnect);
			outToClient.write(replyToConnect.getBytes());
			SimpellaConnectionStatus.addIncomingConnection(clientSocket);
						
		} else {
			System.out.println("Unknown connection request, ignoring");
			return;
		}
		while (true) {
			byte[] header = new byte[23];
			len = inFromClient.read(header, 0, 23);
			//set bytes and packs read
			stats = SimpellaConnectionStatus.getBySocket(clientSocket);
			if(null!=stats){
				stats.setRecvdBytes(len);
				stats.setRecvdPacks();
				SimpellaConnectionStatus.setTotalBytesRecvd(len);
				SimpellaConnectionStatus.setTotalPacketsRecvd();
			}
			if (len == -1) {
				System.out.println("Client has close the socket, exit");
				break;
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
