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
			if (incomingConnectionList[i].remoteIP.equals(inComingIP) && 
					incomingConnectionList[i].remotePort == port) {
					return true;
				}
		}
		return false;
	}
	
	public static void addIncomingConnection(String IP, int portNo) {
		if(incomingConnectionCount == 3){
			return;
		} else {
			for(int i = 0; i < 3; i++) {
				if(incomingConnectionList[i].remoteIP.equals("")) {
					incomingConnectionList[i].remoteIP = IP;
					incomingConnectionList[i].remotePort = portNo;
					incomingConnectionCount++;
					return;
				}
			}
		}
	}
	
	public static void delIncomingConnection(String IP, int portNo) {
		for(int i = 0; i < 3; i++) {
			if(incomingConnectionList[i].remoteIP.equals(IP)) {
				incomingConnectionList[i].remoteIP = "";
				incomingConnectionList[i].remotePort = 0;
				incomingConnectionCount--;
				return;
			}
		}
	}
	
	//TODO check for only IP. Checking both IP and port # for testing purpose only	
	public static boolean isOutConnectionPresent(String inComingIP, int port) {
		// TODO Auto-generated method stub
		for(int i = 0; i < 3; i++) {
			if(outgoingConnectionList[i].remoteIP.equals(inComingIP) &&
					outgoingConnectionList[i].remotePort == port) {
				return true;
			}
		}
		return false;
	}
	public static void addOutgoingConnection(String IP, int portNo) {
		if(outgoingConnectionCount == 3){
			return;
		} else {
			for(int i = 0; i < 3; i++) {
				if(outgoingConnectionList[i].remoteIP.equals("")) {
					outgoingConnectionList[i].remoteIP = IP;
					outgoingConnectionList[i].remotePort = portNo;
					outgoingConnectionCount++;
					return;
				}
			}
		}
	}
	public static void delOutgoingConnection(String IP, int portNo) {
		for(int i = 0; i < 3; i++) {
			if(outgoingConnectionList[i].remoteIP.equals(IP)) {
				outgoingConnectionList[i].remoteIP = "";
				outgoingConnectionList[i].remotePort = 0;
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
	}
	int connectionId; //not used for now
	String remoteIP;
	int remotePort;
}

class OutgoingConnectionTable {
	public OutgoingConnectionTable() {
		super();
		this.connectionId = 0; //not used for now
		this.remoteIP = "";
		this.remotePort = 0;
	}
	int connectionId;
	String remoteIP;
	int remotePort;
}
