import java.net.Socket;

public class SimpellaConnectionStatus {
	public static int incomingConnectionCount = 0;
	public static int outgoingConnectionCount = 0;
	public static IncomingConnectionTable[] incomingConnectionList = new IncomingConnectionTable[3];
	public static OutgoingConnectionTable[] outgoingConnectionList = new OutgoingConnectionTable[3];

	public static void ConnectionStatusInit() {
		for(int i = 0; i < 3; i++) {
			incomingConnectionList[i] = new IncomingConnectionTable();
		}
		for(int j = 0; j < 3; j++) {
			outgoingConnectionList[j] = new OutgoingConnectionTable();
		}
		incomingConnectionCount = 0;
		outgoingConnectionCount = 0;
	}
//TODO check for only IP. Checking both IP and port # for testing purpose only	
	public static boolean isInConnectionPresent(String inComingIP, int port) {
		for(int i = 0; i < 3; i++) {
			if (incomingConnectionList[i].remoteIP.equals(inComingIP) //{
					&& incomingConnectionList[i].remotePort == port) {
					return true;
				}
		}
		return false;
	}
	
	public static void addIncomingConnection(Socket clientSocket) {
		if(incomingConnectionCount == 3){
			return;
		} else {
			for(int i = 0; i < 3; i++) {
				if(incomingConnectionList[i].sessionSocket == null) {
					incomingConnectionList[i].sessionSocket = clientSocket;
					incomingConnectionList[i].remoteIP = clientSocket.getInetAddress().getHostAddress();
					incomingConnectionList[i].remotePort = clientSocket.getPort();
					incomingConnectionCount++;
					return;
				}
			}
		}
	}
	
	public static void delIncomingConnection(Socket clientSocket) {
		for(int i = 0; i < 3; i++) {
			if(incomingConnectionList[i].sessionSocket.equals(clientSocket)) {
				incomingConnectionList[i].remoteIP = "";
				incomingConnectionList[i].remotePort = 0;
				incomingConnectionList[i].sessionSocket = null;
				incomingConnectionCount--;
				return;
			}
		}
	}
	
	//TODO check for only IP. Checking both IP and port # for testing purpose only	
	public static boolean isOutConnectionPresent(String inComingIP, int port) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 3; i++) {
			if(outgoingConnectionList[i].remoteIP.equals(inComingIP) 
					&& outgoingConnectionList[i].remotePort == port) {
				return true;
			}
		}
		return false;
	}
	public static void addOutgoingConnection(Socket clientSocket) {
		if(outgoingConnectionCount == 3){
			return;// TODO send Ping if its the first connection i.e.,
		} else {
			for(int i = 0; i < 3; i++) {
				if(outgoingConnectionList[i].sessionSocket == null) {
					outgoingConnectionList[i].sessionSocket = clientSocket;
					outgoingConnectionList[i].remoteIP = clientSocket.getInetAddress().getHostAddress();
					outgoingConnectionList[i].remotePort = clientSocket.getPort();
					outgoingConnectionCount++;
					return;
				}
			}
		}
	}
	public static void delOutgoingConnection(Socket clientSocket) {
		for(int i = 0; i < 3; i++) {
			if(outgoingConnectionList[i].sessionSocket.equals(clientSocket)) {
				outgoingConnectionList[i].remoteIP = "";
				outgoingConnectionList[i].remotePort = 0;
				outgoingConnectionList[i].sessionSocket = null;
				outgoingConnectionCount--;
				return;
			}
		}
		
	}

}
class IncomingConnectionTable {
	public IncomingConnectionTable() {
		super();
		this.connectionId = 0;
		this.remoteIP = "";
		this.remotePort = 0;
		this.sessionSocket = null;
	}
	int connectionId; //not used for now
	String remoteIP;
	int remotePort;
	Socket sessionSocket;
}

class OutgoingConnectionTable {
	public OutgoingConnectionTable() {
		super();
		this.connectionId = 0; //not used for now
		this.remoteIP = "";
		this.remotePort = 0;
		this.sessionSocket = null;
	}
	int connectionId;
	String remoteIP;
	int remotePort;
	Socket sessionSocket;
}
