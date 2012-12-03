import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Formatter;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class Simpella {
	
	public static boolean debug = true;
	public static final String LOCAL_IP = SimpellaIPUtils.getLocalIPAddress().getHostAddress();
	/*
	 * Global Flags
	 * FIND_flag - is set when find command is running,
	 * cleared when user presses enter
	 * MONITOR_flag - is set when monitor command is running
	 * cleared when user presses enter
	 */
	private static boolean FIND_flag = false;
	private static boolean MONITOR_flag = false;
	public static boolean printDwnload = false;
	
	public static synchronized void setFINDFlag() {
		FIND_flag = true;
	}
	public static synchronized void clearFINDFlag() {
		FIND_flag = false;
	}
	public static synchronized void setMONITORFlag() {
		MONITOR_flag = true;
	}
	public static synchronized void clearMONITORFlag() {
		MONITOR_flag = false;
	}
	public static boolean is_FINDActive(){
		return FIND_flag;
	}
	public static boolean is_MONITORActive(){
		return MONITOR_flag;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		SimpellaNetServer netSrv = new SimpellaNetServer();
		SimpellaFileServer fileSrv = new SimpellaFileServer();
		
		SimpellaConnectionStatus.ConnectionStatusInit();
		int netPort = SimpellaConnectionStatus.simpellaNetPort;
		int fileDwPort = SimpellaConnectionStatus.simpellaFileDownloadPort;
		
		if(args.length == 1) {
			netPort = Integer.parseInt(args[0]);
			SimpellaConnectionStatus.simpellaNetPort = netPort;
		} else 	if(args.length == 2) {
			netPort = Integer.parseInt(args[0]);
			SimpellaConnectionStatus.simpellaNetPort = netPort;

			fileDwPort = Integer.parseInt(args[1]);
			SimpellaConnectionStatus.simpellaFileDownloadPort = fileDwPort;
		}
		SimpellaConnectionStatus.checkAndAddIpToGlobalTable(SimpellaIPUtils.getLocalIPAddress().getHostAddress(),
				SimpellaConnectionStatus.simpellaNetPort);
		//TODO take second argument to be file server
		netSrv.setPort(netPort);
		netSrv.start();
		fileSrv.setPort(fileDwPort);
		fileSrv.start();
		
		SimpellaClient client = new SimpellaClient();

		BufferedReader cmdFromUser = new BufferedReader(new InputStreamReader(
				System.in));
		// CLI begins
		System.out.println("Local IP : " + LOCAL_IP + "\nSimpella Net Port: " + netPort + "\nDownloading Port: " +
				fileDwPort + "\nsimpella version 0.6 (c) University at Buffalo, 2012");
		while (true) {
			System.out.print("Simpella> ");
			String usrInput = null;
			try {
				// trim() deletes leading and trailing whitespace
				usrInput = cmdFromUser.readLine().trim();
			} catch (IOException e) {
				System.out.println("Cannot parse command, please try again");
				continue;
			}

			if (usrInput.length() == 0) {
				continue;
			}
			// This regex ignores whitespace between words
			String[] cmd_args = usrInput.split("\\s+");

			if (cmd_args[0].equals("open")) {
				System.out.println("open command");
				client.setConnectionIP(cmd_args[1]);
				client.setConnectionPort(Integer.parseInt(cmd_args[2]));
				client.connect();

			} else if(cmd_args[0].equals("info")){
				System.out.println("info command");
				if(cmd_args.length!=2){
					System.out.println("Invalid arguments");
					continue;
				} else{
					infoCommand(cmd_args);
				}
				
			} else if(cmd_args[0].equals("update")){
				System.out.println("update command");
				update();
				//TODO Establish new connections if the present
				//outgoing connections in < 2
				
			} else if (cmd_args[0].equals("find")) {
				System.out.println("find command");
				setFINDFlag();
				find(cmd_args[1]);
				//wait until user presses enter
				cmdFromUser.readLine();
				clearFINDFlag();
				int count = SimpellaConnectionStatus.getQueryhitsReceivedCount();
				SimpellaConnectionStatus.clearQueryhitsReceivedCount();
				for(int i = 0; i < count; i++) {
					SimpellaQueryResults res = SimpellaConnectionStatus.queryResults.get(i);
					System.out.println(i+1 + " " + res.getIpAddress() + ":" + res.getPort()
						+ " \t Size:" + res.getFile_size() + " Bytes\nName: " + res.getFileName());
				}
				
			} else if (cmd_args[0].equals("list")) {
				System.out.println("list command");
				for(int i = 0; i < SimpellaConnectionStatus.queryResults.size(); i++) {
					SimpellaQueryResults res = SimpellaConnectionStatus.queryResults.get(i);
					System.out.println(i+1 + " " + res.getIpAddress() + ":" + res.getPort()
						+ " \t Size:" + res.getFile_size() + " Bytes\nName: " + res.getFileName());
				}
				
				
			} else if (cmd_args[0].equals("clear")) {
				System.out.println("clear command");
				if(cmd_args.length == 2) {
					int clear_index = Integer.parseInt(cmd_args[1]);
					SimpellaConnectionStatus.clearQueryResultsTable(clear_index - 1);
				} else {
					SimpellaConnectionStatus.clearQueryResultsTable();
				}			
			} else if (cmd_args[0].equals("download")) {
				System.out.println("download command");
				SimpellaFileClient fileDw = new SimpellaFileClient();
				fileDw.downloadFile(Integer.parseInt(cmd_args[1]));
			
			} else if (cmd_args[0].equals("share")) {
				if(cmd_args.length > 1 && cmd_args[1].equals("-i")) {
					System.out.println("sharing directory " + SimpellaFileShareDB.sharedDirectory);
				} else {
					String sharedDirectory = usrInput.substring(usrInput
							.indexOf(" ") + 1);
					System.out.println("sharing directory " + sharedDirectory);
					File share = new File(sharedDirectory);
					if (!share.exists()) {
						System.out.println("Invalid file name");
						continue;
					}
					SimpellaFileShareDB.setSharedDirectory(sharedDirectory);
				}
			} else if (cmd_args[0].equals("scan")) {
				System.out.println("scanning " + SimpellaFileShareDB.sharedDirectory + " for files...");
				SimpellaFileShareDB fileDb = new SimpellaFileShareDB();
				fileDb.scanSharedDirectory();
				System.out.println("Scanned " + fileDb.getNoOfFiles() + " files and = " 
						+ fileDb.getSizeOfFiles() + " bytes");
				
			} else if (cmd_args[0].equals("monitor")) {
				System.out.println("MONITORING SIMPELLA NETWORK\n" + "Press enter to continue\n" 
						+ "----------------------------");
				setMONITORFlag();
				cmdFromUser.readLine();
				clearMONITORFlag();
				
			} else if (cmd_args[0].equals("quit")) {
				System.out.println("quit command");
				//TODO close all sockets
				System.exit(0);
			} else if (cmd_args[0].equals("debug")) {
				if(debug == true) {
					System.out.println("Switching off debug mode");
					debug = false;
				} else {
					System.out.println("Setting debug mode");
					debug = true;
				}
			} else {
				System.out.println("Invalid Command!");
			}
		}
	}
	

	private static void update() {
		SimpellaHeader pingH = new SimpellaHeader();
		pingH.initializeHeader();
		pingH.setMsgId();
		byte[] pingPacket = pingH.getHeader();
		SimpellaHandleMsg msgHandler = new SimpellaHandleMsg();
		System.out.println("Sending Ping broadcast packets to all known connections");
		try {
			msgHandler.broadcastPing(pingPacket, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private static void infoCommand(String[] cmd) {
		if (cmd[1].equalsIgnoreCase("h")) {
			Formatter info_fmt = new Formatter();
			info_fmt.format("%-15s %-24s %-10s\n", "Hosts", "Files", "Size");
			info_fmt.format("%-15d %-24s %-24s", SimpellaConnectionStatus
					.getTotalHosts(), SimpellaUtils
					.memFormat(SimpellaConnectionStatus.getTotalFiles()),
					SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalFilesSize()));
			System.out.println(info_fmt);
		} else if (cmd[1].equalsIgnoreCase("c")) {
			Formatter info_fmt = new Formatter();
			for (int i = 0; i < SimpellaConnectionStatus.incomingConnectionCount; i++) {
				info_fmt.format(
						"%-30s %-30s %-30s",
						SimpellaConnectionStatus.incomingConnectionList[i]
								.getRemoteIP()
								+ ":"
								+ SimpellaConnectionStatus.incomingConnectionList[i]
										.getRemotePort(),
						"Packs:"
								+ SimpellaConnectionStatus.incomingConnectionList[i]
										.getSentPacks()
								+ ":"
								+ SimpellaConnectionStatus.incomingConnectionList[i]
										.getRecvdPacks(),
						"Bytes:"
								+ SimpellaUtils
										.memFormat(SimpellaConnectionStatus.incomingConnectionList[i]
												.getSentBytes())
								+ ":"
								+ SimpellaUtils
										.memFormat(SimpellaConnectionStatus.incomingConnectionList[i]
												.getRecvdBytes()));
				System.out.println(info_fmt);
			}
			for (int i = 0; i < SimpellaConnectionStatus.outgoingConnectionCount; i++) {
				Formatter info_fmt_2 = new Formatter();
				info_fmt_2.format(
						"%-30s %-30s %-30s",
						SimpellaConnectionStatus.outgoingConnectionList[i]
								.getRemoteIP()
								+ ":"
								+ SimpellaConnectionStatus.outgoingConnectionList[i]
										.getRemotePort(),
						"Packs:"
								+ SimpellaConnectionStatus.outgoingConnectionList[i]
										.getSentPacks()
								+ ":"
								+ SimpellaConnectionStatus.outgoingConnectionList[i]
										.getRecvdPacks(),
						"Bytes:"
								+ SimpellaUtils
										.memFormat(SimpellaConnectionStatus.outgoingConnectionList[i]
												.getSentBytes())
								+ ":"
								+ SimpellaUtils
										.memFormat(SimpellaConnectionStatus.outgoingConnectionList[i]
												.getRecvdBytes()));
				System.out.println(info_fmt_2);
			}

		} else if (cmd[1].equalsIgnoreCase("n")) {
			System.out.println("NET STATUS:");
			System.out.println("Msg Received: "
					+ SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalPacketsRecvd())
					+ "    "
					+ "Msg Sent: "
					+ SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalPacketsSent()));
			System.out.println("Unique GUIds in memory: "
					+ SimpellaConnectionStatus.getTotalUniqueGUIds());
			System.out.println("Bytes Received: "
					+ SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalBytesRecvd())
					+ "    "
					+ "Bytes Sent: "
					+ SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalBytesSent()));
		} else if (cmd[1].equalsIgnoreCase("d")) {
			System.out.println("DOWNLOAD STATS");
			printDwnload=true;
		} else if (cmd[1].equalsIgnoreCase("q")) {
			System.out.println("Queries: "
					+ SimpellaConnectionStatus.getQueriesRecvd() + "   "
					+ "Responses sent: "
					+ SimpellaConnectionStatus.getResponsesSent());

		} else if (cmd[1].equalsIgnoreCase("s")) {
			SimpellaFileShareDB fileShareDb = new SimpellaFileShareDB();
			fileShareDb.scanSharedDirectory();
			SimpellaConnectionStatus.setLocalFilesShared(fileShareDb
					.getNoOfFiles());
			SimpellaConnectionStatus.setLocalFilesSharedSize(fileShareDb
					.getSizeOfFiles());
			System.out.println("Num Shared: "
					+ SimpellaConnectionStatus.getLocalFilesShared()
					+ "   "
					+ "Size Shared: "
					+ SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getLocalFilesSharedSize()));
		} else {
			System.out.println("invalid info value");
		}
	}


	public static void find(String searchTxt) throws Exception

	{
		if (searchTxt.getBytes().length <= 231) {
			SimpellaHeader queryH = new SimpellaHeader();
			queryH.initializeHeader();
			queryH.setMsgId();
			queryH.setMsgType("query");
			queryH.setPayLoadLength(2 + searchTxt.getBytes().length + 1); // +2 for speed +1 for \0
						
			byte[] queryHeader = queryH.getHeader();
			
			// minimum speed, set to 0 for simpella
			byte[] querySpeed = new byte[2];
			querySpeed[0] = 0; //minimum speed, just set it to 0
			querySpeed[1] = 0;

			ByteArrayOutputStream payLoad = new ByteArrayOutputStream();
			payLoad.write(querySpeed);
			payLoad.write((searchTxt + '\0').getBytes()); //make it a null terminated string
			System.out.println("In initialize query. Writing a payLoad of " + payLoad.size());
			String guid = SimpellaRoutingTables.guidToString(queryHeader);
			SimpellaRoutingTables.generatedQueryList.add(guid);
			
			SimpellaHandleMsg msgHandler = new SimpellaHandleMsg();
			msgHandler.broadcastQuery(queryHeader, payLoad.toByteArray(), null);
			
		} else{
			System.out.println("Searchtext out of bound");
		}	
		
		return;
	}
}