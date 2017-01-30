package brown.server;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import brown.assets.accounting.Account;
import brown.assets.accounting.Ledger;
import brown.assets.accounting.Transaction;
import brown.assets.value.Good;
import brown.auctions.Auction;
import brown.auctions.BidBundle;
import brown.messages.BankUpdate;
import brown.messages.Registration;
import brown.messages.Rejection;
import brown.messages.auctions.Bid;
import brown.messages.markets.MarketUpdate;
import brown.messages.markets.PurchaseRequest;
import brown.messages.trades.Trade;
import brown.messages.trades.TradeRequest;
import brown.securities.Exchange;
import brown.securities.Security;
import brown.securities.SecurityWrapper;
import brown.setup.Logging;
import brown.setup.Startup;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

/**
 * This is the server that all trading agent games
 * will implement. It abstracts away all of the communication
 * logic and largely the shared market structures (auctions, markets, trading)
 * so that designers can focus on their game specifics
 */
public abstract class AgentServer {
	protected Map<Connection, Integer> connections;
	protected Map<Integer, Integer> privateToPublic;
	protected Map<Integer, Account> bank;
	//Consider time limiting these
	protected List<TradeRequest> pendingTradeRequests;
	protected Exchange exchange;
	protected Map<Integer, Auction> auctions;
	
	private int agentCount;
	private final int PORT;
	protected Server theServer;
	
	public AgentServer(int port) {
		this.PORT = port;
		this.agentCount = 0;
		this.connections = new ConcurrentHashMap<Connection,Integer>();
		this.privateToPublic = new ConcurrentHashMap<Integer, Integer>();
		this.bank = new ConcurrentHashMap<Integer, Account>();
		this.pendingTradeRequests = new CopyOnWriteArrayList<TradeRequest>();
		this.auctions = new ConcurrentHashMap<Integer, Auction>();
		this.exchange = new Exchange();
		this.privateToPublic.put(-1, -1);
		
		theServer = new Server();
	    theServer.start();
	    Startup.start(theServer.getKryo());
	    try {
			theServer.bind(PORT, PORT);
		} catch (IOException e) {
			Logging.log("[X] Server failed to start due to port conflict");
			return;
		}
	    
	    final AgentServer aServer = this;
	    theServer.addListener(new Listener() {
	        public void received (Connection connection, Object message) {
	        	Integer id = null;
	        	if (connections.containsKey(connection)) {
	        		id = connections.get(connection);
	        	} else if (message instanceof Registration) {
	        		Logging.log("[-] registration recieved from " + connection.getID());
	            	aServer.onRegistration(connection, (Registration) message);
	            	return;
	            } else {
	            	return;
	            }
	        	
	        	if (message instanceof Bid) {
	        		Logging.log("[-] bid recieved from " + id);
	        		aServer.onBid(connection, id, (Bid) message);
	            } else if (message instanceof PurchaseRequest) {
	            	Logging.log("[-] purchaserequest recieved from " + id);
	            	aServer.onPurchaseRequest(connection, id, (PurchaseRequest) message);
	            } else if (message instanceof TradeRequest) {
	            	Logging.log("[-] traderequest recieved from " + id);
	            	aServer.onTradeRequest(connection, id, (TradeRequest) message);
	            } else if (message instanceof Trade) {
	            	Logging.log("[-] trade recieved from " + id);
	            	aServer.onTrade(connection, (Trade) message);
	            }
	        }
	    });
	    Logging.log("[-] server started");
	}

	/*
	 * This method is invoked when a new agent connects to the game
	 * @param connection - details of their connection to the server
	 * @param registration - details of their game logic agent status
	 */
	protected void onRegistration(Connection connection, Registration registration) {
	  Integer theID = this.defaultRegistration(connection, registration);
	  if (theID == null) {
	    //TODO: add rejection
	    return;
	  }
	  theServer.sendToTCP(connection.getID(), new Registration(theID));
	}

	/*
	 * The server receives trade requests and forwards them to the correct agent(s)
	 * @param connection - agent connection info
	 * @param privateID - (safe) privateID of the requesting agent
	 * @param tradeRequest - the trade request
	 */
	protected void onTradeRequest(Connection connection, Integer privateID, TradeRequest tradeRequest) {
		TradeRequest tr = tradeRequest.safeCopy(privateToPublic.get(privateID));
		if (privateID != -1) {
			theServer.sendToAllTCP(tr);
		} else {
			theServer.sendToTCP(connection.getID(), tr);
		}
		pendingTradeRequests.add(tr);
	}
	
