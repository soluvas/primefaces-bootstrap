package org.soluvas.primefacesbootstrap;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
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

import org.apache.jackrabbit.core.TransientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * @author ceefour
 */
@Singleton
@Path("/browser")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BrowserResource {

	private transient Logger log = LoggerFactory
			.getLogger(BrowserResource.class);
	private Session session;

	@PostConstruct
	public void init() throws LoginException, RepositoryException {
		Repository repository = new TransientRepository(new File("/home/ceefour/git/primefaces-bootstrap/jcr-data"));
		session = repository.login(new SimpleCredentials("TestUser", "".toCharArray())); 
		
		String user = session.getUserID();
		String name = repository.getDescriptor(Repository.REP_NAME_DESC);
		log.info("Logged in as {} to a {} repository.", user, name);
		
		createGame(new Game("Half-life", "2010"));
		createGame(new Game("Gatotkaca", "2012"));
	}

	@PreDestroy
	public void destroy() {
		session.logout();
	}

	@GET
	@Path("/game")
	public List<Game> findGames() throws RepositoryException {
		log.info("find all games");
		
		Node root = session.getRootNode();
		List<Game> games = Lists.newArrayList( Iterators.transform(root.getNodes(), new Function<Node, Game>() {
			@Override
			public Game apply(Node node) {
				try {
					return new Game(node.getProperty("name").getString(), node.getProperty("releaseDate").getString());
				} catch (Exception e) {
					log.error("transform", e);
					return null;
				}
			}
		}) );
		return games;
		
//		return ImmutableList.of(new Game("Half-life", "2010"), new Game(
//				"Gatotkaca", "2012"));
	}

	@POST
	@Path("/game")
	public Response createGame(Game game) throws RepositoryException {
		log.info("create game {}", game);
		
		Node root = session.getRootNode();
		Node gameNode = root.addNode(UUID.randomUUID().toString());
		gameNode.setProperty("name", game.getName());
		gameNode.setProperty("releaseDate", game.getReleaseDate());
		session.save();
		
		return Response.created(URI.create(UUID.randomUUID().toString()))
				.entity(game).build();
	}

	@GET
	@Path("/game/{gameId}")
	public Game getGame(@PathParam("gameId") String gameId) {
		log.info("get game {}", gameId);
		return new Game("Half-life", "2010");
	}

	@DELETE
	@Path("/game/{gameId}")
	public void deleteGame(@PathParam("gameId") String gameId) {
		log.info("delete game {}", gameId);
	}

	@PUT
	@Path("/game/{gameId}")
	public Game updateGame(@PathParam("gameId") String gameId, Game game) {
		log.info("update game {} with {}", gameId, game);
		return game;
	}

}
