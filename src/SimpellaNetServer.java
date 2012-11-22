import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
public class SimpellaNetServer {
	int port = 0;
	boolean status = true;

	class TCPServer implements Runnable {
		private int portNumber;

		public TCPServer(int portNumber) {
			this.portNumber = portNumber;
		}

		public void run() {
			try {
				TCPServerThread(portNumber);
			} catch (Exception e) {
				// TODNetSrvO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	public int start() {
		Thread tcp_t = new Thread(new TCPServer(port));
		tcp_t.start();
		return 0;
	}
	
	public void stop() {
		status = false;
	}

	/**
	 * TCP server thread.
	 * 
	 * @param tcpServerPort
	 *            the tcp server port
	 */
	private void TCPServerThread(int tcpServerPort) throws Exception {
		// create client socket
		Socket clientSocket = new Socket();
		System.out.println("Starting TCP Server at port " + tcpServerPort);
		ServerSocket SimpellaTCP = null;
		try {// create server socket
			SimpellaTCP = new ServerSocket(tcpServerPort);
		} catch (SocketException e) {
			System.out.println("TCP Socket already in use.");
			System.exit(1);
		} catch (IOException e) {
			System.out
					.println("Failed to start TCP server at " + tcpServerPort);
			System.exit(1);
		}
		while (status) {
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
			
			DataInputStream inFromClient = new DataInputStream(
					clientSocket.getInputStream());
			byte[] cmd = new byte[22];
			inFromClient.read(cmd);
			String S = new String(cmd);
			if (!S.equals("SIMPELLA CONNECT/0.6\r\n")) {
				System.out.println("Continuing " + S);
				replyToConnect = "SIMPELLA/0.6 503 KO";
			}
			System.out.println("Server replies with " + replyToConnect);
			outToClient.write(replyToConnect.getBytes());
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
