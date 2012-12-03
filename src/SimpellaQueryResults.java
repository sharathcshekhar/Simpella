public class SimpellaQueryResults {
	int file_index; //index returned by the results
	int file_size;
	String fileName;
	String ipAddress;
	int port;
	
	public int getFile_size() {
		return file_size;
	}
	public void setFile_size(int file_size) {
		this.file_size = file_size;
	}
	public int getFile_index() {
		return file_index;
	}
	public void setFile_index(int file_index) {
		this.file_index = file_index;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		if(obj == this) {
			return true;
		}
		SimpellaQueryResults result = (SimpellaQueryResults) obj;
		if(result.ipAddress.equals(ipAddress) 	&&
				result.port == port 			&&
				result.fileName.equals(fileName)&&
				result.file_size == file_size 	&&
				result.file_index == file_index) {
			return true;
		}
		return false;
		
	}
}
