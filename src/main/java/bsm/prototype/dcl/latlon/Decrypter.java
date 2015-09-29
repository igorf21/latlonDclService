package bsm.prototype.dcl.latlon;

// Main parsing is in PROCEDURE [dbo].[usp_i_MessageDecoder]
// Raw data is in dbo.MESSAGE_DECODER_QUEUE
// Decoder is in EXEC dbo.usp_s_DecryptPacket @rawPacket, @outPacket = @decryptedPacket OUTPUT;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsm.dcl.config.dal.DeviceConfigService;
import bsm.dcl.config.dal.entities.DataDefinition;
import bsm.dcl.config.dal.entities.UnitDataDefinition;

public class Decrypter {
	
	private DeviceConfigService deviceConfigService;
	
	// Various mapping objects for data decoding
	private Map<String,String> decoderMap = new HashMap<String, String>();
	private Map<String,DataDefinition> dataDefinitions;
	private Map<String, UnitDataDefinition> unitDataDefinitions;
	
	private static final Logger LOG = LoggerFactory.getLogger(Decrypter.class);

	@SuppressWarnings("unchecked")
	public String decryptMsg(Exchange exchange) throws Exception {
	
		// Initialize Decoder Map
		initDecoderMap();	// This needs to be moved to some sort of cashed object initialized on the start of this interface the 
		// Initialize Decoder Map
		
		Map<String, Object> record = exchange.getIn().getBody(Map.class);
		
		// Set data into header for processing during message decode
		exchange.setProperty("UNIT_ID",record.get("UNIT_ID"));
		exchange.setProperty("RECEIVE_DTTM",record.get("RECEIVE_DTTM"));
		exchange.setProperty("UNIT_ID",record.get("NETWORK"));

		String encryptedMsg = (String)record.get("RAW_PACKET");
		String decryptedMsg = decrypt(encryptedMsg);
		
		return decryptedMsg;
	}
	
	public void decodeMsg(Exchange exchange) throws Exception {
	
		String decryptedPkt = exchange.getIn().getBody(String.class);
		String unitId = (String)exchange.getProperty("UNIT_ID");
		Date receiveDttm = (Date)exchange.getProperty("RECEIVE_DTTM");
		String network = (String)exchange.getProperty("NETWORK");
		
		dataDefinitions = deviceConfigService.findAll();
		
		
		LOG.info("Number of definitions is: " + dataDefinitions.size());

		
		decode(decryptedPkt, network, unitId, receiveDttm);
		

	}
	
