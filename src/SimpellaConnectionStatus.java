import java.net.Socket;
import java.util.Hashtable;
import java.util.Vector;

public class SimpellaConnectionStatus {
	public static int simpellaNetPort = 0;
	public static int simpellaFileDownloadPort = 0;
	public static int incomingConnectionCount = 0;
	public static int outgoingConnectionCount = 0;
	public static SimpellaStats[] incomingConnectionList = 
			new SimpellaStats[3];
	public static SimpellaStats[] outgoingConnectionList = 
			new SimpellaStats[3];
	public static Hashtable<String,Integer> globalIpTable = new Hashtable<String,Integer>();
	
	private static int totalFiles = 0;
	private static int totalFilesSize = 0;
	private static int localFilesShared = 0;
	private static int localFilesSharedSize = 0;
	private static int totalHosts = 0;
	private static int totalUniqueGUIds = 0;
	private static int totalPacketsSent = 0;
	private static int totalPacketsRecvd = 0;
	private static int totalBytesSent = 0;
	private static int totalBytesRecvd = 0;
	private static int queriesRecvd = 0;
	private static int responsesSent = 0;


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

	public static int getLocalFilesShared() {
		return localFilesShared;
	}

	public static void setLocalFilesShared(int localFilesShared) {
		SimpellaConnectionStatus.localFilesShared = localFilesShared;
	}

	public static int getLocalFilesSharedSize() {
		return localFilesSharedSize;
	}

	public static void setLocalFilesSharedSize(int localFilesSharedSize) {
		SimpellaConnectionStatus.localFilesSharedSize = localFilesSharedSize;
	}

	public static int getTotalFiles() {
		return totalFiles;
	}

	public static void setTotalFiles(int totalFiles) {
		SimpellaConnectionStatus.totalFiles= totalFiles;
	}

	public static int getTotalFilesSize() {
		return totalFilesSize;
	}

	public static void setTotalFilesSize(int totalFilesSize) {
		SimpellaConnectionStatus.totalFilesSize= totalFilesSize;
	}

	public static int getQueriesRecvd() {
		return queriesRecvd;
	}

	public static void setQueriesRecvd() {
		SimpellaConnectionStatus.queriesRecvd++;
	}

	public static int getResponsesSent() {
		return responsesSent;
	}

	public static void setResponsesSent() {
		SimpellaConnectionStatus.responsesSent++;
	}

	public static int getTotalBytesSent() {
		return totalBytesSent;
	}

	public static void setTotalBytesSent(int totalBytesSent) {
		SimpellaConnectionStatus.totalBytesSent+= totalBytesSent;
	}

	public static int getTotalBytesRecvd() {
		return totalBytesRecvd;
	}

	public static void setTotalBytesRecvd(int totalBytesRecvd) {
		SimpellaConnectionStatus.totalBytesRecvd+= totalBytesRecvd;
	}
	
	public static int getTotalPacketsSent() {
		return totalPacketsSent;
	}

	public static void setTotalPacketsSent() {
		SimpellaConnectionStatus.totalPacketsSent++;
	}

	public static int getTotalPacketsRecvd() {
		return totalPacketsRecvd;
	}

	public static void setTotalPacketsRecvd() {
		SimpellaConnectionStatus.totalPacketsRecvd++;
	}
	
	public static int getTotalUniqueGUIds() {
		return totalUniqueGUIds;
	}

	public static void setTotalUniqueGUIds(int totalUniqueGUIds) {
		SimpellaConnectionStatus.totalUniqueGUIds = totalUniqueGUIds;
	}

	public static int getTotalHosts() {
		return totalHosts;
	}

	public static void ConnectionStatusInit() {
		for (int i = 0; i < 3; i++) {
			incomingConnectionList[i] = new SimpellaStats();
		}
		for (int j = 0; j < 3; j++) {
			outgoingConnectionList[j] = new SimpellaStats();
		}
		incomingConnectionCount = 0;
		outgoingConnectionCount = 0;
		simpellaNetPort = 6346; //default port
		simpellaFileDownloadPort = 5635; //default port
		}

	public static void checkAndAddIpToGlobalTable(String ip, int port){
		if(!globalIpTable.containsKey(ip))
		{
			globalIpTable.put(ip, Integer.valueOf(port));
			totalHosts++;
		}
		else if(globalIpTable.containsKey(ip) && globalIpTable.get(ip)!=Integer.valueOf(port)){
			globalIpTable.put(ip, Integer.valueOf(port));
			totalHosts++;	
		}
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
	
	public static SimpellaStats getBySocket(Socket clientSocket){
		for(int i=0;i<3;i++){
			if(outgoingConnectionList[i].remoteIP.equals(clientSocket.getInetAddress().getHostAddress()) &&
					outgoingConnectionList[i].remotePort==clientSocket.getPort()){
				return outgoingConnectionList[i];
			}
		}
		
		for(int i=0;i<3;i++){
			if(incomingConnectionList[i].remoteIP.equals(clientSocket.getInetAddress().getHostAddress()) &&
					incomingConnectionList[i].remotePort==clientSocket.getPort()){
				return incomingConnectionList[i];
			}
		}
		return null;
	}
}
