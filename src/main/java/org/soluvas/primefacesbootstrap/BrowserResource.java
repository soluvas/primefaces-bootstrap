package org.soluvas.primefacesbootstrap;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.ejb.Stateless;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * @author ceefour
 */
@Stateless @Path("/browser") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
public class BrowserResource {
	
	private transient Logger log = LoggerFactory.getLogger(BrowserResource.class);
	
	@GET @Path("/game")
	public List<Game> findGames() {
		log.info("find all games");
		return ImmutableList.of(new Game("Half-life", "2010"), new Game("Gatotkaca", "2012"));
	}

	@POST @Path("/game")
	public Response createGame(Game game) {
		log.info("create game {}", game);
		return Response.created(URI.create(UUID.randomUUID().toString())).entity(game).build();
	}

	@GET @Path("/game/{gameId}")
	public Game getGame(@PathParam("gameId") String gameId) {
		log.info("get game {}", gameId);
		return new Game("Half-life", "2010");
	}
	
	@DELETE @Path("/game/{gameId}")
	public void deleteGame(@PathParam("gameId") String gameId) {
		log.info("delete game {}", gameId);
	}
	
	@PUT @Path("/game/{gameId}")
	public Game updateGame(@PathParam("gameId") String gameId, Game game) {
		log.info("update game {} with {}", gameId, game);
		return game;
	}
	
}
