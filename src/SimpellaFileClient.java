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
		Socket clientSocket = null;
		this.fileIndex = index;
		boolean connPresent = false;
		for(SimpellaQueryResults q : SimpellaConnectionStatus.queryResults){
			if(q.file_index==index){
				connPresent=true;
				this.fileName=q.fileName;
				this.serverIP = q.ipAddress;
				this.serverPort = q.port;
				break;
			}
		}
		if(!connPresent){
			System.out.println("Invalid file index");
			return;
		}
		String request = "GET /get/" + fileIndex + "/" + fileName
				+ " HTTP/1.1\r\n";
		String userAgent = "User-Agent: Simpella\r\n";
		//TODO set IP and port
		String host = "Host: "+SimpellaIPUtils.getLocalIPAddress().getHostAddress()+":"+SimpellaConnectionStatus.simpellaFileDownloadPort+"\r\n";
		String connRange = "Connection: Keep-Alive\r\n" + "Range: bytes=0-\r\n"
				+ "\r\n";
		StringBuffer download_cmd = new StringBuffer(request + userAgent + host
				+ connRange);
			
		try {
			clientSocket = new Socket(serverIP, serverPort);
		} catch (UnknownHostException e) {
			System.out.println("Socket error during download");
		} catch (IOException e) {
			System.out.println("Socket error during download");
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
			System.out.println("Response received : " + resp);
		} catch (IOException e) {
			System.out.println("Server Response Error");
		}
		
		StringTokenizer token = new StringTokenizer(resp, "\r\n");
		
		int fileLen = 0;
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
		File tmpFile = new File("." + fileName);
		File newFile = new File(fileName);
		try {
			fo = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e1) {
			System.out.println("Error while downloading file");
		}
		DataOutputStream out = new DataOutputStream(fo);
		byte[] readData = new byte[1024];
		int i = 0;
		int filedownloaded = 0;
		try {
			while (!((i = clientSocket.getInputStream().read(readData, 0, 1024)) == -1)) {
				filedownloaded+=i;
				if(Simpella.printDwnload){
					Formatter info_fmt = new Formatter();
					info_fmt.format("%-15s %-24s %-50s %-100s\n", "Client", "Percentage", "Size", "Name");
					info_fmt.format("%-15s %-24f %-50s %-100s\n", serverIP+":"+Integer.toString(serverPort), 
							Float.toString((float)((float)(filedownloaded*100)/(float)fileLen)),
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
		// rename file
		if (!tmpFile.renameTo(newFile)) {
			System.out.println("File already exists");
			return;
		}
		try {
			clientSocket.close();
			out.close();
		} catch (IOException e) {
			System.out.println("Sockets closed abruplty");
		}
	}

}
