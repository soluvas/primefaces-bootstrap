package org.soluvas.primefacesbootstrap;

import static org.atmosphere.cpr.AtmosphereResource.TRANSPORT.LONG_POLLING;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.Meteor;
import org.atmosphere.plugin.jms.JMSBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soluvas.json.JsonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MeteorChat extends HttpServlet {
	
	private transient Logger log = LoggerFactory.getLogger(MeteorChat.class);
	
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

		m.setBroadcaster(BroadcasterFactory.getDefault().lookup(JMSBroadcaster.class, "/topic/test", true));
		m.resumeOnBroadcast(m.transport() == LONG_POLLING ? true : false)
				.suspend(-1);
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
		String body = IOUtils.toString(req.getReader());
		log.info("Input: {}", body);
		Data data;
		if (body.isEmpty())
			data = new Data("Halo", "Yuhu");
		else
			data = new ObjectMapper().readValue(body, Data.class);
		log.info("Broadcasting {}", data);
		BroadcasterFactory.getDefault().lookup(JMSBroadcaster.class, "/topic/test", true)
				.broadcast(data.toString());
	}

	public final static class Data implements Serializable {

		public String text;
		public String author;

		public Data() {
			super();
		}
		
		public Data(String author, String text) {
			this.author = author;
			this.text = text;
		}

		public String toString() {
			return JsonUtils.asJson(this);
		}
	}
}