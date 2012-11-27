import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpellaRoutingTables {
	public static Hashtable <String, Socket> PingTable = new Hashtable<String, Socket>();
	public static Hashtable <String, Socket> QueryTable = new Hashtable<String, Socket>();
	public static ConcurrentLinkedQueue<String> PingTableQueue = new ConcurrentLinkedQueue<String>();
	public static ConcurrentLinkedQueue<String> QueryTableQueue = new ConcurrentLinkedQueue<String>();
	public static ConcurrentLinkedQueue<String> generatedPingList = new ConcurrentLinkedQueue<String>();
	
	public static String guidToString(byte[] guid) {
		String tmp = "";
		if(guid.length < 16){
			return null;
		}
		for (int i = 0; i < 16; i++) {
			tmp = tmp + guid[i];
		}
		return tmp;
	}
	
	public static void insertPingTable(String key, Socket clientSocket){
		if(PingTable.size() < 160) {
			PingTable.put(key, clientSocket);
			PingTableQueue.add(key);
		} else {
			String keyToRemove = PingTableQueue.remove();
			PingTable.remove(keyToRemove);
			PingTable.put(key, clientSocket);
			PingTableQueue.add(key);
		}
	}
	
	public static void insertQueryTable(String key, Socket clientSocket){
		if(QueryTable.size() < 160) {
			QueryTable.put(key, clientSocket);
			QueryTableQueue.add(key);
		} else {
			String keyToRemove = QueryTableQueue.remove();
			QueryTable.remove(keyToRemove);
			QueryTable.put(key, clientSocket);
			QueryTableQueue.add(key);
		}
	}
}
