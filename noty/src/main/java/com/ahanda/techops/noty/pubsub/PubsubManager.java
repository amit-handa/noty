package com.ahanda.techops.noty.pubsub;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.Message;
import org.apache.activemq.broker.Connection;

public class PubsubManager
{
	private static PubsubManager instance;

	/*
	 * This is for unit testing
	 */
	public static PubsubManager getInstance()
	{
		if (instance == null)
		{
			synchronized (PubsubManager.class)
			{
				if (instance == null)
				{
					instance = new PubsubManager();
				}
			}
		}
		return instance;
	}

	private PubsubManager()
	{

	}

	public static class HelloWorldProducer implements Runnable
	{
		public void run()
		{
			try
			{
				// Create a ConnectionFactory
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

				// Create a Connection
				javax.jms.Connection connection = connectionFactory.createConnection();
				connection.start();

				// Create a Session
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

				// Create the destination (Topic or Queue)
				Destination destination = session.createQueue("TEST.FOO");

				// Create a MessageProducer from the Session to the Topic or Queue
				MessageProducer producer = session.createProducer(destination);
				producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

				for (int i = 0; i < 5; i++)
				{
					TextMessage message = session.createTextMessage(String.valueOf(i));
					producer.send(message);
				}

				// Clean up
				session.close();
				connection.close();
			}
			catch (Exception e)
			{
				System.out.println("Caught: " + e);
				e.printStackTrace();
			}
		}
	}

	public static class HelloWorldConsumer
	{
		HelloWorldConsumer()
		{
			try
			{

				// Create a ConnectionFactory
				ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

				// Create a Connection
				javax.jms.Connection connection = connectionFactory.createConnection();
				connection.start();

				// Create a Session
				Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

				// Create the destination (Topic or Queue)
				Destination destination = session.createQueue("TEST.FOO");

				// Create a MessageConsumer from the Session to the Topic or Queue
				MessageConsumer consumer = session.createConsumer(destination);

				// Wait for a message
				consumer.setMessageListener(new MessageListener()
				{

					@Override
					public void onMessage(javax.jms.Message message)
					{
						if (message instanceof TextMessage)
						{
							TextMessage textMessage = (TextMessage) message;
							String text = null;
							try
							{
								text = textMessage.getText();
							}
							catch (JMSException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							System.out.println("Received: " + text);
						}
						else
						{
							System.out.println("Received: " + message);
						}
					}
				});

				//consumer.close();
				//session.close();
				//connection.close();
			}
			catch (Exception e)
			{
				System.out.println("Caught: " + e);
				e.printStackTrace();
			}

		}
	}

	public static void main(String[] args)
	{
		HelloWorldProducer p = new HelloWorldProducer();
		HelloWorldConsumer c = new HelloWorldConsumer();
		Thread producer = new Thread(p);
		producer.start();
	}
}
