import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;

public class SimpellaCommands {
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
//		changed to DataInputStream  and char[] to byte[]
//		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
//				clientSocket.getInputStream()));
//		char[] replyToConnect = new char[25];

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
		if(SimpellaConnectionStatus.outgoingConnectionCount == 1) {
			System.out.println("Sending ping message");
			sendPing(sessionSocket);
		}
		while (true) {
			try {
			byte[] msg = new byte[24];
				DataInputStream inFromServer = new DataInputStream(
						sessionSocket.getInputStream());
				len = inFromServer.read(msg, 0, 23);
				System.out.println("msg received from server");
				if(len == -1) {
					System.out.println("Client has close the socket, exit");
					break;
				}
				SimpellaNetServer.handleMsg(msg, sessionSocket);
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
		String guid = SimpellaRoutingTables.guidToString(pingH.getHeader());
		SimpellaRoutingTables.generatedPingList.add(guid);
		//String s1 = new String(pingH.getHeader());
		System.out.println("Pinged with Header = " + Arrays.toString(pingH.getHeader()));
		
		DataOutputStream outToServer = new DataOutputStream(
				clientSocket.getOutputStream());
		outToServer.write(pingH.getHeader());
		return;
	}

	public void initializeQuery(String searchTxt) throws Exception
	{
		if (searchTxt.getBytes().length <= 231) {
			Header queryH = new Header();
			queryH.initializeHeader();
			queryH.setMsgId();
			queryH.setMsgType("query");
			byte[] queryHeader = queryH.getHeader();
			//TODO set the length of the payload more elegantly :)
			queryHeader[19] = (byte)0x00;
			queryHeader[20] = (byte)0x00;
			queryHeader[21] = (byte)0x00;
			queryHeader[22] = (byte)(searchTxt.getBytes().length + 1); // +1 for \0
			
			// minimum speed, set to 0 for simpella
			byte[] querySpeed = new byte[2];
			querySpeed[0] = 0; //minimum speed, just set it to 0
			querySpeed[1] = 0;

			ByteArrayOutputStream payLoad = new ByteArrayOutputStream();
			payLoad.write(querySpeed);
			payLoad.write((searchTxt + '\0').getBytes()); //make it a null terminated string
			
			String guid = SimpellaRoutingTables.guidToString(queryHeader);
			SimpellaRoutingTables.generatedQueryList.add(guid);
			SimpellaNetServer.broadcastQuery(queryHeader, payLoad.toByteArray(), null);
			
		} else{
			System.out.println("Searchtext out of bound");
		}	
		
		return;
	}

	

	//TODO send_query()
	//TODO send_query_hit()
	//TODO ping() as a wrapper around 
	/**
	 * Find.
	 *
	 * @param searchTxt the search txt
	 
	public void find(String searchTxt){
		byte[] payload = new byte[4096];
		if (searchTxt.getBytes(Charset.forName("UTF-8")).length < 4063) {

			Header queryH = new Header();
			queryH.setHeader(payload);
			queryH.initializeHeader();
			queryH.setMsgType("query");
			queryH.setMsgId();
			// TODO set and validate message and payload
			String s1 = new String(queryH.getHeader());
			System.out.println("Query message with search string = " + s1);
			DataOutputStream outToServer;
			try {
				outToServer = new DataOutputStream(
						clientSocket.getOutputStream());
				outToServer.write(queryH.getHeader());
			} catch (IOException e) {
				System.out.println("Connection error during find");
			}
		} else {
			System.out.println("Invalid searchtext");
		}
		return;
		//TODO write query
	}	
*/

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
