package bsm.prototype.dcl.latlon;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bsm.dcl.config.dal.entities.DataDefinition;
import bsm.dcl.config.dal.entities.UnitDataDefinition;
import bsm.dcl.messaging.LocomotiveMonitoringUnit;
import bsm.dcl.messaging.SolarTrackingUnit;
import bsm.dcl.messaging.SensorRefrigiration;
import bsm.dcl.messaging.UnitMessage;

public class TestDecoder {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testDecrypt() {
		String encryptedMsg1 = "0F05974D004D4404B204DE4444F1CC411A47AC1F58D0FD402CF8FE4C44444546EEDE4144F03CA8434B44E8FA444444444418414A43441F47AC1F1041CA44414444FD1C114B1A10424747485A86";
		String decryptedMsg1 = "0300407C403E0000A122011B09B21A5634A304C2A6AE020000050DEE3E0100A482B6080700E6AB000000000016010B08001A09B21A14012B00010000A31211071B140C090906";
		String encryptedMsg2 = "0F0187140194B93A13D0939974B2B93C127D92F94704B1490C289499129B76B91C3E999499444444381FB49999999999B9999980999995F6B699B99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999BFA5";
		String decryptedMsg2 = "01809C29450900A18D80932DA40DB01A51821053D701002D08AE80239F000100111111972B81000000000080000075000006BE8E00800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

		Decrypter decrypter = new Decrypter();
		try{
			String result1 = decrypter.decryptTest(encryptedMsg1);
			String result2 = decrypter.decryptTest(encryptedMsg2);
			assertEquals(decryptedMsg1, result1);
			assertEquals(decryptedMsg2, result2);
		}
		catch(Exception e)
		{
			System.out.print(e.toString());
			assertNull(e);
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public final void testDecode() {
		
		String decryptedMsg = "0300407C403E0000A122011B09B21A5634A304C2A6AE020000050DEE3E0100A482B6080700E6AB000000000016010B08001A09B21A14012B00010000A31211071B140C090906";
		//String decryptedMsg2 = "01809C29450900A18D80932DA40DB01A51821053D701002D08AE80239F000100111111972B81000000000080000075000006BE8E00800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
		//String decryptedMsg ="280100170D0000606358052E01D659EE1B2901C902F501FD01A302030000003001B585403E0000A12D010059EE1B0000CB34A3043EA7AE020C16";

		
		Decrypter decrypter = new Decrypter();	
		Map<String,DataDefinition> dataDefinitions = null;
		Map<String,UnitDataDefinition> unitDataDefinitions = null;
		try{		
			//--------------DeSerialize dataDefinition for unit test------------------------------------//
			InputStream file = new FileInputStream("C:/TEMP/dataDefinition.ser");
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			dataDefinitions = (Map<String,DataDefinition>) input.readObject();
			input.close();
			
			InputStream file1 = new FileInputStream("C:/TEMP/unitDataDefinition.ser");
			InputStream buffer1 = new BufferedInputStream(file1);
			ObjectInput input1 = new ObjectInputStream(buffer1);
			unitDataDefinitions = (Map<String,UnitDataDefinition>) input1.readObject();
			input1.close();
			//-----------------------------------------------------------------------------------------//
		}
		catch(Exception e)
		{
			System.out.print(e.toString());
		}
		
		try{	
			// Get Data Dictionaries from DB
			decrypter.setDataDefinitions(dataDefinitions);
			decrypter.setUnitDataDefinitions(unitDataDefinitions);
			
			// Setup message structures for decoded data
			UnitMessage unitMsg = new UnitMessage();
			LocomotiveMonitoringUnit messageLMU = new LocomotiveMonitoringUnit();
			SolarTrackingUnit messageSTU = new SolarTrackingUnit();
			SensorRefrigiration sensorRf = new SensorRefrigiration();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			decrypter.setUnitMsg(unitMsg);
			decrypter.setSensorRf(sensorRf);
			decrypter.setMessageLMU(messageLMU);
			decrypter.getMessageSTU(messageSTU);
			decrypter.setDateFormat(dateFormat);
			String network ="K";
			
			
			decrypter.decodeTest(decryptedMsg, network );	
			
			// Check results
			//UnitMsg unitMsgOut = decrypter.getUnitMsg();
			//assertEquals(unitMsgOut.unitDttm, "2014-11-06 16:06:24.000");
			//assertEquals(unitMsgOut.latitude, "39.674578");
			//assertEquals(unitMsgOut.longitude, "-104.99915");
			//assertEquals(unitMsgOut.unitId, "A100003E4085B5");
			//assertEquals(unitMsgOut.course, "");
			
			LocomotiveMonitoringUnit messageLMUOut = decrypter.getMessageLMU();
			//assertEquals(messageLMUOut.msgId, "");

			
			
		}
		catch(Exception e)
		{
			System.out.print(e.toString());
			assertNull(e);
		}			
		
		
		
		
	}

}
