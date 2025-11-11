import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQMessageProducer;
import org.apache.activemq.artemis.jms.client.ActiveMQSession;

/**
 * Simple publisher application, that publishes an incrementing counter as TextMessage continuously.
 *
 * @author Maximilian Rieder
 */
public class SimplePublisherCount
{
	private ActiveMQConnectionFactory connectionFactory = null;
	private ActiveMQConnection connection = null;
	 //String brokerUrl = "(tcp://hostA:6666,tcp://hostB:6666)?failoverAttempts=-1";
	 String brokerUrl = "tcp://a:6666";
	private String topicName = "count.topic";
	private long ttlOnTopics = 180000;
	private ActiveMQSession session;
	private ActiveMQMessageProducer producer;
	private int interval = 2000;
	private int count = 0;

	public SimplePublisherCount()
	{
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
		System.out.println("Attempt to create  connection");
		connection = (ActiveMQConnection) connectionFactory.createConnection();
		System.out.println("Connection established.");
		connection.start();
		System.out.println("Connection started.");

		session = (ActiveMQSession) connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		producer = (ActiveMQMessageProducer) session.createProducer(null);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		producer.setPriority(Message.DEFAULT_PRIORITY);
		producer.setTimeToLive(ttlOnTopics);
		System.out.println("Producer instantiated.");
	}

	/**
	 * stop and close
	 */
	public void shutdown()
	{
		try
		{
			if ( producer != null )
			{
				producer.close();
				System.out.println("Producer closed.");
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
				System.out.println("Connection stopped.");
				connection.close();
				System.out.println("Connection closed.");
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
	}

	/**
	 * Starts endlessly publishing an incrementing counter.
	 *
	 * @param subject defines the topic that is published to.
	 * @throws JMSException if something fails.
	 */
	public void startPublishLoop(String subject) throws JMSException
	{
		Topic topic = session.createTopic(subject);

		while ( true )
		{
			TextMessage jmsMessage = session.createTextMessage();
			jmsMessage.setText(String.valueOf(count));
			producer.send(topic, jmsMessage);
			System.out.println("Message published. " + jmsMessage);
			count++;
			try
			{
				Thread.sleep(this.interval);
			}
			catch ( InterruptedException e )
			{
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * Start the count publisher (parameters are optional)
	 *
	 * @param args 0: broker url
	 *             1: topic
	 */
	public static void main(String[] args)
	{
		SimplePublisherCount client = new SimplePublisherCount();
		if ( args.length >= 1 )
		{
			client.brokerUrl = args[0];
			System.out.println("set broker url from arguments");
		}
		if ( args.length == 2 )
		{
			client.topicName = args[1];
		}
		try
		{
			client.connect();
			client.startPublishLoop(client.topicName);
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
