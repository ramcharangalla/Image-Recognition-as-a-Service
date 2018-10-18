
package com.cloudproject.cloudimagerecognition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.cloudproject.cloudimagerecognition.AwsHelperClass;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;

/** Example resource class hosted at the URI path "/myresource"
 */
@Path("/")
public class MyResource {
    
	@Context
	private ServletContext context;
	
	private AwsHelperClass helper;
	
	private static Thread t1 = null;
	
	private static final String RESPONSE_QUEUE= "responseQueue";
	
	
	
	private volatile boolean guard = true;
	
	long start;
    /** Method processing HTTP GET requests, producing "text/plain" MIME media
     * type.
     * @return String that will be send back as a response of type "text/plain".
     */
	   @GET
	    @Produces("text/plain") 
	    public String uploadURL(@QueryParam("input") String url) {
	    	String res = null;
	    	String reqName = "";
	    	List<String> reqList = new ArrayList<String>();
	    	
	    	start = System.currentTimeMillis();
	    
		try
		{
	       if(url == "")
	       {
	    	   		return "Enter a valid URL as input";
	       }
	    	
	    		

    			helper = AwsHelperClass.getInstance();
    			
    			url = url.replace("\"", "");
    			reqName = url.substring(url.lastIndexOf('/') + 1);
    		    
    			System.out.println("URL Sent by the user " + url);
    			
    			
    			helper.sendSQSMsg(1, url);
			res = monitorForResult(reqName, url);
	    	
	    }catch (NullPointerException n) {
			n.printStackTrace();
		}/*catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/catch(Exception ex) {	
			System.out.println(ex);
		}
		System.out.println("Returning the result " + res + " for request " + reqName );
		return res;
	}
	    
		public  String monitorForResult( String requestIdentifier, String url) throws InterruptedException {
			String requestId = requestIdentifier;
			final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
			String queueUrl = sqs.getQueueUrl(RESPONSE_QUEUE).getQueueUrl();
			String responseString = null;
			System.out.println("Request Identifier" + requestId);
			try
			{
			    		int count = 0;
			    		while(guard)
			    		{
			    			
			    			//ReceiveMessageRequest req = new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10);
			    			List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();
			    			for(Message m : messages)
			    			{
			    				
			    				//Response for an image request arrives as {ImageRequestString, RecognitionResult}
			    				String list[] = m.getBody().split(",");
			    				
			    				// Result of the Image recognition model obtained
			    				if(list[0].equals(requestId))
			    				{
			    					
			    					guard = false;
			    					responseString = list[1];
			    					//System.out.println("Message concerned " + m.getBody() + " Request ID " + requestId);
			    					sqs.deleteMessage(queueUrl, m.getReceiptHandle());
			    					
			    					long end = System.currentTimeMillis();
			    					
			    					System.out.println("Execution Took : " + ((end - start) / 1000));
			    					break;
			    					
			    				}
			    			}
			    			try {
			    				
			    				    /* App tier taking too long to respond to the request.
			    				     *  Probably , app tier instance processing this request died.
			    				     *  Re-send the request
			    				     */
			    					if(count > 200 )
			    					{
			    						count = 0;
			    						helper.sendSQSMsg(1, url);
			    					}
			    					else
			    					{
			    						count++;
			    					    Thread.sleep(800);
			    						
			    					}
								
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
									e.printStackTrace();
									continue;
								}
				 }
				System.out.println("Returning");
				
			
		} catch(NullPointerException n) {
			n.printStackTrace();
			
		} 
			return responseString;
		}
}
