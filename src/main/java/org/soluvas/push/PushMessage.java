package org.soluvas.push;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
@JsonTypeInfo(use=Id.CLASS)
public class PushMessage implements Serializable {

	private String type;

	public PushMessage() {
		super();
	}
	
	public PushMessage(String type) {
		super();
		this.type = type;
	}
	
	@Override
	public String toString() {
		return String.format("PushMessage [type=%s]", type);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
