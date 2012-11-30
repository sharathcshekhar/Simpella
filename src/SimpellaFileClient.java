import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;


public class SimpellaFileClient {
	public void download(int num){
		Socket clientSocket = null;
		String fileIndex = new String();
		String fileName = new String();
		String request = "GET /get/"+fileIndex+"/"+fileName+" HTTP/1.1\r\n";
		String userAgent = "User-Agent: Simpella\r\n";
		
		//TODO set IP and port from lookup
		String host = "Host: 127.0.0.1:5050\r\n";
		String connRange = "Connection: Keep-Alive\r\n" +"Range: bytes=0-\r\n" +"\r\n";
		StringBuffer download_cmd = new StringBuffer(request+userAgent+host+connRange);
		String ipaddr = null;
		String port = null;
		StringTokenizer token = new StringTokenizer(download_cmd.toString(), "\r\n");
		//System.out.println(token);
		while(token.hasMoreElements()){
			String eachToken[] =  token.nextToken().split(":");
			if(eachToken[0].contains("Host")){
				ipaddr = eachToken[1].trim();
				port = eachToken[2].trim();
			}
		}
			int portnum = Integer.parseInt(port);
			try {
				clientSocket = new Socket(ipaddr,portnum);
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
		try {
			clientSocket.getInputStream().read(readResp);
			System.out.println("Response received : "+new String(readResp));
		} catch (IOException e) {
			System.out.println("Server Response Error");
		}
		   FileOutputStream fo = null;
		   File tmpFile= new File("."+fileName);
		   File newFile= new File(fileName);
		try {
			fo = new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e1) {
			System.out.println("Error while downloading file");
		}
		DataOutputStream out = new DataOutputStream(fo);
		  byte[] readData = new byte[1024];
		  int i=0;
          try {
        	  	while(!((i = clientSocket.getInputStream().read(readData,0,1024))==-1)) {
				   out.write(readData,0,i);
			   }
		} catch (IOException e) {
			System.out.println("Error while downloading file");
		}
          //rename file 
          if(!tmpFile.renameTo(newFile)){
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

	public static int byteArrayToInt(byte[] b) 
	{
	    return   b[3] & 0xFF |
	            (b[2] & 0xFF) << 8 |
	            (b[1] & 0xFF) << 16 |
	            (b[0] & 0xFF) << 24;
	}
	
}
