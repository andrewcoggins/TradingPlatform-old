package brown.auctions.rules;

import java.util.Map;
import java.util.Set;

import brown.assets.value.Tradeable;
import brown.auctions.arules.AllocationType;
import brown.auctions.bundles.BundleType;
import brown.messages.auctions.Bid;
import brown.messages.auctions.TradeRequest;

public interface AllocationRule {

	void tick(long time);

	Map<Integer, Set<Tradeable>> getAllocations(Set<Bid> bids, Set<Tradeable> items);

	TradeRequest getBidRequest(Set<Bid> bids, Integer iD);

	boolean isPrivate();
	
	boolean isOver();

	BundleType getBundleType();

	Set<Bid> withReserve(Set<Bid> bids);

	boolean isValid(Bid bid, Set<Bid> bids);

	AllocationType getAllocationType();

}