	private void decode(String decryptedPacket, String network, String unitId, Date receiveDttm) throws Exception {
		
		String ddIdKey = "";

		//DATA DEFINITIONS VARIABLES
		String valueName = null,lookupTable,bitSet,valueAlias,valueUnit,valueFunction,valueType,value,xmlOpen,xmlClose,uddName = null,eePromRowId,eePromRowData,timezoneRule,datePart,saveDecryptedPacket,rawDataLetter;
		int valueLength,dataDefOrder,ddId;
		Date valueAsDate,UnitDate;
		double linearA,linearB,linearC,linearD,linearE;
		double linearResult;	 
		long valueAsBigInt;	
		boolean useLinearEquation = false;
		boolean newMessage = false;
		boolean hasuStu = false;
		boolean hasLlap = false;			
			
		while( decryptedPacket.length() > 4 ){
				
				String tmp = decryptedPacket.substring(0, 4);	//SUBSTRING(@decryptedPacket, 1, 4)), 1)
				String tmp1 = reverseHexString(tmp);
				ddId = Integer.decode("0x"+tmp1);
				decryptedPacket = decryptedPacket.substring(4, decryptedPacket.length());//SUBSTRING(@decryptedPacket, 5, LEN(@decryptedPacket) - 4);
			
				
				if(ddId == 277 || ddId == 293) 
				{
					//-------------------------------------------------------------------------------------------------------
					//----------------------------------------IMPACT RECORDING-----------------------------------------------
					//-------------------------------------------------------------------------------------------------------
				
				}
				else if(ddId == 279)
				{
					//-------------------------------------------------------------------------------------------------------
					//----------------------------------------SPEED RECORDING------------------------------------------------
					//-------------------------------------------------------------------------------------------------------
				}
				
				else if(ddId == 294)
				{
					//-------------------------------------------------------------------------------------------------------
					//----------------------------------------BREADCRUMB RECORDING------------------------------------------------
					//-------------------------------------------------------------------------------------------------------
				}
				else if ((ddId == 296 || ddId == 298) && !hasuStu )
				{
					hasuStu = true;
			
					
				}
				else if((ddId == 304 || ddId == 303) && hasLlap == false)
				{
					//uStuDttm = s1_txDttm;
					//uStuUnitId = s1_serialNo;
					hasLlap = true;
					
				}
				
				dataDefOrder = 0;
				ddIdKey = Integer.toString(ddId) + "-" + Integer.toString(dataDefOrder);
				DataDefinition dataDefinition = dataDefinitions.get(ddIdKey);
				
				if(dataDefinition == null)
				{
					LOG.error("Error: DataDefinition object is NULL ");
					Exception e = new Exception("DataDefinition lookup is not found in Map");
					throw(e);
				}
				
				valueName = dataDefinition.getName();
				valueAlias = dataDefinition.getAlias();
				valueUnit = dataDefinition.getUnit();
				valueType = dataDefinition.getType();
				valueFunction = dataDefinition.getValueFunction();
				valueLength = dataDefinition.getLength();
				linearA = dataDefinition.getLinearA();
				linearB = dataDefinition.getLinearB();
				linearC = dataDefinition.getLinearC();
				linearD = dataDefinition.getLinearD();
				linearE = dataDefinition.getLinearE();
				rawDataLetter = dataDefinition.getRawDataLetter();
				xmlOpen = dataDefinition.getXmlOpenTag();
				xmlClose = dataDefinition.getXmlCloseTag();
				useLinearEquation = false;			
				LOG.info("DDID : " + ddId);
				LOG.info("Key Name is : " + ddIdKey);
				LOG.info("Value Name is : " + valueName);
				
				
				while(valueName != null){
					
					if(unitId == null){
						LOG.error("Error: unitId is null");
						Exception e = new Exception("unitId is null");
						throw(e);
					}
					ddIdKey = Integer.toString(ddId) + "-" + unitId + "-" + valueName;
					UnitDataDefinition unitDataDefinition = unitDataDefinitions.get(ddIdKey);
					
					if(unitDataDefinition != null)
					{
						uddName = unitDataDefinition.getName();
						valueAlias = unitDataDefinition.getAlias();
						valueUnit = unitDataDefinition.getUnit();
						valueType = unitDataDefinition.getType();
						valueFunction = unitDataDefinition.getFunction();
						linearA = unitDataDefinition.getLinearA();
						linearB = unitDataDefinition.getLinearB();
						linearC = unitDataDefinition.getLinearC();
						linearD = unitDataDefinition.getLinearD();
						linearE = unitDataDefinition.getLinearE();
						rawDataLetter = unitDataDefinition.getRawDataLetter();
						useLinearEquation = false;																		
						
						LOG.debug("uddName is : " + uddName);
						LOG.debug("Key Name is : " + ddIdKey);
						LOG.debug("Value Name is : " + valueName);							
					}
					
					if(valueFunction.contains("~"))
					{
						int startInd = valueFunction.indexOf("~");		
						lookupTable = valueFunction.substring(startInd+1);
						valueFunction = valueFunction.substring(0,startInd-1);		
					}
					
					if(rawDataLetter.equals("A") || rawDataLetter.equals("B") || rawDataLetter.equals("C") || rawDataLetter.equals("D") || rawDataLetter.equals("E")){
						useLinearEquation = true;
						if( linearA == 0 || linearB == 0  || linearC == 0  || linearD == 0  || linearE == 0 )
							useLinearEquation = false;			
					}
					
					value = decryptedPacket.substring(0, valueLength);

					if(value.length() > 2 && !valueName.contains("EEPROM"))
						value = reverseHexString(value);
						
					//-------------------------------------------------------------------------------------------------------
					//----------------------------------------LINEAR EQUATIONS-----------------------------------------------
					//-------------------------------------------------------------------------------------------------------
					
					String hexValue = value;

					if(useLinearEquation){
				
						if(rawDataLetter.equals("A"))
							linearA = Integer.decode("0x"+value);
						else if(rawDataLetter.equals("B"))
							linearA = Integer.decode("0x"+value);
						else if(rawDataLetter.equals("C"))
							linearA = Integer.decode("0x"+value);
						else if(rawDataLetter.equals("D"))
							linearA = Integer.decode("0x"+value);
						else if(rawDataLetter.equals("E"))
							linearA = Integer.decode("0x"+value);	
						
						
						if(linearE != 0){
							linearResult = linearA + ((linearB * (linearC + linearD)) / linearE);
							Double tmpLinResDbl = new Double(linearResult);
							value = Double.toString(linearResult);
							if(valueType.equals("int"))
								linearResult = tmpLinResDbl.intValue();
							else if(valueType.equals("bigint"))
								linearResult = tmpLinResDbl.longValue();
							else if(valueType.equals("bigint"))
								linearResult = tmpLinResDbl.floatValue();
							
						}
					}
					//-------------------------------------------------------------------------------------------------------
					//----------------------------------------END LINEAR EQUATIONS-----------------------------------------------
					//-------------------------------------------------------------------------------------------------------

					else{
						
					}

				
				}
		
				
		}
			
			
	}
		
	public void decodeTest(String decryptedPacket,String network, String unitId, Date receiveDttm) {
			
			try {
				decode(decryptedPacket, network,  unitId,  receiveDttm);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		


		private static String reverseHexString (String hex_string)
		{


			String reverse_hex_string = "";
			int counter = hex_string.length() - 1;

			while( counter > -1 ){
				reverse_hex_string = reverse_hex_string + hex_string.substring(counter - 1, counter + 1);
				counter = counter - 2;
			}

			return reverse_hex_string;

		}
	
	 public DeviceConfigService getDeviceConfigService() {
		    return deviceConfigService;
	}

	 public void setDeviceConfigService(DeviceConfigService deviceConfigService) {
		    this.deviceConfigService = deviceConfigService;
	}
	

	private String decrypt(String encodedMsg) {
		
		String packet = encodedMsg.substring(10, encodedMsg.length() - 4);
		
		short iNybbleKey = 4;
		short maxDigit = 16;
		int packetLength = packet.length();
		
		String sShift = encodedMsg.substring(iNybbleKey, iNybbleKey + 1);
		int iShift = hexStringToInt('0' + sShift);
		
		String outPacket = "";
		
		for (int i = 0; i < packetLength; i++){
			
			String fromValue = packet.substring(0,1);
			String sToValue = mapValues(fromValue) ;
			
			int iToValue = hexStringToInt('0' + sToValue);
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
	
private int hexStringToInt(String input) {
		
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
	
	private String mapValues(String fromValue) {
	
		String toValue = decoderMap.get(fromValue);
		return toValue;
	}
	
	public void initDecoderMap() {

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



	public String decryptTest(String encryptedMsg1) {
		initDecoderMap();
		return decrypt(encryptedMsg1);
	}




	
}
