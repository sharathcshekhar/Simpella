import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Formatter;
import java.util.StringTokenizer;

public class SimpellaFileClient {
	//TODO use SimpellaQueryResults object instead.
	private int fileIndex;
	private String fileName;
	private String serverIP;
	private int serverPort;
	StringBuffer download_cmd;
	public int getFileIndex() {
		return fileIndex;
	}

	public void setFileIndex(int fileIndex) {
		this.fileIndex = fileIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getServerIP() {
		return serverIP;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void downloadFile(int index) {
		
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
		
		Socket clientSocket = null;
		this.fileIndex = SimpellaConnectionStatus.queryResults.get(index-1).file_index;
		this.fileName = SimpellaConnectionStatus.queryResults.get(index-1).fileName;
		this.serverIP=SimpellaConnectionStatus.queryResults.get(index-1).getIpAddress();
		this.serverPort=SimpellaConnectionStatus.queryResults.get(index-1).getPort();
		String request = "GET /get/" + fileIndex + "/" + fileName
				+ " HTTP/1.1\r\n";
		String userAgent = "User-Agent: Simpella\r\n";
		//TODO set IP and port
		String host = "Host: "+SimpellaIPUtils.getLocalIPAddress().getHostAddress()+":"+SimpellaConnectionStatus.simpellaFileDownloadPort+"\r\n";
		String connRange = "Connection: Keep-Alive\r\n" + "Range: bytes=0-\r\n"
				+ "\r\n";
			download_cmd = new StringBuffer(request + userAgent + host
					+ connRange);	
		
		Thread clienListner_t = new Thread(new clientConnectionThread(
				clientSocket));
		clienListner_t.start();
			}
	
	private void connectionListener(Socket clientSocket) {
		
		try {
			clientSocket = new Socket(serverIP, serverPort);
		} catch (UnknownHostException e) {
			System.out.println("Socket error during download");
		} catch (IOException e) {
			System.out.println("Socket error during download");
		}
		
		if(Simpella.debug) {
			System.out.println("spawned a thread for file download!");
		}
		try {
			DataOutputStream outToServer = new DataOutputStream(
					clientSocket.getOutputStream());

			outToServer.write(download_cmd.toString().getBytes());
		} catch (IOException e) {
			System.out.println("Error during sending request to server");
		}

		byte[] readResp = new byte[1000];
		String resp = null;
		try {
			clientSocket.getInputStream().read(readResp);
			resp = new String(readResp);
			if((null == resp)|| !resp.contains("200 OK")&& !resp.contains("503")){
			System.out.println("There was an error during file download");	
			return;
			}
			System.out.println("Response received : " + resp);
		} catch (IOException e) {
			System.out.println("Server Response Error");
		}
		
		StringTokenizer token = new StringTokenizer(resp, "\r\n");
		
		long fileLen = 0;
		while (token.hasMoreElements()) {
			String eachToken[] = token.nextToken().split(":");
			for (String each : eachToken) {
				if (each.contains("Content-length")) {
					fileLen = Integer.parseInt(eachToken[1].trim());
					break;
				}
			}
		}
		
		FileOutputStream fo = null;
		File tmpFile = null;
		String dir = SimpellaFileShareDB.sharedDirectory;
		DataOutputStream out = null;
		File newFile = null;
		if(null!=dir){
			newFile = new File(dir+"/"+fileName);
			tmpFile =  new File(dir+"/"+fileName+"_temp");
		}
		else{
			newFile = new File(fileName);
			tmpFile =  new File(fileName+"_temp");				
		}
		
		try {
			fo = new FileOutputStream(tmpFile);
			out = new DataOutputStream(fo);
		} catch (FileNotFoundException e1) {
			System.out.println("Error while downloading file");
		}

		byte[] readData = new byte[1024];
		int i = 0;
		long filedownloaded = 0;
		try {
			while (!((i = clientSocket.getInputStream().read(readData, 0, 1024)) == -1)) {
				filedownloaded+=i;
				if(Simpella.printDwnload){
					System.out.println(" ");
					Formatter info_fmt = new Formatter();
					info_fmt.format("%-15s %-24s %-50s %-100s\n", "Client", "Percentage", "Size", "Name");
					info_fmt.format("%-15s %-24s %-50s %-100s\n", serverIP+":"+Integer.toString(serverPort), 
							Float.toString((float)(((double)(filedownloaded*100))/((double)fileLen)))+"%",
							SimpellaUtils.memFormat(filedownloaded)+"/"+SimpellaUtils.memFormat(fileLen),
							fileName);
					System.out.println(info_fmt);
					Simpella.printDwnload=false;
				}
				out.write(readData, 0, i);
			}
			Simpella.printDwnload=false;
		} catch (IOException e) {
			System.out.println("Error while downloading file");
		}
		//delete to if exists to rename it
		newFile.delete();
		// rename file
		/*if (!tmpFile.renameTo(newFile)) {
			System.out.println("File already exists");
			return;
		}*/
		try {
			clientSocket.close();
			out.close();
			if (!tmpFile.renameTo(newFile)) {
				System.out.println("File already exists");
				return;
			} 
		} catch (IOException e) {
			System.out.println("Sockets closed abruplty");
		}

		
	}

}
