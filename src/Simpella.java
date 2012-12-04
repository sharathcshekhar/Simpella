import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Formatter;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class Simpella {
	
	public static boolean debug = false;
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
	public static int prevSharedFiles = 0;
	public static int prevSharedFilesSize = 0;
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
	public static void main(String[] args){

		SimpellaNetServer netSrv = new SimpellaNetServer();
		SimpellaFileServer fileSrv = new SimpellaFileServer();
		
		SimpellaConnectionStatus.ConnectionStatusInit();
		int netPort = SimpellaConnectionStatus.simpellaNetPort;
		int fileDwPort = SimpellaConnectionStatus.simpellaFileDownloadPort;
		
		try{
		if(args.length == 1) {
			netPort = Integer.parseInt(args[0]);
			SimpellaConnectionStatus.simpellaNetPort = netPort;
			//UUID is a function of IP and port #, so update UUID
			SimpellaConnectionStatus.servent_UUID = SimpellaUtils.generateServentID();
		} else 	if(args.length == 2) {
			netPort = Integer.parseInt(args[0]);
			SimpellaConnectionStatus.simpellaNetPort = netPort;

			fileDwPort = Integer.parseInt(args[1]);
			SimpellaConnectionStatus.simpellaFileDownloadPort = fileDwPort;
		}else{
			System.out.println("Invalid arguments");
			System.exit(0);
		}
		}
		catch(NumberFormatException ne){
			System.out.println("Enter only numbers for port numbers");
			System.exit(0);
		}
		try {
			SimpellaConnectionStatus.checkAndAddIpToGlobalTable(
						InetAddress.getLocalHost().getHostAddress(), netPort);
		} catch (UnknownHostException e2) {
			System.out.println("Unable to connect to host.. exiting");
			System.exit(0);
		}
		netSrv.setPort(netPort);
		netSrv.start();
		fileSrv.setPort(fileDwPort);
		fileSrv.start();
		
		SimpellaClient client = new SimpellaClient();

		BufferedReader cmdFromUser = new BufferedReader(new InputStreamReader(
				System.in));
		// CLI begins
		if(Simpella.debug){
			System.out.println("Local IP : " + LOCAL_IP + "\nSimpella Net Port: " + netPort + "\nDownloading Port: " +
				fileDwPort + "\nsimpella version 0.6 (c) University at Buffalo, 2012");
		}
		while (true) {
			System.out.print("Simpella> ");
			String usrInput = null;
			try {
				if(null==cmdFromUser){
					System.out.println("Simpella forcibly ended.. Thank you for using me");
					System.exit(0);
				}
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
			int portNo;
			if (cmd_args[0].equals("open")) {
				
				if(cmd_args.length != 3 ){
					System.out.println("Invalid input for open command. Sample input 'open <Ip address/Hostname> <port>'");
					continue;
				}
				String ip_afterConversion="";
				String connectionIP = cmd_args[1];
				try {
					if(InetAddress.getByName(connectionIP).isLoopbackAddress()){
						System.out.println("Loopback address not allowed");
						continue;
					}
                    ip_afterConversion = InetAddress.getByName(connectionIP).getHostAddress();
                    if(Simpella.debug){
                    	System.out.println("converted : "+ip_afterConversion+",  Actual ip : "+connectionIP+",   isLoopback: "+InetAddress.getByName(connectionIP).isLoopbackAddress());
                    }
	            } catch (UnknownHostException e1) {
	                   System.out.println("Host not found");
	            }

				portNo = SimpellaIPUtils.StringtoPort(cmd_args[2]);
				if (portNo == -1) {
					continue;
				}
				
				client.setConnectionIP(ip_afterConversion);
				client.setConnectionPort(Integer.parseInt(cmd_args[2]));
				client.connect();

			} else if(cmd_args[0].equals("info")){
				if(cmd_args.length!=2){
					info_help();
					continue;
				} else{
					infoCommand(cmd_args);
				}
				
			} else if(cmd_args[0].equals("update")){
				if(cmd_args.length!=1){
					System.out.println("Invalid arguments for update. Enter only 'update'");
				}else{
					update();
				}
				
			} else if (cmd_args[0].equals("find")) {

				if(cmd_args.length == 1 ){
					System.out.println("find needs arguments. Usage: find 'text to search'");
					continue;
				}
				setFINDFlag();
				String userTxt = usrInput.substring(usrInput
                       .indexOf(" ") + 1);
				find(userTxt);
				//wait until user presses enter
				try {
					cmdFromUser.readLine();
				} catch (IOException e) {
					System.out.println("Error while reading from user");
					continue;
				}
				clearFINDFlag();
				int count = SimpellaConnectionStatus.getQueryhitsReceivedCount();
				SimpellaConnectionStatus.clearQueryhitsReceivedCount();
				for(int i = 0; i < count; i++) {
					SimpellaQueryResults res = SimpellaConnectionStatus.queryResults.get(i);
					System.out.println(i+1 + " " + res.getIpAddress() + ":" + res.getPort()
						+ " \t Size:" + res.getFile_size() + " Bytes\nName: " + res.getFileName());
				}
				
			} else if (cmd_args[0].equals("list")) {
				if(cmd_args.length!=1){
					System.out.println("List command does not take any arguments. Please enter only 'list'");
				}
				else{
					for(int i = 0; i < SimpellaConnectionStatus.queryResults.size(); i++) {
						SimpellaQueryResults res = SimpellaConnectionStatus.queryResults.get(i);
						System.out.println(i+1 + " " + res.getIpAddress() + ":" + res.getPort()
							+ " \t Size:" + res.getFile_size() + " Bytes\nName: " + res.getFileName());
					}
				}
				
				
			} else if (cmd_args[0].equals("clear")) {
				if(cmd_args.length>2){
					System.out.println("Invalid arguments Clear command. Usage: clear 'file_number'");
					continue;
				} else if(cmd_args.length == 2) {
						int clear_index = Integer.parseInt(cmd_args[1]);
						SimpellaConnectionStatus.clearQueryResultsTable(clear_index - 1);
				} else { //len == 1, clear all
						SimpellaConnectionStatus.clearQueryResultsTable();
				}	
				
			} else if (cmd_args[0].equals("download")) {
				System.out.println("download command");
				if(cmd_args.length!=2){
					System.out.println("Invalid arguments for download");
					continue;
				} else {	
				SimpellaFileClient fileDw = new SimpellaFileClient();
					try {
						fileDw.downloadFile(Integer.parseInt(cmd_args[1]));
					} catch (NumberFormatException ne) {
						System.out.println("Only number allowed in download command");
						continue;
					} catch (Exception e){
						System.out.println("Error during file download");
						continue;
					}
				}
			
			} else if (cmd_args[0].equals("share")) {
				if(cmd_args.length > 1 && cmd_args[1].equals("-i")) {
					if(SimpellaFileShareDB.sharedDirectory == null) {
						System.out.println("No files shared in the system. Use share /path/to/shared/folder");
					} else {
						System.out.println("sharing directory " + SimpellaFileShareDB.sharedDirectory);
					}
				} else {
					String sharedDirectory = usrInput.substring(usrInput
							.indexOf(" ") + 1);
					System.out.println("sharing directory " + sharedDirectory);
					File share = new File(sharedDirectory);
					if (!share.exists() || !share.isDirectory()) {
						System.out.println("Invalid directory name");
						continue;
					}
					SimpellaFileShareDB.setSharedDirectory(sharedDirectory);
				}
			} else if (cmd_args[0].equals("scan")) {
				if(cmd_args.length!=1){
					System.out.println("Invalid arguments for scan");
					continue;
				}
				if(SimpellaFileShareDB.sharedDirectory == null) {
					System.out.println("No files shared in the system. Use share /path/to/shared/folder");
					continue;
				}
				System.out.println("scanning " + SimpellaFileShareDB.sharedDirectory + " for files...");
				SimpellaFileShareDB fileDb = new SimpellaFileShareDB();
				fileDb.scanSharedDirectory();
				System.out.println("Scanned " + fileDb.getNoOfFiles() + " files and = " 
						+ fileDb.getSizeOfFiles() + " bytes");
				
			} else if (cmd_args[0].equals("monitor")) {
				if(cmd_args.length!=1){
					System.out.println("Invalid arguments for monitor");
					continue;
				}
				System.out.println("MONITORING SIMPELLA NETWORK\n" + "Press enter to continue\n" 
						+ "----------------------------");
				setMONITORFlag();
				try {
					cmdFromUser.readLine();
				} catch (IOException e) {
					System.out.println("Error while reading from user");
					continue;
				}
				clearMONITORFlag();
				
			} else if (cmd_args[0].equals("quit")) {
				if(cmd_args.length!=1){
					System.out.println("Only type 'quit' without arguments to quit from Simpella");
					continue;
				}
				System.out.println("quit command");
				System.out.println("Thank you for using Simpella. See you again");
				//TODO close all sockets
				System.exit(0);
			} else if (cmd_args[0].equals("debug")) {//for developer usage
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
		String msgID = SimpellaRoutingTables.guidToString(pingPacket);
		SimpellaRoutingTables.generatedPingList.add(msgID);
		msgHandler.broadcastPing(pingPacket, null);
	}


	private static void infoCommand(String[] cmd) {
		if (cmd[1].equalsIgnoreCase("h")) {
			SimpellaFileShareDB fd = new SimpellaFileShareDB();
			System.out.println("Scanning local files for changes and set total files of Simpella system accordingly");
			fd.scanSharedDirectory();
			SimpellaConnectionStatus.setLocalFilesShared(fd.getNoOfFiles());
			SimpellaConnectionStatus.setLocalFilesSharedSize(fd.getSizeOfFiles());
			Formatter info_fmt = new Formatter();
			info_fmt.format("%-15s %-24s %-10s\n", "Hosts", "Files", "Size");
			info_fmt.format("%-15d %-24s %-24s", SimpellaConnectionStatus
					.getTotalHosts(), SimpellaUtils
					.memFormat(SimpellaConnectionStatus.getTotalFiles()),
					SimpellaUtils.memFormat(SimpellaConnectionStatus
							.getTotalFilesSize()));
			System.out.println(info_fmt);
		} else if (cmd[1].equalsIgnoreCase("c")) {
			for (int i = 0; i < SimpellaConnectionStatus.incomingConnectionCount; i++) {
				Formatter info_fmt = new Formatter();
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
			System.out.println("Queries Received: "
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
			info_help();
		}
	}

	public static void info_help() {
		System.out.println("Invalid value for info. Usage: \n" +
				"info [cdhnqs] - Display list of current connections.\n" +
				"c - Simpella network connections\n" +
				"d - file transfer in progress (downloads only)\n" +
				"h - number of hosts, number of files they are sharing, and total size of those shared files\n" +
				"n - Simpella statistics: packets received and sent, " +
				
					"number of unique packet IDs in memory\n(routing tables)," + 
					"total Simpella bytes received and sent so far.\n" +
				"q - queries received and replies sent\n" +
				"s - number and total size of shared files on this host\n" );
	}

	public static void find(String searchTxt)

	{
		if (searchTxt.getBytes().length < 254) {
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
			try {
				payLoad.write(querySpeed);
				payLoad.write((searchTxt + '\0').getBytes()); //make it a null terminated string
			} catch (IOException e) {
				System.out.println("Error while sending query");
			}
			if(Simpella.debug) {
				System.out.println("In initialize query. Writing a payLoad of " + payLoad.size());
			}
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