package org.soluvas.primefacesbootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.jackrabbit.core.TransientRepository;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.plugin.jms.JMSBroadcaster;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soluvas.json.JsonUtils;
import org.soluvas.push.CollectionAdd;
import org.soluvas.push.CollectionDelete;
import org.soluvas.push.CollectionUpdate;
import org.soluvas.push.Notification;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * @author ceefour
 */
@Singleton @Startup
@Path("/comment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CommentResource {

	private transient Logger log = LoggerFactory
			.getLogger(CommentResource.class);
	private Session session;
	private Node commentRoot;
	private TransientRepository repository;
	public static CamelContext camel;
	ProducerTemplate producer;
	@Resource(mappedName="java:/ConnectionFactory") ConnectionFactory jmsFactory;
	
	@PostConstruct
	public void init() throws Exception {
		log.info("Starting CommentResource");
		repository = new TransientRepository(new File("/home/ceefour/git/primefaces-bootstrap/jcr-data"));
		session = repository.login(new SimpleCredentials("TestUser", "".toCharArray())); 
		
		String user = session.getUserID();
		String name = repository.getDescriptor(Repository.REP_NAME_DESC);
		log.info("Logged in as {} to a {} repository.", user, name);
		
		Node root = session.getRootNode();
		if (!root.hasNode("comment")) {
			log.info("Creating comment root node under {}", root);
			commentRoot = root.addNode("comment");
			log.info("Created comment root node: {} - {}", commentRoot.getIdentifier(), commentRoot.getPath());
			session.save();
		} else {
			commentRoot = root.getNode("comment");
		}
		
		camel = new DefaultCamelContext();
		camel.addComponent("jms", new JmsComponent(new JmsConfiguration(jmsFactory)));
		camel.start();
		producer = camel.createProducerTemplate();
		producer.start();
	}

	@PreDestroy
	public void destroy() throws Exception {
		producer.stop();
		camel.stop();
		session.logout();
		repository.shutdown();
	}
	
	protected Broadcaster getBroadcaster() {
		return BroadcasterFactory.getDefault().lookup(JMSBroadcaster.class, "/topic/test", true);
	}

	@GET
	public List<Comment> findAll() throws RepositoryException {
		log.info("find all comments");
		
		List<Comment> comments = Lists.newArrayList( Iterators.transform(commentRoot.getNodes(), new Function<Node, Comment>() {
			@Override
			public Comment apply(Node node) {
				return new Comment(node);
			}
		}) );
		return comments;
	}

	@POST
	public Response create(Comment comment) throws RepositoryException, CamelExecutionException, JsonGenerationException, JsonMappingException, IOException {
		log.info("create comment {}", comment);
		
		Node commentNode = commentRoot.addNode(comment.getId());
		commentNode.setProperty("authorName", comment.getAuthorName());
		commentNode.setProperty("body", comment.getBody());
		commentNode.setProperty("created", comment.getCreated().toGregorianCalendar());
		commentNode.setProperty("lastModified", comment.getLastModified().toGregorianCalendar());
		session.save();
		
//		CollectionPush<Comment> push = new CollectionPush<Comment>("add", "comment", comment);
//		getBroadcaster().broadcast(JsonUtils.asJson(push));
		CollectionAdd<Comment> push = new CollectionAdd<Comment>("comment", comment);
//		producer.sendBodyAndHeader("jms:topic:product", push, "productId", "zibalabel_t01");
		producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(push), "productId", "zibalabel_t01");
		
		Notification notification = new Notification(comment.getAuthorName() +" berkomentar: "+ comment.getBody());
		producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(new CollectionAdd<Notification>("notification", notification)), "productId", "zibalabel_t01");
		
		return Response.created(URI.create(comment.getId()))
				.entity(comment).build();
	}

	@GET @Path("/{commentId}")
	public Comment findOne(@PathParam("commentId") String commentId) {
		log.info("get comment {}", commentId);
		try {
			Node commentNode = commentRoot.getNode(commentId);
			Comment comment = new Comment(commentNode);
			return comment;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@DELETE @Path("/{commentId}")
	public Response delete(@PathParam("commentId") String commentId) {
		log.info("delete comment {}", commentId);
		try {
			Node commentNode = commentRoot.getNode(commentId);
			Comment comment = new Comment(commentNode);
			commentNode.remove();
			session.save();
			
//			CollectionPush<Comment> push = new CollectionPush<Comment>("delete", "comment", comment);
//			getBroadcaster().broadcast(JsonUtils.asJson(push));
			CollectionDelete push = new CollectionDelete("comment", commentId);
			producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(push), "productId", "zibalabel_t01");
			
			Notification notification = new Notification("Komentar dari "+ comment.getAuthorName() +" dihapus.");
			producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(new CollectionAdd<Notification>("notification", notification)), "productId", "zibalabel_t01");
			
			return Response.noContent().build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@PUT @Path("/{commentId}")
	public Comment update(@PathParam("commentId") String commentId, Comment comment) {
		log.info("update comment {} with {}", commentId, comment);
		try {
			Node commentNode = commentRoot.getNode(commentId);
			commentNode.setProperty("body", comment.getBody());
			commentNode.setProperty("lastModified", new DateTime().toGregorianCalendar());
			session.save();
			
			Comment updatedComment = new Comment(commentNode);
			
//			CollectionPush<Comment> push = new CollectionPush<Comment>("update", "comment", updatedComment);
//			getBroadcaster().broadcast(JsonUtils.asJson(push));
			CollectionUpdate<Comment> push = new CollectionUpdate<Comment>("comment", commentId, updatedComment);
			producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(push), "productId", "zibalabel_t01");
			
			Notification notification = new Notification(comment.getAuthorName() +" menyunting komentarnya.");
			producer.sendBodyAndHeader("jms:topic:product", ExchangePattern.InOnly, JsonUtils.asJson(new CollectionAdd<Notification>("notification", notification)), "productId", "zibalabel_t01");
			
			return comment;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
