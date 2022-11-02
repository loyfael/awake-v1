package data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import kernel.Cmd;
import kernel.Cmd.Color;
import kernel.Config;
import variables.Action;
import variables.Map;
import variables.NPC_tmpl;
import variables.NPC_tmpl.NPC_question;
import variables.NPC_tmpl.NPC_reponse;
import variables.Item;
import variables.Item.ObjTemplate;
import variables.Spell;
import variables.Spell.SortStats;
import client.Account;
import client.Player;

import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

public class DatabaseManager {
	private static Connection Connection;
	private static Lock myConnectionLocker = new ReentrantLock();
	private static Timer timerCommit;
	private static boolean needCommit;

	public static boolean setUpConnexion() {
		try {
			myConnectionLocker.lock();
			Connection = DriverManager.getConnection("jdbc:mysql://" + Config.DB_HOST + "/" + Config.DB_NAME, Config.DB_USER, Config.DB_PASS);
			Connection.setAutoCommit(false);
			if (!Connection.isValid(1000)) {
				if(Config.DEBUG)
					Cmd.println("SQLError : Conexion a la BD invalida !", Color.RED);
				return false;
			}
			needCommit = false;
			TIMER(true);
			return true;
		} catch (SQLException e) {
			Cmd.println("SQL ERROR: " + e.getMessage());
			e.printStackTrace();
		} finally {
			myConnectionLocker.unlock();
		}
		return false;
	}

	public static void TIMER(boolean start){
		if (start) {
			timerCommit = new Timer();
			timerCommit.schedule(new TimerTask() {
				public void run() {
					if(!needCommit)
						return;
					commitTransacts();
					needCommit = false;

				}
			}, Config.CONFIG_DB_COMMIT, Config.CONFIG_DB_COMMIT);
		} else {
			timerCommit.cancel();
		}
	}

	public static void commitTransacts() {
		try {
			myConnectionLocker.lock();
			Connection().commit();
		} catch (SQLException e) {
			if(Config.DEBUG)
				Cmd.println("SQL ERROR:" + e.getMessage(), Color.RED);
			e.printStackTrace();
		} finally {
			myConnectionLocker.unlock();
		}
	}

	public static Connection Connection(){ //Skryn Return
		try{
			myConnectionLocker.lock();
			boolean valid = true;
			try {
				valid = !Connection.isClosed();
			} catch (SQLException e) {
				valid = false;
			}
			if (Connection== null || !valid){
				closeCons();
				setUpConnexion();
			}
			return Connection;
		} finally {
			myConnectionLocker.unlock();
		}
	}

	public static void closeCons() {
		try {
			commitTransacts();
			try{
				myConnectionLocker.lock();
				Connection.close();
			} finally {
				myConnectionLocker.unlock();
			}
		} catch (Exception e) {
			Cmd.println("Error, closing SQL connections:"+ e.getMessage(), Color.RED);
			e.printStackTrace();
		}
	}

	public static ResultSet executeQuery(String query) throws SQLException {
		Connection DB;
		DB = Connection();
		Statement stat = DB.createStatement();
		ResultSet RS = stat.executeQuery(query);
		stat.setQueryTimeout(300);
		return RS;
	}

	public static PreparedStatement newTransact(String baseQuery, Connection dbCon) throws SQLException {
		PreparedStatement toReturn = (PreparedStatement) dbCon.prepareStatement(baseQuery);
		needCommit = true;
		return toReturn;
	}

