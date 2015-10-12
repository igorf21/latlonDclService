package bsm.prototype.dcl.latlon;

// Main parsing is in PROCEDURE [dbo].[usp_i_MessageDecoder]
// Raw data is in dbo.MESSAGE_DECODER_QUEUE
// Decrypting is in EXEC dbo.usp_s_DecryptPacket @rawPacket, @outPacket = @decryptedPacket OUTPUT;
// Decoding is in dbo.usp_i_MessageDecoder @rawPacketId

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bsm.dcl.config.dal.DeviceConfigService;
import bsm.dcl.config.dal.entities.DataDefinition;
import bsm.dcl.config.dal.entities.UnitDataDefinition;
import bsm.dcl.messaging.Impact;
import bsm.dcl.messaging.LocomotiveMonitoringUnit;
import bsm.dcl.messaging.SolarTrackingUnit;
import bsm.dcl.messaging.SpacialTracking;
import bsm.dcl.messaging.SpeedRecording;
import bsm.dcl.messaging.UnitMessage;
import bsm.dcl.messaging.SensorRefrigiration;

public class Decrypter {
	
	private DeviceConfigService deviceConfigService;
	
	// Various mapping objects for data decoding
	private Map<String,String> decoderMap = new HashMap<String, String>();
	private Map<String,DataDefinition> dataDefinitions;
	private Map<String, UnitDataDefinition> unitDataDefinitions;
	
	private DataDefinition dataDefinition;
	
	// Decoded Data
	private UnitMessage unitMsg;
	private SensorRefrigiration sensorRf;
	private LocomotiveMonitoringUnit messageLMU;
	private SolarTrackingUnit messageSTU;
	private Impact impact;
	private SpacialTracking spacial;
	private SpeedRecording speedRecording;

	// Helper members
	private SimpleDateFormat dateFormat;
	boolean hasuStu;

	
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
		exchange.setProperty("NETWORK",record.get("NETWORK"));
		exchange.setProperty("RAW_PACKET_ID",record.get("RAW_PACKET_ID"));

		String encryptedMsg = (String)record.get("RAW_PACKET");
		String decryptedMsg = decrypt(encryptedMsg);
		
