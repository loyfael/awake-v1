package variables;

import java.util.ArrayList;

import org.apache.mina.core.session.IoSession;

public class AntiFlood {
	private static ArrayList<String> connection = new ArrayList<String>(2000);
	private static int MAX_CONNECTION = 5;

	public static boolean contains(IoSession session) {
		String ip = session.getRemoteAddress().toString().substring(1).split(":")[0];
		int a = 0;
		boolean enter = true;
		for (int i = 0; i < connection.size(); i++) {
			if (!enter)
				continue;
			if (connection.get(i).contains(ip)) {
				a += 1;
				if (a >= MAX_CONNECTION)
					enter = false;
			}
		}
		if (!enter)
			return true;
		return false;
	}

	public static void add(IoSession session) {
		String ip = session.getRemoteAddress().toString().substring(1).split(":")[0];
		String port = session.getRemoteAddress().toString().split(":")[1];
		int a = 0;
		boolean enter = true;
		for (int i = 0; i < connection.size(); i++) {
			if (!enter)
				continue;
			if (connection.get(i).contains(ip)) {
				a += 1;
				if (a >= MAX_CONNECTION)
					enter = false;
			}
		}
		if (enter)
			connection.add(ip+","+port);
	}

	public static void remove(IoSession session) {
		String ip = session.getRemoteAddress().toString().substring(1).split(":")[0];
		String port = session.getRemoteAddress().toString().split(":")[1];
		for (int i = 0; i < connection.size(); i++) {
			if (connection.get(i).contains(ip) && connection.get(i).contains(port)) {
				connection.remove(i);
			}
		}
	}
}