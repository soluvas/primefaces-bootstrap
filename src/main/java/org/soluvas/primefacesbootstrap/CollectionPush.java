package org.soluvas.primefacesbootstrap;

import java.io.Serializable;

@SuppressWarnings("serial")
public class CollectionPush<T> implements Serializable {
	
	String op;
	String collection;
	T data;
	
	public CollectionPush() {
	}
	
	public CollectionPush(String op, String collection, T data) {
		super();
		this.op = op;
		this.collection = collection;
		this.data = data;
	}

	public String getOp() {
		return op;
	}
	public void setOp(String op) {
		this.op = op;
	}
	public String getCollection() {
		return collection;
	}
	public void setCollection(String collection) {
		this.collection = collection;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}

}