	/*
	 * What happens when a trade request is accepted or rejected
	 * @param connection - agent connection info
	 * @param trade - tuple of trade request and acceptance boolean
	 */
	protected void onTrade(Connection connection, Trade trade) {
		Integer privateTo = connections.get(connection);
		Integer privateFrom = publicToPrivate(trade.tradeRequest.fromID);
		if (privateFrom == null) {
			return;
		}
		if (pendingTradeRequests.contains(trade.tradeRequest)) {
			if (!trade.accept) {
				if (privateToPublic.get(privateTo) == trade.tradeRequest.fromID || 
						privateToPublic.get(privateTo) == trade.tradeRequest.toID) {
					pendingTradeRequests.remove(trade.tradeRequest);
				}
			} else if (privateToPublic.get(privateTo) == trade.tradeRequest.toID 
					|| trade.tradeRequest.toID == -1) {
				Account toAccount = bank.get(privateTo);
				Account fromAccount = bank.get(privateFrom);
				
				if (trade.tradeRequest.isSatisfied(toAccount, fromAccount)) {
					Account middleTo = toAccount.remove(trade.tradeRequest.moniesRequested,
							trade.tradeRequest.sharesRequested);
					Account newTo = middleTo.addAll(trade.tradeRequest.moniesOffered,
							trade.tradeRequest.sharesOffered);
					
					Account middleFrom = fromAccount.remove(trade.tradeRequest.moniesOffered,
							trade.tradeRequest.sharesOffered);
					Account newFrom = middleFrom.addAll(trade.tradeRequest.moniesRequested,
							trade.tradeRequest.sharesRequested);
					
					bank.put(privateTo, newTo);
					bank.put(privateFrom, newFrom);
					
					List<Integer> ids = new LinkedList<Integer>();
					ids.add(privateTo); 
					ids.add(privateFrom);
					sendBankUpdates(ids);
				}
			}
		}
	}

	/*
	 * This will handle the logic for requests to purchase from public markets
	 */
	protected void onPurchaseRequest(Connection connection, Integer privateID, PurchaseRequest purchaseRequest) {
		Security market = exchange.getSecurity(purchaseRequest.market.getID());
		Ledger ledger = exchange.getLedger(purchaseRequest.market.getID());
		Account oldAccount = bank.get(privateID);
		synchronized(market) {
			synchronized(oldAccount) {
				if (market == null) {
					Rejection rej = new Rejection(privateID, purchaseRequest);
					this.theServer.sendToTCP(connection.getID(), rej);
					return;
				}
				
				double cost = market.cost(purchaseRequest.buy,
						purchaseRequest.sell);
				if (oldAccount.monies >= cost) {
					//TODO: Update so that purchases can affect multiple agents
					//using multiple transactions; all safely locked etc
					List<Good> update = new LinkedList<Good>();
					if (purchaseRequest.buy > 0) {
						Transaction yes = market.buy(privateID, purchaseRequest.buy);
						update.add(yes);
						ledger.add(yes);
					}
					
					if (purchaseRequest.sell > 0) {
						Transaction no = market.sell(privateID, purchaseRequest.sell);
						update.add(no);
						ledger.add(no);
					}
					
					Account newAccount = oldAccount.addAll(0, update);
					newAccount = newAccount.remove(cost, null);
					bank.put(privateID, newAccount);
					BankUpdate bu = new BankUpdate(privateID, oldAccount, newAccount);
					theServer.sendToTCP(connection.getID(), bu);
					//this.sendMarketUpdate(market);
				} else {
					Rejection rej = new Rejection(privateID, purchaseRequest);
					theServer.sendToTCP(connection.getID(), rej);
				}
			}
		}
	}

	/*
	 * This will handle what happens when an agent sends in a bid in response to a 
	 * BidRequest for an auction
	 */
	protected void onBid(Connection connection, Integer privateID, Bid bid) {
		Auction auction = this.auctions.get(bid.AuctionID);
		if (auction != null) {
			synchronized(auction) {
				Account account = this.bank.get(privateID);
				if (account.monies >= bid.Bundle.getCost()) {
					auction.handleBid(bid.safeCopy(privateID));
				} else {
					Rejection rej = new Rejection(privateID, bid);
					this.theServer.sendToTCP(connection.getID(), rej);
				}
			}
		}
	}
	
	/*
	 * Sends a bank update to a set of agents
	 * @param List<Integer> set of IDs to send to
	 */
	public void sendBankUpdates(List<Integer> IDs) {
		synchronized(IDs) {
			for (Integer ID : IDs) {
				Connection connection = privateToConnection(ID);
				if (connection == null) {
					continue;
				}
				BankUpdate bu = new BankUpdate(ID, null, bank.get(ID));
				theServer.sendToTCP(connection.getID(), bu);
			}
		}
	}
	
	/*
	 * Sends a market update to every agent
	 * about the state of all the public markets
	 * 
	 * NOTE: No need for sync since this is access only
	 */
	public void sendAllMarketUpdates(List<Security> securities) {
		List<SecurityWrapper> markets = new LinkedList<SecurityWrapper>();
		for (Security sec : securities) {
			markets.add(sec.wrap());
		}
		MarketUpdate mupdate = new MarketUpdate(new Integer(0), markets);
		theServer.sendToAllTCP(mupdate);
	}
	
