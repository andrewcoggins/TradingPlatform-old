package brown.test;

import java.util.LinkedList;
import java.util.List;

import brown.assets.Account;
import brown.messages.Registration;
import brown.server.AgentServer;

import com.esotericsoftware.kryonet.Connection;

public class TestServer extends AgentServer {
	
	public TestServer(int port) {
		super(port);
		GameSetup.setup(this.theServer.getKryo());
	}

	private final double B = 1.0;
	
	@Override
	protected void onRegistration(Connection connection, Registration registration) {
		super.onRegistration(connection, registration);
		
		Account oldAccount = bank.get(connections.get(connection));
		Account newAccount = oldAccount.add(100, null);
		bank.put(connections.get(connection), newAccount);
		
		List<Integer> IDS = new LinkedList<Integer>();
		IDS.add(connections.get(connection));
		this.sendBankUpdates(IDS);
	}
	
	public void startGame() {
		this.markets.put(1, new TestPM(1, B));
	}

}
