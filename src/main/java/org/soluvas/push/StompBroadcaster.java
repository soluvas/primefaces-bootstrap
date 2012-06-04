package org.soluvas.push;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.ConnectionFactory;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.BroadcasterFuture;
import org.atmosphere.cpr.DefaultBroadcaster.Entry;
import org.atmosphere.plugin.jms.JMSBroadcaster;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soluvas.primefacesbootstrap.CommentResource;

import pk.aamir.stompj.Connection;
import pk.aamir.stompj.Message;
import pk.aamir.stompj.MessageHandler;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class StompBroadcaster extends AbstractBroadcasterProxy {
    private static final String JMS_TOPIC = JMSBroadcaster.class.getName() + ".topic";
    private static final String JNDI_NAMESPACE = JMSBroadcaster.class.getName() + ".JNDINamespace";
    private static final String JNDI_FACTORY_NAME = JMSBroadcaster.class.getName() + ".JNDIConnectionFactoryName";
    private static final String JNDI_TOPIC = JMSBroadcaster.class.getName() + ".JNDITopic";
    private static final Logger logger = LoggerFactory.getLogger(StompBroadcaster.class);

    private String topicId = "atmosphere";
    private String factoryName = "atmosphereFactory";
    private String namespace = "jms/";
    private Set<String> subscribedEndpoints = new HashSet<String>();
    private DefaultCamelContext camel;
    private ProducerTemplate producer;
    private Connection conn;
    private Queue<String> messageQueue = new ConcurrentLinkedQueue<String>();
	private ScheduledExecutorService executor;

    public StompBroadcaster(String id, AtmosphereConfig config) {
        super(id, null, config);
        setUp();
    }

    private void setUp() {
        try {
        	conn = new Connection("localhost", 61613, "guest", "password");
        	logger.info("Connecting to {}", conn);
        	conn.connect();
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
    	String stompTopic = "jms.topic." + topicName;
    	logger.info("{} Subscribing Stomp topic {}", getID(), stompTopic);
    	conn.subscribe(stompTopic, true);
    	conn.addMessageHandler(stompTopic, new MessageHandler() {
			@Override
			public void onMessage(Message msg) {
				StompBroadcaster.this.onMessage(msg.getContentAsString());
			}
		});
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
    	logger.warn("Destroying broadcaster {}", getID());
    	executor.shutdownNow();
    	conn.disconnect();
    }
}
