import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

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
					// TODO Auto-generated catch block
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

			} catch (IOException e) {
				System.out.println("Accept failed at " + tcpServerPort);
				continue;
			}
			System.out.println("Accepted!");
			String inComingIP = clientSocket.getInetAddress().getHostAddress();
			int inComingPort = clientSocket.getPort();
			if (SimpellaConnectionStatus.isInConnectionPresent(inComingIP, inComingPort) || 
					SimpellaConnectionStatus.isOutConnectionPresent(inComingIP, inComingPort)) {
				System.out.println("Duplicate connection");
			} else {
				if(SimpellaConnectionStatus.incomingConnectionCount < 3){
					Thread tcp_serverResp_t = new Thread(new TCPserverResponseThread(
						clientSocket));
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
	
	public void sendPong(Socket clientSocket, byte[] header){
		//Reply with a pong on ping 

		System.out.println("Ping received");
		byte[] payload = new byte[37];
		System.arraycopy(header, 0, payload, 0, 22);
		Header h2 = new Header();
		h2.initializeHeader();
		h2.copyMsgId(header);
		h2.setHeader(payload);
		h2.setMsgType("pong");
		//TODO files and size
		byte[] filesShared = null;
		byte[] kbsShared = null;
		//No need to set MsgId as it should be same as ping					
		h2.setPongPayload(clientSocket,payload,filesShared,kbsShared);
		
		DataOutputStream outToClient = null;
		try {
			outToClient = new DataOutputStream(
					clientSocket.getOutputStream());
			outToClient.write(payload);
		} catch (IOException e) {
			System.out.println("Socket Connection Error during pong write");
		}
		
		System.out.println("Server replies with pong : " + Arrays.toString(payload));
	}
	
	/**
	 * TC pserver response.
	 *
	 * @param clientSocket the client socket
	 */
	public void TCPserverResponse(Socket clientSocket) throws Exception {
		//New connection, check for the connection headers
		String replyToConnect = "SIMPELLA/0.6 200 OK";
		DataOutputStream outToClient = new DataOutputStream(
				clientSocket.getOutputStream());

		DataInputStream inFromClient = new DataInputStream(
				clientSocket.getInputStream());
		byte[] cmd = new byte[512]; 
		//TODO read larger messages in chunks
		int len = inFromClient.read(cmd);
		
		String S = new String(cmd);
		if (S.substring(0, len).equals("SIMPELLA CONNECT/0.6\r\n")) {
			System.out.println("Server replies with " + replyToConnect);
			outToClient.write(replyToConnect.getBytes());
			SimpellaConnectionStatus.addIncomingConnection(clientSocket);
		} else {
			System.out.println("Unknown connection request, ignoring");
		}
		while(true) {
			// call blocking read, this is where ping, pong, query 
			// and query-hit msgs are received
			//TODO check headers for ping, pong, query or query-hit messages
			byte[] header = new byte[23];
			len = inFromClient. read(header, 0, 23);
			if (header[16] == (byte) 0x00) {
				System.out.println("Ping received");
				//TODO check routing table if the ping is seen before,
				String key = SimpellaRoutingTables.guidToString(header);
				if(SimpellaRoutingTables.PingTable.containsKey(key)) {
					//TODO combine if and else to one if ignore Ping if the node has seen the request!
				} else {
					SimpellaRoutingTables.insertPingTable(key, clientSocket);
					//TODO reply with a pong					

					if(header[17] > 1) {
						header[17]--; //decrement TTL
						header[18]++; //Increment hops
						sendPong(clientSocket, header);
						broadcastPing(header, clientSocket);
					}
				}
			} //TODO else if header[16] == 0x01, forward
			else if(header[16] == (byte)0x80){
				BufferedReader d
		          = new BufferedReader(new InputStreamReader(inFromClient));
				System.out.println("Download query === " + new String(d.readLine()));
			}
			//TODO switch statement to process the input
			// if header[16] == pong message,
			// See if the pong message was for this system. If not, 
			// forware the pong according to the routing table
		}
	}
	public void broadcastPing(byte[] pingMsg, Socket sender) throws Exception {
		Socket clientSocket = null;
		String clientIP = "";
		for(int i = 0; i < 3; i++) {
			clientIP = 
			SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			if(!(clientIP.equals("")) && 
					!(sender.getInetAddress().getHostAddress().equals(clientIP))) {
				/* send to everyone apart from this node */
				SimpellaCommands.sendPing(clientSocket);
			}
		}
		for(int j = 0; j < 3; j++) {
			clientIP = 
			SimpellaConnectionStatus.outgoingConnectionList[j].remoteIP;
			if(!(clientIP != null) && 
					!(sender.getInetAddress().getHostAddress().equals(clientIP))) {
				/* send to everyone apart from this node */
				SimpellaCommands.sendPing(clientSocket);
			}
		}
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
