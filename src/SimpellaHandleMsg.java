import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SimpellaHandleMsg {
private SimpellaStats stats;
	public void handleMsg(byte[] header, Socket sessionSocket) {

		DataInputStream inFromClient;
		try {
			inFromClient = new DataInputStream(
					sessionSocket.getInputStream());
		} catch (IOException e2) {
			System.out.println("Unable to handle data packet, connection gone bad, will try again");
			return;
		}
		/*
		 * Handle PING Message
		 */
		if (header[16] == (byte) 0x00) {
			System.out.println("Ping received");
			String key = SimpellaRoutingTables.guidToString(header);
			if (SimpellaRoutingTables.PingTable.containsKey(key) || //seen ping before
					SimpellaRoutingTables.generatedPingList.contains(key)) { //self generated ping propagating back
				System.out.println("Ping message seen before or self ping, ignoring");
				return;
			} else {
				
				SimpellaRoutingTables.insertPingTable(key, sessionSocket);
				if (header[17] > 1) {
					header[17]--; // decrement TTL
					header[18]++; // Increment hops
					broadcastPing(header, sessionSocket);
					// Set TTL and hops to default values
					header[17] = (byte) 0x07;
					header[18] = (byte) 0x00; // Increment hops
					sendPong(sessionSocket, header);
				}
			}
		} 
		/*
		 * Handle PONG message
		 */
		else if (header[16] == (byte) 0x01) {
			String guid = SimpellaRoutingTables.guidToString(header);
			// Read the pong payload length
			byte[] pong_tmp_buf = new byte[4];
			pong_tmp_buf[0] = header[19];
			pong_tmp_buf[1] = header[20];
			pong_tmp_buf[2] = header[21];
			pong_tmp_buf[3] = header[22];
			int payLoadLen = SimpellaUtils.byteArrayToInt(pong_tmp_buf);
			
			byte[] pongPayLoad = new byte[14];

			int len;
			try {
				len = inFromClient.read(pongPayLoad, 0, 14);
			} catch (IOException e1) {
				System.out.println("Unable to read data from pong message, ignoring packet");
				return;
			}
			if (len != payLoadLen) {
				System.out.println("Something has gone wrong!");
				return;
			}
			if(Simpella.debug) {
				System.out.println(len + " bytes of pong payload read, payload length = " + payLoadLen);
			}
			if (SimpellaRoutingTables.generatedPingList.contains(guid)) {
				System.out.println("Pong is for me!");
				ByteArrayInputStream msg = new ByteArrayInputStream(
						pongPayLoad);
				//read port no - 2 bytes
				pong_tmp_buf[0] = 0; //set lower bits to 0
				pong_tmp_buf[1] = 0;
				msg.read(pong_tmp_buf, 2, 2);
				int port_number = SimpellaUtils.byteArrayToInt(pong_tmp_buf);
				
				//read ip address, 4 bytes
				msg.read(pong_tmp_buf, 0, 4);
				String ip;
				try {
					ip = InetAddress.getByAddress(pong_tmp_buf).getHostAddress();
				} catch (UnknownHostException e) {
					System.out.println("Unable to resolve host in pong message, ignoring packet");
					return;
				}
				
				//read no of files shared - 4 bytes
				msg.read(pong_tmp_buf, 0, 4);
				int no_of_file_shared = SimpellaUtils.byteArrayToInt(pong_tmp_buf);
				
				//read no of size of files shared - 4 bytes
				msg.read(pong_tmp_buf, 0, 4);
				int size_shared = SimpellaUtils.byteArrayToInt(pong_tmp_buf);
				
				if(Simpella.debug) {
					System.out.println("Pong received from IP " + ip 
							+ " port " + port_number + " No of files: " + no_of_file_shared 
							+ " size = " + size_shared);
				}			
				//add if unique ip to global list
				SimpellaConnectionStatus.checkAndAddIpToGlobalTable(ip, port_number);
				//set shared files data
				int otherFiles = SimpellaConnectionStatus.getOtherFiles();
				SimpellaConnectionStatus.setOtherFiles(otherFiles+no_of_file_shared);
				int otherFilesSize = SimpellaConnectionStatus.getOtherFilesSize();
				SimpellaConnectionStatus.setOtherFilesSize(otherFilesSize+size_shared);
				//TODO Initiate connections depending on the results
				if(SimpellaConnectionStatus.outgoingConnectionCount < 2) {
				/* try to maintain at least 2 outgoing connections
				 * connect only to unique IPs, check if the IP from where
				 * pong originated is present in the connectionTables
				 */
					if(!SimpellaConnectionStatus.isIPConnectionPresent(ip)){
						SimpellaClient newClient = new SimpellaClient();
						newClient.setConnectionIP(ip);
						newClient.setConnectionPort(port_number);
						newClient.connect();
					}
				}
				return;
		
			} else {
				// forward the pong using the routing table
				// If not present in the routing table, it's a stale pong. Ignore
				if (SimpellaRoutingTables.PingTable.containsKey(guid)) {
					Socket pongFwdSocket = SimpellaRoutingTables.PingTable
							.get(guid);
					System.out.println("Pong received for ip "
							+ pongFwdSocket.getInetAddress().getHostAddress());
					header[17]--; // decrement TTL
					header[18]++; // Increment hops
					if(header[17] == 0) {
						//drop the packet if TTL limit has reached
						return;
					}
					DataOutputStream pongToClient = null;
					try {
						pongToClient = new DataOutputStream(
								pongFwdSocket.getOutputStream());
						byte[] pongPacket = SimpellaHeader.getSimpellaPacket(header, pongPayLoad);
						//pongToClient.write(header, 0, 23);
						//pongToClient.write(pongPayLoad);
						pongToClient.write(pongPacket);
						
						//set Simpella connection and global bit and pack for info command
						stats = SimpellaConnectionStatus.getBySocket(pongFwdSocket);
						if (null != stats) {
							stats.setSentBytes(pongPayLoad.length+header.length);
							stats.setSentPacks();
							SimpellaConnectionStatus.setTotalBytesSent(pongPayLoad.length+header.length);
							SimpellaConnectionStatus.setTotalPacketsSent();
						}
					} catch (IOException e) {
						System.out
								.println("Socket Connection Error during pong write");
					}System.out.println("Pong received");
				}
			}
			
		} 
		
		/*
		 * Handle QUERY message 
		 */
		else if (header[16] == (byte) 0x80) {
			System.out.println("Query-message");
			byte[] tmp = new byte[4];
			tmp[0] = header[19];
			tmp[1] = header[20];
			tmp[2] = header[21];
			tmp[3] = header[22];
			int payLoadLen = SimpellaUtils.byteArrayToInt(tmp);
			if (payLoadLen > 256) {
				// report error
				System.out.println("payload > 256 bytes, dropping the packet");
				//discard the packet by consuming the bytes in the packet
				try {
					inFromClient.skipBytes(payLoadLen);
				} catch (IOException e) {
					System.out.println("Failed to read Query message, ignoring");
				}
				return;
			}
			// consume payLoadLen amount of data irrespective of it belongs to you or not!
			byte[] queryPayLoad = new byte[payLoadLen];
			int len;
			try {
				len = inFromClient.read(queryPayLoad, 0, payLoadLen);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Unable to read Query data from the stream");
				return;
			}
					
			String queryid = SimpellaRoutingTables.guidToString(header);
			
			if (SimpellaRoutingTables.QueryTable.containsKey(queryid) || // seen query before
					SimpellaRoutingTables.generatedQueryList.contains(queryid)) { // my own query
				System.out.println("Query message seen before or self query, ignoring");
				
				return;
			} else {
				SimpellaRoutingTables.insertQueryTable(queryid, sessionSocket);
				SimpellaConnectionStatus.setQueriesRecvd();
				
				//set received bytes and packs size
				stats = SimpellaConnectionStatus.getBySocket(sessionSocket);
				if	(null != stats) {
					stats.setRecvdBytes(len);
					stats.setRecvdPacks();
					SimpellaConnectionStatus.setTotalBytesRecvd(len);
					SimpellaConnectionStatus.setTotalPacketsRecvd();
				}
				
				ByteArrayInputStream msg = new ByteArrayInputStream(
						queryPayLoad);
				byte[] querySpeed = new byte[2];
				msg.read(querySpeed, 0, 2);
				byte[] rawQueryString = new byte[payLoadLen - 2];
				// ignore querySpeed for Simpella
				len = msg.read(rawQueryString, 0, payLoadLen - 2);
				if (rawQueryString[len - 1] != (byte) 0x00) {
					System.out.println("Not null terminatd String, error!");
					return;
				}
				String searchString = new String(rawQueryString);
				if (Simpella.debug || Simpella.is_MONITORActive()) {
					System.out.println("Search: " + searchString);
				}
				replyWithQueryHit(sessionSocket, searchString, header);

				if (header[17] > 1) {
					header[17]--; // decrement TTL
					header[18]++; // Increment hops
					broadcastQuery(header, queryPayLoad, sessionSocket);
				}
			}
			
		} 
		
		/*
		 * Handle QUERY-HIT 
		 */
		else if (header[16] == (byte) 0x81) {
			
			byte[] qHit_tmp_buffer = new byte[4];
			qHit_tmp_buffer[0] = header[19];
			qHit_tmp_buffer[1] = header[20];
			qHit_tmp_buffer[2] = header[21];
			qHit_tmp_buffer[3] = header[22];
			int payLoadLen = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
			
			byte[] queryHitPayLoad = new byte[payLoadLen];
			int len;
			try {
				len = inFromClient.read(queryHitPayLoad, 0, payLoadLen);
			} catch (IOException e1) {
				System.out.println("Unable to read date from the socket");
				return;
			}

			
			if(Simpella.debug) {
			System.out.println("Query-hit message received with payload len = "
					+ payLoadLen + "bytes read = " + len);
			}

			//set received bytes and packs size
			stats = SimpellaConnectionStatus.getBySocket(sessionSocket);
			if (null != stats) {
				stats.setRecvdBytes(len);
				stats.setRecvdPacks();
				SimpellaConnectionStatus.setTotalBytesRecvd(len);
				SimpellaConnectionStatus.setTotalPacketsRecvd();
			}
			
			String guid = SimpellaRoutingTables.guidToString(header);
			if (SimpellaRoutingTables.generatedQueryList.contains(guid)) {
				
				System.out.println("Recived Query-hit for me! :)");
				ByteArrayInputStream msg = new ByteArrayInputStream(queryHitPayLoad);
				int no_of_files = msg.read();
				qHit_tmp_buffer[0] = (byte) 0x00;
				qHit_tmp_buffer[1] = (byte) 0x00;
				msg.read(qHit_tmp_buffer, 2, 2);
				int port_no = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
				byte[] ip_address = new byte[4];
				msg.read(ip_address, 0, 4);
				String ip_str = null;
				try {
					ip_str = InetAddress.getByAddress(ip_address).getHostAddress();
				} catch (UnknownHostException e) {
					System.out.println("Unable to resolve host from which Queryhit came");
					return;
				}
				msg.read(qHit_tmp_buffer, 0, 4);
				int speed = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
				if (Simpella.debug) {
					System.out.println("no of files " + no_of_files
							+ " port_num = " + port_no + " ip = "
							+ ip_str + " Speed = " + speed);
					int k;
					for (k = 0; k < len; k++) {
						System.out.println("Received QueryHit payLoad[" + k
								+ "] = " + queryHitPayLoad[k]);
					}
				}
				if(Simpella.is_FINDActive()) {
					SimpellaConnectionStatus.addToQueryhitsReceivedCount(no_of_files);
					System.out.println(SimpellaConnectionStatus.getQueryhitsReceivedCount() + 
							" Responses received");
				}
				int bytes_read = 0;
				// 11 bytes would be header, 16 bytes trailer
				while (bytes_read < (payLoadLen - 11 - 16)) {
					SimpellaQueryResults queryHitRes = new SimpellaQueryResults();
					queryHitRes.setIpAddress(ip_str);
					queryHitRes.setPort(port_no);
					
					msg.read(qHit_tmp_buffer, 0, 4);
					int file_index = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
					bytes_read += 4;
					queryHitRes.setFile_index(file_index);

					msg.read(qHit_tmp_buffer, 0, 4);
					int file_size = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
					bytes_read += 4;
					queryHitRes.setFile_size(file_size);

					byte[] songname = new byte[512]; // limit the songname to 512
					int i = 0;
					do {
						songname[i] = (byte) msg.read();
						i++;
						bytes_read++;
					} while ((songname[i - 1] != 0)
							&& (bytes_read < (payLoadLen - 11 - 16)));
					
					String song_str = new String(songname, 0, i);
					queryHitRes.setFileName(song_str);
					SimpellaConnectionStatus.insertToQueryResultsTable(queryHitRes);
					if(Simpella.debug) {
						System.out.println("file index = " + file_index + " size = "
								+ file_size + " name = " + song_str);
					}
					
				}
				
				byte[] serventID = new byte[16];
				msg.read(serventID, 0, 16); //no use of this

			} else if (SimpellaRoutingTables.QueryTable.containsKey(guid)) {
				// if not route to the appropriate node
				Socket queryHitFwdSocket = SimpellaRoutingTables.QueryTable
						.get(guid);
				if (Simpella.debug) {
					System.out.println("Query-hit received for ip "
							+ queryHitFwdSocket.getInetAddress().getHostAddress()
							+ ":" + queryHitFwdSocket.getPort());
				}
				header[17]--; // decrement TTL
				header[18]++; // Increment hops
				if (header[17] == 0) {
					//drop the packet
					return;
				}
				DataOutputStream queryHitToClient = null;
				try {
					queryHitToClient = new DataOutputStream(
							queryHitFwdSocket.getOutputStream());
					byte[] queryHPacket = SimpellaHeader.getSimpellaPacket(header, queryHitPayLoad);
				//	queryHitToClient.write(header, 0, 23);
				//	queryHitToClient.write(queryHitPayLoad);
					queryHitToClient.write(queryHPacket);
					//set global and Simpella Connection bit and pack for info command
					stats = SimpellaConnectionStatus.getBySocket(queryHitFwdSocket);
					if(null!=stats){
						stats.setSentBytes(queryHitPayLoad.length+header.length);
						stats.setSentPacks();
						SimpellaConnectionStatus.setTotalBytesSent(queryHitPayLoad.length+header.length);
						SimpellaConnectionStatus.setTotalPacketsSent();
					}
					
				} catch (IOException e) {
					System.out
							.println("Socket Connection Error during pong write");
				}
			} else {
				System.out.println("Stale query-hit, ignoring");
			}
		}
	}

	public void sendPong(Socket clientSocket, byte[] header) {
		// Reply with a pong on ping
		System.out.println("Sending pong");
		byte[] pongPacket = new byte[37];
		// copy 23 bytes from header, starting at 0,
		// to pongPacket, from position 0
		System.arraycopy(header, 0, pongPacket, 0, 23);
		SimpellaHeader pongHeader = new SimpellaHeader();
		pongHeader.initializeHeader();
		pongHeader.setHeader(pongPacket);
		pongHeader.setMsgType("pong");
		pongHeader.setPayLoadLength(14); // 14 bytes for pong
		
		// No need to set MsgId as it should be same as ping
		pongHeader.setPongPayload(clientSocket, pongPacket);
		DataOutputStream outToClient = null;
		try {
			outToClient = new DataOutputStream(clientSocket.getOutputStream());
			outToClient.write(pongPacket);
			//set bit and pack for info command
			stats = SimpellaConnectionStatus.getBySocket(clientSocket);
			if(null!=stats){
			stats.setSentBytes(pongPacket.length);
			stats.setSentPacks();
			SimpellaConnectionStatus.setTotalBytesSent(pongPacket.length);
			SimpellaConnectionStatus.setTotalPacketsSent();		
			}
		} catch (IOException e) {
			System.out.println("Socket Connection Error during pong write");
		}
		if(Simpella.debug) {
			System.out.println("Server replies with pong : "
				+ Arrays.toString(pongPacket));
		}
	}
	
	/*
	 * If 'sender' is a valid socket, send the message to all connections,
	 * except sender. If sender = null, send message to all existing
	 * connections
	 */
	public void broadcastPing(byte[] pingMsg, Socket sender) {
		Socket clientSocket = null;
		String clientIP = "";
		System.out.println("In broadcast ping");
		for (int i = 0; i < 3; i++) {

			clientSocket = SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.incomingConnectionList[i].remotePort;

			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {
					/* send to everyone apart from this node */
					System.out.println("sending ping to IP: " + clientIP
							+ " Port = " + clientPort);
					DataOutputStream outToServents;
					try {
						outToServents = new DataOutputStream(
								clientSocket.getOutputStream());
						outToServents.write(pingMsg);
					} catch (IOException e) {
							System.out.println("Connection terminated at the Server end");
							continue;
							//TODO remove connection List
						}
				//set bit and pack for info command
				stats = SimpellaConnectionStatus.getBySocket(clientSocket);
				if (null != stats) {
					stats.setSentBytes(pingMsg.length);
					stats.setSentPacks();
					SimpellaConnectionStatus.setTotalBytesSent(pingMsg.length);
					SimpellaConnectionStatus.setTotalPacketsSent();		
				}
			}
		}
	}		
		for (int j = 0; j < 3; j++) {
			clientSocket = SimpellaConnectionStatus.outgoingConnectionList[j].sessionSocket;

			clientIP = SimpellaConnectionStatus.outgoingConnectionList[j].remoteIP;
			int clientPort = SimpellaConnectionStatus.outgoingConnectionList[j].remotePort;

/*			if (!(clientIP.equals(""))
					&& !((sender.getInetAddress().getHostAddress()
							.equals(clientIP)) && (sender.getPort() == clientPort))) { 
*/
			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {
					/* send to everyone apart from this node */
					System.out.println("sending ping to IP: " + clientIP
							+ " Port = " + clientPort);
					DataOutputStream outToServents;
					try {
						outToServents = new DataOutputStream(
								clientSocket.getOutputStream());
						outToServents.write(pingMsg);
					} catch (IOException e) {
						System.out.println("Connection terminated at the Client end");
						continue;
						//TODO remove connection List
					}
					//set bit and pack for info command
					stats = SimpellaConnectionStatus.getBySocket(clientSocket);
					if(null != stats){
						stats.setSentBytes(pingMsg.length);
						stats.setSentPacks();
						SimpellaConnectionStatus.setTotalBytesSent(pingMsg.length);
						SimpellaConnectionStatus.setTotalPacketsSent();		
				}
			}
		}
	}
}
	/**
	 * Broadcast query.
	 * 
	 * @param payload
	 *            the payload
	 * @param queryPayLoad
	 * @param sender
	 *            the sender
	 */
	public void broadcastQuery(byte[] header, byte[] queryPayLoad,
			Socket sender) {
		Socket clientSocket = null;
		String clientIP = "";
		byte[] queryPacket = SimpellaHeader.getSimpellaPacket(header, queryPayLoad);

		if (Simpella.debug) {
			System.out.println("In broadcast query with packet Length = "
					+ queryPayLoad.length);
			for (int j = 0; j < queryPayLoad.length; j++) {
				System.out.println("broadcast: Query Payload[" + j + "] = "
						+ queryPayLoad[j]);
			}
		}

		for (int i = 0; i < 3; i++) {
			if (Simpella.debug) {
				System.out.println("In broadcast incoming " + i);
			}
			clientSocket = SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.incomingConnectionList[i].remotePort;

			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {

					if (Simpella.debug) {
						System.out.println("broadcast incoming " + clientIP
								+ " port = " + clientPort);
					}
					/* send to everyone apart from this node */
					DataOutputStream outToServents;

					try {
						outToServents = new DataOutputStream(
								clientSocket.getOutputStream());
						outToServents.write(queryPacket);
					} catch (IOException e) {
						System.out.println("Connection terminated at the Client end");
						continue;
						//TODO remove connection List
					}
					// set bit and pack for info command
					stats = SimpellaConnectionStatus.getBySocket(clientSocket);
					if (null != stats) {
						stats.setSentBytes(queryPayLoad.length + header.length);
						stats.setSentPacks();
						SimpellaConnectionStatus
								.setTotalBytesSent(queryPayLoad.length
										+ header.length);
						SimpellaConnectionStatus.setTotalPacketsSent();
					}
				}
			}
		}
		for (int j = 0; j < 3; j++) {
			if (Simpella.debug) {
				System.out.println("In broadcast outgoing " + j);
			}
			clientSocket = SimpellaConnectionStatus.outgoingConnectionList[j].sessionSocket;
			clientIP = SimpellaConnectionStatus.outgoingConnectionList[j].remoteIP;
			int clientPort = SimpellaConnectionStatus.outgoingConnectionList[j].remotePort;

			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {
					if (Simpella.debug) {
						System.out.println("broadcast outgoing " + clientIP
								+ " port = " + clientPort);
					}
					/*
					 * send to everyone apart from this node If sender is null,
					 * send it to all
					 */
					DataOutputStream outToServents;
					try {
						outToServents = new DataOutputStream(
								clientSocket.getOutputStream());
						outToServents.write(queryPacket);
					} catch (IOException e) {
							System.out.println("Connection terminated at the Server end");
							continue;
							//TODO remove connection List
					}
					// set bit and pack for info command
					stats = SimpellaConnectionStatus.getBySocket(clientSocket);
					if (null != stats) {
						stats.setSentBytes(queryPayLoad.length + header.length);
						stats.setSentPacks();
						SimpellaConnectionStatus
								.setTotalBytesSent(queryPayLoad.length
										+ header.length);
						SimpellaConnectionStatus.setTotalPacketsSent();
					}
				}
			}
		}
	}

	public void replyWithQueryHit(Socket sessionSocket,
			String searchString, byte[] queryHeader) {
		
		SimpellaHeader queryHitHeader = new SimpellaHeader();
		queryHitHeader.initializeHeader();
		// retain the msgID in query header in the query-hit header
		queryHitHeader.setMsgId(queryHeader);
		queryHitHeader.setMsgType("queryhit");
		SimpellaFileShareDB db = new SimpellaFileShareDB();
		ArrayList<Object> searchResults = db.getMatchingFiles(searchString);

		if(Simpella.debug) {
			System.out.println("In replyWithQuery, searchString " + searchString);
			Iterator<Object> itr1 = searchResults.iterator();
			while (itr1.hasNext()) {
				Integer fileIndex = (Integer) itr1.next();
				Long size = (Long) itr1.next();
				String filename = (String) itr1.next();
				if(Simpella.debug){
					System.out.println("File index = " + fileIndex + " size in long "
							+ size + " size in int = " + size.intValue() + " filename "
							+ filename);
					System.out.println("replying with a query-hit");
				}
			}
		}
		
		Iterator<Object> itr = searchResults.iterator();
		ByteArrayOutputStream payLoad = new ByteArrayOutputStream();

		if (!searchResults.isEmpty()) {
			SimpellaConnectionStatus.setResponsesSent();
			
			byte[] tmp = new byte[4];
			int offset = 0;
			// write no. of files to 1st byte
			tmp[0] = (byte) (searchResults.size() / 3);
			payLoad.write(tmp, 0, 1);
			offset += 1;
			// byte 1-2 is file download Port
			tmp = SimpellaUtils.toBytes(SimpellaConnectionStatus.simpellaFileDownloadPort);
			payLoad.write(tmp, 2, 2);
			offset += 2;
			
			byte[] ip_bytes = sessionSocket.getLocalAddress().getAddress();
			if(Simpella.debug) {
				System.out.println("ip[0] = " + ip_bytes[0] + " ip[1] = "
						+ ip_bytes[1] + " ip[2] = " + ip_bytes[2] + " ip[3] = "
						+ ip_bytes[3]);
			}
			payLoad.write(ip_bytes, 0, 4);
			offset += 4;
			// Speed, set arbitrarily to 10000
			tmp = SimpellaUtils.toBytes(10000);
			payLoad.write(tmp, 0, 4);
			offset += 4;

			while (itr.hasNext()) {
				Integer fileIndex = (Integer) itr.next();
				Long size = (Long) itr.next();
				String filename = (String) itr.next();

				tmp = SimpellaUtils.toBytes(fileIndex.intValue());
				payLoad.write(tmp, 0, 4);
				offset += 4;

				tmp = SimpellaUtils.toBytes(size.intValue());
				payLoad.write(tmp, 0, 4);
				offset += 4;

				payLoad.write((filename + '\0').getBytes(), 0,
						filename.length() + 1);
				offset += filename.length() + 1;
			}
			byte[] serventID = SimpellaConnectionStatus.servent_UUID;
			payLoad.write(serventID, 0, 16);
			offset += 16;
			byte[] payLoadArray = new byte[offset];
			payLoadArray = payLoad.toByteArray();
			DataOutputStream outToClient;
			byte[] queryHitHeaderBytes;
			byte[] queryHPacket;
			try {
				outToClient = new DataOutputStream(
						sessionSocket.getOutputStream());

				queryHitHeader.setPayLoadLength(offset);
				queryHitHeaderBytes = queryHitHeader.getHeader();
				if (Simpella.debug) {
					System.out
							.println("offset in int " + offset + " 0:1:2:3 "
									+ queryHitHeaderBytes[19]
									+ queryHitHeaderBytes[20]
									+ queryHitHeaderBytes[21]
									+ queryHitHeaderBytes[22]);
					for (int k = 0; k < payLoadArray.length; k++) {
						System.out.println("QueryHit: payLoadArray[" + k
								+ "] = " + payLoadArray[k]);
					}
				}

				queryHPacket = SimpellaHeader.getSimpellaPacket(
						queryHitHeaderBytes, payLoadArray);

				outToClient.write(queryHPacket);
			} catch (IOException e) {
				System.out.println("Connection has closed abruptly, cannot sent Query-hit, closing connection");
				return;
			}
			//set bit and pack for info command
			stats = SimpellaConnectionStatus.getBySocket(sessionSocket);
			if (null != stats) {
				stats.setSentBytes(payLoadArray.length + queryHitHeaderBytes.length);
				stats.setSentPacks();
				SimpellaConnectionStatus.setTotalBytesSent(payLoadArray.length+queryHitHeaderBytes.length);
				SimpellaConnectionStatus.setTotalPacketsSent();
			}
		}
	}
	
	public void sendPing(Socket clientSocket)
	{
		SimpellaHeader pingH = new SimpellaHeader();
		pingH.setMsgType("ping");
		pingH.initializeHeader();
		pingH.setMsgId();
		String guid = SimpellaRoutingTables.guidToString(pingH.getHeader());
		SimpellaRoutingTables.generatedPingList.add(guid);
		if(Simpella.debug) {
			System.out.println("Pinged with Header = " + Arrays.toString(pingH.getHeader()));
		}
		DataOutputStream outToServer;
		try {
			outToServer = new DataOutputStream(
					clientSocket.getOutputStream());
		
			outToServer.write(pingH.getHeader());
		} catch (IOException e) {
			System.out.println("Connection has closed abruptly cannot sent Ping, closing connection");
			return;
		}
		//set bit and pack for info command
		stats = SimpellaConnectionStatus.getBySocket(clientSocket);
		if(null!=stats){
			stats.setSentBytes(pingH.getHeader().length);
			stats.setSentPacks();
			SimpellaConnectionStatus.setTotalBytesSent(pingH.getHeader().length);
			SimpellaConnectionStatus.setTotalPacketsSent();
		}
		return;
	}

}
