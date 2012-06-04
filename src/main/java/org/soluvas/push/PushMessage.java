package org.soluvas.push;

import java.io.Serializable;

import org.soluvas.json.JsonUtils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
@JsonTypeInfo(use=Id.CLASS)
public class PushMessage implements Serializable {

	public PushMessage() {
		super();
	}
	
	@Override
	public String toString() {
		return JsonUtils.asJson(this);
	}

}
