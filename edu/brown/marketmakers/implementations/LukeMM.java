package brown.marketmakers.implementations;

public class LukeMM extends LiquiditySensitive {
	private int count;

	public LukeMM(double alpha) {
		super(alpha);
		this.count = 2;
	}
	
	/*
	 * Returns a share to an agent that buys yes
	 * @param shareNum : int
	 */
	public void yes(Integer agentID, double shareNum) {
		this.yes += shareNum;
		count++;
	}
	
	/*
	 * Returns a share to an agent that buys no
	 * @param shareNum : int
	 */
	public void no(Integer agentID, double shareNum) {
		this.no += shareNum;
		count++;
	}
	
	@Override
	protected double getB() {
		return (this.alpha) * (this.yes + this.no) * this.count;
	}
	
	public static void main(String[] args) {
		LukeMM luke = new LukeMM(.2);
		System.out.println(luke.ask(1));
		/*luke.no(null, 100);
		luke.yes(null, 100);
		*/
		luke.no(null, 50);
		luke.yes(null, 50);
		System.out.println(luke.ask(1));
		luke.no(null, 50);
		luke.yes(null, 50);
		System.out.println(luke.ask(1));
		luke.no(null, 100);
		luke.yes(null, 100);
		System.out.println(luke.ask(1));
	}

}
