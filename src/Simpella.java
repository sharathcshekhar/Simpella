import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 */

/**
 * @author sharath
 * 
 */
public class Simpella {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		SimpellaNetServer NetSrv = new SimpellaNetServer();
		int port = 6346;
		if(args.length == 1) {
			port = Integer.parseInt(args[0]);
		}
		SimpellaConnectionStatus.ConnectionStatusInit();
		//TODO take second argument to be file server
		NetSrv.setPort(port);
		NetSrv.start();
		SimpellaCommands cmd = new SimpellaCommands();

		BufferedReader cmdFromUser = new BufferedReader(new InputStreamReader(
				System.in));
		// CLI begins
		while (true) {
			System.out.print("Simpella> ");
			String usrInput = null;
			try {
				// trim() deletes leading and trailing whitespace
				usrInput = cmdFromUser.readLine().trim();
			} catch (IOException e) {
				System.out.println("Cannot parse command, please try again");
				continue;
			}

			if (usrInput.length() == 0) {
				continue;
			}
			// This regex ignores whitespace between words
			String[] cmd_args = usrInput.split("\\s+");

			if (cmd_args[0].equals("open")) {
				System.out.println("open command");
				cmd.setConnectionIP(cmd_args[1]);
				cmd.setConnectionPort(Integer.parseInt(cmd_args[2]));
				cmd.connect();
			} else if(cmd_args[0].equals("download")){
				try {
					int num = Integer.parseInt(cmd_args[1]);
					SimpellaFileClient sc= new SimpellaFileClient();
					sc.download(num);
				} catch (NumberFormatException e) {
					System.out.println("Enter proper number");
				}
				
			} else if(cmd_args[0].equals("find")){
				cmd.find(cmd_args[1]);
			} else {
				//TODO implement quit/bye
				//TODO implement other commands
				System.out.println("Command not yet implemented!");
			}
		}
	}
}