		return decryptedMsg;
	}
	
	public UnitMessage decodeMsg(Exchange exchange) throws Exception {
	
		// Initialize output message structures
		unitMsg = new UnitMessage();
		sensorRf = new SensorRefrigiration();
		messageLMU = new LocomotiveMonitoringUnit();
		messageSTU = new SolarTrackingUnit();
		impact = new Impact();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String decryptedPkt = exchange.getIn().getBody(String.class);
		unitMsg.unitId = (String)exchange.getProperty("UNIT_ID");
		unitMsg.receiveDttm = exchange.getProperty("RECEIVE_DTTM").toString();
		String network = (String)exchange.getProperty("NETWORK");
//		Long rowPacketId = (Long)exchange.getProperty("RAW_PACKET_ID");
	
		if(decryptedPkt == null || network == null || unitMsg.receiveDttm == null){
			LOG.error("Error: data string metadata is missing");
			Exception e = new Exception( "Network: " + network + ", Date Received:"
			+ unitMsg.receiveDttm + ", Decrypted Packet is: " + decryptedPkt);
			throw(e);
		}	

		dataDefinitions = deviceConfigService.readDataDefinitionsFromDb();
		unitDataDefinitions = deviceConfigService.readUnitDataDefinitionsFromDb();
		
		//--------------Serialize dataDefinition for unit test------------------------------------//
		//OutputStream file = new FileOutputStream("C:/TEMP/dataDefinition.ser");
	    //OutputStream buffer = new BufferedOutputStream(file);
	    //ObjectOutput output = new ObjectOutputStream(buffer);
	    //output.writeObject(dataDefinitions);
	    //output.flush();
	    //output.close();
		
		//OutputStream file = new FileOutputStream("C:/TEMP/unitDataDefinition.ser");
	    //OutputStream buffer = new BufferedOutputStream(file);
	    //ObjectOutput output = new ObjectOutputStream(buffer);
	    //output.writeObject(unitDataDefinitions);
	    //output.flush();
	    //output.close();
		//-----------------------------------------------------------------------------------------//

		decode(decryptedPkt, network);
	
		unitMsg.sensorRf 	= sensorRf;
		unitMsg.messageLMU 	= messageLMU;
		unitMsg.messageSTU 	= messageSTU;
			
		return unitMsg;
		
	}
	
	
	private void decode(String decryptedPacket, String network) throws Exception {
		
		String ddIdKey = "";
		String value;
		int dataDefOrder,ddId;
		boolean hasLlap = false;	
		
		unitMsg.decodeDttm = dateFormat.format(new Date());

			
		while( decryptedPacket.length() > 4 ){
				
				String tmp = decryptedPacket.substring(0, 4);	//SUBSTRING(@decryptedPacket, 1, 4)), 1)
				String tmp1 = reverseHexString(tmp);
				ddId = Integer.decode("0x"+tmp1);
				decryptedPacket = decryptedPacket.substring(4, decryptedPacket.length());//SUBSTRING(@decryptedPacket, 5, LEN(@decryptedPacket) - 4);
			
				
				if(ddId == 277 || ddId == 293) 
				{
					impactRecording(decryptedPacket);
				}
				else if(ddId == 279)
				{
					speedRecording(decryptedPacket);	
				}
				
				else if(ddId == 294)
				{
					breadcrumbRecording(decryptedPacket);
				}
				else if ((ddId == 296 || ddId == 298) && !hasuStu )
				{
					hasuStu = true;
							
				}
				else if((ddId == 304 || ddId == 303) && hasLlap == false)
				{
					//messageSTU.uStuDttm = s1_txDttm;
					//uStuUnitId = s1_serialNo;
					hasLlap = true;
					
				}
				
				dataDefOrder = 0;
				ddIdKey = Integer.toString(ddId) + "-" + Integer.toString(dataDefOrder);
				dataDefinition = dataDefinitions.get(ddIdKey);
				
				if(dataDefinition == null)
				{
					continue;
				}				
				
				while(dataDefinition != null && dataDefinition.getName() != null){
					
					if( unitMsg.unitId != null ){
						changeDataDefinition(ddId);					
					}		
						
					value = getValueFromFunction(decryptedPacket);				
				
					if(!processlinearEquations(value)){
						
						processValueFunction(value);
						processStandardDefinitions(value);					
					}

					if(decryptedPacket.length() > 0){
						try{
							decryptedPacket = decryptedPacket.substring(dataDefinition.getLength());
						}
						catch(IndexOutOfBoundsException  e){
							decryptedPacket = "";
							dataDefinition = null;
							LOG.info("Exception: " + e.toString());
							continue;
						}
					}
					
					//if(divisionId == null){
					//add handler
					//}
					
				
					dataDefOrder++;
					ddIdKey = Integer.toString(ddId) + "-" + Integer.toString(dataDefOrder);
					dataDefinition = dataDefinitions.get(ddIdKey);	
				
				}
		}				
	}
			

	private void breadcrumbRecording(String decryptedPacket) {
		 throw new  UnsupportedOperationException("Breadcrumb Not implemented yet");
		
	}

	private void impactRecording(String decryptedPacket) {
		 throw new  UnsupportedOperationException("impactRecording Not implemented yet");
		
	}

	private void speedRecording(String decryptedPacket) {
		
		String speedPacket = decryptedPacket;
		int lenSpeed = Integer.decode ("0x" + speedPacket.substring(0, 1));
		
		String saveSpeedPacket = speedPacket;
		speedPacket = speedPacket.substring(2, lenSpeed * 2 - 1);
		int speedOrder = 0;
		
		speedRecording.records = new HashMap<String,String>();

		while( speedPacket.length() > 0 ){							
			int speedRecord = Integer.decode ("0x" + speedPacket.substring(0, 1));
			speedRecording.records.put(Integer.toString(speedOrder), Integer.toString(speedRecord));
			speedOrder = speedOrder + 1;
			speedPacket = speedPacket.substring( 2, lenSpeed*2 - 1);				
		}
		decryptedPacket = decryptedPacket.substring( decryptedPacket.length() - saveSpeedPacket.length());			
	}

	private String getValueFromFunction(String decryptedPacket) {
		
		String valueFunction = dataDefinition.getFunction();
		int valueLength = dataDefinition.getLength();
		String valueName = dataDefinition.getName();;
		String value = "";
	
		if(valueFunction != null && valueFunction.contains("~"))
		{
			int startInd = valueFunction.indexOf("~");		
//			lookupTable = valueFunction.substring(startInd+1);
			dataDefinition.setFunction(valueFunction.substring(0,startInd-1));		
		}
		
		try{
			value = decryptedPacket.substring(0, valueLength);
		}
		catch(IndexOutOfBoundsException  e){
			value = decryptedPacket;
			LOG.info("Exception: " + e.toString());
		}

		if(value.length() > 2 && !valueName.contains("EEPROM"))
			value = reverseHexString(value);			
	
		return value;
	}

	private void changeDataDefinition(int ddId) {
		if( !unitMsg.unitId.isEmpty() ){
			String ddIdKey = Integer.toString(ddId) + "-" + unitMsg.unitId + "-" + dataDefinition.getName();
			UnitDataDefinition unitDataDefinition = unitDataDefinitions.get(ddIdKey);
			
			if(unitDataDefinition != null)
			{
				dataDefinition.redefineData(unitDataDefinition);								
			}
		}
		
	}

	private void processStandardDefinitions(String value) {
		 if (dataDefinition.getFunction() != null && !dataDefinition.getFunction().isEmpty())
			 return;
		 
		 String valueName = dataDefinition.getName();
		 
		 if ( valueName == null || valueName.isEmpty())
			 return;
		 
		 if ( decodeSpeed(valueName, value) )
			 return;
		 else if( decodeMaxSpeed(valueName, value) )
			 return;
		 else if( decodeGpsSatteliteInView(valueName, value) )
			 return;
		 else if( decodeMessageType(valueName, value) )
			 return;
		 
		
	}

	private boolean decodeMessageType(String valueName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeGpsSatteliteInView(String valueName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeMaxSpeed(String valueName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean decodeSpeed(String valueName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	private boolean processValueFunction(String value) {
		if( dataDefinition.getFunction() == null)
			return false;
		
		if( processUnitIdAndSerialNumber(value) )
			return true;
		else if( processUnitDateTime(value) )
			return true;
		else if( processMsgType(value) )
			return true;
		else if( processLatitude(value) )
			return true;
		else if( processLongitude(value) )
			return true;
		else if( processCourse(value) )
			return true;
		else if( processG(value) )	
				return true;
		
		return false;		
	}

	private boolean processG(String value) {
		
		String valueName = dataDefinition.getName();
		if( !dataDefinition.getFunction().equals("udf_g") || valueName == null )
			return false;
		
		Float xyzG = Long.decode("0x"+value).floatValue()/10;

		if( valueName.equals("X_G_10HZ"))
			impact.x10Hz = xyzG.toString();
		else if(valueName.equals("Y_G_10HZ"))
			impact.y10Hz = xyzG.toString();
		else if(valueName.equals("Z_G_10HZ"))
			impact.z10Hz = xyzG.toString();
		else if(valueName.equals("X_G_100HZ"))
			impact.x100Hz = xyzG.toString();
		else if(valueName.equals("Y_G_100HZ"))
			impact.y100Hz = xyzG.toString();
		else if(valueName.equals("Z_G_100HZ"))
			impact.z100Hz = xyzG.toString();
		
		return true;
	}

	private boolean processCourse(String value) {
		if(!dataDefinition.getFunction().equals("udf_course") && !dataDefinition.getName().equals("COURSE"))
			return false;
		spacial.course = Integer.toString(Integer.decode("0x" + value) * 2);
		
		return true;
	}

	private boolean processMsgType(String value) {
		
		String valueFunction = dataDefinition.getFunction();
		if( valueFunction.equals("udf_MsgType") && dataDefinition.getName().equals("MESSAGE_TYPE") )
		{
				unitMsg.uStuMsgTypeId = Integer.decode("0x"+value);
				return true;
		}
		return false;
		
	}

	private boolean processLongitude(String value) {
		
		if(	!dataDefinition.getFunction().equals("udf_longitude") && !dataDefinition.getName().equals("LONGITUDE")){
			return false;
		}
		
		Double dLongitude = Long.decode("0x" + value)/ 600000.000000 - 180;

		if(dLongitude >= 180d)
			spacial.longitude = "-180.000002";
		else{
			String strLongitude = dLongitude.toString();
			int len = strLongitude.length();
			int truncatePt = strLongitude.indexOf(".") + 7;
			if(truncatePt < len + 1)
				spacial.longitude = strLongitude.substring(0,truncatePt);
			else
				spacial.longitude = strLongitude;				
		}
			
		return true;		
		
	}

	private boolean processLatitude(String value) {
		
		if(	!dataDefinition.getFunction().equals("udf_latitude") && !dataDefinition.getName().equals("LATITUDE")){
			return false;
		}
		
		Double dLatitude = Long.decode("0x" + value)/600000.000000 - 90;

		if(dLatitude >= 90d)
			spacial.latitude = "-90.000002";
		else{
			String strLatitude = dLatitude.toString();
			int len = strLatitude.length();
			int truncatePt = strLatitude.indexOf(".") + 7;
			if(truncatePt < len + 1)
				spacial.latitude = strLatitude.substring(0,truncatePt);
			else
				spacial.latitude = strLatitude;
		}
			
		return true;		
	}

	private boolean processUnitDateTime(String value) {
		
		if( !dataDefinition.getFunction().equals("udf_unitDateTime"))
			return false;
		

		long valueAsBigInt = Long.decode("0x" + value) * 1000;

		String basedate = "2000-01-01 00:00:00.000";
		Date dUnitDate;
		
		try {
			dUnitDate = dateFormat.parse(basedate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return true;
		}
	
		String txDtmm = dateFormat.format(new Date(valueAsBigInt + dUnitDate.getTime()));

		if(dataDefinition.getName().equals("UNIT_DTTM") ){					 
			unitMsg.unitDttm = txDtmm;
		}
		else if( dataDefinition.getName().equals("TX_DTTM") ){
	
			if (sensorRf.s1_txDttm == null)
				sensorRf.s1_txDttm = txDtmm;
			else if(sensorRf.s2_txDttm == null)
				sensorRf.s2_txDttm= txDtmm;
			else if(sensorRf.s3_txDttm == null)
				sensorRf.s3_txDttm = txDtmm;
			else if(sensorRf.s4_txDttm == null)
				sensorRf.s4_txDttm = txDtmm;
			else if(sensorRf.s5_txDttm == null)
				sensorRf.s5_txDttm = txDtmm;
			else if(sensorRf.s6_txDttm == null)
				sensorRf.s6_txDttm = txDtmm;
			else if(sensorRf.s7_txDttm == null)
				sensorRf.s7_txDttm = txDtmm;
			else if(sensorRf.s8_txDttm == null)
				sensorRf.s8_txDttm = txDtmm;
			else if(sensorRf.s9_txDttm == null)
				sensorRf.s9_txDttm = txDtmm;
			else if(sensorRf.s10_txDttm == null)
				sensorRf.s10_txDttm = txDtmm;

			if(txDtmm.contains("2000-01-01")){
				sensorRf.excludeRF = true;
			}
			else{
				sensorRf.excludeRF = false;
			}
		}
		else if( dataDefinition.getName().equals("GPS_DTTM")){
			spacial.gpsDttm = txDtmm;
		}
		
		return true;		
	}

	private boolean processUnitIdAndSerialNumber(String value) {
			
		String valueFunction = dataDefinition.getFunction();
		String valueName = dataDefinition.getName();
		
		if( valueFunction.equals("udf_unitId") ){

			if(valueName.equals("UNIT_ID")){
				unitMsg.unitId = value;
			}
			else if(valueName.equals("SERIAL_NUMBER") && !sensorRf.excludeRF){
				unitMsg.tempUnitId = unitMsg.unitId;
			}
			if( sensorRf.sensor == 1){	
				sensorRf.s1_serialNo = value;
				unitMsg.unitId = sensorRf.s1_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 2){					
				sensorRf.s2_serialNo = value;
				unitMsg.unitId = sensorRf.s2_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 3){		
				sensorRf.s3_serialNo = value;
				unitMsg.unitId = sensorRf.s3_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 4){			
				sensorRf.s4_serialNo = value;
				unitMsg.unitId = sensorRf.s4_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 5){	
				sensorRf.s5_serialNo = value;
				unitMsg.unitId = sensorRf.s5_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 6){			
				sensorRf.s6_serialNo = value;
				unitMsg.unitId = sensorRf.s6_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 7){		
				sensorRf.s7_serialNo = value;
				unitMsg.unitId = sensorRf.s7_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 8){		
				sensorRf.s8_serialNo = value;
				unitMsg.unitId = sensorRf.s8_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 9){			
				sensorRf.s9_serialNo = value;
				unitMsg.unitId = sensorRf.s9_serialNo;
				return true;
			}
			else if( sensorRf.sensor == 10){					
				sensorRf.s10_serialNo = value;
				unitMsg.unitId = sensorRf.s10_serialNo;
				return true;
			}
		}			
		return false;
	}

	private boolean processlinearEquations(String value) {
		
		String valueAlias,valueUnit,valueFunction,valueType,xmlOpen,xmlClose,uddName = null,eePromRowId,rawDataLetter;
		double linearA,linearB,linearC,linearD,linearE;
		double linearResult;	 

		rawDataLetter = dataDefinition.getRawDataLetter();	
		
		if(rawDataLetter == null)
			return false;
		
		if( !rawDataLetter.equals("A") || !rawDataLetter.equals("B") || !rawDataLetter.equals("C") || 
				!rawDataLetter.equals("D") || !rawDataLetter.equals("E"))
			return false;
			
		linearA = dataDefinition.getLinearA();
		linearB = dataDefinition.getLinearB();
		linearC = dataDefinition.getLinearC();
		linearD = dataDefinition.getLinearD();
		linearE = dataDefinition.getLinearE();

		if( linearA == 0 || linearB == 0  || linearC == 0  || linearD == 0  || linearE == 0 )
			return false;			
				
		uddName = dataDefinition.getUddName();
		valueAlias = dataDefinition.getAlias();
		valueUnit = dataDefinition.getUnit();
		valueType = dataDefinition.getType();
		valueFunction = dataDefinition.getFunction();
		
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
		return true;
		
	}

	public void decodeTest(String decryptedPacket,String network) {
			
			try {
				decode(decryptedPacket, network);
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



	public String decryptTest(String encryptedMsg) {
		initDecoderMap();
		return decrypt(encryptedMsg);
	}

	public void setDataDefinitions(Map<String,DataDefinition> dataDefinitions){
		
		this.dataDefinitions = dataDefinitions;
	}
	public void setUnitMsg(UnitMessage unitMsg){
		
		this.unitMsg = unitMsg;
	}

	public void setUnitDataDefinitions(Map<String, UnitDataDefinition> unitDataDefinitions) {
		
		this.unitDataDefinitions = unitDataDefinitions;
		
	}

	public void setSensorRf(SensorRefrigiration sensorRf) {
		this.sensorRf = sensorRf;
		
	}

	public UnitMessage getUnitMsg() {
		// TODO Auto-generated method stub
		return this.unitMsg;
	}

	public LocomotiveMonitoringUnit getMessageLMU() {
		// TODO Auto-generated method stub
		return this.messageLMU;
	}
	
	public void setMessageLMU(LocomotiveMonitoringUnit messageLMU) {
		this.messageLMU = messageLMU;
		
	}

	public void getMessageSTU(SolarTrackingUnit messageSTU) {
		this.messageSTU = messageSTU;
		
	}

	public void setDateFormat(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
		
	}

	
}
