import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SimpellaHandleMsg {

	public void handleMsg(byte[] header, Socket sessionSocket) throws Exception {

		DataInputStream inFromClient = new DataInputStream(
				sessionSocket.getInputStream());
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
			System.out.println("Pong received with payload " + payLoadLen);
			
			byte[] pongPayLoad = new byte[14];
			int len = inFromClient.read(pongPayLoad, 0, 14);
			
			if (len != payLoadLen) {
				System.out.println("Something has gone wrong!");
				return;
			}
			System.out.println(len + " bytes of pong payload read");
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
				String ip = InetAddress.getByAddress(pong_tmp_buf).getHostAddress();
				
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
				// Initiate connections depending on the results
				
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
						pongToClient.write(header, 0, 23);
						pongToClient.write(pongPayLoad);
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
				inFromClient.skipBytes(payLoadLen);
				return;
			}
			// consume payLoadLen amount of data irrespective of it belongs to you or not!
			byte[] queryPayLoad = new byte[payLoadLen];
			int len = inFromClient.read(queryPayLoad, 0, payLoadLen);
			System.out.println("Query-message");
			String queryid = SimpellaRoutingTables.guidToString(header);
			if (SimpellaRoutingTables.QueryTable.containsKey(queryid) || // seen query before
					SimpellaRoutingTables.generatedQueryList.contains(queryid)) { // my own query
				System.out.println("Query message seen before or self query, ignoring");
				
				return;
			} else {
				SimpellaRoutingTables.insertQueryTable(queryid, sessionSocket);
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
				System.out.println("Search String is " + searchString);
				/*
				 * Crude way of setting GUID 
				 * TODO make it more elegant
				 */
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
			// TODO handle message
			byte[] qHit_tmp_buffer = new byte[4];
			qHit_tmp_buffer[0] = header[19];
			qHit_tmp_buffer[1] = header[20];
			qHit_tmp_buffer[2] = header[21];
			qHit_tmp_buffer[3] = header[22];
			int payLoadLen = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
			
			byte[] queryHitPayLoad = new byte[payLoadLen];
			int len = inFromClient.read(queryHitPayLoad, 0, payLoadLen);
			
			if(Simpella.debug) {
			System.out.println("Query-hit message received with payload len = "
					+ payLoadLen + "bytes read = " + len);
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
				msg.read(ip_address);
				msg.read(qHit_tmp_buffer);
				int speed = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
				if (Simpella.debug) {
					System.out.println("no of files " + no_of_files
							+ " port_num = " + port_no + " ip = "
							+ InetAddress.getByAddress(ip_address).getHostAddress()
							+ " Speed = " + speed);
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
					queryHitRes.setIpAddress(InetAddress.getByAddress(ip_address).getHostAddress());
					queryHitRes.setPort(port_no);
					
					msg.read(qHit_tmp_buffer);
					int file_index = SimpellaUtils.byteArrayToInt(qHit_tmp_buffer);
					bytes_read += 4;
					queryHitRes.setFile_index(file_index);

					msg.read(qHit_tmp_buffer);
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
				msg.read(serventID); //no use of this

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
					queryHitToClient.write(header, 0, 23);
					queryHitToClient.write(queryHitPayLoad);
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
	public void broadcastPing(byte[] pingMsg, Socket sender)
			throws Exception {
		Socket clientSocket = null;
		String clientIP = "";
		System.out.println("In broadcast ping");
		for (int i = 0; i < 3; i++) {

			clientSocket = SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.incomingConnectionList[i].remotePort;
/*			if (!(clientIP.equals(""))
					&& !((sender.getInetAddress().getHostAddress()
							.equals(clientIP)) && (sender.getPort() == clientPort))) { */
			if (!clientIP.equals("")) {
				if ((sender == null)
						|| !((sender.getInetAddress().getHostAddress()
								.equals(clientIP)) && (sender.getPort() == clientPort))) {
					/* send to everyone apart from this node */
					System.out.println("sending ping to IP: " + clientIP
							+ " Port = " + clientPort);
					DataOutputStream outToServents = new DataOutputStream(
							clientSocket.getOutputStream());
					outToServents.write(pingMsg);
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
					System.out.println("sending ping to IP: " + clientIP
							+ " Port = " + clientPort);
					/* send to everyone apart from this node */
					DataOutputStream outToServents = new DataOutputStream(
							clientSocket.getOutputStream());
					outToServents.write(pingMsg);
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
	 * @throws Exception
	 *        static     the exception
	 */
	public void broadcastQuery(byte[] header, byte[] queryPayLoad,
			Socket sender) throws Exception {
		Socket clientSocket = null;
		String clientIP = "";
		
		System.out.println("In broadcast query with packet Length = " + queryPayLoad.length);
		if(Simpella.debug) {
			for(int j = 0; j < queryPayLoad.length; j++) {
				System.out.println("broadcast: Query Payload["+j+"] = " + queryPayLoad[j]);
			}
		}
		for (int i = 0; i < 3; i++) {
			System.out.println("In broadcast incoming " + i);
			clientSocket = SimpellaConnectionStatus.incomingConnectionList[i].sessionSocket;
			clientIP = SimpellaConnectionStatus.incomingConnectionList[i].remoteIP;
			int clientPort = SimpellaConnectionStatus.incomingConnectionList[i].remotePort;

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

					System.out.println("broadcast outgoing " + clientIP
							+ " port = " + clientPort);
					/*
					 * send to everyone apart from this node If sender is null,
					 * send it to all
					 */
					DataOutputStream outToServents = new DataOutputStream(
							clientSocket.getOutputStream());
					outToServents.write(header);
					outToServents.write(queryPayLoad);
					
				}
			}
		}
	}

	public void replyWithQueryHit(Socket sessionSocket,
			String searchString, byte[] queryHeader) throws IOException {
		
		SimpellaHeader queryHitHeader = new SimpellaHeader();
		queryHitHeader.initializeHeader();
		// retain the msgID in query header in the query-hit header
		queryHitHeader.setMsgId(queryHeader);
		queryHitHeader.setMsgType("queryhit");
		SimpellaFileShareDB db = new SimpellaFileShareDB();
		
		System.out.println("In replyWithQuery, searchString " + searchString);
		ArrayList<Object> searchResults = db.getMatchingFiles(searchString);
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
		
		Iterator<Object> itr = searchResults.iterator();
		ByteArrayOutputStream payLoad = new ByteArrayOutputStream();

		if (!searchResults.isEmpty()) {
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
			// TODO this has to be a constant value stored in
			// a static variable somewhere.
			byte[] serventID = new byte[16];
			payLoad.write(serventID, 0, 16);
			offset += 16;
			byte[] payLoadArray = new byte[offset];
			payLoadArray = payLoad.toByteArray();
			DataOutputStream outToClient = new DataOutputStream(
					sessionSocket.getOutputStream());
			queryHitHeader.setPayLoadLength(offset);
			byte[] queryHitHeaderBytes = queryHitHeader.getHeader();
			/*
			byte[] payLoadLength = SimpellaUtils.toBytes(offset);
			queryHitHeaderBytes[19] = payLoadLength[0];
			queryHitHeaderBytes[20] = payLoadLength[1];
			queryHitHeaderBytes[21] = payLoadLength[2];
			queryHitHeaderBytes[22] = payLoadLength[3];
			*/
			if(Simpella.debug) {
				System.out.println("offset in int " + offset + " 0:1:2:3 " +  queryHitHeaderBytes[19] +
						queryHitHeaderBytes[20] +  queryHitHeaderBytes[21] +  queryHitHeaderBytes[22]);
				for (int k = 0; k < payLoadArray.length; k++) {
					System.out.println("QueryHit: payLoadArray[" + k + "] = "
							+ payLoadArray[k]);
				}
			}
			// write header
			outToClient.write(queryHitHeaderBytes, 0, 23);
			// write payload
			outToClient.write(payLoadArray, 0, offset);
		}
	}
	
	public void sendPing(Socket clientSocket) throws Exception
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
		DataOutputStream outToServer = new DataOutputStream(
				clientSocket.getOutputStream());
		outToServer.write(pingH.getHeader());
		return;
	}

}
