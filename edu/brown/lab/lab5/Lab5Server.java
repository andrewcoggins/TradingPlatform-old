package brown.lab.lab5;

import brown.auctions.onesided.OneSidedAuction;
import brown.lab.GameSetup;
import brown.server.AgentServer;
import brown.setup.Logging;

public class Lab5Server extends AgentServer {

	public Lab5Server(int port) {
		super(port, new GameSetup());
	}
	
	private void delay(int amt, boolean update) {
		int i = 0;
		while (i < amt) {
			try {
				if (update) {
					this.updateAllAuctions();
				}
				Thread.sleep(1000);
				Logging.log("[-] pause phase " + i++);
			} catch (InterruptedException e) {
				Logging.log("[+] woken: " + e.getMessage());
			}
		}
	}

	public void runGame() {
		delay(10, false);
		for (int i = 0; i < 5; i++) {
			this.manager.open(new OneSidedAuction(i, null, new LemonadeAllocation(), new LemonadePayment()));
			delay(5, true);
		}
	}
	
	public static void main(String[] args) {
		new Lab5Server(2121)
			.runGame();
	}

}
