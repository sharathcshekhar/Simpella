import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * 
 */

/**
 * @author sharath
 *
 */
public class Simpella {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		class TCPServer implements Runnable {
			private int portNumber;

			public TCPServer(int portNumber) {
				this.portNumber = portNumber;
			}

			public void run() {
				try {
					TCPServerThread(portNumber);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		int port = Integer.parseInt(args[0]);
		Thread tcp_t = new Thread(new TCPServer(port));
		tcp_t.start();
		
		BufferedReader cmdFromUser = new BufferedReader(new InputStreamReader(
				System.in));
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

			if(cmd_args[0].equals("open")){
				System.out.println("open command");
			}
			int connectionPort = Integer.parseInt(cmd_args[1]);
			Socket clientSocket;
			clientSocket = new Socket("localhost", connectionPort);
			String connect_cmd = "SIMPELLA CONNECT/0.6\r\n";
			DataOutputStream outToServer = new 
				DataOutputStream(clientSocket.getOutputStream());
			outToServer.write(connect_cmd.getBytes());
			BufferedReader inFromServer = new BufferedReader(new 
					InputStreamReader(clientSocket.getInputStream()));
			char[] replyToConnect = new char[24];
			inFromServer.read(replyToConnect);
			String S = new String(replyToConnect);
			System.out.println("Server replies with " + S);
			clientSocket.close();
			
		}	
}
	/**
	 * TCP server thread.
	 *
	 * @param tcpServerPort the tcp server port
	 */
	private static void TCPServerThread(int tcpServerPort) throws Exception {
		//create client socket
		Socket clientSocket = new Socket();
		System.out.println("Starting TCP Server at port " + tcpServerPort);
		ServerSocket SimpellaTCP = null;
		try {//create server socket
			SimpellaTCP = new ServerSocket(tcpServerPort);
		} catch (SocketException e) {
			System.out.println("TCP Socket already in use.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Failed to start TCP server at " + tcpServerPort);
			System.exit(1);
		}
		while (true) {
			try {
				clientSocket = SimpellaTCP.accept();

			} catch (IOException e) {
				System.out.println("Accept failed at " + tcpServerPort);
				continue;
			}
			System.out.println("Accepted!");
			String replyToConnect = "SIMPELLA/0.6 200 OK";
			DataOutputStream outToClient = new DataOutputStream(
					clientSocket.getOutputStream());
			outToClient.writeBytes(replyToConnect);
			DataInputStream inFromClient = new 
					DataInputStream(clientSocket.getInputStream());
			byte[] cmd = new byte[22];
			inFromClient.read(cmd);
			String S = new String(cmd);
			if(!S.equals("SIMPELLA CONNECT/0.6\r\n")){
				System.out.println("Continuing " + S);
				continue;
			}
			System.out.println("Server replies with " + replyToConnect);
			outToClient.writeBytes(replyToConnect);
				
		}
	}

}
