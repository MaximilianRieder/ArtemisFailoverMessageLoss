import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQMessageConsumer;
import org.apache.activemq.artemis.jms.client.ActiveMQSession;

/**
 * Simple listener application, that counts the received and missed messages.
 *
 * @author Maximilian Rieder
 */
public class SimpleListenerCount implements MessageListener
{
	private ActiveMQConnectionFactory connectionFactory = null;
	private ActiveMQConnection connection = null;
	private ActiveMQMessageConsumer consumer = null;
	private int threeCorrectCounter = 3;
	private ActiveMQSession session = null;
	String brokerUrl =
			"(tcp://a:6666,tcp://b:6666)?failoverAttempts=-1;connectionTTL=120000;useTopologyForLoadBalancing=false";
	private String clientId = "testbestasd";
	private String topicName = "count.topic";
	boolean exited = false;
	int lastCount = 0;
	int missedMessages = 0;

	public SimpleListenerCount()
	{
	}

	private void resetThreeCorrectCounter()
	{
		threeCorrectCounter = 3;
	}

	/**
	 * checks if the message that was previously received is the current integer - 1
	 *
	 * @param message received message (TextMessage as single integer)
	 */
	public void onMessage(Message message)
	{
		try
		{
			String messageString = ((TextMessage) message).getText();
			if ( "exit".equals(messageString) )
			{
				shutdown();
				return;
			}
			int messageCount = 0;
			try
			{
				messageCount = Integer.parseInt(messageString);
			}
			catch ( NumberFormatException e )
			{
				System.err.println("message could not be parsed to int.");
				return;
			}
			if ( lastCount == 0 )
			{
				lastCount = messageCount;
				System.out.println("handleEventNow started counter at: " + lastCount);
				return;
			}
			if ( (lastCount + 1) == messageCount )
			{
				if ( threeCorrectCounter > 0 )
				{
					System.out.println("*********  handleEventNow received message with count: " + messageCount +
					                   " correctly. Total missed messages = " + missedMessages);
					threeCorrectCounter--;
				}
			}
			else
			{
				missedMessages = missedMessages + (messageCount - lastCount);
				System.out.println("********* previous count=" + lastCount +
				                   " current count=" + messageCount +
				                   " missed total messages since startup: " + missedMessages);
				resetThreeCorrectCounter();
			}
			lastCount = messageCount;
		}
		catch ( JMSException e )
		{
			e.printStackTrace();
		}
	}

	public void subscribe(String subject) throws JMSException
	{
		Destination topic = session.createTopic(subject);
		consumer = (ActiveMQMessageConsumer) session.createConsumer(topic);
		consumer.setMessageListener(this);
		System.out.println("listening to:" + topic);
	}

	/**
	 * Connect to a broker and start the connection
	 *
	 * @throws JMSException if connection fails
	 */
	public void connect() throws JMSException
	{
		System.out.println("Attempt to create authentication disabled connection factory.");
		connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
		connectionFactory.setClientID(clientId);
		System.out.println("Attempt to create  connection with url: " + brokerUrl);
		connection = (ActiveMQConnection) connectionFactory.createConnection();
		System.out.println("Connection established.");
		connection.start();
		System.out.println("Connection started.");
		session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		System.out.println("Session created.");
	}

	/**
	 * stop and close
	 */
	private void shutdown()
	{
		try
		{
			if ( consumer != null )
			{
				consumer.close();
				System.out.println("Consumer closed.");
			}
		}
		catch ( JMSException e )
		{
			e.printStackTrace();
		}
		try
		{
			if ( session != null )
			{
				session.close();
				System.out.println("Session closed.");
			}
		}
		catch ( JMSException e )
		{
			e.printStackTrace();
		}
		try
		{
			if ( connection != null )
			{
				connection.stop();
				connection.close();
				System.out.println("Connection stopped.");
			}
		}
		catch ( JMSException e )
		{
			e.printStackTrace();
		}
		if ( connection != null )
		{
			connectionFactory.close();
			System.out.println("ConnectionFactory closed.");
		}
		exited = true;
	}

	/**
	 * Start the count listener (parameters are optional)
	 *
	 * @param args 0: broker url
	 *             1: topic
	 * @throws JMSException
	 */
	public static void main(String[] args) throws JMSException
	{
		SimpleListenerCount client = new SimpleListenerCount();

		int length = args.length;
		if ( length >= 1 )
		{
			System.out.println("set broker url from arguments");
			client.brokerUrl = args[0];
		}
		if ( length >= 2 )
		{
			client.clientId = args[1];
		}
		if ( length == 3 )
		{
			client.topicName = args[2];
		}
		try
		{
			client.connect();
			client.subscribe(client.topicName);
			while ( !client.exited )
			{
				Thread.sleep(100);
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			client.shutdown();
		}
	}
}
