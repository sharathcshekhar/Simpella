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
	static String sharedDirectory = null;
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
		sharedDirectory = dir;
	}
	
	public void scanSharedDirectory() {
		if(sharedDirectory == null) {
			System.out.println("Scan request received, no direcotry shared");
			return;
		}
		recurssiveScanDir(sharedDirectory);
	}
	
	public ArrayList<Object> getMatchingFiles(String pattern)
	{
		String[] keys = pattern.split("[\\s\\-_\\.]+");
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
	/* Will return null if the file index/filename pair does not match */
	public String getFullFilePath(String filename, int hashcode) {
		return recurssiveGetFile(sharedDirectory, filename, hashcode);
	}

	/*Private helper functions*/
	
	void recurssiveScanDir(String directory) {
		if (directory == null) {
			System.out.println("No directory is shared!");
			return;
		}

		File dir = new File(directory);
		String[] files = dir.list();
		/* if files are null, directory is empty */
		if(files == null ) {
			return;
		}
		for (int i = 0; i < files.length; i++) {
			File filename = new File(directory, files[i]);
			if (filename.isDirectory()) {
				recurssiveScanDir(filename.toString());
			} else {
				if (Simpella.debug) {
					System.out.println("File scanned = " + filename.getName()
							+ " size = " + filename.length() + " full path = "
							+ filename.getAbsolutePath());
				}
				noOfFiles++;
				sizeOfFiles = sizeOfFiles + (int) filename.length();
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
					return result; // else continue on the next iteration
			} else {
				if(file.getName().equals(filename)) {
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
		ArrayList<Object> results = new ArrayList<Object>();
		boolean isFileFound = false;
				
		for (int i = 0; i < filesInDir.length; i++){
			isFileFound = false;
			File file = new File(rootDir, filesInDir[i]);
			if(file.isDirectory()) {
				if (Simpella.debug) {
					System.out.println("Recurssing the directory");
				}
				//Hashtable<Integer, String> tmp = recurssiveFileSearch(keys, file.getAbsolutePath());
				ArrayList<Object> tmp = recurssiveFileSearch(keys, file.getAbsolutePath());
				results.addAll(tmp);
			} else {
				String[] splitFileName = file.getName().split("[\\s\\-_\\.]+");
				for(String s : keys) {
					if (Simpella.debug) {
						System.out.println("ELSE: Checking if filename " + file.getName() + " contains " 
							+ s + " result = " + file.getName().contains(s.trim()));
					}
					for (String s2 : splitFileName) {
						if (Simpella.debug) {
							System.out.println("Checking if filename keyword " + s2);
						}
						if (s2.equalsIgnoreCase(s.trim())) {
							results.add(file.hashCode());
							results.add(file.length());
							results.add(file.getName());
							isFileFound = true;
							break; //if file is already added, move to next file.
						}
					}
					if(isFileFound) {
						break; //if file is already added, move to next file.
					}
				}
			}
		}
		return results;
	}
}