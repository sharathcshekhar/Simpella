import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Formatter;
import java.util.StringTokenizer;

import com.sun.net.httpserver.HttpHandler;

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

		byte[] readResp = new byte[512];
		String resp = null;
		int no_of_CR_LF = 0;
		int no_of_bytesRead = 0;
		byte previousByteRead = 0;
		ByteArrayOutputStream msg = new ByteArrayOutputStream();
		try {
			while(no_of_CR_LF < 5) {
				byte tmpbuf;
				tmpbuf = (byte)clientSocket.getInputStream().read();
				msg.write(tmpbuf);
				System.out.println("read " + no_of_bytesRead + " bytes");
				no_of_bytesRead++;
				if((previousByteRead == 13) && (tmpbuf == 10)) {
					System.out.println("CR/LF encountered");
					no_of_CR_LF++;
				}
				previousByteRead = tmpbuf;
			}
			byte[] http_header = msg.toByteArray();
			resp = new String(http_header);
			//clientSocket.getInputStream().read(readResp);
			//resp = new String(readResp);
			
			if((null == resp)|| !resp.contains("200 OK")&& 
					!resp.contains("503")){
			System.out.println("There was an error during file download");	
			return;
			}
			System.out.println("Response received : " + new String(http_header));
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
		String tmpName = "tmp" + fileName;
		if(null!=dir){
			newFile = new File(dir+"/"+fileName);
			tmpFile =  new File(dir+"/"+fileName+"_temp");
		}
		
		else{
			newFile = new File("works");
			tmpFile =  new File(tmpName);				
		}
		//System.out.println("filename to write = " + tmpFil);
		//try {
		//	tmpFile.createNewFile();
	//	} catch (IOException e2) {
			// TODO Auto-generated catch block
			//e2.printStackTrace();
	//	}
		System.out.println("Filename = " + tmpFile.getAbsolutePath());
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
			//	System.out.println("Reading cycle " + i);
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
		//newFile.delete();
		// rename file
		/*if (!tmpFile.renameTo(newFile)) {
			System.out.println("File already exists");
			return;
		}*/
		
		try {
			out.flush();
			fo.flush();
			
			out.close();
			fo.close();
			
			if (!tmpFile.renameTo(newFile)) {
				System.out.println("File already exists");
				return;
			}
			
			clientSocket.close();
			System.out.println("done reading.. closed everything");
		} catch (IOException e) {
			System.out.println("Sockets closed abruplty");
		}
		return;
		
	}

}
