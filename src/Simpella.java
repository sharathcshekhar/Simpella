import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class Simpella {
	
	public static final boolean debug = true;
	/*
	 * Global Flags
	 * FIND_flag - is set when find command is running,
	 * cleared when user presses enter
	 * MONITOR_flag - is set when monitor command is running
	 * cleared when user presses enter
	 */
	private static boolean FIND_flag = false;
	private static boolean MONITOR_flag = false;
	
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
		
		//TODO take second argument to be file server
		netSrv.setPort(netPort);
		netSrv.start();
		fileSrv.setPort(fileDwPort);
		fileSrv.start();
		
		SimpellaClient client = new SimpellaClient();

		BufferedReader cmdFromUser = new BufferedReader(new InputStreamReader(
				System.in));
		// CLI begins

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

			} else if(cmd_args[0].equals("update")){
				System.out.println("update command");
				//TODO broadcast ping
				
			} else if (cmd_args[0].equals("find")) {
				System.out.println("update command");
				setFINDFlag();
				find(cmd_args[1]);
				//wait until user presses enter
				cmdFromUser.readLine();
				SimpellaConnectionStatus.clearQueryhitsReceivedCount();
				clearFINDFlag();
				int count = SimpellaConnectionStatus.getQueryhitsReceivedCount();
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
				SimpellaConnectionStatus.clearQueryResultsTable();
							
			} else if (cmd_args[0].equals("download")) {
				System.out.println("download command");
				// test code
				SimpellaFileClient fileDw = new SimpellaFileClient();
				fileDw.setFileIndex(1);
				fileDw.setFileName("test.mp3");
				fileDw.setServerIP("localhost");
				fileDw.setServerPort(8080);
				fileDw.downloadFile();
				//TODO implement download
				
			} else if (cmd_args[0].equals("share")) {
				String sharedDirectory = usrInput.substring(usrInput.indexOf(" ") + 1);
				System.out.println("sharing directory " + sharedDirectory);
				File share = new File(sharedDirectory);
				if(! share.exists()) {
					System.out.println("Invalid file name");
					continue;
				}
				SimpellaFileShareDB.setSharedDirectory(sharedDirectory);
				
			} else if (cmd_args[0].equals("scan")) {
				System.out.println("scan command");
				SimpellaFileShareDB fileDb = new SimpellaFileShareDB();
				fileDb.scanSharedDirectory();
				System.out.println("No of files = " + fileDb.getNoOfFiles() 
						+ " Total Size = " + fileDb.getSizeOfFiles() + " bytes");
				
			} else if (cmd_args[0].equals("monitor")) {
				System.out.println("monitor command");
				setMONITORFlag();
				cmdFromUser.readLine();
				clearMONITORFlag();
				
				
			} else if (cmd_args[0].equals("quit")) {
				System.out.println("quit command");
				//TODO close all sockets
				System.exit(0);
			} else {
				System.out.println("Command not yet implemented!");
			}
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
			
			//TODO set the length of the payload more elegantly :)
			/*
			queryHeader[19] = (byte)0x00;
			queryHeader[20] = (byte)0x00;
			queryHeader[21] = (byte)0x00;
			queryHeader[22] = (byte)(2 + searchTxt.getBytes().length + 1); // +2 for speed +1 for \0
			*/
			
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