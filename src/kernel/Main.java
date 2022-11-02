package kernel;

import java.util.Timer;

import kernel.Cmd.Color;
import realm.RealmServer;
import data.Constant;
import data.DatabaseManager;
import data.World;

public class Main {
	private static RealmServer RealmServer = null;
	public static Timer TimersPelea = new Timer();
	public static int MAXPA = 6;
	public static int MAXPM = 8;
	public static void main(String[] args) {
		Cmd.clear();
		Cmd.setTitle("Team awake "+Constant.version + "");
		Cmd.println("---------------------------------------\n", Color.CYAN);
		Cmd.println("  Awake starting ! Please waiting...\n", Color.BLUE);
		Cmd.println("---------------------------------------\n", Color.CYAN);
		if(Config.DEBUG)
			Cmd.println("Loading configuration...", Color.YELLOW);
		Config.loadConfiguration();
		if (Config.DEBUG) {
			Cmd.println("Configuration loaded.", Color.BG_GREEN);
			Cmd.println("Connecting Database...", Color.YELLOW);
		}
		if (DatabaseManager.setUpConnexion()) {
			if(Config.DEBUG)
				Cmd.println("Database is connected.", Color.BG_GREEN);
		} else {
			Cmd.println("[ERROR] Disabled DB connexion ! Please check available user and password !", Color.RED);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
		World.LoadWorld();
		RealmServer = new RealmServer();
		RealmServer.start();
	}
}