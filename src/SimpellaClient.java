import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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
				connectionListener(clientSocket);
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
		//convert connectionIP to IP and send
		//TODO test code, to be moved to Simpella.java
		String ip_afterConversion = null;
		try {
			ip_afterConversion = InetAddress.getByName(connectionIP).getHostAddress();
		 
			System.out.println("connectionIP = " + connectionIP + " afterConversion = " + ip_afterConversion 
				+ " Is loopback connection? " + InetAddress.getByName(connectionIP).isLoopbackAddress());
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
								+ SimpellaConnectionStatus.outgoingConnectionCount +
								" New connection from " + clientSocket.getLocalAddress().getHostAddress() +
								":" + clientSocket.getLocalPort() + " to " + 
								clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
				}
				ret = 0;
				System.out.println(S.substring(17, len - 2));
				//Acknowledge the connection and complete the 3 way handshake.
				outToServer.write(S.getBytes());
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
		if(Simpella.debug) {
			System.out.println("Sending ping message");
		}
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
				if(null != simpellaStats){
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
					//establish a new connection before quitting!
					ipConfig ipconf = SimpellaConnectionStatus.getNewHostFromGlobalTable();
					if(ipconf != null) {
						/* nothing to do. There are no connection in 
						 * the global table  
						 */
						SimpellaClient newClient = new SimpellaClient();
						if(Simpella.debug) {
							System.out.println("Trying to establish connection with " + ipconf.getIpAddress()
									+ ":" + ipconf.getPort() );
						}
						newClient.setConnectionIP(ipconf.getIpAddress());
						newClient.setConnectionPort(ipconf.getPort());
						newClient.connect();
					} else {
						if(Simpella.debug) {
							System.out.println("No new servents to establish the connection");
						}
					}
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
