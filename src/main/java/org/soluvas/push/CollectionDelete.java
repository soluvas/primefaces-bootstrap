package org.soluvas.push;

/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class CollectionDelete extends PushMessage {

	private String collectionName;
	private String entryId;
	
	public CollectionDelete() {
		super();
	}

	public CollectionDelete(String collectionName, String entryId) {
		super("collection_delete");
		this.collectionName = collectionName;
		this.entryId = entryId;
	}

	public String getCollectionName() {
		return collectionName;
	}

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
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
				"CollectionDelete [collectionName=%s, entryId=%s]",
				collectionName, entryId);
	}

}
