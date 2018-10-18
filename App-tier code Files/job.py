import boto3
import boto3
import model
import json
import time
import os
import urllib2
from subprocess import Popen, PIPE
from os import environ
import shlex
import sys

def loadConfig():
    data = None
    try:
        with open('config.json', 'r') as e:
            data = json.load(e)
    except Exception as e:
        print 'error loading the config file'
        raise
    return data


def sendMessageQueue(sqs,queueUrl, message):
    #sqs.send_message(QueueUrl=queueUrl, MessageBody=message)
     sqs.send_message(QueueUrl=queueUrl, MessageBody=message)

def recieveMessageQueue(sqs, queueUrl):

    response = sqs.receive_message(
        QueueUrl=queueUrl,
        MaxNumberOfMessages=1,
        VisibilityTimeout=2,
        WaitTimeSeconds=0
    )

   # print ("Messages ",len(response))

    if len(response) > 1:
        message = response['Messages'][0]
        receipt_handle = message['ReceiptHandle']

        print(message['Body'])
        # Delete received message from queue
        sqs.delete_message(
            QueueUrl=queueUrl,
            ReceiptHandle=receipt_handle
        )
        return message['Body']
    else:
        return 0

if __name__ == '__main__':

    config = loadConfig()
    count = 0
    risk_count = 0
    s3 = boto3.resource('s3')
    sqs = boto3.client('sqs')
    ec2 = boto3.resource('ec2')
    bucket = s3.Bucket(config['S3Bucket'])
    mdl = model.Model(config)
    # while count < config['termination']:
    #     image = recieveMessageQueue(sqs, config["ImageSQSQueue"])
    #     if type(image) != type('str'):
    #         count += 1
    #     else:
    #         # Resetting the count as a possible image is found
    #         count = 0

    #sendMessageQueue(sqs, config["responseQURL"], "Hello cloud comrades")
    try:
	while(1):
            sendMessageQueue(sqs, config["healthQURL"], "health")
            msg = recieveMessageQueue(sqs, config["requestQURL"])
            if type(msg) == type('str'):
                msg_url = msg.rsplit('/', 1)[-1]
                result = mdl.predict(msg)
                result_str = msg_url + "," + result
                sendMessageQueue(sqs, config["responseQURL"], result_str)
                #message = ' '.join(map(bin,bytearray(result_str)))
                s3.meta.client.put_object(Body=result_str, Bucket=config['S3Bucket'], Key=msg_url)

	    time.sleep(1)

        # # Terminate the instance if idling for risk_count * poll_delay time
        # if(risk_count > 6):
        #
        #     instanceids = []
        #     #print "risk Count ", risk_count
        #     ## COMMENTED FOR THE DEFAULT APP TIER. DO NOT PUSH THIS TO i-0814eefe90f324505 Instance
        #     instanceid = urllib2.urlopen('http://169.254.169.254/latest/meta-data/instance-id').read()
        #     instanceids.append(instanceid)
        #
        #     sendMessageQueue(sqs, config["terminateQURL"], instanceid)
        #     ec2.instances.filter(InstanceIds=instanceids).terminate()
        #     break;

    except Exception as ex:
        instanceids = []
        instanceid = urllib2.urlopen('http://169.254.169.254/latest/meta-data/instance-id').read()
        instanceids.append(instanceid)
        ec2.instances.filter(InstanceIds=instanceids).terminate()







