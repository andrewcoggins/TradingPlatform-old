package brown.agent.library;

import java.util.HashMap;
import java.util.Set;

import brown.agent.Agent;
import brown.assets.value.FullType;
import brown.exceptions.AgentCreationException;
import brown.markets.ContinuousDoubleAuction;
import brown.markets.LMSR;
import brown.markets.SimpleAuction;
import brown.messages.Ack;
import brown.messages.BankUpdate;
import brown.messages.Registration;
import brown.messages.auctions.BidRequest;
import brown.messages.markets.GameReport;
import brown.messages.trades.NegotiateRequest;
import brown.registrations.ValuationRegistration;
import brown.setup.Logging;
import brown.setup.library.LabGameSetup;
import brown.valuation.ValuationBundle;

public class SimpleSingleGoodAgent extends Agent {
  
  protected ValuationBundle myValuation;
  
  public SimpleSingleGoodAgent(String host, int port) throws AgentCreationException {
    super(host, port, new LabGameSetup());
    this.myValuation = new ValuationBundle();
  }
  
  @Override
  public void onRegistration(Registration registration) {
    super.onRegistration(registration);
    ValuationRegistration valuationRegistration = (ValuationRegistration) registration;
    this.myValuation.addAll(valuationRegistration.getValues());
    System.out.println(myValuation);
  }


  @Override
  public void onSimpleSealed(SimpleAuction simpleWrapper) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onSimpleOpenOutcry(SimpleAuction simpleWrapper) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onLMSR(LMSR market) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onContinuousDoubleAuction(ContinuousDoubleAuction market) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onAck(Ack message) {
    if (message.REJECTED) {
      Logging.log("[x] rej: " + message.failedBR);
    }
    
  }

  @Override
  public void onMarketUpdate(GameReport marketUpdate) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onBankUpdate(BankUpdate bankUpdate) {
    Logging.log("[-] bank " + bankUpdate.newAccount.monies);
    if (bankUpdate.newAccount.tradeables.size() > 0) {
      Logging.log("[+] victory!");
    }
    
  }

  @Override
  public void onTradeRequest(BidRequest bidRequest) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void onNegotiateRequest(NegotiateRequest tradeRequest) {
    // TODO Auto-generated method stub
    
  }
}