	public static void closePreparedStatement(PreparedStatement p) {
		try {
			p.clearParameters();
			p.close();
			if (p != null) {
				p = null;
			}
		} catch(MySQLNonTransientConnectionException ex) {
			setUpConnexion();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static void closeResultSet(ResultSet RS) {
		try {
			RS.getStatement().close();
			RS.close();
			if (RS != null) {
				RS = null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void ChargeAccountName(String user) {
		ResultSet RS = null;
		try {
			RS = DatabaseManager.executeQuery("SELECT * FROM accounts WHERE account='"+user+"' LIMIT 1;");
			int fatalError;
			if (RS.next()){
				fatalError = RS.getInt("guid");
				World.addAccount(fatalError, new Account(fatalError, RS.getString("account"), RS.getString("pass"), RS.getString("pseudo"), RS.getString("question"), RS.getInt("points")));
				loadCharacters(fatalError);
			}
		} catch(SQLException e) {
			e.printStackTrace();
			Cmd.println("SQL Error : "+e.getMessage(), Color.RED);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void loadCharacters(int fatalError) {
		ResultSet RS = null;
		try {
			RS = DatabaseManager.executeQuery("SELECT * FROM personnages WHERE account='"+fatalError+"';");
			boolean old = false;
			Account account = null;
			while (RS.next()){
				if (account == null && !old) {
					account = World.getAccountByGuid(RS.getInt("account"));
					old = true;
				}
				if (account == null)
					continue;
				TreeMap<Integer,Integer> stats = new TreeMap<Integer,Integer>();
				Player perso = new Player(RS.getInt("guid"), RS.getString("name"), account, RS.getInt("level"), RS.getInt("sexe"), RS.getInt("gfxID"), RS.getInt("color1"), RS.getInt("color2"), RS.getInt("color3"), RS.getInt("color4"), RS.getInt("color5"), RS.getInt("cellID"), RS.getInt("orientation"), RS.getInt("size"), RS.getInt("accessories"),
						RS.getShort("map"), RS.getString("channel"), RS.getInt("title"), RS.getString("title"), RS.getString("items"), RS.getInt("exp"), RS.getInt("life"), false, RS.getString("spells"), RS.getInt("spellpoint"), stats);
				World.addPlayer(RS.getInt("guid"), perso);
				account.addPerso(perso);
			}
		} catch(SQLException e) {
			e.printStackTrace();
			Cmd.println("SQL Error : "+e.getMessage(), Color.RED);
		} finally {
			closeResultSet(RS);
		}
	}

	public static boolean addPlayer(Player perso){
		PreparedStatement p = null;
		try {
			String baseQuery = "INSERT INTO personnages(`guid`,`name`,`account`,`level`,`sexe`,`gfxID`,`color1`,`color2`,`color3`,`color4`,`color5`,`cellID`,`orientation`,`size`,`accessories`,`items`,`spells`,`spellpoint`) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
			p = newTransact(baseQuery, Connection());
			p.setInt(1, perso.getId());
			p.setString(2, perso.getName());
			p.setInt(3, perso.getAccount().getGuid());
			p.setInt(4, perso.getLevel());
			p.setInt(5, perso.getSexe());
			p.setInt(6, perso.getGfxID());
			p.setInt(7, perso.getColor1());
			p.setInt(8, perso.getColor2());
			p.setInt(9, perso.getColor3());
			p.setInt(10, perso.getColor4());
			p.setInt(11, perso.getColor5());
			p.setInt(12, perso.get_curCell().getID());
			p.setInt(13, perso.getOrientation());
			p.setInt(14, perso.getSize());
			p.setInt(15, perso.getAccessories());
			p.setString(16, "");
			p.setString(17, perso.parseSpellToDB());
			p.setInt(18,perso.get_spellPts());
			p.execute();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			Cmd.println("SQL Error : "+e.getMessage(), Color.RED);
			return false;
		} finally {
			closePreparedStatement(p);
		}
	}

	public static boolean delPlayer(Player perso) {
		PreparedStatement p = null;
		try{
			String baseQuery = "DELETE FROM personnages WHERE guid=?;";
			p = newTransact(baseQuery, Connection());
			p.setInt(1, perso.getId());
			p.execute();
			return true;
		} catch(SQLException e) {
			e.printStackTrace();
			Cmd.println("SQL Error : "+e.getMessage(), Color.RED);
			return false;
		} finally {
			closePreparedStatement(p);
		}
	}

	public static void LOAD_MAPS() {
		ResultSet RS = null;
		try {
			RS = DatabaseManager.executeQuery("SELECT * from maps;");
			while(RS.next()) {
				World.addCarte(new Map(RS.getShort("id"), RS.getString("date"), RS.getByte("width"), RS.getByte("height"), RS.getString("key"), RS.getString("places"), RS.getString("mapData"), RS.getString("cells")));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void SAVE_PERSONNAGE(Player perso) {
		String baseQuery = "UPDATE `personnages` SET `level`= ?, `cellID`= ?, `map`= ?, `orientation`= ?, `items`= ?, `life`= ?, `spells`= ?, `spellpoint`= ? WHERE `guid` = ? LIMIT 1;";
		PreparedStatement p = null;
		try {
			p = newTransact(baseQuery, Connection());
			p.setLong(1,perso.getLevel());
			p.setInt(2,perso.get_curCell().getID());
			p.setInt(3,perso.get_curCarte().get_id());
			p.setInt(4,perso.getOrientation());
			p.setString(5, perso.parseObjetsToDB());
			p.setInt(6, perso.get_PDV());
			p.setString(7,perso.parseSpellToDB());
			p.setInt(8,perso.get_spellPts());
			p.setInt(9,perso.getId());
			p.executeUpdate();

			boolean ok = true;
			baseQuery = "UPDATE `items` SET qua = ?, pos= ? WHERE guid = ?;";
			try {
				p = newTransact(baseQuery, Connection());
			} catch (SQLException e1) {
				ok = false;
				e1.printStackTrace();
			}
			if (ok) {
				for(String idStr : perso.getItemsIDSplitByChar(":").split(":")) {
					try {
						int guid = Integer.parseInt(idStr);
						Item obj = World.getObjet(guid);
						if (obj == null)
							continue;
						p.setInt(1, obj.getQuantity());
						p.setInt(2, obj.getPosition());
						p.setInt(3, Integer.parseInt(idStr));
						p.execute();
					} catch(Exception e){
						continue;
					}
				}
			}
		} catch(Exception e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("Character could not be saved");
			System.exit(1);
		} finally {
			closePreparedStatement(p);
			commitTransacts();
		}
	}

	public static void LOAD_ITEMS(String ids) {
		String req = "SELECT * FROM items WHERE guid IN ("+ids+");";
		ResultSet RS = null;
		try {
			RS = DatabaseManager.executeQuery(req);
			while(RS.next()) {
				int guid = RS.getInt("guid");
				int tempID = RS.getInt("template");
				int qua	= RS.getInt("qua");
				int pos	= RS.getInt("pos");
				World.addObjet(World.newObjet(guid, tempID, qua, pos), false);
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.out.println("[REQUEST]: \n"+req);
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_OBJ_TEMPLATE() {
		ResultSet RS = null;
		try {
			RS = DatabaseManager.executeQuery("SELECT * from item_template;");
			while(RS.next()) {
				World.addObjTemplate(new ObjTemplate(RS.getInt("id"), RS.getString("name"), RS.getInt("type"), RS.getInt("level"), RS.getInt("pod"), RS.getInt("prix"), RS.getString("condition"), RS.getInt("points")));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void SAVE_NEW_ITEM(Item item) {
		PreparedStatement p = null;
		try {
			String baseQuery = "REPLACE INTO `items` VALUES(?,?,?,?);";
			p = newTransact(baseQuery, Connection());
			p.setInt(1,item.getGuid());
			p.setInt(2,item.getTemplate().getID());
			p.setInt(3,item.getQuantity());
			p.setInt(4,item.getPosition());
			p.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closePreparedStatement(p);
			commitTransacts();
		}
	}

	public static void DELETE_ITEM(int guid) {
		String baseQuery = "DELETE FROM items WHERE guid = ?;";
		PreparedStatement p = null;
		try {
			p = newTransact(baseQuery, Connection());
			p.setInt(1, guid);
			p.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closePreparedStatement(p);
			commitTransacts();
		}
	}

	public static void SAVE_ITEM(Item item) {
		String baseQuery = "REPLACE INTO `items` VALUES (?,?,?,?);";
		PreparedStatement p = null;
		try {
			p = newTransact(baseQuery, Connection());
			p.setInt(1, item.getGuid());
			p.setInt(2, item.getTemplate().getID());
			p.setInt(3, item.getQuantity());
			p.setInt(4, item.getPosition());
			p.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closePreparedStatement(p);
			commitTransacts();
		}
	}

	public static int getNextObjetID() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT MAX(guid) AS max FROM items;");
			int guid = 0;
			boolean found = RS.first();
			if (found)
				guid = RS.getInt("max");
			return guid;
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
		return 0;
	}

	public static int getNextPlayerID() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT MAX(guid) AS max FROM personnages;");
			int guid = 0;
			boolean found = RS.first();
			if (found)
				guid = RS.getInt("max");
			return guid;
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
		return 0;
	}

	public static void LOAD_EXP() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * from experience;");
			while(RS.next()) {
				World.addExpLevel(RS.getInt("lvl"), new ExpLevel(RS.getLong("perso"),RS.getInt("metier"),RS.getInt("pvp")));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_NPCS() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * from npcs;");
			while(RS.next()) {
				Map map = World.getCarte(RS.getShort("mapid"));
				if(map == null)
					continue;
				map.addNpc(RS.getInt("npcid"), RS.getInt("cellid"), RS.getInt("orientation"));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		} finally {
			closeResultSet(RS);
		}
		return;
	}

	public static void LOAD_NPC_TEMPLATE() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * FROM npc_template;");
			while(RS.next()) {
				int id = RS.getInt("id");
				int bonusValue = RS.getInt("bonusValue");
				int gfxID = RS.getInt("gfxID");
				int scaleX = RS.getInt("scaleX");
				int scaleY = RS.getInt("scaleY");
				int sex = RS.getInt("sex");
				int color1 = RS.getInt("color1");
				int color2 = RS.getInt("color2");
				int color3 = RS.getInt("color3");
				String access = RS.getString("accessories");
				int extraClip = RS.getInt("extraClip");
				int customArtWork = RS.getInt("customArtWork");
				int initQId = RS.getInt("initQuestion");
				World.addNpcTemplate(new NPC_tmpl(id, bonusValue, gfxID, scaleX, scaleY, sex, color1, color2, color3, access, extraClip, customArtWork, initQId));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_NPC_QUESTIONS() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * FROM npc_questions;");
			while(RS.next()) {
				World.addNPCQuestion(new NPC_question(RS.getInt("ID"), RS.getString("responses"), RS.getString("params"), RS.getString("cond"), RS.getInt("ifFalse")));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_NPC_ANSWERS() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * FROM npc_reponses_actions;");
			while(RS.next()) {
				int id = RS.getInt("ID");
				int type = RS.getInt("type");
				String args = RS.getString("args");
				if (World.getNPCreponse(id) == null)
					World.addNPCreponse(new NPC_reponse(id));
				World.getNPCreponse(id).addAction(new Action(type,args,""));

			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_IOTEMPLATE() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT * from interactive_objects_data;");
			while(RS.next()) {
				World.addIOTemplate(new IOTemplate(RS.getInt("id"), RS.getInt("respawn"), RS.getInt("duration"), RS.getInt("unknow"), RS.getInt("walkable")==1));
			}
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			e.printStackTrace();
		} finally {
			closeResultSet(RS);
		}
	}

	public static void LOAD_SORTS() {
		ResultSet RS = null;
		try {
			RS = executeQuery("SELECT  * from sorts;");
			while(RS.next()) {
				int id = RS.getInt("id");
				Spell spell = new Spell(id, RS.getString("nom"), RS.getInt("sprite"), RS.getString("spriteInfos"), RS.getString("effectTarget"));
				SortStats l1 = parseSortStats(id,1,RS.getString("lvl1"));
				SortStats l2 = parseSortStats(id,2,RS.getString("lvl2"));
				SortStats l3 = parseSortStats(id,3,RS.getString("lvl3"));
				SortStats l4 = parseSortStats(id,4,RS.getString("lvl4"));
				SortStats l5 = null;
				if(!RS.getString("lvl5").equalsIgnoreCase("-1"))
					l5 = parseSortStats(id,5,RS.getString("lvl5"));
				SortStats l6 = null;
				if(!RS.getString("lvl6").equalsIgnoreCase("-1"))
					l6 = parseSortStats(id,6,RS.getString("lvl6"));
				spell.addSortStats(1,l1);
				spell.addSortStats(2,l2);
				spell.addSortStats(3,l3);
				spell.addSortStats(4,l4);
				spell.addSortStats(5,l5);
				spell.addSortStats(6,l6);
				World.addSort(spell);
			}

			closeResultSet(RS);
		} catch(SQLException e) {
			System.out.println("Game: SQL ERROR: "+e.getMessage());
			System.exit(1);
		} finally {
			closeResultSet(RS);
		}
	}

	private static SortStats parseSortStats(int id, int lvl, String str) {
		try {
			String[] stat = str.split(",");
			String effets = stat[0], CCeffets = stat[1];
			int PACOST = 6;

			try {
				PACOST = Integer.parseInt(stat[2].trim());
			} catch (NumberFormatException ignored) {}

			int POm = Integer.parseInt(stat[3].trim());
			int POM = Integer.parseInt(stat[4].trim());
			int TCC = Integer.parseInt(stat[5].trim());
			int TEC = Integer.parseInt(stat[6].trim());

			boolean line = stat[7].trim().equalsIgnoreCase("true");
			boolean LDV = stat[8].trim().equalsIgnoreCase("true");
			boolean emptyCell = stat[9].trim().equalsIgnoreCase("true");
			boolean MODPO = stat[10].trim().equalsIgnoreCase("true");

			int MaxByTurn = Integer.parseInt(stat[12].trim());
			int MaxByTarget = Integer.parseInt(stat[13].trim());
			int CoolDown = Integer.parseInt(stat[14].trim());

			String type = stat[15].trim();

			int level = Integer.parseInt(stat[stat.length - 2].trim());
			boolean endTurn = stat[19].trim().equalsIgnoreCase("true");

			return new SortStats(id, lvl, PACOST, POm, POM, TCC, TEC, line, LDV, emptyCell, MODPO, MaxByTurn, MaxByTarget, CoolDown, level, endTurn, effets, CCeffets, type);
		} catch (Exception e) {

			System.out.println("SortData parseSortStats" + e);
			return null;

		}
	}

	private static Map.Case loadCellsTeleport(int MapID) {
		Map.Case cell = null;
		try {
			ResultSet result = executeQuery("SELECT * FROM `scripted_cells` WHERE `mapid` = " + MapID + ";");
			while (result.next()) {
				if (World.getCarte(result.getShort("MapID")) == null)
					continue;

				if (World.getCarte(result.getShort("MapID")).GetCases().get(result.getInt("CellID")) == null)
					continue;

				if (result.getInt("EventID") == 1)
					((Map.Case) World.getCarte(result.getShort("MapID")).GetCases().get(result.getInt("CellID"))).addOnCellStopAction(result.getInt("ActionID"), result.getString("ActionsArgs"), result.getString("Conditions"));
			}
			closeResultSet(result);
		} catch (Exception e) {
			System.out.println("SQL ERROR(CellData): " + e.getMessage());
		}
		return null;
	}
}