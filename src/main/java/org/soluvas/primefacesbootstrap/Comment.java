package org.soluvas.primefacesbootstrap;

import java.io.Serializable;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.joda.time.DateTime;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class Comment implements Serializable {

	public String id;
	public String authorName;
	public String body;
	public DateTime created;
	public DateTime lastModified;
	
	public Comment() {
		super();
		this.id = UUID.randomUUID().toString();
		this.created = new DateTime();
		this.lastModified = new DateTime();
	}
	
	public Comment(Node node) {
		super();
		try {
			this.id = node.getName();
			this.authorName = node.getProperty("authorName").getString();
			this.body = node.getProperty("body").getString();
			this.created = new DateTime(node.getProperty("created").getDate());
			this.lastModified = new DateTime(node.getProperty("lastModified").getDate());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Comment(String authorName, String body) {
		super();
		this.id = UUID.randomUUID().toString();
		this.authorName = authorName;
		this.body = body;
		this.created = new DateTime();
		this.lastModified = new DateTime();
	}
	
	public String getAuthorName() {
		return authorName;
	}
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public DateTime getCreated() {
		return created;
	}
	public void setCreated(DateTime created) {
		this.created = created;
	}
	public DateTime getLastModified() {
		return lastModified;
	}
	public void setLastModified(DateTime lastModified) {
		this.lastModified = lastModified;
	}
	
	@Override
	public String toString() {
		return String
				.format("Comment [authorName=%s, body=%s, created=%s, lastModified=%s]",
						authorName, body, created, lastModified);
	}

	public String getId() {
		return id;
	}

}
