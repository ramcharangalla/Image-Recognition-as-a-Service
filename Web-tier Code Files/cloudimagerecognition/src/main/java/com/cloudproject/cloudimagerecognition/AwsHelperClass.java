package com.cloudproject.cloudimagerecognition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.mediaconvert.model.CreateQueueResult;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
//import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.CreateTagsRequest;

public class AwsHelperClass extends Thread {

	private static int AppServers = 1;
	
	private static AwsHelperClass aws;
	
	private static final String QUEUE_NAME = "testQueue";
	 
	private static final String REQUEST_QUEUE= "requestQueue";
	private static final String RESPONSE_QUEUE= "responseQueue";
	
	
	private static List<String> provisionedInstances = new ArrayList<String>();
	private static List<String> runningInstances = new ArrayList<String>();
	
	private static String InstanceId = "";
	private final static Object lock = new Object();
	
	private static final String SECONDARY_APP_TIER_AMI_ID = "ami-73485913";
	
	private static final int health_timeout = 3;
	private static int health_risk = 0;
	private static boolean spanningInProgress = false;
	private static int count = 1;
	
	private AwsHelperClass() {
		
	}
	
	
	public static AwsHelperClass getInstance() {
		if(aws == null) {
			aws = new AwsHelperClass();
		}
		return aws;
	}
	
	public static String createEC2Instance(String imageId) {
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		System.out.println("create an instance");

		RunInstancesRequest rir = new RunInstancesRequest(imageId, 1, 1);
		rir.setInstanceType("t2.micro");
		rir.withSecurityGroupIds("sg-5cec9725");
		
		Region usWest2 = Region.getRegion(Regions.US_WEST_1);
		
		RunInstancesResult result = ec2.runInstances(rir);
		List<Instance> resultInstance = 
				result.getReservation().getInstances();
		
		for (Instance instance : resultInstance) 
		{
			count = count +1;
		  CreateTagsRequest createTagsRequest = new CreateTagsRequest();
		  createTagsRequest.withResources(instance.getInstanceId()) //
		      .withTags(new Tag("Name", "App-Tier" + count));
		  ec2.createTags(createTagsRequest);
		}
		InstanceId = resultInstance.get(0).getInstanceId();
		
		if(InstanceId != "")
		{
			provisionedInstances.add(InstanceId);
		}
		else
		{
			System.out.println("EC2 Provisining failed");
		}
		return InstanceId;
	}
	
	
	public void startinstance(String instanceId)

	{

		runningInstances.add(InstanceId);
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceId);

