package org.soluvas.push;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class CollectionAdd<T> extends PushMessage {

	private String collectionName;
	private T entry;
	
	public CollectionAdd() {
		super();
	}

	public CollectionAdd(String collectionName, T entry) {
		super("collection_add");
		this.collectionName = collectionName;
		this.entry = entry;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}

	public T getEntry() {
		return entry;
	}

	public void setEntry(T entry) {
		this.entry = entry;
	}

	@Override
	public String toString() {
		return String.format(
				"CollectionAdd [collectionName=%s, entry=%s]",
				collectionName, entry);
	}

}
