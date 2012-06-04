package org.soluvas.primefacesbootstrap;

import static org.atmosphere.cpr.AtmosphereResource.TRANSPORT.LONG_POLLING;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.Meteor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soluvas.push.AmqpBroadcaster;
import org.soluvas.push.PushMessage;
import org.soluvas.push.SubscribeTopic;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("serial")
public class MeteorChat extends HttpServlet {
	
	private transient Logger log = LoggerFactory.getLogger(MeteorChat.class);
	
//	@Inject CamelContext camel;
	
	/**
	 * Create a {@link Meteor} and use it to suspend the response.
	 * 
	 * @param req
	 *            An {@link HttpServletRequest}
	 * @param res
	 *            An {@link HttpServletResponse}
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		// Set the logger level to TRACE to see what's happening.
		Meteor m = Meteor.build(req).addListener(
				new AtmosphereResourceEventListenerAdapter());
		// X-Atmosphere-tracking-id
		String trackingId = req.getHeader("X-Atmosphere-tracking-id");

//		m.setBroadcaster(BroadcasterFactory.getDefault().lookup(JMSBroadcaster.class, "/topic/test", true));
//		AdvancedJmsBroadcaster broadcaster = (AdvancedJmsBroadcaster) BroadcasterFactory.getDefault().lookup(AdvancedJmsBroadcaster.class, trackingId, true);
		AmqpBroadcaster broadcaster = getBroadcaster(trackingId);
		m.setBroadcaster(broadcaster);
		m.resumeOnBroadcast(m.transport() == LONG_POLLING ? true : false)
				.suspend(-1);
		log.info("Meteor suspend3 {} {}", m.getBroadcaster().getID(), m.getBroadcaster().getScope());
	}

	/**
	 * Re-use the {@link Meteor} created on the first GET for broadcasting
	 * message.
	 * 
	 * @param req
	 *            An {@link HttpServletRequest}
	 * @param res
	 *            An {@link HttpServletResponse}
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		String trackingId = req.getHeader("X-Atmosphere-tracking-id");
//		AdvancedJmsBroadcaster broadcaster = (AdvancedJmsBroadcaster) BroadcasterFactory.getDefault().lookup(AdvancedJmsBroadcaster.class, trackingId, true);
		AmqpBroadcaster broadcaster = getBroadcaster(trackingId);
		
		ObjectMapper mapper = new ObjectMapper();
		PushMessage message = mapper.readValue(req.getReader(), PushMessage.class);
		log.info("Atmosphere Client pushes: {}", message);
		if (message instanceof SubscribeTopic) {
			SubscribeTopic subscribeTopic = (SubscribeTopic)message;
			broadcaster.subscribeTopic(subscribeTopic.getTopic(), subscribeTopic.getFilterName(), subscribeTopic.getFilterValue());
		}
	}
	
	protected AmqpBroadcaster getBroadcaster(String trackingId) {
//		StompBroadcaster broadcaster = (StompBroadcaster)BroadcasterFactory.getDefault().lookup(StompBroadcaster.class, trackingId, true);
		AmqpBroadcaster broadcaster = (AmqpBroadcaster)BroadcasterFactory.getDefault().lookup(AmqpBroadcaster.class, trackingId, true);
		return broadcaster;
	}

}