		ec2.startInstances(request);

	}
	
	public  void stopinstance(String instanceId)

	{
		
		runningInstances.add(InstanceId);
		final AmazonEC2 ec2 = AmazonEC2ClientBuilder.defaultClient();

		StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceId);

		ec2.stopInstances(request);

	}
	
	public void createSQSQueue(int queue_type) {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String name;
		if(queue_type == 1) {
			name = REQUEST_QUEUE;
		} else if (queue_type == 2){
			name = "charan";
		}else {
			name = RESPONSE_QUEUE;
		}
		
		try
		{
			
			com.amazonaws.services.sqs.model.CreateQueueResult create = sqs.createQueue(name);
            System.out.println("Queue creation successful" + create);
		}catch (final AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means " +
                    "your request made it to Amazon SQS, but was " +
                    "rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (final AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means " +
                    "the client encountered a serious internal problem while " +
                    "trying to communicate with Amazon SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
			
		
	}
	
	public  void sendSQSMsg(int queue_type, String url) {
		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		String name;
		if(queue_type == 1) {
			name = REQUEST_QUEUE;
		}else {
			name = RESPONSE_QUEUE;
		}
		
		try
		{
			// Create a FIFO queue
	    		String queueURL = sqs.getQueueUrl(name).getQueueUrl();
	    		
	    		SendMessageRequest send = new SendMessageRequest()
	    				.withQueueUrl(queueURL)
	    				.withDelaySeconds(5)
	    				.withMessageBody(url);
	    		sqs.sendMessage(send);
		}catch (final AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means " +
                    "your request made it to Amazon SQS, but was " +
                    "rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (final AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means " +
                    "the client encountered a serious internal problem while " +
                    "trying to communicate with Amazon SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
		
	}
	
	public void startLoadMonitoring()  {
	    t1.start();
	    //t2.start();

	}
	public  void stopLoadMonitoring()  {
	    t1.interrupt();
	}
	
	private static  Thread t1 = new Thread(new Runnable() {
    	
        public void run() {
		     	final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
		    		String queueUrl = sqs.getQueueUrl(REQUEST_QUEUE).getQueueUrl();
		    		boolean guard = true;
		    		int i=0;
		    		do
		    		{
		    			List<String> attributeNames = new ArrayList<String>();
		    			attributeNames.add("ApproximateNumberOfMessages");
		    			
		    			GetQueueAttributesRequest request = new GetQueueAttributesRequest(queueUrl);
		    			request.setAttributeNames(attributeNames);
		    			
		    			Map<String, String> attributes = sqs.getQueueAttributes(request).getAttributes();
		    			int no_request_messages = Integer.parseInt(attributes.get("ApproximateNumberOfMessages"));
		    			
		    			//System.out.println("Messages in the queue: " + no_request_messages);
		    			// auto_scale_switch acts as a switch for auto scaling logic
		    			// Change auto_scale_switch to true if you want to use custom scale up
		    			Boolean auto_scale_switch =false;
		    			int required_servers=0;
		    			if(auto_scale_switch)
		    				required_servers =  (int)Math.max(0.0, ((3*no_request_messages/4) - (34*AppServers/4)));
		    			else
		    				required_servers = no_request_messages;
		    			
		    			if(required_servers > 1  && AppServers < 19 )
		    			{
		    				//System.out.println("Number of App Servers to be increased " + required_servers);
		    				for(int j=AppServers;j<required_servers && AppServers < 19 && required_servers > AppServers; j++ )//&& no_request_messages > AppServers;j++)
		    				//for(int j=0;j<required_servers && AppServers <20 ;j++)
		    				{
		    					createEC2Instance(SECONDARY_APP_TIER_AMI_ID);
		    					AppServers++;
		    					System.out.println("Value of AppServers " + AppServers);
		    				}
		    				no_request_messages=0;
		    				System.out.println("Print at createEC2 instance call" + AppServers);
		    			}
		    			try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
		    			String terminate_queueUrl = sqs.getQueueUrl("terminateQueue").getQueueUrl();
						
						List<Message> termination_messages = sqs.receiveMessage(terminate_queueUrl).getMessages();
						// delete messages from the queue
						for (Message m : termination_messages) {
								System.out.println("Values of AppServers in Termination Loop "+ AppServers);
								sqs.deleteMessage(terminate_queueUrl, m.getReceiptHandle());
								if(AppServers > 1)
									AppServers--;
						}
					
		    		} while (guard);
        }

   });  
	

	
private static  Thread t2 = new Thread(new Runnable() {
    	
		int i = 0;
        public void run() {
        	
        	try
        	{
        		while(true)
        		{
	        		final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
                
	        		String healthMonitorQ = sqs.getQueueUrl("healthQueue").getQueueUrl();
					
					List<Message> healthMessages = sqs.receiveMessage(healthMonitorQ).getMessages();
					
					if(healthMessages.size() == 0) 
					{
						System.out.println("Length of the health queue zero h count " + health_risk);
						if(health_risk <= health_timeout)
						{
							health_risk++;
						}
						
					}	
					else
					{
						if(spanningInProgress)
						{
							System.out.println("Resetting the health monitoring");
							health_risk = 0;
							spanningInProgress = false;
						}
						
						for(Message m: healthMessages) {
							
							i++;
							if(i > 2)
							{
								i = 0;
								break;	
							}
								
							health_risk = 0;
							sqs.deleteMessage(healthMonitorQ, m.getReceiptHandle());
						}
					}
					
					 //The primary App tier didn't check in itself for 12 seconds . Recreate the instance 
					if(health_risk > health_timeout && (spanningInProgress == false))
					{
						System.out.println("Creating a backup app tier ");
						createEC2Instance("ami-ac4c5dcc");
						spanningInProgress = true;
					}
					Thread.sleep(3000);
				//System.out.println("AppServers after terminateQueue " + AppServers);
        		}
        	} catch(InterruptedException ex) {
        		System.out.println("Exception " + ex);
        	}
        	
        }

   });  
	
	
	
	
}
