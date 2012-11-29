import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class SimpellaNetServer {
	/*
	 * TODO change in architecture:
	 * Have a queue of size 3 objects of type SimpellaServerResponse
	 * Every time a connection is received, a new object is created
	 * and store in this queue, if the size is not already greater than 3
	 * Before closing the socket connection, remove the item from the
	 * queue. All future communication with this object shall be made
	 * through calling private variables.
	 */
	
	int port;
	boolean status;

	class TCPServer implements Runnable {
		private int portNumber;

		public TCPServer(int portNumber) {
			this.portNumber = portNumber;
		}

		public void run() {
			try {
				TCPServerThread(portNumber);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public int start() {
		status = true;
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
		class TCPserverResponseThread implements Runnable {
			private Socket clientSocket;
			public TCPserverResponseThread(Socket clientSocket) {
				this.clientSocket = clientSocket;
			}
		public void run() {
				try {
					TCPserverResponse(clientSocket);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		;
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
			String inComingIP = clientSocket.getInetAddress().getHostAddress();
			int inComingPort = clientSocket.getPort();
			if (SimpellaConnectionStatus.isInConnectionPresent(inComingIP, inComingPort) || 
					SimpellaConnectionStatus.isOutConnectionPresent(inComingIP, inComingPort)) {
				System.out.println("Duplicate connection");
				clientSocket.close();
			} else {
				if(SimpellaConnectionStatus.incomingConnectionCount < 3){
					Thread tcp_serverResp_t = new Thread(new TCPserverResponseThread(
						clientSocket));
					tcp_serverResp_t.start();
				} else {
						String error = "SIMPELLA/0.6 503 Maximum number of connections reached";
						System.out.println("Server replies with " + error);
						DataOutputStream outToClient = new DataOutputStream(
								clientSocket.getOutputStream());
						outToClient.write(error.getBytes());
						clientSocket.close();
				}
			}
		}
	}
	
	public static void sendPong(Socket clientSocket, byte[] header){
		//Reply with a pong on ping 
		System.out.println("Sending pong");
		byte[] payload = new byte[37];
		System.arraycopy(header, 0, payload, 0, 22);
		Header h2 = new Header();
		h2.initializeHeader();
		h2.setHeader(payload);
		h2.setMsgType("pong");
		//TODO files and size
		byte[] filesShared = null;
		byte[] kbsShared = null;
		//No need to set MsgId as it should be same as ping					
		h2.setPongPayload(clientSocket,payload,filesShared,kbsShared);

		DataOutputStream outToClient = null;
		try {
			outToClient = new DataOutputStream(
					clientSocket.getOutputStream());
			outToClient.write(payload);
		} catch (IOException e) {
			System.out.println("Socket Connection Error during pong write");
		}
		
		System.out.println("Server replies with pong : " + Arrays.toString(payload));
	}
	
	/**
	 * TCP server response.
	 *
	 * @param clientSocket the client socket
	 */
	public void TCPserverResponse(Socket clientSocket) throws Exception {
		//New connection, check for the connection headers
		String replyToConnect = "SIMPELLA/0.6 200 OK";
		DataOutputStream outToClient = new DataOutputStream(
				clientSocket.getOutputStream());

		DataInputStream inFromClient = new DataInputStream(
				clientSocket.getInputStream());
		byte[] cmd = new byte[512]; 
		//TODO read larger messages in chunks
		int len = inFromClient.read(cmd);
		
		String S = new String(cmd);
		if (S.substring(0, len).equals("SIMPELLA CONNECT/0.6\r\n")) {
			System.out.println("Server replies with " + replyToConnect);
			outToClient.write(replyToConnect.getBytes());
			SimpellaConnectionStatus.addIncomingConnection(clientSocket);
		} else {
			System.out.println("Unknown connection request, ignoring");
			return;
		}
		while(true) {
			byte[] header = new byte[23];
			len = inFromClient.read(header, 0, 23);
			if(len == -1) {
				System.out.println("Client has close the socket, exit");
				break;
			}
			handleMsg(header, clientSocket);
		}
	}
	public static void broadcastPing(byte[] pingMsg, Socket sender) throws Exception {
		Socket clientSocket = null;
		String clientIP = "";
		System.out.println("In broadcast ping");
		for(int i = 0; i < 3; i++) {
			
			clientSocket = 
					SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = 
					SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.incomingConnectionList[i].remotePort;
			if(!(clientIP.equals("")) && 
					!((sender.getInetAddress().getHostAddress().equals(clientIP)) &&
							(sender.getPort() == clientPort))) {
				/* send to everyone apart from this node */
				System.out.println("sending ping to IP: " + clientIP + " Port = " + clientPort);
				DataOutputStream outToServents = new DataOutputStream(
						clientSocket.getOutputStream());
				outToServents.write(pingMsg);
			}
		}
		for(int j = 0; j < 3; j++) {
			clientSocket = 
					SimpellaConnectionStatus.outgoingConnectionList[j].sessionSocket;
			
			clientIP = 
			SimpellaConnectionStatus.outgoingConnectionList[j].remoteIP;
			int clientPort = SimpellaConnectionStatus.outgoingConnectionList[j].remotePort;
			if(!(clientIP.equals("")) && 
					!((sender.getInetAddress().getHostAddress().equals(clientIP)) &&
							(sender.getPort() == clientPort))) {
				System.out.println("sending ping to IP: " + clientIP + " Port = " + clientPort);
				/* send to everyone apart from this node */
				DataOutputStream outToServents = new DataOutputStream(
						clientSocket.getOutputStream());
				outToServents.write(pingMsg);
			}
		}
	}
			/**
			 * Broadcast query.
			 *
			 * @param payload the payload
			 * @param queryPayLoad 
			 * @param sender the sender
			 * @throws Exception the exception
			 */
	public static void broadcastQuery(byte[] header, byte[] queryPayLoad,
			Socket sender) throws Exception {
		Socket clientSocket = null;
		String clientIP = "";
		
		for (int i = 0; i < 3; i++) {
			clientSocket = SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.outgoingConnectionList[i].remotePort;
			
			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {
					System.out.println("broadcast incoming " + clientIP
							+ " port = " + clientPort);
					/* send to everyone apart from this node */
					DataOutputStream outToServents = new DataOutputStream(
							clientSocket.getOutputStream());
					outToServents.write(header);
					outToServents.write(queryPayLoad);
				}
			}
		}
		for (int j = 0; j < 3; j++) {
			System.out.println("In broadcast outgoing " + j);
			clientSocket = SimpellaConnectionStatus.outgoingConnectionList[j].sessionSocket;
			clientIP = SimpellaConnectionStatus.outgoingConnectionList[j].remoteIP;
			int clientPort = SimpellaConnectionStatus.outgoingConnectionList[j].remotePort;
			
			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {

					System.out.println("broadcast incoming " + clientIP
							+ " port = " + clientPort);
					/* send to everyone apart from this node
					 * If sender is null, send it to all 
					 */
					DataOutputStream outToServents = new DataOutputStream(
							clientSocket.getOutputStream());
					outToServents.write(header);
					outToServents.write(queryPayLoad);
				}
			}
		}
	} 
	
	
	public static void handleMsg(byte[] header, Socket sessionSocket) throws Exception{
		
		DataInputStream inFromClient = new DataInputStream(
				sessionSocket.getInputStream());
		if (header[16] == (byte) 0x00) {
			System.out.println("Ping received");
			String key = SimpellaRoutingTables.guidToString(header);
			if(SimpellaRoutingTables.PingTable.containsKey(key)) {
				//TODO combine if and else to one if ignore Ping if the node has seen the request!
			} else {
				SimpellaRoutingTables.insertPingTable(key, sessionSocket);
				if(header[17] > 1) {
					header[17]--; //decrement TTL
					header[18]++; //Increment hops
					broadcastPing(header, sessionSocket);
					//Set TTL and hops to default values
					header[17] = (byte)0x07;
					header[18] = (byte)0x00; //Increment hops
					sendPong(sessionSocket, header);
				}
			}
			//TODO else if header[16] == 0x01, forward	
		} else if(header[16] == (byte)0x01) {
			System.out.println("Pong received");
			String guid = SimpellaRoutingTables.guidToString(header);
			//Read the pong payload
			byte[] pongPayLoad = new byte[14];
			int len = inFromClient.read(pongPayLoad, 0, 14);
			if(len != 14){
				System.out.println("Something has gone wrong!");
				return;
			}

			System.out.println(len + " bytes of pong payload read");
			if(SimpellaRoutingTables.generatedPingList.contains(guid)) {
				// pong is for me
				System.out.println("Pong received for self: PayLoad = " + 
						" 2: " + pongPayLoad[2] + " 3: " + pongPayLoad[3] + 
						" 4: " + pongPayLoad[4] + " 5: " + pongPayLoad[5]);
				return;
				//TODO read contents and store them in a store
			} else {
				// forward the pong using the routing table
				if(SimpellaRoutingTables.PingTable.containsKey(guid)) {
					Socket pongFwdSocket = SimpellaRoutingTables.PingTable.get(guid);
					System.out.println("Pong received for ip " + 
							pongFwdSocket.getInetAddress().getHostAddress());
					header[17]--; //decrement TTL
					header[18]++; //Increment hops
					DataOutputStream pongToClient = null;
					try {
						pongToClient = new DataOutputStream(
								pongFwdSocket.getOutputStream());
						pongToClient.write(header, 0, 23);
						pongToClient.write(pongPayLoad);
					} catch (IOException e) {
						System.out.println("Socket Connection Error during pong write");
					}	
				}
			}
		} else if(header[16] == (byte)0x80){
			//TODO handle message
			System.out.println("Query-message");
			String queryid = SimpellaRoutingTables.guidToString(header);
			if(SimpellaRoutingTables.QueryTable.containsKey(queryid)) {
				//TODO combine if and else to one if ignore Ping if the node has seen the request!
			} else {
				//TODO see if you have an answer to the queried string
				// if header[19-22] > 256, drop the packet!
				//if not read those many number of bytes.
				byte[] tmp = new byte[4];
				tmp[0] = header[19];
				tmp[1] = header[20];
				tmp[2] = header[21];
				tmp[3] = header[22];
				int payLoadLen = SimpellaUtils.byteArrayToInt(tmp);
				if(payLoadLen > 256) {
					//report error
					return;
				}
				byte[] queryPayLoad = new byte[payLoadLen];
				int len = inFromClient.read(queryPayLoad, 0, payLoadLen);
				ByteArrayInputStream msg = new ByteArrayInputStream(queryPayLoad);
				byte[] querySpeed = new byte[2];
				msg.read(querySpeed, 0, 2);
				byte[] rawQueryString = new byte[payLoadLen - 2];
				//ignore querySpeed for Simpella
				len = msg.read(rawQueryString , 0, payLoadLen - 2);
				if (rawQueryString[len - 1] != (byte)0x00) {
					System.out.println("Not null terminatd String, error!");
				}
				String searchString = new String(rawQueryString);
				System.out.println("Search String is " + searchString);
				
				replyWithQueryHit(sessionSocket, searchString);
				SimpellaRoutingTables.insertQueryTable(queryid, sessionSocket);
				if(header[17] > 1) {
					header[17]--; //decrement TTL
					header[18]++; //Increment hops
					broadcastQuery(header, queryPayLoad, sessionSocket);
				}
			}
		} else if(header[16] == (byte)0x81){
			//TODO handle message
			byte[] qHit_tmp_buffer = new byte[4];
			qHit_tmp_buffer[0] = header[19];
			qHit_tmp_buffer[1] = header[20];
			qHit_tmp_buffer[2] = header[20];
			qHit_tmp_buffer[3] = header[22];
			int payLoadLen = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
			System.out.println("Query-hit message received with payload len = " + payLoadLen);
			
			byte[] queryHitPayLoad = new byte[payLoadLen];
			int len = inFromClient.read(queryHitPayLoad, 0, payLoadLen);
			
			System.out.println(len + " Bytes of data read from query");
			
			ByteArrayInputStream msg = new ByteArrayInputStream(queryHitPayLoad);
			int no_of_files = msg.read();
			qHit_tmp_buffer[0] = (byte) 0x00;
			qHit_tmp_buffer[1] = (byte) 0x00;
			msg.read(qHit_tmp_buffer, 2, 2);
			int port_no = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
			byte[] ip_address = new byte[4];
			msg.read(ip_address);
			msg.read(qHit_tmp_buffer);
			int speed = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
			System.out.println("no of files " + no_of_files + " port_num = " + port_no + " ip = " 
					+ InetAddress.getByAddress(ip_address).getHostAddress() +
					" Speed = " + speed);
			
			int k;
			for(k = 0; k < len; k++) {
				System.out.println("Received Query payLoad[" + k + "] = " + queryHitPayLoad[k]);
			}
			// 11 bytes would be header, 16 bytes trailer. Middle (payLoadLen - 10 - 16)
			// should be file info
			int bytes_read = 0;
			while(bytes_read < (payLoadLen - 11 - 16)) {
				msg.read(qHit_tmp_buffer);
				int file_index = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
				bytes_read += 4;
				
				msg.read(qHit_tmp_buffer);
				int file_size = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
				bytes_read += 4;
				
				byte[] songname = new byte[512]; //limit the songname to 512
				int i = 0;
				do {
					songname[i] = (byte) msg.read();
					i++;
					bytes_read++;
				} while ((songname[i - 1] != 0) && 
						(bytes_read < (payLoadLen - 11 - 16)));
				String song_str = new String(songname, 0, i);
				System.out.println("file index = " + file_index + " size = " + file_size 
						+ " name = " + song_str);
			}
			byte[] serventID = new byte[16];
			msg.read(serventID);
			
			String guid = SimpellaRoutingTables.guidToString(header);
			if(SimpellaRoutingTables.generatedQueryList.contains(guid)) {
				//TODO initiate file download if the hit is for self
			} else if (SimpellaRoutingTables.QueryTable.containsKey(guid)) {
				// if not route to the appropriate node
				Socket queryHitFwdSocket = SimpellaRoutingTables.QueryTable
						.get(guid);
				System.out.println("Query-hit received for ip "
						+ queryHitFwdSocket.getInetAddress().getHostAddress());
				header[17]--; // decrement TTL
				header[18]++; // Increment hops
				DataOutputStream queryHitToClient = null;
				try {
					queryHitToClient = new DataOutputStream(
							queryHitFwdSocket.getOutputStream());
					queryHitToClient.write(header, 0, 23);
					queryHitToClient.write(queryHitPayLoad);
				} catch (IOException e) {
					System.out
							.println("Socket Connection Error during pong write");
				}
			}
			
		}
	}
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public static void replyWithQueryHit(Socket sessionSocket, String searchString) throws IOException {
		Header queryHeader = new Header();
		queryHeader.initializeHeader();
		queryHeader.setMsgType("queryhit");
		//SimpellaFileShareDB db = new SimpellaFileShareDB();
		//ArrayList<Object> searchResults = db.getMatchingFiles(searchString);
		/* For testing: */
		
		ArrayList<Object> searchResults = new ArrayList<Object>();
		searchResults.add(12345);
		searchResults.add(100);
		searchResults.add("one");
		searchResults.add(67890);
		searchResults.add(200);
		searchResults.add("two");
		//end of testing
		Iterator<Object> itr = searchResults.iterator();
		ByteArrayOutputStream payLoad = new ByteArrayOutputStream();
		
		if(!searchResults.isEmpty()) {
			//TODO Reply with a query-hit
			byte[] tmp = new byte[4];
			int offset = 0;
			//write no. of files to 1st byte
			tmp[0] = (byte) (searchResults.size()/3);
			//payLoad.write(tmp, offset, 1);
			payLoad.write(tmp, 0, 1);
			offset += 1;
			//byte 1-2 is file download Port, 8888 for now
			tmp = SimpellaUtils.toBytes(8888);
			payLoad.write(tmp, 2, 2);
			offset += 2;
			//TODO This has to be replaced with the IP address of the 192.168.1.2
			InetAddress ip = InetAddress.getByName("192.168.1.2");
			byte[] ip_bytes = ip.getAddress();
			System.out.println("ip[0] = " + ip_bytes[0] + " ip[1] = " 
					+ ip_bytes[1] + " ip[2] = " + ip_bytes[2] + " ip[3] = " + ip_bytes[3]);
			
			payLoad.write(ip_bytes, 0, 4);
			offset += 4;
			//Speed, set arbitrarily to 10000
			tmp = SimpellaUtils.toBytes(10000);
			payLoad.write(tmp, 0, 4);
			offset += 4;
			
			while(itr.hasNext()){
				Integer fileIndex = (Integer)itr.next();
				Integer size = (Integer)itr.next();
				String filename = (String)itr.next();
				
				tmp = SimpellaUtils.toBytes(fileIndex.intValue());
				payLoad.write(tmp, 0, 4);
				offset += 4;
				
				tmp = SimpellaUtils.toBytes(size.intValue());
				payLoad.write(tmp, 0, 4);
				offset += 4;
				
				payLoad.write((filename + '\0').getBytes(), 0, filename.length() + 1);
				offset += filename.length() + 1;
			}
			//TODO this has to be a constant value stored in 
			// a static variable somewhere.
			byte[] serventID = new byte[16];
			payLoad.write(serventID, 0, 16);
			offset += 16;	
			byte[] payLoadArray = new byte [offset];
			payLoadArray = payLoad.toByteArray();
			DataOutputStream outToClient = new DataOutputStream(
					sessionSocket.getOutputStream());
			//outToClient.write(header, 23, offset);
			byte[] queryHeader1 = queryHeader.getHeader();
			queryHeader1[19] = (byte) 0x00; 
			queryHeader1[20] = (byte) 0x00;
			queryHeader1[21] = (byte) 0x00;
			queryHeader1[22] = (byte) 51;
			for(int k = 0; k < payLoadArray.length; k ++) {
				System.out.println("Query: payLoadArray[" + k + "] = " + payLoadArray[k]);
			}
			//write header
			outToClient.write(queryHeader1, 0, 23);
			//write payload
			outToClient.write(payLoadArray, 0, offset);
		}
		
	}
}
