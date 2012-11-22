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
		int port = Integer.parseInt(args[0]);

		NetSrv.setPort(port);
		NetSrv.start();
		SimpellaCommads cmd = new SimpellaCommads();

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
				cmd.setConnectionIP("127.0.0.1");
				cmd.setConnectionPort(Integer.parseInt(cmd_args[1]));
				cmd.connect();
			} else {
				System.out.println("Command not yet implemented!");
			}
		}
	}
}