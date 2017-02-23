package brown.agent;

import java.io.IOException;

import brown.auctions.OneSidedWrapper;
import brown.auctions.TwoSidedWrapper;
import brown.exceptions.AgentCreationException;
import brown.messages.BankUpdate;
import brown.messages.Registration;
import brown.messages.Rejection;
import brown.messages.auctions.TradeRequest;
import brown.messages.markets.MarketUpdate;
import brown.messages.trades.NegotiateRequest;
import brown.setup.Setup;
import brown.setup.Startup;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

/**
 * All bidding agents will implement this class It abstracts away the
 * communication issues and let's authors focus on writing bidding logic.
 * 
 * @author lcamery
 */
public abstract class Agent {
	/**
	 * Kryo's client object.
	 */
	public final Client CLIENT;

	/**
	 * Agent id.
	 */
	public Integer ID;

	/**
	 * Implementations should always invoke super()
	 * 
	 * @param host
	 * @param port
	 * @param gameSetup
	 * @throws AgentCreationException
	 */
	public Agent(String host, int port, Setup gameSetup)
			throws AgentCreationException {
		this.CLIENT = new Client();
		this.ID = null;

		CLIENT.start();
		Log.TRACE();
		Kryo agentKryo = CLIENT.getKryo();
		Startup.start(agentKryo);
		if (gameSetup != null) {
			gameSetup.setup(agentKryo);
		}

		try {
			CLIENT.connect(5000, host, port, port);
		} catch (IOException e) {
			throw new AgentCreationException("Failed to connect to server");
		}

		final Agent agent = this;
		CLIENT.addListener(new Listener() {
			public void received(Connection connection, Object message) {
				synchronized (agent) {
					agent.onMessage(connection, message);
				}
			}
		});

		CLIENT.sendTCP(new Registration(-1));
	}

	/**
	 * Handles messaging logic so that game designers can override and add new
	 * message types
	 * 
	 * @param connection
	 *            : kryo connection
	 * @param message
	 *            : still in object form
	 */
	protected void onMessage(Connection connection, Object message) {
		if (message instanceof BankUpdate) {
			this.onBankUpdate((BankUpdate) message);
		} else if (message instanceof TradeRequest) {
			this.onTradeRequest((TradeRequest) message);
		} else if (message instanceof NegotiateRequest) {
			this.onNegotiateRequest((NegotiateRequest) message);
		} else if (message instanceof Registration) {
			this.onRegistration((Registration) message);
		} else if (message instanceof MarketUpdate) {
			MarketUpdate mu = (MarketUpdate) message;
			switch(mu.MECHANISM) {
			case ContinuousDoubleAuction:
				this.onContinuousDoubleAuction(mu.TMARKET);
			case LMSR:
				this.onLMSR(mu.TMARKET);
			case OpenOutcry:
				this.onOpenOutcry(mu.OMARKET);
			case SealedBid:
				this.onSealedBid(mu.OMARKET);
			default:
				this.onMarketUpdate(mu);
			}
		} else if (message instanceof Rejection) {
			this.onRejection((Rejection) message);
		}
	}

	/**
	 * Provides response to sealed bid auction
	 * @param SealedBid wrapper
	 */
	protected abstract void onSealedBid(OneSidedWrapper market);

	/**
	 * Provides agent response to OpenOutcry auction
	 * @param OpenOutcry wrapper
	 */
	protected abstract void onOpenOutcry(OneSidedWrapper market);

	/**
	 * Provides agent response to LMSR
	 * @param LMSR wrapper
	 */
	protected abstract void onLMSR(TwoSidedWrapper market);

	/**
	 * Provides agent response to CDAs
	 * @param market : CDA wrapper
	 */
	protected abstract void onContinuousDoubleAuction(TwoSidedWrapper market);

	/**
	 * Agents must accept their IDs from the server
	 * 
	 * @param registration
	 *            : includes the agent's new ID
	 */
	protected void onRegistration(Registration registration) {
		this.ID = registration.getID();
	}

	/**
	 * Whenever a request is rejected, this method is sent with the rejected
	 * request
	 * 
	 * @param rejection
	 *            : includes the rejected method and might say why
	 */
	protected abstract void onRejection(Rejection message);

	/**
	 * Whenever an unknown market changes state
	 * @param marketUpdate
	 */
	protected abstract void onMarketUpdate(MarketUpdate marketUpdate);

	/**
	 * Whenever an agent's bank changes, the server sends a bank update
	 * 
	 * @param bankUpdate
	 *            - contains the old bank state and new bank state note: both
	 *            accounts provided are immutable
	 */
	protected abstract void onBankUpdate(BankUpdate bankUpdate);

	/**
	 * Whenever an auction is occurring, the server will request a bid using
	 * this method and provide information about the auction as a part of the
	 * request
	 * 
	 * @param bidRequest
	 *            - auction metadata
	 */
	protected abstract void onTradeRequest(TradeRequest bidRequest);

	/**
	 * Whenever another agent requests a trade either directly with this agent
	 * or to all agents, this method is invoked with the details of the trade.
	 * 
	 * @param tradeRequest
	 *            - from fields describe what this agent will receive and to
	 *            fields describe what it will give up
	 */
	protected abstract void onNegotiateRequest(NegotiateRequest tradeRequest);
}
