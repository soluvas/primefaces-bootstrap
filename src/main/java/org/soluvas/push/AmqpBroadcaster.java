package org.soluvas.push;

import java.io.IOException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Queue.BindOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class AmqpBroadcaster extends AbstractBroadcasterProxy {
    private static final Logger logger = LoggerFactory.getLogger(AmqpBroadcaster.class);

    private Set<String> subscribedEndpoints = new HashSet<String>();
    private Queue<String> messageQueue = new ConcurrentLinkedQueue<String>();
	private ScheduledExecutorService executor;
	private Connection conn;
	private Channel channel;
	private String queue;

    public AmqpBroadcaster(String id, AtmosphereConfig config) {
        super(id, null, config);
        setUp();
    }

    private void setUp() {
        try {
        	ConnectionFactory connFactory = new ConnectionFactory();
        	connFactory.setUri("amqp://guest:guest@localhost/%2F");
//        	connFactory.setHost("localhost");
//        	connFactory.setVirtualHost("/");
//        	connFactory.setUsername("guest");
//        	connFactory.setPassword("guest");
        	logger.info("Connecting to {}", connFactory);
        	conn = connFactory.newConnection();
        	channel = conn.createChannel();
        	queue = channel.queueDeclare().getQueue();
        	logger.debug("Created temporary private queue {}", queue);
        	channel.basicConsume(queue, true, new DefaultConsumer(channel) {
        		@Override
        		public void handleDelivery(String consumerTag,
        				Envelope envelope, BasicProperties properties,
        				byte[] body) throws IOException {
    				AmqpBroadcaster.this.onMessage(new String(body));
        		}
        	});
        	
        	executor = Executors.newSingleThreadScheduledExecutor();
        	executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					String msg = messageQueue.poll();
					if (msg != null) {
						logger.debug("{} broadcast {}", getID(), msg);
						broadcastReceivedMessage(msg);
					}
				}
			}, 100, 1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            String msg = "Unable to configure JMSBroadcaster";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void incomingBroadcast() {
        // we setup the consumer in the setID method. No need to do it here too
    }

    @Override
    public void setID(String id) {
        super.setID(id);
//        try {
//			channel.exchangeDeclare(id, "direct", false, true, new HashMap<String, Object>());
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
    }
    
	public void onMessage(String jsonBody) {
//		synchronized(this) {
			try {
				logger.info("{}:{} consumes {}", new Object[] {
						getID(), destroyed,
						StringUtils.abbreviate(
								StringUtils.replace(jsonBody, "\n", " "), 140) });
//				BroadcasterFuture<Object> f = new BroadcasterFuture<Object>(
//						jsonBody);
//				try {
//					Object newMsg = filter(jsonBody);
//					push(new Entry(newMsg, null, f, jsonBody));
//				} finally {
//					f.done();
//				}
	
				messageQueue.add(jsonBody);
//				 broadcastReceivedMessage(jsonBody); // this doesn't work. two consecutive broadcasts are broadcasted as one concatenated string
//				Thread.sleep(100); // FIXME: How do I avoid this? Even with this, two immediately consecutive broadcasts are not always broadcasted properly
			} catch (Exception ex) {
				logger.warn("Failed to broadcast message " + jsonBody, ex);
			}
//		}
	}
    
    public void subscribeTopic(String topicName, String filterName, String filterValue) {
    	String amqpExchange = topicName;
    	logger.info("{} Subscribing AMQP fanout exchange {} to queue {}",
    			new Object[] { getID(), amqpExchange, queue });
    	try {
			DeclareOk exchangeDeclare = channel.exchangeDeclare(amqpExchange, "fanout", true);
			logger.info("Exchange declare {}", exchangeDeclare);
			BindOk queueBind = channel.queueBind(queue, amqpExchange, amqpExchange);
			logger.info("Queue bind {}", queueBind);
		} catch (IOException e) {
			logger.error("Subscribe " + topicName, e);
			throw new RuntimeException("Subscribe " + topicName, e);
		}
    }
    
    public void unsubscribeTopic(final String topicName) {
//    	logger.info("{} Unsubscribing topic {}", getID(), topicName);
//    	final String consumerName = "jms:" + topicName;
//    	subscribedEndpoints = Sets.filter(subscribedEndpoints, new Predicate<String>() {
//    		@Override
//    		public boolean apply(String input) {
//    			if (input.startsWith(consumerName)) {
//    				logger.info("Unsubscribing from endpoint {}", input);
//    				return false;
//    			}
//    			return true;
//    		}
//		});
//    	reloadRoutes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void outgoingBroadcast(Object message) {
//        try {
//            String id = getID();
//            if (id.startsWith("/*")) {
//                id = "atmosphere";
//            }
//
//            if (publisherSession == null) {
//                throw new IllegalStateException("JMS Session is null");
//            }
//
//            TextMessage textMessage = publisherSession.createTextMessage(message
//                    .toString());
//            textMessage.setStringProperty("BroadcasterId", id);
//            publisher.send(textMessage);
//        } catch (JMSException ex) {
//            logger.warn("Failed to send message over JMS", ex);
//        }
    }

    /**
     * Close all related JMS factory, connection, etc.
     */
    @Override
    public synchronized void releaseExternalResources() {
    	logger.debug("Destroying broadcaster {}", getID());
    	executor.shutdownNow();
    	try {
			channel.close();
		} catch (IOException e) {
			logger.warn("Close AMQP channel", e);
		}
    	try {
			conn.close();
		} catch (IOException e) {
			logger.warn("Close AMQP connection", e);
		}
    }
}
