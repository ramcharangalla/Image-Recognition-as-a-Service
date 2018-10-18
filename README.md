# Image-Recognition-as-a-Service

Scalable, loosely-coupled full-stack web application offering web services to perform image recognition as a service with a load balancer to dynamically scale-up and scale-down the workers based on the number of requests in SQS queue.


The system is divided into two main components.
1. Web Tier: This houses the web service that interacts with outside world and handle user
requests. In addition to the web service it also takes care of scaling the number of instances up.
2. App Tier: This houses the deep learning logic which predicts the category of the image provided.
An instance in the app tier is completely stateless and has no information about the web service
or any other instances running in the app tier. The instances simply pick up the next available
requests from the request queue (we will describe the various queues next), process the
request, put the response back in the response queue and also push the results to an S3 bucket


## AutoScaling

In our architecture, we have one web-tier and one app-tier always running even though there
are no messages in the requestQueue. We have implemented the auto-scaler in web-tier in a separate
thread. The auto-scaler adds the incoming requests to the SQS requestQueue to make them available to
the app-tiers. From the requestQueue, the messages would be consumed by the app-tiers and
processed by deep learning model. If the number of messages in the requestQueue are less, then
always-up-app-tier instance consumes the messages and processes them. If the number of request
messages in the requestQueue at anytime are more and cannot be handled by single app-tier instance,
then auto-scaler increases the number of app-tier instances to improve the latency for the requests.
Auto-scaler scales up the number of app-tier instances to serve the requests at high speed and at the
same time using less number of resources (to reduce the cost). The number of app-tier instances will be
scaled down when some of the app-tier instances are idle and there are no messages in the
requestQueue.
Scale down of the app-tier instance will be done by the respective individual instances by
shutting itself down when it is idle and there are no messages in the requestQueue. The
shutting-down-app-tier instance communicates to the auto-scaler using terminateQueue that it is going to shut down. The auto-scaler keeps listening to the terminateQueue and updates its local variable that
keep track of number of app-tier instances.
We have implemented the scale up logic considering the following factors that affect the latency
of the request and cost to process the request.
1. Number of messages in the requestQueue (no_request_messages)
2. Number of app-tier instances that are already up (AppServers)
3. Time taken by each EC2 instance boot-up (Approximately 30 seconds)
4. Response time for the request
5. Time taken by the deep learning model to process one request ( Approximately 3 seconds)
Using the aforementioned factors, we have calculated the number of required servers to be
newly booted up with the following formula
required_servers = Max( 0 , ( 3 * no_request_messages / 4 ) - ( 34 * AppServers / 4) )
After calculating the required number of app-tiers, the web-tier creates new ec2 instances to
process the messages from requestQueue and reduce the load on already up servers. As the AWS
free-tier provides maximum of 20 EC2 instances, we use one EC2 instance for web-tier hosting and we
scale up number of app-tier instances to 20 - 1 = 19 as upper limit. This calculation makes us use EC2
instances cost-effectively.


Web Tier setup conditions

1) The Web tier code (unzip cloudimagerecognition folder) needs to be placed in the Webapps folder of the Tomcat7 installation. 

2) Start the server by using the following command

    $(tomcat_installation)/bin/startup.sh
