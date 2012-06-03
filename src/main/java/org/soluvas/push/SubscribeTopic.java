package org.soluvas.push;


/**
 * @author ceefour
 */
@SuppressWarnings("serial")
public class SubscribeTopic extends PushMessage {
	
	private String topic;
	private String filterName;
	private String filterValue;

	public SubscribeTopic() {
	}

	public SubscribeTopic(String topic, String filterName, String filterValue) {
		super();
		this.topic = topic;
		this.filterName = filterName;
		this.filterValue = filterValue;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getFilterName() {
		return filterName;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public String getFilterValue() {
		return filterValue;
	}

	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}

	@Override
	public String toString() {
		return String.format(
				"SubscribeTopic [topic=%s, filterName=%s, filterValue=%s]",
				topic, filterName, filterValue);
	}
	
}
