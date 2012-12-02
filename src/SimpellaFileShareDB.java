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
	static String sharedDirectory = "/home/sharath/simpella_share/share1";
	int noOfFiles = 0;
	int sizeOfFiles = 0;
	
	public String getSharedDirectory() {
		return sharedDirectory;
	}

	public int getNoOfFiles() {
		return noOfFiles;
	}

	public int getSizeOfFiles() {
		return sizeOfFiles;
	}
	
	public static void setSharedDirectory(String dir)
	{
		//TODO if relative path, use pwd:dir
		sharedDirectory = dir;
	}
	
	public void scanSharedDirectory() {
		recurssiveScanDir(sharedDirectory);
	}
	
	public ArrayList<Object> getMatchingFiles(String pattern)
	{
		String[] keys = pattern.split("\\s+");
		if(Simpella.debug) {
			System.out.println("In getMatchingFiles, string = " + pattern);
			for(String tmp : keys) {
				System.out.println("keys = " + tmp);
			}
		}
		//Hashtable<Integer, String> results = recurssiveFileSearch(keys, sharedDirectory);
		ArrayList<Object> results = recurssiveFileSearch(keys, sharedDirectory);
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
		for (int i = 0; i < files.length; i++){
			File filename = new File(directory, files[i]);
			if(filename.isDirectory()) {
				recurssiveScanDir(filename.toString());
			} else {
				if(Simpella.debug) {
					System.out.println("File scanned = " + filename.getName() + 
							" size = " + filename.length() + 
							" full path = " + filename.getAbsolutePath());
				}
				noOfFiles ++;
				sizeOfFiles = sizeOfFiles + (int)filename.length();
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

	ArrayList<Object> recurssiveFileSearch(String[] keys, String rootDir) {
		File dir = new File(rootDir);
		String[] filesInDir = dir.list();
		//Hashtable<Integer, String> results = new Hashtable<Integer, String>();
		ArrayList<Object> results = new ArrayList<Object>();
				
		for (int i = 0; i < filesInDir.length; i++){
			File file = new File(rootDir, filesInDir[i]);
			System.out.println("Checking filename " + file.getName());
			if(file.isDirectory()) {
				System.out.println("if dir");
				//Hashtable<Integer, String> tmp = recurssiveFileSearch(keys, file.getAbsolutePath());
				ArrayList<Object> tmp = recurssiveFileSearch(keys, file.getAbsolutePath());
				results.addAll(tmp);
			} else {
				System.out.println("Else.. ");
				for(String s : keys) {
					System.out.println("Checking if filename " + file.getName() + " contains " 
							+ s + " result = " + file.getName().contains(s.trim()));
					if(file.getName().contains(s.trim())) {
						//results.put(file.hashCode(), file.getName());
						results.add(file.hashCode());
						results.add(file.length());
						results.add(file.getName());
					}
				}
			}
		}
		return results;
	}
}