	/*
	 * Sends a auction update to every agent or
	 * closes out any finished auctions.
	 * about the state of all the public auctions
	 */
	public void updateAllAuctions() {
		synchronized(auctions) {
			List<Auction> toRemove = new LinkedList<Auction>();
			for (Auction auction : auctions.values()) {
				auction.tick(System.currentTimeMillis());
				if (auction.isOver()) {
					toRemove.add(auction);
					BidBundle winner = auction.getWinner();
					if (winner.getAgent() == null) {
						continue;
					}
					Account account = this.bank.get(winner.getAgent());
					if (account == null) {
						continue;
					}
					Good good = auction.getGood();
					good.setAgentID(winner.getAgent());
					Account newA = account.add(-1 * winner.getCost(), good);
					this.bank.put(winner.getAgent(), newA);
					this.sendBankUpdate(winner.getAgent(), account, newA);
				} else {
					for (Map.Entry<Connection, Integer> id : this.connections.entrySet()) {
						this.theServer.sendToTCP(id.getKey().getID(), auction.getBidRequest(id.getValue()));
					}
				}
			}
			
			for (Auction auction : toRemove) {
				auctions.remove(auction);
			}
		}
	}
	
	/*
	 * Sends a MarketUpdate about this specific market to all agents
	 * @param Security : the market to update on
	 */
	public void sendMarketUpdate(Security market) {
		List<SecurityWrapper> markets = new LinkedList<SecurityWrapper>();
		markets.add(market.wrap());
		MarketUpdate mupdate = new MarketUpdate(new Integer(0), markets);
		theServer.sendToAllTCP(mupdate);
	}
	
	/*
	 * Agents only know each other's public IDs. Private IDs are only known to the agents
	 * themselves and are needed to authorize any actions. The server refers to agents
	 * by their private IDs at all times.
	 */
	protected Integer publicToPrivate(Integer id) {
		for (Entry<Integer, Integer> ptp : privateToPublic.entrySet()) {
			if (ptp.getValue() == id) {
				return ptp.getKey();
			}
		}
		
		return null;
	}
	
	/*
	 * Retrieves a connection (needed to send a message to a client) from
	 * the agent's private ID
	 */
	protected Connection privateToConnection(Integer id) {
		for (Entry<Connection, Integer> ctp : connections.entrySet()) {
			if (ctp.getValue() == id) {
				return ctp.getKey();
			}
		}
		
		return null;
	}
	
	/*
	 * Retrieves an agent's bank account from its public ID
	 */
	public Account publicToAccount(Integer id) {
		return bank.get(publicToPrivate(id));
	}
	
	/*
	 * Sets an agent's bank account from its public ID
	 */
	public void setAccount(Integer id, Account account) {
		bank.put(publicToPrivate(id), account);
	}

	/*
	 * Sends a bank update to a set of agents
	 * @param List<Integer> set of IDs to send to
	 */
	public void sendBankUpdates(Set<Integer> IDs) {
		synchronized(IDs) {
			for (Integer ID : IDs) {
				Connection connection = privateToConnection(ID);
				if (connection == null) {
					continue;
				}
				BankUpdate bu = new BankUpdate(ID, null, bank.get(ID));
				theServer.sendToTCP(connection.getID(), bu);
			}
		}
	}
	
	/**
	 * Singular bank update
	 * @param ID
	 * @param oldA
	 * @param newA
	 */
	public void sendBankUpdate(Integer ID, Account oldA, Account newA) {
	  BankUpdate bu = new BankUpdate(ID, oldA, newA);
	  theServer.sendToTCP(this.privateToConnection(ID).getID(), bu);
	}
	
	public Integer defaultRegistration(Connection connection, Registration registration) {
	  if (registration.getID() == null) {
      return null;
    }
    
    Collection<Integer> allIds = connections.values();
    Integer theID = registration.getID();
    if (allIds.contains(theID)) {
      Connection oldConnection = null;
      for (Connection c : connections.keySet()) {
        if (connections.get(c).equals(theID)) {
          oldConnection = c;
          if (!oldConnection.getRemoteAddressTCP().equals(connection.getRemoteAddressTCP())) {
            return null;
          }
          break;
        }
      }
      connections.remove(oldConnection);
      
      return null;
    } else {
      theID = new Integer((int) (Math.random() * 1000000000));
      while(allIds.contains(theID)) {
        theID = new Integer((int) (Math.random() * 1000000000));
      }
      
      privateToPublic.put(theID, agentCount++);
      bank.put(theID, new Account(theID));
      
      connections.put(connection, theID);
      Logging.log("[-] registered " + theID);
      
      return theID;
    }
	}

}
