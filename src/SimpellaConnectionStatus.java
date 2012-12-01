import java.net.Socket;
import java.util.Vector;

public class SimpellaConnectionStatus {
	public static int simpellaNetPort = 0;
	public static int simpellaFileDownloadPort = 0;
	public static int incomingConnectionCount = 0;
	public static int outgoingConnectionCount = 0;
	public static IncomingConnectionTable[] incomingConnectionList = 
			new IncomingConnectionTable[3];
	public static OutgoingConnectionTable[] outgoingConnectionList = 
			new OutgoingConnectionTable[3];
	
//	public static Hashtable<Integer, SimpellaQueryResults> queryResults = 
//	new Hashtable<Integer, SimpellaQueryResults>();
//	static int nextInsPosQueryRes = 0;

	/*
	 *  noOfQueryhitsRecived received per find command
	 *  will be reset as soon as user presses enter in find command
	 *  if 3 files are received in a single query hit command, the 
	 *  count increases by 3
	 */
	
	private static int queryhitsReceivedCount = 0;
	
	public static synchronized void addToQueryhitsReceivedCount(int no_of_files) {
		queryhitsReceivedCount += no_of_files;
	}
	
	public static synchronized void clearQueryhitsReceivedCount() {
		queryhitsReceivedCount = 0;
	}
	
	public static int getQueryhitsReceivedCount () {
		return queryhitsReceivedCount;
	}
	
	public static Vector<SimpellaQueryResults> queryResults = new  Vector<SimpellaQueryResults>();
	
	public static void insertToQueryResultsTable(SimpellaQueryResults results) {
		if(queryResults.size() < 50) {
			queryResults.add(0, results);
		} else {
			queryResults.remove(50);
			queryResults.add(0, results);
		}
	}
	
	public static void clearQueryResultsTable() {
		if(!queryResults.isEmpty()) {
			queryResults.clear();
		}
	}
	
	public static void ConnectionStatusInit() {
		for (int i = 0; i < 3; i++) {
			incomingConnectionList[i] = new IncomingConnectionTable();
		}
		for (int j = 0; j < 3; j++) {
			outgoingConnectionList[j] = new OutgoingConnectionTable();
		}
		incomingConnectionCount = 0;
		outgoingConnectionCount = 0;
		simpellaNetPort = 6346; //default port
		simpellaFileDownloadPort = 5635; //default port
		}

	// TODO check for only IP. Checking both IP and port # for testing purpose
	// only
	public static boolean isInConnectionPresent(String inComingIP, int port) {
		for (int i = 0; i < 3; i++) {
			if (incomingConnectionList[i].remoteIP.equals(inComingIP) // {
					&& incomingConnectionList[i].remotePort == port) {
				return true;
			}
		}
		return false;
	}

	public static void addIncomingConnection(Socket clientSocket) {
		if (incomingConnectionCount == 3) {
			return;
		} else {
			for (int i = 0; i < 3; i++) {
				if (incomingConnectionList[i].sessionSocket == null) {
					incomingConnectionList[i].sessionSocket = clientSocket;
					incomingConnectionList[i].remoteIP = clientSocket
							.getInetAddress().getHostAddress();
					incomingConnectionList[i].remotePort = clientSocket
							.getPort();
					incomingConnectionCount++;
					System.out
							.println("Added connection to incominConnectionList");
					return;
				}
			}
		}
	}

	public static void delIncomingConnection(Socket clientSocket) {
		for (int i = 0; i < 3; i++) {
			if (incomingConnectionList[i].sessionSocket.equals(clientSocket)) {
				incomingConnectionList[i].remoteIP = "";
				incomingConnectionList[i].remotePort = 0;
				incomingConnectionList[i].sessionSocket = null;
				incomingConnectionCount--;
				System.out
						.println("Added connection to outgoingConnectionList");
				return;
			}
		}
	}

	// TODO check for only IP. Checking both IP and port # for testing purpose
	// only
	public static boolean isOutConnectionPresent(String inComingIP, int port) {
		// TODO Auto-generated method stub
		for (int i = 0; i < 3; i++) {
			if (outgoingConnectionList[i].remoteIP.equals(inComingIP)
					&& outgoingConnectionList[i].remotePort == port) {
				return true;
			}
		}
		return false;
	}

	public static void addOutgoingConnection(Socket clientSocket) {
		if (outgoingConnectionCount == 3) {
			return;// TODO send Ping if its the first connection i.e.,
		} else {
			for (int i = 0; i < 3; i++) {
				if (outgoingConnectionList[i].sessionSocket == null) {
					outgoingConnectionList[i].sessionSocket = clientSocket;
					outgoingConnectionList[i].remoteIP = clientSocket
							.getInetAddress().getHostAddress();
					outgoingConnectionList[i].remotePort = clientSocket
							.getPort();
					outgoingConnectionCount++;
					System.out
							.println("Added connection to outgoingConnectionList");
					return;
				}
			}
		}
	}

	public static void delOutgoingConnection(Socket clientSocket) {
		for (int i = 0; i < 3; i++) {
			if (outgoingConnectionList[i].sessionSocket.equals(clientSocket)) {
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

	int connectionId; // not used for now
	String remoteIP;
	int remotePort;
	Socket sessionSocket;
}

class OutgoingConnectionTable {
	public OutgoingConnectionTable() {
		super();
		this.connectionId = 0; // not used for now
		this.remoteIP = "";
		this.remotePort = 0;
		this.sessionSocket = null;
	}

	int connectionId;
	String remoteIP;
	int remotePort;
	Socket sessionSocket;
}
