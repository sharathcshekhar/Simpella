import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class SimpellaClient {
	int connectionPort = 0;
	String connectionIP = "";
	//TODO for testing purpose moved socket out of connect()
	Socket clientSocket;
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

		DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
		
		byte[] replyToConnect = new byte[25];
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
		
		int len = 0;
		SimpellaHandleMsg msgHandler = new SimpellaHandleMsg();
	//	if(SimpellaConnectionStatus.outgoingConnectionCount == 1) {
		// send ping to the new connection	
		System.out.println("Sending ping message");
		msgHandler.sendPing(sessionSocket);
	//	}
		while (true) {
			try {
			byte[] header = new byte[23];
				DataInputStream inFromServer = new DataInputStream(
						sessionSocket.getInputStream());
				len = inFromServer.read(header, 0, 23);
				System.out.println("msg received from server " + 
						sessionSocket.getInetAddress().getHostAddress() + " At port " + 
						sessionSocket.getPort());

				if(len == -1) {
					System.out.println("Client has close the socket, exit");
					break;
				}
				msgHandler.handleMsg(header, sessionSocket);
			} catch (SocketException E) {
				System.out.println("Server closed the connection");
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
