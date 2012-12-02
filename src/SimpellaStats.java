import java.net.Socket;

// TODO: Auto-generated Javadoc
/**
 * The Class SimpellaStats.
 */
public class SimpellaStats {
	
	/** The sent packs. */
	int sentPacks;
	
	/** The recvd packs. */
	int recvdPacks;
	
	/** The sent bytes. */
	int sentBytes;
	
	/** The recvd bytes. */
	int recvdBytes;
	
	/** The connection id. */
	int connectionId;
	
	/** The remote ip. */
	String remoteIP;
	
	/** The remote port. */
	int remotePort;
	
	/** The session socket. */
	Socket sessionSocket;
	
	/** The download status. */
	float downloadStatus = 0;
	
		/**
		 * Instantiates a new simpella stats.
		 */
		SimpellaStats(){
			this.sentPacks=0;
			this.recvdPacks=0;
			this.sentBytes=0;
			this.recvdBytes=0;
			this.connectionId=0;
			this.remoteIP="";
			this.remotePort=0;
			this.sessionSocket=null;
		}
	
	/**
	 * Gets the sent packs.
	 *
	 * @return the sent packs
	 */
	public int getSentPacks() {
		return sentPacks;
	}

	/**
	 * Sets the sent packs.
	 */
	public void setSentPacks() {
		this.sentPacks++;
	}

	/**
	 * Gets the recvd packs.
	 *
	 * @return the recvd packs
	 */
	public int getRecvdPacks() {
		return recvdPacks;
	}

	/**
	 * Sets the recvd packs.
	 *
	 */
	public void setRecvdPacks() {
		this.recvdPacks++;
	}

	/**
	 * Gets the sent bytes.
	 *
	 * @return the sent bytes
	 */
	public int getSentBytes() {
		return sentBytes;
	}

	/**
	 * Sets the sent bytes.
	 *
	 * @param sentBytes the new sent bytes
	 */
	public void setSentBytes(int sentBytes) {
		this.sentBytes+= sentBytes;
	}

	/**
	 * Gets the recvd bytes.
	 *
	 * @return the recvd bytes
	 */
	public int getRecvdBytes() {
		return recvdBytes;
	}

	/**
	 * Sets the recvd bytes.
	 *
	 * @param recvdBytes the new recvd bytes
	 */
	public void setRecvdBytes(int recvdBytes) {
		this.recvdBytes+= recvdBytes;
	}

	/**
	 * Gets the connection id.
	 *
	 * @return the connection id
	 */
	public int getConnectionId() {
		return connectionId;
	}

	/**
	 * Sets the connection id.
	 *
	 * @param connectionId the new connection id
	 */
	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	/**
	 * Gets the remote ip.
	 *
	 * @return the remote ip
	 */
	public String getRemoteIP() {
		return remoteIP;
	}

	/**
	 * Sets the remote ip.
	 *
	 * @param remoteIP the new remote ip
	 */
	public void setRemoteIP(String remoteIP) {
		this.remoteIP = remoteIP;
	}

	/**
	 * Gets the remote port.
	 *
	 * @return the remote port
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * Sets the remote port.
	 *
	 * @param remotePort the new remote port
	 */
	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	/**
	 * Gets the session socket.
	 *
	 * @return the session socket
	 */
	public Socket getSessionSocket() {
		return sessionSocket;
	}

	/**
	 * Sets the session socket.
	 *
	 * @param sessionSocket the new session socket
	 */
	public void setSessionSocket(Socket sessionSocket) {
		this.sessionSocket = sessionSocket;
	}
	

	/**
	 * Gets the download status.
	 *
	 * @return the download status
	 */
	public float getDownloadStatus() {
		return downloadStatus;
	}

	/**
	 * Sets the download status.
	 *
	 * @param downloadStatus the new download status
	 */
	public void setDownloadStatus(float downloadStatus) {
		this.downloadStatus = downloadStatus;
	}
}
