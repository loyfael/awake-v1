package realm;

import java.util.ArrayList;
import java.util.Random;

import kernel.Cmd;
import kernel.Config;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import variables.AntiFlood;

public class RealmThread implements IoHandler {
	public static ArrayList<String> attacker = new ArrayList<String>(2000);

	@Override
	public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
	}

	@Override
	public void messageReceived(IoSession arg0, Object arg1) throws Exception {
		Client client = Client._client.get(arg0.getId());
		if (client == null)
			return;
		String list = (String) arg1;
		if (!list.isEmpty()) {
			for (String packet : list.split("\n")) {
				if(Config.DEBUG)
					Cmd.println("[RECEIVED] "+packet);
				client.login(packet);
			}
		}
	}

	@Override
	public void messageSent(IoSession arg0, Object arg1) throws Exception {
	}

	@Override
	public void sessionClosed(IoSession arg0) throws Exception {
		Client client = Client._client.get(arg0.getId());
		if (client == null)
			return;
		AntiFlood.remove(arg0);
		client.getAccount().setLogged(false);
		Client._client.remove(arg0.getId());
		client.getAccount().deconnexion();
		client._send = null;
		client._account = null;
		client._perso = null;
		client._ioSession = null;
		client = null;
		if(Config.DEBUG)
			Cmd.println("Log out >> Online: "+ Client._client.size());
	}

	public static void remover(IoSession session) {
		String ip = session.getRemoteAddress().toString().substring(1).split(":")[0];
		for (int i = 0; i < attacker.size(); i++) {
			if (attacker.get(i).contains(ip)) {
				attacker.remove(i);
			}
		}
	}

	@Override
	public void sessionCreated(IoSession arg0) throws Exception {
		String ip = arg0.getRemoteAddress().toString().substring(1).split(":")[0];
		int a = 0;
		boolean ban = false;
		if (attacker.size() > 0) {
			for (int i = 0; i < attacker.size(); i++) {
				if (ban)
					continue;
				if (attacker.get(i).contains(ip)) {
					a += 1;
					if (a >= 10)
						ban = true;
				}
			}
		}
		if (ban) {
			Process child = null;
			try {
				String command = "netsh advfirewall firewall add rule name=DDOSEmu dir=in interface=any action=block remoteip="+ip+"";
				child = Runtime.getRuntime().exec(command);
			} catch (Exception e) {
				System.out.println("The banned IP could not be added to the firewall: "+ip+" Error: " + e.getMessage() + " "+child);
			}
			arg0.suspendRead();
			arg0.suspendWrite();
			arg0.getCloseFuture().setClosed();
			if (arg0.isConnected())
				arg0.close(true);
			return;
		}
		attacker.add(ip);
		if (AntiFlood.contains(arg0)) {
			arg0.close(true);
			return;
		}
		AntiFlood.add(arg0);
		Client client = new Client(arg0, genKey());
		client._send.REALM_SEND_HC_PACKET(client.getKey());
		if(Config.DEBUG)
			Cmd.println("<< New login >>");
	}

	@Override
	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
	}

	@Override
	public void sessionOpened(IoSession arg0) throws Exception {

	}

	public String genKey() {
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		StringBuilder hashKey = new StringBuilder();
		Random rand = new Random();
		for(int i = 0; i < 32; i++) 
			hashKey.append(alphabet.charAt(rand.nextInt(alphabet.length())));
		return hashKey.toString();
	}
}