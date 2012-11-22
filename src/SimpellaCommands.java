import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

public class SimpellaCommands {
	int connectionPort = 0;
	String connectionIP = "";
	
	public int connect() throws Exception {
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
				SimpellaConnectionStatus.addOutgoingConnection(connectionIP,
						connectionPort);
				System.out
						.println("Connection successfully accepted, no. of connections = "
								+ SimpellaConnectionStatus.outgoingConnectionCount);
				ret = 0;
				// TODO send Ping if its the first connection i.e.,
				// if(outgoingConnectionCount == 1)
			} else if (S.startsWith("SIMPELLA/0.6 503")) {
				System.out.println("Connection failed: " + S);
				ret = 1;
			} else {
				System.out.println("Unknown error: " + S);
				ret = 1;
			}
		} catch (SocketException E) {
			System.out.println("Server closed the connection");
			return ret;
		}
			
		clientSocket.close();
		
		return ret;
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
