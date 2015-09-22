package bsm.prototype.database.latlon;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DclServiceXmlTest extends CamelBlueprintTestSupport {

	 @Override
	    protected String getBlueprintDescriptor() {
	          
	        return "OSGI-INF/blueprint/dclService.xml";
	    }


	
	// Templates to send to input endpoints
	@Produce(uri = "sqlSrc:{{sql.selectMessage}}")
	protected ProducerTemplate inputEndpoint;


	@Test
	public void testLMURoute() throws Exception {
				
		// advice the first route using the inlined AdviceWith route builder
		// which has extended capabilities than the regular route builder
		context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				replaceFromWith("direct:in");
				interceptSendToEndpoint("activemq:queue:LatLonLMUDevice")
             .skipSendToOriginalEndpoint().to("mock:queue:LatLonLMUDevice");
				interceptSendToEndpoint("activemq:queue:LatLonAlarm")
             .skipSendToOriginalEndpoint().to("mock:queue:LatLonAlarm");
			}
		});
		  
		// Prepare Test Data
		
		List<Object> params = new ArrayList<Object>();
		
		Map<String, Object> recordNoAlarm = new HashMap<String, Object>();		   		  
		recordNoAlarm.put("MESSAGE_TYPE_ID", new String("1"));
		recordNoAlarm.put("LMU_ID", new String("2222"));
		params.add(recordNoAlarm);
		
		Map<String, Object> recordAlarm1 = new HashMap<String, Object>();		   		  
		recordAlarm1.put("MESSAGE_TYPE_ID", new String("4"));
		recordAlarm1.put("LMU_ID", new String("3333"));
		params.add(recordAlarm1);
		
		Map<String, Object> recordAlarm2 = new HashMap<String, Object>();		   		  
		recordAlarm2.put("MESSAGE_TYPE_ID", new String("5"));
		recordAlarm2.put("LMU_ID", new String("4444"));
		params.add(recordAlarm2);

		context.start();

		// Define some expectations
		getMockEndpoint("mock:queue:LatLonLMUDevice").expectedMessageCount(3);
		getMockEndpoint("mock:queue:LatLonAlarm").expectedMessageCount(2);
		getMockEndpoint("mock:queue:LatLonLMUDevice").expectedBodiesReceived(params);

		// Send test message to input endpoint
		inputEndpoint.sendBody("direct:in",recordNoAlarm);
		inputEndpoint.sendBody("direct:in",recordAlarm1);
		inputEndpoint.sendBody("direct:in",recordAlarm2);
		

		// Validate our expectations
		assertMockEndpointsSatisfied();
	}

	@Test
	public void testSTURoute() throws Exception {
				
		// advice the first route using the inlined AdviceWith route builder
		// which has extended capabilities than the regular route builder
		context.getRouteDefinitions().get(0).adviceWith(context, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				replaceFromWith("direct:in");
				interceptSendToEndpoint("activemq:queue:LatLonSTUDevice")
             .skipSendToOriginalEndpoint().to("mock:queue:LatLonSTUDevice");
				interceptSendToEndpoint("activemq:queue:LatLonAlarm")
             .skipSendToOriginalEndpoint().to("mock:queue:LatLonAlarm");
			}
		});
		  
		// Prepare Test Data
		
		List<Object> params = new ArrayList<Object>();
		
		Map<String, Object> recordNoAlarm = new HashMap<String, Object>();		   		  
		recordNoAlarm.put("MESSAGE_TYPE_ID", new String("1"));
		recordNoAlarm.put("STU_ID", new String("2222"));
		params.add(recordNoAlarm);
		
		Map<String, Object> recordAlarm1 = new HashMap<String, Object>();		   		  
		recordAlarm1.put("MESSAGE_TYPE_ID", new String("4"));
		recordAlarm1.put("STU_ID", new String("3333"));
		params.add(recordAlarm1);
		
		Map<String, Object> recordAlarm2 = new HashMap<String, Object>();		   		  
		recordAlarm2.put("MESSAGE_TYPE_ID", new String("5"));
		recordAlarm2.put("STU_ID", new String("4444"));
		params.add(recordAlarm2);

		context.start();

		// Define some expectations
		getMockEndpoint("mock:queue:LatLonSTUDevice").expectedMessageCount(3);
		getMockEndpoint("mock:queue:LatLonAlarm").expectedMessageCount(2);
		getMockEndpoint("mock:queue:LatLonSTUDevice").expectedBodiesReceived(params);

		// Send test message to input endpoint
		inputEndpoint.sendBody("direct:in",recordNoAlarm);
		inputEndpoint.sendBody("direct:in",recordAlarm1);
		inputEndpoint.sendBody("direct:in",recordAlarm2);
		

		// Validate our expectations
		assertMockEndpointsSatisfied();
	}

}
