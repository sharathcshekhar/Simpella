import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.UUID;


public class SimpellaUtils {
	static Random r = new SecureRandom();
	public static void getR(byte[] head) {
		 r.nextBytes(head);
	}
	
	public static void setR(Random r) {
		SimpellaUtils.r = r;
	}

	/*
	 * Generate UUID as a function of IP address and port
	 */
	public static byte[] generateServentID() {
		byte[] uuid = new byte[16];
		UUID servantID = UUID.nameUUIDFromBytes(
				(Simpella.LOCAL_IP + SimpellaConnectionStatus.simpellaNetPort).getBytes());
				
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 DataOutputStream dos = new DataOutputStream(baos);
		 try {
			dos.writeLong(servantID.getMostSignificantBits());
			dos.writeLong(servantID.getLeastSignificantBits());
			dos.flush(); // May not be necessary
		 } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 uuid = baos.toByteArray();
		 return uuid;
	}

	
	/**
	 * Byte array to int.
     * Byte array should be in Big Endian 
     * 
     * @param b the byte
     * @return the int
     */
   public static int byteArrayToInt(byte[] b)
   {
       return  b[3] & 0xFF |
               (b[2] & 0xFF) << 8 |
               (b[1] & 0xFF) << 16 |
               (b[0] & 0xFF) << 24;
   }
   
   /**
    * To bytes in Big Endian format.
    *
    * @param i the i
    * @return the byte[]
    */
   public static byte[] toBytes(int i)
   {
     byte[] result = new byte[4];

     result[0] = (byte) (i >> 24);
     result[1] = (byte) (i >> 16);
     result[2] = (byte) (i >> 8);
     result[3] = (byte) (i); /* i >> 0 */

     return result;
   }
   
   	public static String memFormat(long mem) {
	    if(mem <= 0) return "0";
	    else if(mem<1024){
	    	return Long.toString(mem);
	    }
	    else{	
	    final String[] units = new String[] {"B", "K", "M", "G"};
	    int digits = (int) (Math.log10(mem)/Math.log10(1024));
	    return new DecimalFormat("#,##0.##").format(mem/Math.pow(1024, digits)) + "" + units[digits];
	    }
	}
}
