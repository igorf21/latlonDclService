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
import bsm.dcl.latlon.Decoder;
import bsm.dcl.messaging.Common;
import bsm.dcl.messaging.Impact;
import bsm.dcl.messaging.LocomotiveMonitoringUnit;
import bsm.dcl.messaging.MessageControl;
import bsm.dcl.messaging.SolarTrackingUnit;
import bsm.dcl.messaging.SensorRefrigiration;
import bsm.dcl.messaging.SpacialTracking;
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

	
	
	@SuppressWarnings("unchecked")
	@Test
	public final void testDecode() {
		// MSG_TYPE = 0
		String decryptedMsg1 = "2801F4145800000D1700002E01EB96341D2901BD024102FC010100000000002F012257122273252D01EB96341D030037B09E044D2DB2020916";
		//String decryptedMsg2 = "280151165800000D1700002E0172B93C1D290101030502FC01C700800000002F013057122273252D0172B93C1D03004ADCA104F662B0020B16";
		//String decryptedMsg3 = "280151165800000D1700002E0101573C1D2901FD020502FC01E700800000002F013057122273252D0101573C1D03005EDCA104DB62B0020E16";
		//String decryptedMsg4 = "280151165800000D1700002E01C01D381D2901FE02FF01F5010101800000002F013057122273252D01C01D381D03000C35A3042EA7AE020C16";
		//String decryptedMsg5 = "2801F4145800000D1700002E016390361D2901BD023C02F4010100000000002F012257122273252D016390361D03003E35A30403A7AE020C16";
		//String decryptedMsg6 = "280159145800000D1700002E01012E351D2901FA020C026901AE00800000002F013057122273252D01012E351D03005DDCA104CD62B0020A16";
		//String decryptedMsg7 = "28015E165800000D1700002E01017B381D2901BB02F0018B022001000000002F013057122273252D01017B381D030084DCA104CF62B0020B16";
			
		  int MsgId_1 = 638980067;
		  int MsgId_2 = 642309067;
		  int MsgId_3 = 642167576;
		  int MsgId_4 = 640307173;
		  int MsgId_5 = 639616971;
		  int MsgId_6 = 639144500;
		  int MsgId_7 = 640468687;
		  
		  
		Decoder decoder = new Decoder();	
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
			decoder.setDataDefinitions(dataDefinitions);
			decoder.setUnitDataDefinitions(unitDataDefinitions);
			
			// Setup message structures for decoded data
			decoder.messageControl = new MessageControl();
			decoder.common = new Common();
			decoder.messageLMU = new LocomotiveMonitoringUnit();
			decoder.messageSTU = new SolarTrackingUnit();
			decoder.sensorRf = new SensorRefrigiration();
			decoder.spacial = new SpacialTracking();
			decoder.impact = new Impact();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

			
			decoder.setDateFormat(dateFormat);
			String network ="K";
			
//			UnitMessage unitMsgOut = new UnitMessage();
			decoder.decodeTest(decryptedMsg1, network );	
			
			// Check results
			assertEquals(decoder.messageControl.messageId,"638980067");
			assertEquals(decoder.messageControl.unitDttm, "2015-07-12 03:09:31.000");
			//assertEquals(decoder.messageControl.receiveDttm, "2015-07-12 03:10:39.483");
			assertEquals(decoder.messageControl.localDttm, "2015-07-11 21:09:31.000");
			assertEquals(decoder.messageControl.localDttmTz, "MDT");
			assertEquals(decoder.messageControl.messageType, "0");
			assertEquals(decoder.spacial.latitude, "39.181105");
			assertEquals(decoder.spacial.longitude, "-104.614272");
			assertEquals(decoder.messageControl.unitId, "00170D00005814F4");
			//assertEquals(unitMsgOut.course, "");
			
	System.out.print("done decoding");
			//assertEquals(messageLMUOut.msgId, "");

			
			
		}
		catch(Exception e)
		{
			System.out.print(e.toString());
			assertNull(e);
		}			
		
		
		
		
	}

}
