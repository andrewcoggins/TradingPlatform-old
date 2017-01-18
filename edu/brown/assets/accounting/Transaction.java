package brown.assets.accounting;

import brown.assets.value.Good;
import brown.securities.Security;

public final class Transaction implements Good {
	private final Security security;
	private final double count;
	private final Integer ID;
	private final double price;
	private final long timestamp;
	
	public Transaction() {
		this.security = null;
		this.count = 0;
		this.ID = null;
		this.price = 0;
		this.timestamp = 0;
	}
	
	public Transaction(Security security, double count, Integer ID, double price) {
		this.security = security;
		this.count = count;
		this.ID = ID;
		this.price = price;
		this.timestamp = System.currentTimeMillis();
	}
	
	public Security getSecurity() {
		return security;
	}
	
	public double getCount() {
		return count;
	}
	
	public Integer getAgentID() {
		return ID;
	}
	
	public double getTransactedPrice() {
		return price;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setAgentID(Integer ID) {
		//Noop
	}
}
