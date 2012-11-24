import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

public class SimpellaCommands {
	int connectionPort = 0;
	String connectionIP = "";
	
	public int connect() throws Exception {
		
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
		Socket clientSocket;
		int ret = 0;
		if(SimpellaConnectionStatus.outgoingConnectionCount == 3) {
			System.out.println("Error: Outgoing connection Limit reached");
			return 1;
		}
		clientSocket = new Socket(connectionIP, connectionPort);
		String connect_cmd = "SIMPELLA CONNECT/0.6\r\n";
		DataOutputStream outToServer = new DataOutputStream(
				clientSocket.getOutputStream());
		outToServer.write(connect_cmd.getBytes());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
				clientSocket.getInputStream()));
		char[] replyToConnect = new char[25];
		try {
			int len = inFromServer.read(replyToConnect);

			String S = new String(replyToConnect);
			System.out.println("Server replies with " + S);
			if (S.substring(0, len).equals("SIMPELLA/0.6 200 OK")) {
				SimpellaConnectionStatus.addOutgoingConnection(clientSocket);
				System.out
						.println("Connection successfully accepted, no. of connections = "
								+ SimpellaConnectionStatus.outgoingConnectionCount);
				ret = 0;

				// TODO spawn thread and listen
				Thread clienListner_t = new Thread(new clientConnectionThread(
						clientSocket));
				clienListner_t.start();
				System.out.println("spawned a listner in infnite loop!");

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
			System.out.println("Server closed the connection");
			clientSocket.close();
			return ret;
		}
		return ret;
	}
	

	public void connectionListener(Socket sessionSocket) throws Exception {
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
				sessionSocket.getInputStream()));
		char[] replyToConnect = new char[512];
		//TODO send ping message
		if(SimpellaConnectionStatus.outgoingConnectionCount == 1) {
			System.out.println("Sending ping message");
			sendPing(sessionSocket);
		}
		@SuppressWarnings("unused")
		int len;
		while (true) {
			try {
				len = inFromServer.read(replyToConnect);
				// TODO handle the message
			} catch (SocketException E) {
				System.out.println("Server closed the connection");
				return;
			}
		}
	}

	
	//TODO should take TTL & hops as input
	public static void sendPing(Socket clientSocket) throws Exception
	{
		Header pingH = new Header();
		pingH.setMsgType("ping");
		pingH.initializeHeader();
		pingH.setMsgId();
		String s1 = new String(pingH.getHeader());
		System.out.println("Pinged with Header = " + s1);
		
		DataOutputStream outToServer = new DataOutputStream(
				clientSocket.getOutputStream());
		outToServer.write(pingH.getHeader());
		return;
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
