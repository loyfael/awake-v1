package kernel;

import java.io.BufferedReader;
import java.io.FileReader;

public class Config {
	private static final String CONFIG_FILE = "config.txt";
	public static boolean DEBUG = false; 
	public static String DB_HOST;
	public static String DB_USER;
	public static String DB_PASS;
	public static String DB_NAME;
	public static int CONFIG_DB_COMMIT = 30*1000;
	public static int REALM_PORT = 444;
	public static int LEVEL_START = 1;
	public static int CELLID_DEPART = 198;
	private static BufferedReader config;

	public static void loadConfiguration() {
		try {
			config = new BufferedReader(new FileReader(CONFIG_FILE));
			String line = "";
			while ((line=config.readLine())!=null) {
				if(line.split("=").length == 1) 
					continue;
				String param = line.split("=")[0].trim();
				String value = line.split("=")[1].trim();
				if(param.equalsIgnoreCase("DEBUG")) {
					if(value.equalsIgnoreCase("true")) {
						DEBUG = true;
						Cmd.println("Mode Debug: On");
					}
				} else if(param.equalsIgnoreCase("DB_HOST")) {
					DB_HOST = value;
				} else if(param.equalsIgnoreCase("DB_USER")) {
					DB_USER = value;
				} else if(param.equalsIgnoreCase("DB_PASS")) {
					if(value == null) 
						value = "";
					DB_PASS = value;
				} else if(param.equalsIgnoreCase("DB_NAME")) {
					DB_NAME = value;
				} else if(param.equalsIgnoreCase("REALM_PORT")) {
					REALM_PORT = Integer.parseInt(value);
				}
			}
			if(DB_NAME == null || DB_HOST == null || DB_PASS == null || DB_USER == null) {
				throw new Exception();
			}
		} catch (Exception e) {
			Cmd.println(e.getMessage());
			Cmd.println("Server closed...");
			System.exit(1);
		}
	}
}