package org.soluvas.push;

import java.io.Serializable;

/**
 * General purpose growl-like notification value object.
 * 
 * It is similar to JSF's FacesMessage or SLF4J logging event.
 * @author ceefour
 */
@SuppressWarnings("serial")
public class Notification implements Serializable {

	private String message;
	private String category = "info"; 
	
	public Notification(String message, String category) {
		super();
		this.message = message;
		this.category = category;
	}

	public Notification(String message) {
		super();
		this.message = message;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return String.format("Notification [category=%s, message=%s]",
				category, message);
	}
	
}