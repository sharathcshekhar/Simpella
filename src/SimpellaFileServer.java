import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;



public class SimpellaFileServer {
	public static void main(String[] args){
	// open server socket
    ServerSocket socket = null; 
    try {
        socket = new ServerSocket(5050); 
    } catch (IOException e) {
        System.exit(-1);
    }
    
    while (true) {
        Socket connection = null;
        try {
            // wait for request
            connection = socket.accept();
            byte[] input = new byte[200];
            connection.getInputStream().read(input);
            String request = new String(input);
            StringTokenizer token = new StringTokenizer(request, "\r\n");

            boolean flag = false;
    		while(token.hasMoreElements()){
    			String eachToken[] =  token.nextToken().split(":");
    			for(String each:eachToken){
    			if(each.contains("bytes=0-")){
    				flag = true;
    				break;
    			}
    			}
    		}
    		
    		if(!flag){
    			return;
    		}
    		StringTokenizer token1 = new StringTokenizer(request, "\r\n");
    		String[] req = token1.nextToken().split(" ");
    		System.out.println(req[1]);
    		   		
    		  String f = req[1]; 
              File fileLen = new File(f);
              
              String response = null;
              if(!fileLen.exists()){
            	response = "HTTP/1.1 503 File not found.\r\n\r\n";
          		connection.getOutputStream().write(response.getBytes());
          	   return;
          	}
    		              
              byte[] filec = new byte[1024];
              
    		response = "HTTP/1.1 200 OK\r\n";
    		String server = "Server: Simpella0.6\r\n";
    		String contentType = "Content-type: application/binary\r\n";
    		String contentLen = "Content-length: "+fileLen.length()+"\r\n\r\n";
    		StringBuffer responseBuf = new StringBuffer(response+server+contentType+contentLen);
    		connection.getOutputStream().write(responseBuf.toString().getBytes());
            
            //connection.getOutputStream().write(length);
            
            DataInputStream di = new DataInputStream(new FileInputStream(fileLen));
           
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            int i=0;
            while(!((i=di.read(filec, 0, 1024))==-1)){
            	dos.write(filec, 0, i);
            }
            dos.close();
        }
        catch(Exception e){
        	System.out.println("Error");
        }
}
}
	
}
