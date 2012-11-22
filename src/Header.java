


public class Header {
	byte[] header;
	String msgType;
	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public byte[] getHeader() {
		return header;
	}

	public void setHeader(byte[] header) {
		this.header = header;
	}

	public Header(){
		header = new byte[22];
	}
	
	public void initializeHeader(){
		int i;
		for(i=0;i<8;i++){
			header[i]=0;
		}
		header[8]=(byte) 0xff;//for all bits to be true
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
		for(i=9;i<15;i++){
			header[i]=0;
		}
		header[15]=0;
		header[17]=7;
		header[18]=0;
		for(i=19;i<=21;i++){
			header[i]=0;
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
		for(int i=9;i<=15;i++){
			header[i]=bytes[i-9];
		}
	}
	
}
