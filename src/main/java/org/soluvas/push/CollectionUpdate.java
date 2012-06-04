package org.soluvas.push;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class CollectionUpdate<T> extends PushMessage {

	private String collectionName;
	private String entryId;
	private T entry;
	
	public CollectionUpdate() {
		super();
	}

	public CollectionUpdate(String collectionName, String entryId, T entry) {
		super();
		this.collectionName = collectionName;
		this.entryId = entryId;
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
	
	public String getEntryId() {
		return entryId;
	}

	public void setEntryId(String entryId) {
		this.entryId = entryId;
	}

	@Override
	public String toString() {
		return String.format(
				"CollectionUpdate [collectionName=%s, entryId=%s, entry=%s]",
				collectionName, entryId, entry);
	}

}
