import java.security.SecureRandom;
import java.util.Random;


public class SimpellaUtils {
	static Random r = new SecureRandom();
	public static void getR(byte[] head) {
		 r.nextBytes(head);
	}
	public static void setR(Random r) {
		SimpellaUtils.r = r;
	}
}
