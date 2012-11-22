import java.io.File;
import java.util.ArrayList;

/**
 * 
 */

/**
 * @author sharath
 *
 */
public class SimpellaFileShareDB {
	String sharedDirectory = "/home/sharath/Downloads";
	ArrayList<FileInfo> FileDb;
	public void scanSharedDirectory(String sharedDir)
	{
		File dir = new File(sharedDir);
		String[] files = dir.list();
		for (int i = 0; i < dir.length(); i++){
			File filename = new File(files[i]);
			if(filename.isDirectory()) {
				scanSharedDirectory(filename.toString());
			} else {
				//TODO maintain the below info in a table
				//filename:path:size:index:custom-info
				System.out.println("File scanned = " + filename.toString() + 
						"size = " + filename.length() + 
						"full path = " + filename.getAbsolutePath());
			}
		}
}

	public String getFileInfoByname()
	{
		return null;
	}
	
	public boolean isFilePresent(String keys)
	{
		//TODO return true if any of the keys match the name of the file
		return false;
	}
	
	public void shareDirectory(String dir)
	{
		
	}
}

class FileInfo {
	String name;
	int index;
	String path;
	int size;
}