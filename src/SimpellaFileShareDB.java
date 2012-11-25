import java.io.File;
import java.util.Hashtable;

/**
 * 
 */

/**
 * @author sharath
 *
 */
public class SimpellaFileShareDB {
	String sharedDirectory = "/home/sharath/Downloads";
	int noOfFiles = 0;
	long sizeOfFiles = 0;
	
	public String getSharedDirectory() {
		return sharedDirectory;
	}

	public int getNoOfFiles() {
		return noOfFiles;
	}

	public long getSizeOfFiles() {
		return sizeOfFiles;
	}
	
	public void setSharedDirectory(String dir)
	{
		//TODO if relative path, use pwd:dir
		this.sharedDirectory = dir;
	}
	
	public void scanSharedDirectory() {
		recurssiveScanDir(sharedDirectory);
	}
	
	public Hashtable<Integer, String> getMatchingFiles(String pattern)
	{
		String[] keys = pattern.split(" ");
		Hashtable<Integer, String> results = recurssiveFileSearch(keys, sharedDirectory);
		return results;
	}
	
	public String getFullFilePath(String filename, int hashcode) {
		return recurssiveGetFile(sharedDirectory, filename, hashcode);
	}

	/*Private helper functions*/
	
	void recurssiveScanDir(String directory)
	{
		File dir = new File(directory);
		String[] files = dir.list();
		for (int i = 0; i < dir.length(); i++){
			File filename = new File(directory, files[i]);
			if(filename.isDirectory()) {
				recurssiveScanDir(filename.toString());
			} else {
				//TODO maintain the below info in a table
				//filename:path:size:index:custom-info
				System.out.println("File scanned = " + filename.getName() + 
						"size = " + filename.length() + 
						"full path = " + filename.getAbsolutePath());
				noOfFiles ++;
				sizeOfFiles = sizeOfFiles + filename.length();
			}
		}
	}

	
	String recurssiveGetFile(String rootDirectory, String filename, int hashcode) {
		File dir = new File(rootDirectory);
		String[] filesInDir = dir.list();
	
		for (String tmpFileName : filesInDir) {
			File file = new File(rootDirectory, tmpFileName);
			if(file.isDirectory()) {
				String result = recurssiveGetFile(file.getAbsolutePath(), filename, hashcode);
				if(result != null)
					return result;
			} else {
				if(file.getAbsolutePath().equals(filename)) {
					if(file.hashCode() == hashcode) {
						return file.getAbsolutePath();
					}
				}
			}
		}
		return null;
	}

	Hashtable<Integer, String> recurssiveFileSearch(String[] keys, String rootDir) {
		File dir = new File(rootDir);
		String[] filesInDir = dir.list();
		Hashtable<Integer, String> results = new Hashtable<Integer, String>();
		for (int i = 0; i < filesInDir.length; i++){
			File file = new File(rootDir, filesInDir[i]);
			if(file.isDirectory()) {
				Hashtable<Integer, String> tmp = recurssiveFileSearch(keys, file.getAbsolutePath());
				results.putAll(tmp);
			} else {
				for(String s: keys) {
					if(file.getName().contains(s)) {
						results.put(file.hashCode(), file.getName());
					}
				}
			}
		}
		return results;
	}
}