package com.cloudproject.cloudimagerecognition;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;




public class ServletContextClass implements ServletContextListener
{
	AwsHelperClass helper = AwsHelperClass.getInstance();
	
public void contextInitialized(ServletContextEvent arg0) 
{   
		//helper.startECInstances();
		//helper.createSQSQueue(1);
	
		//helper.createEC2Instance();
		helper.startLoadMonitoring();
		System.out.println("Hellos wors");

	
}//end contextInitialized method


public void contextDestroyed(ServletContextEvent arg0) 
{     
	helper.stopLoadMonitoring();
}//end constextDestroyed method

}
