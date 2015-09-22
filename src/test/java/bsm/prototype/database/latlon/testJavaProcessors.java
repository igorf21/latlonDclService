package bsm.prototype.database.latlon;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;



public class testJavaProcessors {
	
	public static Map<String,String> decoderMap = new HashMap<String, String>();

	public static void main(String[] args) {
		//String encodedMsg = "0F05974D004D4404B204DE4444F1CC411A47AC1F58D0FD402CF8FE4C44444546EEDE4144F03CA8434B44E8FA444444444418414A43441F47AC1F1041CA44414444FD1C114B1A10424747485A86";
		//String decodedMsg = "0300407C403E0000A122011B09B21A5634A304C2A6AE020000050DEE3E0100A482B6080700E6AB000000000016010B08001A09B21A14012B00010000A31211071B140C090906";
		//String encodedMsg = "0F0187140194B93A13D0939974B2B93C127D92F94704B1490C289499129B76B91C3E999499444444381FB49999999999B9999980999995F6B699B99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999BFA5";
		//String decodedMsg = "01809C29450900A18D80932DA40DB01A51821053D701002D08AE80239F000100111111972B81000000000080000075000006BE8E00800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
		
	int a = Integer.decode("0x0d");
	int b = Integer.decode("0x00");
	int c = a | b;
	String binStr = Integer.toBinaryString(c);
	int d = Integer.parseInt(binStr,2);
		
		initDecoderMap();
		
		String encriptedMsg = "0F0187140194B93A13D0939974B2B93C127D92F94704B1490C289499129B76B91C3E999499444444381FB49999999999B9999980999995F6B699B99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999BFA5";
		String decriptedMsg = decript(encriptedMsg);		
		System.out.print(decriptedMsg);
	}

	public static void initDecoderMap() {

		decoderMap.put("0", "D");
		decoderMap.put("1", "A");
		decoderMap.put("2", "5");
		decoderMap.put("3", "1");
		decoderMap.put("4", "9");
		decoderMap.put("5", "E");
		decoderMap.put("6", "6");
		decoderMap.put("7", "2");
		decoderMap.put("8", "F");
		decoderMap.put("9", "8");
		decoderMap.put("A", "4");
		decoderMap.put("B", "0");
		decoderMap.put("C", "B");
		decoderMap.put("D", "C");
		decoderMap.put("E", "7");
		decoderMap.put("F", "3");
		
		
	}

	private static String decript(String encodedMsg) {
		
		String packet = encodedMsg.substring(10, encodedMsg.length() - 4);
		
		short iNybbleKey = 4;
		short maxDigit = 16;
		int packetLength = packet.length();
		
		String sShift = encodedMsg.substring(iNybbleKey, iNybbleKey + 1);
		int iShift = hexStringToSmallint('0' + sShift);
		
		String outPacket = "";
		
		for (int i = 0; i < packetLength; i++){
			
			String fromValue = packet.substring(0,1);
			String sToValue = convert(fromValue) ;
			
			int iToValue = hexStringToSmallint('0' + sToValue);
			iToValue = iToValue - iShift + maxDigit;
			iToValue = iToValue % maxDigit;
			sToValue = Integer.toString(iToValue);

					if (sToValue.equals("10"))
						sToValue = "A";
					else if (sToValue.equals("11"))
						sToValue = "B";
					else if (sToValue.equals("12"))
						sToValue = "C";
					else if (sToValue.equals("13"))
						sToValue = "D";
					else if (sToValue.equals("14"))
						sToValue = "E";
					else if (sToValue.equals("15"))
						sToValue = "F";			

			outPacket = outPacket + sToValue;
			packet = packet.substring(1,packet.length());
		}
		
		return outPacket;
		
	}

	private static String convert(String fromValue) {
		
		String toValue = decoderMap.get(fromValue);
		return toValue;
	}

	private static int hexStringToSmallint(String input) {
		
		String bStr = "";
		byte[] result = new byte[4];
		int len = input.length() / 2;

		for (int i = 0; i < len; i++){
			String tmp1 = input.substring( i * 2, i * 2 + 1).toLowerCase();
			int tmpRes1 = 0x00;
			int tmpRes2 = 0x00;
			String tmp2 = input.substring( i * 2 + 1, i * 2 + 2).toLowerCase();
		
			switch (tmp1) {
        	case "0":  tmpRes1 = Integer.decode("0x00");
                 break;
        	case "1":  tmpRes1 = Integer.decode("0x10;");
                 break;
        	case "2":  tmpRes1 = Integer.decode("0x20");
                 break;
        	case "3":  tmpRes1 = Integer.decode("0x30");
                 break;
        	case "4":  tmpRes1 = Integer.decode("0x40");
                 break;
        	case "5":  tmpRes1 = Integer.decode("0x50");
                 break;
        	case "6":  tmpRes1 = Integer.decode("0x60");
                 break;
        	case "7":  tmpRes1 = Integer.decode("0x70");
                 break;
        	case "8":  tmpRes1 = Integer.decode("0x80");
                 break;
        	case "9": tmpRes1 = Integer.decode("0x90");
        		 break;
        	case "a": tmpRes1 = Integer.decode("0xa0"); 
                 break;
        	case "b": tmpRes1 = Integer.decode("0xb0"); 
                 break;
        	case "c": tmpRes1 = Integer.decode("0xc0"); 
        		 break;
        	case "d": tmpRes1 = Integer.decode("0xd0"); 
        		 break;
        	case "e": tmpRes1 = Integer.decode("0xe0"); 
        		 break;
        	case "f": tmpRes1 = Integer.decode("0xf0"); 
                  break;
			}
		
			switch (tmp2) {
        	case "0": tmpRes2 = Integer.decode("0x00");
                 break;
        	case "1": tmpRes2 = Integer.decode("0x01");
                 break;
        	case "2": tmpRes2 = Integer.decode("0x02");
                 break;
        	case "3": tmpRes2 = Integer.decode("0x03");
                 break;
        	case "4": tmpRes2 = Integer.decode("0x04");
                 break;
        	case "5": tmpRes2 = Integer.decode("0x05");
                 break;
        	case "6": tmpRes2 = Integer.decode("0x06");
                 break;
        	case "7": tmpRes2 = Integer.decode("0x07");
                 break;
        	case "8": tmpRes2 = Integer.decode("0x08");
                 break;
        	case "9": tmpRes1 = Integer.decode("0x09");
                 break;
        	case "a": tmpRes2 = Integer.decode("0x0a");
                 break;
        	case "b": tmpRes2 = Integer.decode("0x0b");
                 break;
        	case "c": tmpRes2 = Integer.decode("0x0c");
        		 break;
        	case "d": tmpRes2 = Integer.decode("0x0d");
        		 break;
        	case "e": tmpRes2 = Integer.decode("0x0e");
        		 break;
        	case "f": tmpRes2 = Integer.decode("0x0f");
                 break;
			}
			
			bStr = bStr + Integer.toBinaryString(tmpRes1 | tmpRes2);
		}

		return Integer.parseInt(bStr, 2);
	}

}
