package brown.assets.value;

public class FullType {
	public final TradeableType TYPE;
	public final Integer ID;
	
	public FullType() {
		this.TYPE = null;
		this.ID = null;
	}
	
	public FullType(TradeableType type, Integer ID) {
		this.TYPE = type;
		this.ID = ID;
	}
	
	@Override
	public String toString() {
		return "(" + TYPE + " " + ID + ")";
	}
}
