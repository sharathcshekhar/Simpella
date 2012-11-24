import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
//TODO to be named SimpellaHeader.java



public class Header {
	byte[] header;
	String msgType;
	public String getMsgType() {
		return msgType;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] header) {
		this.header = header;
	}
	
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

	public Header(){
		header = new byte[23];
	}
	
	public void setTTL(int ttl){
		header[17]=(byte) ttl;
	}
	
	public byte getTTL(){
		return header[17];
	}
	
	public void setHop(int hop){
		header[18]=(byte)hop;
	}
	
	public byte getHop(){
		return header[18];
	}
	
	
	public void setPayLoadLength(byte[] plen){
		header[19] = plen[0];
		header[20] = plen[1];
	}
	
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
	 * @param header the new msg id
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
	 * @param header the new msg id
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
	
	public void setPongPayload(Socket clientSocket,byte[] payload,byte[] files, byte[] kBs){
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.BIG_ENDIAN); 
		b.putInt(clientSocket.getLocalPort());
		byte[] payload_port = new byte[2]; 
		//TODO Endianness validation
		payload_port = b.array();
		payload[23] = payload_port[2];
		payload[24] = payload_port[3];
		
		byte[] ipAddr = new byte[4]; 
		ipAddr = clientSocket.getInetAddress().getAddress();
		payload[28] = ipAddr[0];
		payload[27] = ipAddr[1];
		payload[26] = ipAddr[2];
		payload[25] = ipAddr[3];
		
		/*//Number of files shared - 4 bytes
		payload[29] = files[0];
		payload[30] = files[1];
		payload[31] = files[2];
		payload[32] = files[3];*/
		
		/*//Number of kilobytes shared - 4bytes
		payload[33] = kBs[0];
		payload[34] = kBs[1];
		payload[35] = kBs[2];
		payload[36] = kBs[3];*/
	}
	
	public void setQueryPayload(byte[] payload, byte[] speed, String search){
		//TODO size and payload validation
		payload[24]=speed[0];
		payload[23]=speed[1];
		//payload[25]=search.getBytes();
	}
	//TODO
	public void setQueryHitPayload(){
	
	}

}
