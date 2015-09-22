package bsm.prototype.dcl.latlon;

// Main parsing is in PROCEDURE [dbo].[usp_i_MessageDecoder]
// Raw data is in dbo.MESSAGE_DECODER_QUEUE
// Decoder is in EXEC dbo.usp_s_DecryptPacket @rawPacket, @outPacket = @decryptedPacket OUTPUT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decoder {
	
	
	private static final Logger LOG = LoggerFactory.getLogger(Decoder.class);

	public String decode(Exchange exchange) throws Exception {
		
		Map<String, Object> record = exchange.getIn().getBody(Map.class);
		

		String encodedMsg = (String)record.get("RAW_PACKET");
		encodedMsg = "0F05974D004D4404B204DE4444F1CC411A47AC1F58D0FD402CF8FE4C44444546EEDE4144F03CA8434B44E8FA444444444418414A43441F47AC1F1041CA44414444FD1C114B1A10424747485A86";
		
		
		
		String decodedMsg ="result";
		return decodedMsg;
	}
	
	

	
}
