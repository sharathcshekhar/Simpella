import java.net.Socket;



// TODO: Auto-generated Javadoc
/**
 * The Class Header.
 */
public class SimpellaHeader {
	
	/** The header. */
	byte[] header;
	
	/** The msg type. */
	String msgType;
	
	/**
	 * Gets the msg type.
	 *
	 * @return the msg type
	 */
	public String getMsgType() {
		return msgType;
	}

	/**
	 * Gets the header.
	 *
	 * @return the header
	 */
	public byte[] getHeader() {
		return header;
	}

	/**
	 * Sets the header.
	 *
	 * @param header the new header
	 */
	public void setHeader(byte[] header) {
		this.header = header;
	}
	

	public void setMsgId(byte[] headerToSet) {
		if (headerToSet.length < 16) {
			System.out.println("Header too small to set guid");
			return;
		}
		for(int i = 0; i < 16; i++) {
			this.header[i] = headerToSet[i];
		}
	}
	

	/**
	 * Sets the msg type.
	 *
	 * @param msgType the new msg type
	 */
	public void setMsgType(String msgType) {
		this.msgType = msgType;
		if(msgType.equalsIgnoreCase("pong")){
			header[16]=(byte)0x01;
		}
		else if(msgType.equalsIgnoreCase("query")){
			header[16]=(byte)0x80;
		}
		else if(msgType.equalsIgnoreCase("queryhit")){
			header[16]=(byte)0x81;
		}
		else{
			header[16]=(byte)0x00;//default ping
		}
	}


	public SimpellaHeader(){
	/**
	 * Instantiates a new header.
	 */
		header = new byte[23];
	}
	
	/**
	 * Sets the ttl.
	 *
	 * @param ttl the new ttl
	 */
	public void setTTL(int ttl){
		header[17]=(byte) ttl;
	}
	
	/**
	 * Gets the ttl.
	 *
	 * @return the ttl
	 */
	public byte getTTL(){
		return header[17];
	}
	
	/**
	 * Sets the hop.
	 *
	 * @param hop the new hop
	 */
	public void setHop(int hop){
		header[18]=(byte)hop;
	}
	
	/**
	 * Gets the hop.
	 *
	 * @return the hop
	 */
	public byte getHop(){
		return header[18];
	}
	
	
	/**
	 * Sets the pay load length.
	 *
	 * @param plen the new pay load length
	 */
	public void setPayLoadLength(int plen){
		byte[] plenb =	SimpellaUtils.toBytes(plen);
		header[22] = plenb[3];
		header[21] = plenb[2];
		header[20] = plenb[1];
		header[19] = plenb[0];
	}
	
	/**
	 * Initialize header.
	 */
	public void initializeHeader(){
		int i;
		for(i=0;i<8;i++){
			header[i]=0;
		}
		header[8]=(byte) 0xff;//for all bits to be true
		
		header[16]=(byte)0x00;//default ping
		
		for(i=9;i<15;i++){
			header[i]=0;
		}
		header[15]=0;
		header[17]=7; //TTL
		header[18]=0; //Hopes
		
		/* set pay load length to 0 */
		for(i=19;i<=22;i++){
			header[i]=0;
		}
	}
	

	/**
	 * Sets the msg id.
	 *
	 * @param id the id
	 */
	public void copyMsgId(byte[] id){
		for(int i=0;i<8;i++){
		header[i]=id[i];
		}
		for(int i=9;i<15;i++){
			header[i]=id[i-9];
		}
	}
	
	/**
	 * Sets the msg id.
	 *
	 */
	public void setMsgId(){
		byte[] bytes = new byte[8];
		SimpellaUtils.getR(bytes);
		for(int i=0;i<8;i++){
		header[i]=bytes[i];
		}
		SimpellaUtils.getR(bytes);
		for(int i=9;i<15;i++){
			header[i]=bytes[i-9];
		}
	}
		
	/**
	 * Sets the pong payload.
	 *
	 * @param clientSocket the client socket
	 * @param payload the payload
	 */
	public void setPongPayload(Socket clientSocket, byte[] payload){
		
		byte[] payload_port = SimpellaUtils.toBytes(SimpellaConnectionStatus.simpellaNetPort);
		
		payload[24] = payload_port[3];
		payload[23] = payload_port[2];
		System.out.println("Setting port no. to  0:1 " + 
				payload_port[2] + payload_port[3]) ;
		
		byte[] ipAddr = new byte[4]; 
		ipAddr = clientSocket.getLocalAddress().getAddress();
		payload[28] = ipAddr[3];
		payload[27] = ipAddr[2];
		payload[26] = ipAddr[1];
		payload[25] = ipAddr[0];
		
		SimpellaFileShareDB fileDb = new SimpellaFileShareDB();
		fileDb.scanSharedDirectory();
		System.out.println("replying with Pong, No of files = " + fileDb.getNoOfFiles() 
				+ " Size = " + fileDb.getSizeOfFiles() + " bytes");
		byte[] fileShared = SimpellaUtils.toBytes(fileDb.getNoOfFiles());
		byte[] fileSize = SimpellaUtils.toBytes(fileDb.getSizeOfFiles());
		
		payload[29] = fileShared[0];
		payload[30] = fileShared[1];
		payload[31] = fileShared[2];
		payload[32] = fileShared[3];
		
		payload[33] = fileSize[0];
		payload[34] = fileSize[1];
		payload[35] = fileSize[2];
		payload[36] = fileSize[3];
		
	}
	//System.arraycopy(src, src pos, dest, dest pos, len)
	public static byte[] getSimpellaPacket(byte[] header, byte[] payLoad) {
		byte[] packet = new byte[header.length + payLoad.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(payLoad, 0, packet, header.length, payLoad.length);
		return packet;
	}
	
	public byte[] getSimpellaPacket(byte[] payLoad) {
		byte[] packet = new byte[header.length + payLoad.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(payLoad, 0, packet, header.length, payLoad.length);
		return packet;
	}
}
