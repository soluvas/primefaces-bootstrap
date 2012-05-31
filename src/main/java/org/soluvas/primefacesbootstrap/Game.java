package org.soluvas.primefacesbootstrap;

import java.io.Serializable;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class Game implements Serializable {

	private String name;
	private String releaseDate;
	
	public Game() {
	}
	
	public Game(String name, String releaseDate) {
		super();
		this.name = name;
		this.releaseDate = releaseDate;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	@Override
	public String toString() {
		return String.format("Game [name=%s, releaseDate=%s]", name,
				releaseDate);
	}
	
}
