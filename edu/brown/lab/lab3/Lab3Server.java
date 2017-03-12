package brown.lab.lab3;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import brown.assets.accounting.Account;
import brown.assets.value.ITradeable;
import brown.auctions.arules.OpenOutcryRule;
import brown.auctions.arules.SealedBidRule;
import brown.auctions.bundles.BundleType;
import brown.auctions.bundles.SimpleBidBundle;
import brown.auctions.onesided.OneSidedAuction;
import brown.auctions.prules.FirstPriceRule;
import brown.auctions.prules.SecondPriceRule;
import brown.auctions.rules.PaymentRule;
import brown.lab.GameSetup;
import brown.lab.ValuationRegistration;
import brown.messages.Registration;
import brown.server.AgentServer;
import brown.setup.Logging;

import com.esotericsoftware.kryonet.Connection;

/**
 * Implementation of lab3 server.
 * 
 * @author lcamery
 */
public class Lab3Server extends AgentServer {

	public Lab3Server(int port) {
		super(port, new GameSetup());
	}

	@Override
	protected void onRegistration(Connection connection,
			Registration registration) {
		Integer theID = this.defaultRegistration(connection, registration);
		if (theID == null) {
			return;
		}

		double nextValue = Math.random() * 100;
		this.theServer.sendToTCP(connection.getID(), new ValuationRegistration(
				theID, nextValue));

		Account oldAccount = bank.get(connections.get(connection));
		Account newAccount = oldAccount.addAll(100, null);
		bank.put(connections.get(connection), newAccount);

		List<Integer> IDS = new LinkedList<Integer>();
		IDS.add(connections.get(connection));
		this.sendBankUpdates(IDS);
	}

	public void runGame(boolean outcry, boolean firstprice, double reserve) {
		// Constructs auction according to rules
		Set<ITradeable> theSet = new HashSet<ITradeable>();
		theSet.add(new Lab3Good());
		PaymentRule prule = firstprice ? new FirstPriceRule()
				: new SecondPriceRule();
		if (outcry) {
			this.manager.open(new OneSidedAuction(0, theSet,
					new OpenOutcryRule(BundleType.Simple, true, 5,
							new SimpleBidBundle(reserve, null,
									BundleType.Simple)), prule));
		} else {
			this.manager.open(new OneSidedAuction(0, theSet, new SealedBidRule(
					BundleType.Simple, true, 5, new SimpleBidBundle(reserve,
							null, BundleType.Simple)), prule));
		}

		// Gives everyone 20 seconds to join the auction
		int i = 0;
		while (i < 10) {
			try {
				Thread.sleep(1000);
				Logging.log("[-] setup phase " + i++);
			} catch (InterruptedException e) {
				Logging.log("[+] woken: " + e.getMessage());
			}
		}

		// Runs the auction to completion
		OneSidedAuction market = this.manager.getOneSided(0);
		while (!market.isClosed()) {
			try {
				Thread.sleep(1000);
				this.updateAllAuctions();
			} catch (InterruptedException e) {
				Logging.log("[+] woken: " + e.getMessage());
			}
		}

		// Logs the winner and price
		//Map<BidBundle, Set<ITradeable>> bundles = this.manager.getOneSided(0)
		//		.getWinners();
		//Logging.log("[-] auction over");
		//for (BidBundle b : bundles.keySet()) {
		//	Logging.log("[-] winner: " + b.getAgent() + " for " + b.getCost());
		//}
	}

	public static void main(String[] args) {
		Lab3Server l3s = new Lab3Server(2121);
		l3s.runGame(true, true, 0);
	}

}
