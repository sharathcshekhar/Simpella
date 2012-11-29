import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;



// TODO: Auto-generated Javadoc
/**
 * The Class Header.
 */
public class Header {
	
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

	/**
	 * Instantiates a new header.
	 */
	public Header(){
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
	public void setPayLoadLength(byte[] plen){
		header[22] = plen[0];
		header[21] = plen[1];
		header[20] = plen[2];
		header[19] = plen[3];
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
		header[17]=7;
		header[18]=0;
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
	 * Sets the numof files shared.
	 *
	 * @param numF the new numof files shared
	 */
	public void setNumofFilesShared(int numF){
		byte[] numFiles = new byte[4];
		numFiles = SimpellaUtils.toBytes(numF);
		header[32]=numFiles[0];
		header[31]=numFiles[1];
		header[30]=numFiles[2];
		header[29]=numFiles[3];
	}
	
	/**
	 * Gets the numof files shared.
	 *
	 * @return the numof files shared
	 */
	public int getNumofFilesShared(){
		byte[] files = new byte[4];
		files[0]=header[32];
		files[1]=header[31];
		files[2]=header[30];
		files[3]=header[29];
		int result = SimpellaUtils.byteArrayToInt(files);
		return result;
	}
	
	public void setKbsShared(int kbF){
		byte[] numFiles = new byte[4];
		numFiles = SimpellaUtils.toBytes(kbF);
		header[36]=numFiles[0];
		header[35]=numFiles[1];
		header[34]=numFiles[2];
		header[33]=numFiles[3];
	}
	
	public int getKbsShared(){
		byte[] files = new byte[4];
		files[0]=header[36];
		files[1]=header[35];
		files[2]=header[34];
		files[3]=header[33];
		int result = SimpellaUtils.byteArrayToInt(files);
		return result;
	}
	
	/**
	 * Sets the pong payload.
	 *
	 * @param clientSocket the client socket
	 * @param payload the payload
	 */
	public void setPongPayload(Socket clientSocket,byte[] payload){
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.BIG_ENDIAN); 
		b.putInt(clientSocket.getLocalPort());
		byte[] payload_port = new byte[4]; 
		//TODO Endianness validation
		payload_port = b.array();
		payload[24] = payload_port[3];
		payload[23] = payload_port[2];
		
		byte[] ipAddr = new byte[4]; 
		ipAddr = clientSocket.getInetAddress().getAddress();
		payload[28] = ipAddr[3];
		payload[27] = ipAddr[2];
		payload[26] = ipAddr[1];
		payload[25] = ipAddr[0];
	}
	
	/**
	 * Sets the query payload.
	 *
	 * @param payload the payload
	 * @param speed the speed
	 * @param search the search
	 */
	public void setQueryPayload(byte[] payload, byte[] speed, String search){
		//TODO size and payload validation
		payload[24]=speed[0];
		payload[23]=speed[1];
		//payload[25]=search.getBytes();
	}
	//TODO
	public void setQueryHitPayload(Hashtable<Integer, String> searchResults) {
		//byte[] payload;
		/*
		 * payload[0] = no. of files matched
		 * payload[1-2] = port
		 * payload[3-6] = ip
		 * payload[7 - 10] = speed = 10,000
		 * FOR ALL files
		 * payload[11 - 14] = file index
		 * payload[15 - 18] = file_size
		 * payload[18 - ] = file name 
		 * payload[last 16 byte] = unique ID of the servent
		 */
	}
	

}
