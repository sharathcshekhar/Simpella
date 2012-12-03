import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class SimpellaClient {
	SimpellaStats simpellaStats;
	int connectionPort = 0;
	String connectionIP = "";
	public int connect() {
		
		class clientConnectionThread implements Runnable {
			private Socket clientSocket;
			public clientConnectionThread(Socket clientSocket) {
				this.clientSocket = clientSocket;
			}
		public void run() {
				try {
					connectionListener(clientSocket);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		;
		Socket clientSocket = null;
		int ret = 0;
		byte[] replyToConnect = new byte[128];
		String connect_cmd = "SIMPELLA CONNECT/0.6\r\n";
		
		if(SimpellaConnectionStatus.outgoingConnectionCount == 3) {
			System.out.println("Error: Outgoing connection Limit reached");
			return 1;
		}

		if(SimpellaConnectionStatus.isOutConnectionPresent(connectionIP, connectionPort)) {
			 System.out.println("Connection to the Simpella Servent already present");
			 return 1;
		}
		try {
			clientSocket = new Socket(connectionIP, connectionPort);
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
			outToServer.write(connect_cmd.getBytes());
			DataInputStream inFromServer = new DataInputStream(
					clientSocket.getInputStream());		
		
			int len = inFromServer.read(replyToConnect);
			if(len == -1) {
				System.out.println("Server closed the connection unexpectedly");
				clientSocket.close();
				return 1;
			}
			String S = new String(replyToConnect, 0, len);
			if(Simpella.debug) {
				System.out.println(len + " bytes of date received from server, reply = " + S);
			}
			
			if (S.substring(0, len).startsWith("SIMPELLA/0.6 200 ")) {
				SimpellaConnectionStatus.addOutgoingConnection(clientSocket);
				if(Simpella.debug) {
					System.out
						.println("Connection successfully accepted, no. of connections = "
								+ SimpellaConnectionStatus.outgoingConnectionCount);
				}
				ret = 0;
				System.out.println(S.substring(17, len - 2));
				//Acknowledge the connection and complete the 3 way handshake.
				outToServer.write(S.getBytes());
				//add if unique ip to global list
				SimpellaConnectionStatus.checkAndAddIpToGlobalTable(connectionIP,connectionPort);
				//Spawn a thread to handle the connection
				Thread clienListner_t = new Thread(new clientConnectionThread(
						clientSocket));
				clienListner_t.start();
				if(Simpella.debug) {
					System.out.println("spawned a listner in infnite loop!");
				}
			} else if (S.startsWith("SIMPELLA/0.6 503")) {
				System.out.println("Connection failed: " + S);
				ret = 1;
				clientSocket.close();
			} else {
				System.out.println("Unknown error: " + S);
				ret = 1;
				clientSocket.close();
			}
		} catch (SocketException E) {
			SimpellaConnectionStatus.delOutgoingConnection(clientSocket);
			System.out.println("Connection failed, server terminated the connection");
			return ret;
		} catch (IOException e) {
			SimpellaConnectionStatus.delOutgoingConnection(clientSocket);
			System.out.println("Connection failed, server terminated the connection");
			return ret;
		}
		return ret;
	}

	public void connectionListener(Socket sessionSocket)  {
		
		int len = 0;
		SimpellaHandleMsg msgHandler = new SimpellaHandleMsg();
		// send ping to the new connection	
		System.out.println("Sending ping message");
		msgHandler.sendPing(sessionSocket);
	
		while (true) {
			try {
				byte[] header = new byte[23];
				DataInputStream inFromServer = new DataInputStream(
						sessionSocket.getInputStream());
				len = inFromServer.read(header, 0, 23);
				if(Simpella.debug) {
				System.out.println("msg received from server " + 
						sessionSocket.getInetAddress().getHostAddress() + " At port " + 
						sessionSocket.getPort());
				}
				//set packet and bit for info command
				simpellaStats = SimpellaConnectionStatus.getBySocket(sessionSocket);
				if(null!=simpellaStats){
					simpellaStats.setRecvdBytes(len);
					simpellaStats.setRecvdPacks();
					SimpellaConnectionStatus.setTotalBytesRecvd(len);
					SimpellaConnectionStatus.setTotalPacketsRecvd();
				}
				
				if(len == -1) {
					System.out.println("Server at " + sessionSocket.getInetAddress().getHostAddress() 
							+ " has terminated the connection");
					SimpellaConnectionStatus.delOutgoingConnection(sessionSocket);
					sessionSocket.close();
					return;
				}
				msgHandler.handleMsg(header, sessionSocket);
			} catch (SocketException E) {
				System.out.println("Error communicating with the server, closing connection");
				SimpellaConnectionStatus.delOutgoingConnection(sessionSocket);
				return;
			} catch (IOException e) {
				System.out.println("Server has ended session abruptly. Closing connection");
				SimpellaConnectionStatus.delOutgoingConnection(sessionSocket);
				return;
			}
		}
	}

	public int getConnectionPort() {
		return connectionPort;
	}

	public void setConnectionPort(int connectionPort) {
		this.connectionPort = connectionPort;
	}

	public String getConnectionIP() {
		return connectionIP;
	}

	public void setConnectionIP(String connectionIP) {
		this.connectionIP = connectionIP;
	}
	
}
