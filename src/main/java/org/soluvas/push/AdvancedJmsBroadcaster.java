/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 */
package org.soluvas.push;

import java.util.HashSet;
import java.util.Set;

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

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Simple {@link org.atmosphere.cpr.Broadcaster} implementation based on JMS
 * <p/>
 * The {@link ConnectionFactory} name's is jms/atmosphereFactory The
 * {@link Topic} by constructing "BroadcasterId =
 * {@link org.atmosphere.cpr.Broadcaster#getID}
 *
 * @author Jeanfrancois Arcand
 */
public class AdvancedJmsBroadcaster extends AbstractBroadcasterProxy {
    private static final String JMS_TOPIC = JMSBroadcaster.class.getName() + ".topic";
    private static final String JNDI_NAMESPACE = JMSBroadcaster.class.getName() + ".JNDINamespace";
    private static final String JNDI_FACTORY_NAME = JMSBroadcaster.class.getName() + ".JNDIConnectionFactoryName";
    private static final String JNDI_TOPIC = JMSBroadcaster.class.getName() + ".JNDITopic";
    private static final Logger logger = LoggerFactory.getLogger(AdvancedJmsBroadcaster.class);

    private String topicId = "atmosphere";
    private String factoryName = "atmosphereFactory";
    private String namespace = "jms/";
    private Set<String> subscribedEndpoints = new HashSet<String>();
    private DefaultCamelContext camel;
    private ProducerTemplate producer;

    public AdvancedJmsBroadcaster(String id, AtmosphereConfig config) {
        super(id, null, config);
        setUp();
    }

    private void setUp() {
        try {
            // For backward compatibility.
            if (config.getInitParameter(JMS_TOPIC) != null) {
                topicId = config.getInitParameter(JMS_TOPIC);
            }

            if (config.getInitParameter(JNDI_NAMESPACE) != null) {
                namespace = config.getInitParameter(JNDI_NAMESPACE);
            }

            if (config.getInitParameter(JNDI_FACTORY_NAME) != null) {
                factoryName = config.getInitParameter(JNDI_FACTORY_NAME);
            }

            if (config.getInitParameter(JNDI_TOPIC) != null) {
                topicId = config.getInitParameter(JNDI_TOPIC);
            }

    		logger.info("Looking up Connection Factory {}", namespace + factoryName);
            Context ctx = new InitialContext();
            ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(namespace + factoryName);

    		camel = new DefaultCamelContext();
    		camel.addComponent("jms", new JmsComponent(new JmsConfiguration(connectionFactory)));
    		camel.start();
    		producer = camel.createProducerTemplate();
    		producer.start();
        } catch (Exception e) {
            String msg = "Unable to configure JMSBroadcaster";
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
    
    public void reloadRoutes() {
    	try {
    		logger.info("Reloading routes for {}", getID());
			camel.removeRouteDefinitions(camel.getRouteDefinitions());
			camel.addRoutes(new RouteBuilder() {
				@Override
				public void configure() throws Exception {
					for (String endpoint : subscribedEndpoints) {
						logger.info("Subscribing to {}", endpoint);
						from(endpoint).process(new Processor() {
							@Override
							public void process(Exchange exchange) throws Exception {
								onMessage((String)exchange.getIn().getBody());
							}
						});
//						from(endpoint).to("log:" + getID());
					}
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("reloadRoutes", e);
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
		synchronized(this) {
			try {
				logger.info(
						"{} consumes {}",
						getID(),
						StringUtils.abbreviate(
								StringUtils.replace(jsonBody, "\n", " "), 140));
				BroadcasterFuture<Object> f = new BroadcasterFuture<Object>(
						jsonBody);
				try {
					Object newMsg = filter(jsonBody);
					push(new Entry(newMsg, null, f, jsonBody));
				} finally {
					f.done();
				}
	
				// broadcastReceivedMessage(jsonBody); // this doesn't work. two consecutive broadcasts are broadcasted as one concatenated string
				Thread.sleep(100); // FIXME: How do I avoid this? Even with this, two immediately consecutive broadcasts are not always broadcasted properly
			} catch (Exception ex) {
				logger.warn("Failed to broadcast message " + jsonBody, ex);
			}
		}
	}
    
    public void subscribeTopic(String topicName, String filterName, String filterValue) {
    	String consumerName = "jms:topic:" + topicName;// + "?selector=" + filterName + "%3D" + filterValue;
    	if (subscribedEndpoints.contains(consumerName)) {
    		logger.warn("{} Ignoring subscribing using existing consumer {}", getID(), consumerName);
    		return;
    	}
        try {
			logger.info("{} Subscribing to {}", new Object[] { getID(), consumerName });
			subscribedEndpoints.add(consumerName);
			reloadRoutes();
//			MessageConsumer consumer = consumerSession.createConsumer(topic, selector);
//            consumer.setMessageListener(new MessageListener() {
//                @Override
//                public void onMessage(Message msg) {
//                    try {
//                    	logger.info("{} consumes {}", getID(), msg);
//                    	msg.acknowledge();
//                    	TextMessage textMessage = (TextMessage) msg;
//                    	broadcastReceivedMessage(textMessage.getText());
////                        ObjectMessage objMessage = (ObjectMessage) msg;
////                        ObjectMapper mapper = new ObjectMapper();
////                        String objStr = mapper.writeValueAsString(objMessage.getObject());
////                        broadcastReceivedMessage(objStr);
//                    } catch (Exception ex) {
//                        logger.warn("Failed to broadcast message "+ msg, ex);
//                    }
//                }
//            });
		} catch (Exception e) {
			throw new RuntimeException("Cannot subscribe to +"+ topicName + " with "+ filterName +"="+ filterValue, e);
		}
    }
    
    public void unsubscribeTopic(final String topicName) {
    	logger.info("{} Unsubscribing topic {}", getID(), topicName);
    	final String consumerName = "jms:" + topicName;
    	subscribedEndpoints = Sets.filter(subscribedEndpoints, new Predicate<String>() {
    		@Override
    		public boolean apply(String input) {
    			if (input.startsWith(consumerName)) {
    				logger.info("Unsubscribing from endpoint {}", input);
    				return false;
    			}
    			return true;
    		}
		});
    	reloadRoutes();
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
        try {
            producer.stop();
        } catch (Throwable ex) {
            logger.warn("releaseExternalResources: close consumerSession", ex);
        }
        try {
        	camel.stop();
        } catch (Throwable ex) {
            logger.warn("releaseExternalResources: close connection", ex);
        }
    }
}
