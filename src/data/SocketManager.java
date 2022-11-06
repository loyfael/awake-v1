package data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;

import kernel.Cmd;
import realm.Client;

import org.apache.mina.core.session.IoSession;

import variables.Fight;
import variables.Fight.Fighter;
import variables.Map;
import variables.Map.Case;
import variables.Map.InteractiveObject;
import variables.NPC_tmpl.NPC;
import variables.Item;
import client.Account;
import client.Player;


public class SocketManager {
	private IoSession ioSession;
	private Account compte;

	public SocketManager(IoSession ioSession) {
		this.ioSession = ioSession;
	}

	private void send(String packet) {
		this.ioSession.write(packet);
		Cmd.println("[SENDED] "+packet);
	}

	public static void GAME_SEND_ERASE_ON_MAP_TO_MAP(Map map, int guid) {
		String packet = "GM|-"+guid;
		for(Player z : map.getPersos()) {
			if(z.getAccount().getClient() == null)
				continue;
			send(z, packet);
		}
	}

	private static void send(Player perso, String packet) {
		perso.getAccount().getClient().getIoSession().write(packet);
		Cmd.println("[SENDED] "+packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_MAP(Map map, String suffix, int guid, String name, String msg) {
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_ADD_CANAL(Player out, String chans) {
		String packet = "cC+"+chans;
		send(out, packet);
	}

	public static void GAME_SEND_OAEL_PACKET(Player out) {
		String packet = "OAEL";
		send(out,packet);
	}

	public static void GAME_SEND_REMOVE_ITEM_PACKET(Player out, int guid) {
		String packet = "OR"+guid;
		send(out,packet);
	}

	public static void GAME_SEND_OBJET_MOVE_PACKET(Player out, Item obj) {
		String packet = "OM"+obj.getGuid()+"|";
		if(obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED)
			packet += obj.getPosition();
		send(out,packet);
	}

	public static void GAME_SEND_OT_PACKET(Player out, int id) {
		String packet =  "OT";
		if(id > 0)
			packet += id;
		send(out,packet);
	}

	public static void GAME_SEND_Ow_PACKET(Player perso) {
		String packet =  "Ow0|100000";
		send(perso,packet);
	}

	public static void GAME_SEND_cC_PACKET(Player perso, char c, String s) {
		String packet = "cC"+c+s;
		send(perso,packet);
	}

	public static void GAME_SEND_Im_PACKET(Player out, String str) {
		String packet = "Im"+str;
		send(out,packet);
	}

	public void GAME_SEND_CREATE_OK() {
		String packet = "AAK";
		send(packet);
	}

	public void GAME_SEND_DELETE_PERSO_FAILED() {
		String packet = "ADE";
		send(packet);
	}

	public void REALM_SEND_HC_PACKET(String key) {
		String packet = "HC"+key;
		send(packet);
	}

	public void REALM_SEND_LOGIN_ERROR() {
		String packet = "AlEf";
		send(packet);
	}

	public void MULTI_SEND_Af_PACKETs() {
		send("Af" + 1 + "|" + 1 + "|" + 1 + "|" + 1 + "|" + 1);
	}

	public void REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS() {
		String packet = "Ad" + compte.getPseudo() + (char) 0x00 +
				"Ac0" + (char) 0x00 +
				"AH1;" + 1 + ";110;1" + (char) 0x00 +
				"AlK" + 1 + (char) 0x00 + //GmLevel test
				"AQ" + compte.get_question().replace(" ", "+");
		send(packet);
	}

	public void REALM_SEND_ALREADY_CONNECTED() {
		String packet = "AlEc";
		send(packet);
	}

	public void GAME_SEND_PERSO_LIST() {
		StringBuilder packet = new StringBuilder();
		packet.append("ALK");
		packet.append(compte.getPersos().size());
		for (Player perso : compte.getPersos()) {
			if (perso==null)
				continue;
			packet.append("|");
			packet.append(perso.getId()).append(";");
			packet.append(perso.getName()).append(";");
			packet.append(perso.getLevel()).append(";");
			packet.append(perso.getGfxID()).append(";");
			packet.append((perso.getColor1()==-1?"-1":Integer.toHexString(perso.getColor1()))).append(";");
			packet.append((perso.getColor2()==-1?"-1":Integer.toHexString(perso.getColor2()))).append(";");
			packet.append((perso.getColor3()==-1?"-1":Integer.toHexString(perso.getColor3()))).append(";");
			packet.append((perso.getColor4()==-1?"-1":Integer.toHexString(perso.getColor4()))).append(";");
			packet.append((perso.getColor5()==-1?"-1":Integer.toHexString(perso.getColor5()))).append(";");
			packet.append(perso.getGMStuffString()).append(";0;1;0;");
		}
		send(packet.toString());

	}

	public void ACTUALISE_PJ_MAP(Map map, Player perso) {
		String packet = "GM|~"+perso.parseToGM();
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_MAP(Map map, String gameActionID, int actionID, String s1, String s2) {
		StringBuilder packet = new StringBuilder();
		packet.append("GA").append(gameActionID).append(";").append(actionID).append(";").append(s1);
		if (!s2.equals(""))
			packet.append(";").append(s2);
		for(Player z : map.getPersos())
			send(z,packet.toString());
		System.out.
				println("Game: Map: Send>>" + packet);
	}

	public static void GAME_SEND_GDK_PACKET(Player p) {
		String packet = "GDK";
		send(p, packet);
	}

	public static void GAME_SEND_GAME_CREATE(Player p, String name) {
		String packet = "GCK|1|"+name;
		send(p, packet);
	}

	public void GAME_SEND_PERSO_SELECTION_FAILED() {
		String packet = "ASE";
		send(packet);
	}

	public static void GAME_SEND_MAP_GMS_PACKETS(Map map, Player perso) {
		String packet = map.getGMsPackets();
		send(perso, packet);
	}

	public static void GAME_SEND_ADD_PLAYER_TO_MAP(Map map, Player perso) {
		String packet = "GM|+"+perso.parseToGM();
		for(Player z : map.getPersos())
			send(z,packet);
		System.out.println("Game: Map "+map.get_id()+": Send>>"+packet);
	}

	public static void GAME_SEND_STATS(Player p){
		StringBuilder packet = new StringBuilder();
		int pdv = p.get_PDV();
		int pdvMax = p.get_PDVMAX();
		if(p.get_fight() != null) {
			Fighter f = p.get_fight().getFighterByPerso(p);
			if(f!= null) {
				pdv = f.getPDV();
				pdvMax = f.getPDVMAX();
			}
		}
		packet.append("As"); //Nom du packet A = Account s = Stats
		packet.append(p.xpString(",")).append("|"); // Xp max
		packet.append(10).append("|"); // Kamas
		packet.append(0).append("|"); // Capital
		packet.append(p.get_spellPts()).append("|"); // Spell Points
		packet.append("1~1").append(","); // Alignements
		packet.append("0").append(","); // Alignement Level
		packet.append("0").append(","); // Grade
		packet.append("0").append(","); // Honneur
		packet.append("0").append(","); // Deshonneur
		packet.append("0").append("|"); // Activer ou pas ailes
		packet.append(pdv).append(","); // Pdv min
		packet.append(pdvMax).append("|"); // Pdv max
		packet.append("50").append(","); // Energie actuel
		packet.append("100").append("|"); // Energie max
		packet.append(p.getInitiative()).append("|"); //Initiative
		packet.append(p._baseStats.getEffect(Constant.STATS_ADD_PROS)+((int)Math.ceil(p._baseStats.getEffect(Constant.STATS_ADD_CHAN)/10))+p.getBuffsStats().getEffect(Constant.STATS_ADD_PROS)).append("|");
		packet.append(p._baseStats.getEffect(Constant.STATS_ADD_PA)).append(",").append(0).append(",").append(p.getDonsStats().getEffect(Constant.STATS_ADD_PA)).append(",").append(p.getBuffsStats().getEffect(Constant.STATS_ADD_PA)).append(",").append(p.getTotalStats().getEffect(Constant.STATS_ADD_PA)).append("|");
		packet.append(p._baseStats.getEffect(Constant.STATS_ADD_PM)).append(",").append(0).append(",").append(p.getDonsStats().getEffect(Constant.STATS_ADD_PM)).append(",").append(p.getBuffsStats().getEffect(Constant.STATS_ADD_PM)).append(",").append(p.getTotalStats().getEffect(Constant.STATS_ADD_PM)).append("|");

		send(p, packet.toString());
	}

	public static void GAME_SEND_MAPDATA(Player p, int id, String date, String key){
		String packet = "GDM|"+id+"|"+date+"|"+key;
		send(p,packet);
	}

	public static void GAME_SEND_ASK(Player perso) {
		String packet = "ASK|" + perso.getId() + "|" + perso.getName() + "|" +
				perso.getLevel() + "|" + "-1" + "|" + perso.getSexe() +
				"|" + perso.getGfxID() + "|" +
				(perso.getColor1() == -1 ? "-1" : Integer.toHexString(perso.getColor1())) + "|" +
				(perso.getColor2() == -1 ? "-1" : Integer.toHexString(perso.getColor2())) + "|" +
				(perso.getColor3() == -1 ? "-1" : Integer.toHexString(perso.getColor3())) + "|" +
				(perso.getColor4() == -1 ? "-1" : Integer.toHexString(perso.getColor4())) + "|" +
				(perso.getColor5() == -1 ? "-1" : Integer.toHexString(perso.getColor5())) + "|" +
				perso.parseItemToASK();
		send(perso, packet);
	}

	public void GAME_SEND_CREATE_FAILED() {
		String packet = "AAEF";
		send(packet);
	}

	public void GAME_SEND_NAME_ALREADY_EXIST() {
		String packet = "AAEa";
		send(packet);
	}

	public void SEND_PACKET(String packet) {
		send(packet);
	}

	public static void GAME_SEND_TITLES(Player p, String str, int titulo) {
		String packet = "wTL"+titulo+";"+str;
		send(p,packet);
	}

	public static void GAME_SEND_TITLES2(Player p, String str, int title) {
		String packet = "wTL";
		send(p,packet);
	}

	public void GAME_MESSAGE_CHANGE_TITLE(String str) {
		String packet = "wTA"+str;
		send(packet);
	}

	public void REALM_SEND_REQUIRED_APK() {
		String chars = "abcdefghijklmnopqrstuvwxyz"; // Tu supprimes les lettres dont tu ne veux pas
		StringBuilder pass = new StringBuilder("");
		for(int x=0;x<5;x++) {
			int i = (int)Math.floor(Math.random() * 26); // Si tu supprimes des lettres tu diminues ce nb
			pass.append(chars.charAt(i));
		}
		String packet = "APK"+pass;
		send(packet);
	}

	public static void GAME_SEND_PONG(Player out) {
		String packet = "pong";
		send(out,packet);
	}

	public static void GAME_SEND_EMOTICONE_TO_MAP(Map map, int guid, int id) {
		String packet = "cS"+guid+"|"+id;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_EMOTICONE_TO_FIGHT(Fight fight, int teams, int guid, int id) {
		String packet = "cS"+guid+"|"+id;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_OBJECT_QUANTITY_PACKET(Player out, Item obj) {
		String packet = "OQ"+obj.getGuid()+"|"+obj.getQuantity();
		send(out,packet);
	}

	public static void GAME_SEND_OAKO_PACKET(Player out, Item obj) {
		String packet = "OAKO"+obj.parseItem();
		send(out,packet);
	}

	public static void GAME_SEND_cMK_PACKET(Player perso, String suffix, int guid, String name, String msg) {
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		send(perso,packet);
	}

	public static void GAME_SEND_CHAT_ERROR_PACKET(Player play, String name) {
		String packet = "cMEf"+name;
		send(play,packet);
	}

	public static void GAME_SEND_ON_EQUIP_ITEM(Map map, Player _perso) {
		String packet = _perso.parseToOa();
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_DELETE_OBJECT_FAILED_PACKET(Player out) {
		String packet = "OdE";
		send(out,packet);
	}

	public static void GAME_SEND_EMOTE_LIST(Player perso, String s, String s1) {
		String packet = "eL"+s+"|"+s1;
		send(perso, packet);
	}

	public static void GAME_SEND_eUK_PACKET_TO_MAP(Map map, int guid, int emote) {
		String packet = "eUK"+guid+"|"+emote;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_ILS_PACKET(Player out, int i) {
		String packet = "ILS"+i;
		send(out,packet);
	}

	public static void GAME_SEND_ILF_PACKET(Player P, int i) {
		String packet = "ILF"+i;
		send(P,packet);
	}

	public static void GAME_SEND_END_DIALOG_PACKET(Player out) {
		String packet = "DV";
		send(out,packet);
	}

	public static void GAME_SEND_DCK_PACKET(Player out, int id) {
		String packet = "DCK"+id;
		send(out,packet);
	}

	public static void GAME_SEND_MAP_NPCS_GMS_PACKETS(Player out, Map carte) {
		String packet = carte.getNpcsGMsPackets();
		if(packet.equals("") && packet.length() < 4)
			return;
		send(out,packet);
	}

	public static void GAME_SEND_ADD_NPC_TO_MAP(Map map, NPC npc) {
		String packet = "GM|"+npc.parseGM();
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_QUESTION_PACKET(Player out, String str) {
		String packet = "DQ"+str;
		send(out,packet);
	}

	public static void GAME_SEND_BUY_ERROR_PACKET(Player out) {
		String packet = "EBE";
		send(out, packet);
	}

	public static void GAME_SEND_BUY_OK_PACKET(Player out) {
		String packet = "EBK";
		send(out,packet);
	}

	public static void GAME_SEND_MESSAGE(Player out, String mess, String color) {
		String packet = "cs<font color='#"+color+"'>"+mess+"</font>";
		send(out,packet);
	}

	public static void GAME_SEND_ECK_PACKET(Player out, int type, String str) {
		String packet = "ECK"+type;
		if(!str.equals(""))
			packet += "|"+str;
		send(out,packet);
	}

	public static void GAME_SEND_DUEL_Y_AWAY(Player out, int guid) {
		String packet = "GA;903;"+guid+";o";
		send(out,packet);
	}

	public static void GAME_SEND_DUEL_E_AWAY(Player out, int guid) {
		String packet = "GA;903;"+guid+";z";
		send(out,packet);
	}

	public static void GAME_SEND_MAP_NEW_DUEL_TO_MAP(Map map, int guid, int guid2) {
		String packet = "GA;900;"+guid+";"+guid2;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(Map map) {
		String packet = "fC"+map.getNbrFight();
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight fight, int teams,int state, int cancelBtn, int duel, int spec, int time, int type) {
		StringBuilder packet = new StringBuilder();
		packet.append("GJK").append(state).append("|").append(cancelBtn).append("|").append(duel).append("|").append(spec).append("|").append(time).append("|").append(type);
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			send(f.getPersonnage(),packet.toString());
		}
	}

	public static void GAME_SEND_FIGHT_LIST_PACKET(Player out, Map map) {
		StringBuilder packet = new StringBuilder();
		packet.append("fL");
		for(Entry<Integer,Fight> entry : map.get_fights().entrySet()) {
			if(packet.length()>2) {
				packet.append("|");
			}
			packet.append(entry.getValue().parseFightInfos());
		}
		send(out,packet.toString());
	}

	public static void GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(Fight fight, int teams,int state, int cancelBtn, int duel, int spec, long time, int type) {
		StringBuilder packet = new StringBuilder();
		packet.append("GJK").append(state).append("|");
		packet.append(cancelBtn).append("|").append(duel).append("|");
		packet.append(spec).append("|").append(time).append("|").append(type);
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			send(f.getPersonnage(),packet.toString());
		}
	}

	public static void GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(Fight fight,int teams, String places, int team) {
		String packet = "GP"+places+"|"+team;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight,int teams, int actionID,String s1, String s2) {
		String packet = "GA;"+actionID+";"+s1;
		if(!s2.equals(""))
			packet+=";"+s2;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			System.out.println("ES :"+f.getPersonnage());
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(Map map, int arg1, int guid1, int guid2, int cell1, String str1, int cell2, String str2) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gc+").append(guid1).append(";").append(arg1).append("|").append(guid1).append(";").append(cell1).append(";").append(str1).append("|").append(guid2).append(";").append(cell2).append(";").append(str2);
		for(Player z : map.getPersos())
			send(z,packet.toString());
	}

	public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(Map map, int teamID, Fighter perso) {
		StringBuilder packet = new StringBuilder();
		packet.append("Gt").append(teamID).append("|+").append(perso.getGUID()).append(";").append(perso.getPacketsName()).append(";").append(perso.get_lvl());
		for(Player z : map.getPersos())
			send(z,packet.toString());
	}

	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(Fight fight, int teams, Map map) {
		String packet = map.getFightersGMsPackets();
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(Fight fight, int teams, Map map, int guid, int cell) {
		String packet = "GIC|"+guid+";"+cell+";1";
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(Map map, int guid) {
		String packet = "Gc-"+guid;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_GIC_PACKETS_TO_FIGHT(Fight fight,int teams) {
		StringBuilder packet = new StringBuilder();
		packet.append("GIC|");
		for(Fighter p : fight.getFighters(3)) {
			if(p.get_fightCell() == null)
				continue;
			packet.append(p.getGUID()).append(";").append(p.get_fightCell().getID()).append(";1|");
		}
		for(Fighter perso:fight.getFighters(teams)) {
			if(perso.hasLeft())continue;
			if(perso.getPersonnage() == null || !perso.getPersonnage().getOnline())
				continue;
			send(perso.getPersonnage(),packet.toString());
		}
	}

	public static void GAME_SEND_GS_PACKET_TO_FIGHT(Fight fight,int teams) {
		String packet = "GS";
		for(Fighter f:fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			f.initBuffStats();
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GTL_PACKET_TO_FIGHT(Fight fight, int teams) {
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),fight.getGTL());
		}
	}

	public static void GAME_SEND_GTM_PACKET_TO_FIGHT(Fight fight, int teams) {
		StringBuilder packet = new StringBuilder();
		packet.append("GTM");
		for(Fighter f : fight.getFighters(3)) {
			packet.append("|").append(f.getGUID()).append(";");
			if(f.isDead()) {
				packet.append("1");
				continue;
			} else
				packet.append("0;").append(f.getPDV()).append(";").append(f.getPA()).append(";").append(f.getPM()).append(";");
			packet.append(f.get_fightCell().getID()).append(";");//On envoie pas la cell d'un invisible :p
			packet.append(";");//??
			packet.append(f.getPDVMAX());
		}
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet.toString());
		}
	}

	public static void GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(Fight fight,int teams, int guid, int time) {
		String packet = "GTS"+guid+"|"+time;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(Fight fight,int teams, int guid) {
		String packet = "GTF"+guid;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GTR_PACKET_TO_FIGHT(Fight fight, int teams,int guid) {
		String packet = "GTR"+guid;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GA903_ERROR_PACKET(Player out, char c, int guid) {
		String packet = "GA;903;"+guid+";"+c;
		send(out,packet);
	}

	public static void GAME_SEND_GJK_PACKET(Player out, int state, int cancelBtn, int duel, int spec, long time, int unknown) {
		send(out, "GJK" + state + "|" + cancelBtn + "|" + duel + "|" + spec + "|" + time + "|" + unknown);
	}

	public static void GAME_SEND_FIGHT_PLACES_PACKET(Player out, String places, int team) {
		String packet = "GP"+places+"|"+team;
		send(out,packet);
	}

	public static void GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(Map map, char s, char option, int guid) {
		String packet = "Go"+s+option+guid;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_Im_PACKET_TO_FIGHT(Fight fight,int teams, String id) {
		String packet = "Im"+id;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GV_PACKET(Player P) {
		String packet = "GV";
		send(P,packet);
	}

	public static void GAME_SEND_FIGHT_PLAYER_JOIN(Fight fight,int teams, Fighter _fighter) {
		String packet = _fighter.getGmPacket('+');
		for(Fighter f : fight.getFighters(teams)) {
			if (f != _fighter) {
				if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
					continue;
				if(f.getPersonnage() != null && f.getPersonnage().getAccount().getClient() != null)
					send(f.getPersonnage(),packet);
			}
		}
	}

	public static void GAME_SEND_MAP_FIGHT_GMS_PACKETS(Fight fight, Map map, Player _perso) {
		String packet = map.getFightersGMsPackets();
		send(_perso, packet);
	}

	public static void GAME_SEND_GA_PACKET_TO_FIGHT(Fight fight,int teams,int gameActionID,String s1, String s2,String s3) {
		String packet = "GA"+gameActionID+";"+s1+";"+s2+";"+s3;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GAS_PACKET_TO_FIGHT(Fight fight,int teams, int guid) {
		String packet = "GAS"+guid;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GA_PACKET(Player out, String actionID, String s0, String s1, String s2) {
		String packet = "GA"+actionID+";"+s0;
		if(!s1.equals(""))
			packet += ";"+s1;
		if(!s2.equals(""))
			packet+=";"+s2;
		send(out,packet);
	}

	public static void GAME_SEND_GAMEACTION_TO_FIGHT(Fight fight, int teams,String packet) {
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GAF_PACKET_TO_FIGHT(Fight fight, int teams, int i1,int guid) {
		String packet = "GAF"+i1+"|"+guid;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}


	public static void GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(Fight fight, int teams, int win) {
		String packet = "wDS1000"+((char)0x00);
		packet += "wDWI21;I22"+((char)0x00);
		packet += "GE"+((char)0x00);
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft() || f.getPersonnage() == null)
				continue;
			if(f.getPersonnage().getOnline())
				send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(Fight fight, int teams,int guid) {
		String packet = "GA;103;"+guid+";"+guid;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft() || f.getPersonnage() == null)
				continue;
			if(f.getPersonnage().getOnline())
				send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_ON_FIGHTER_KICK(Fight f, int guid, int team) {
		String packet = "GM|-"+guid;
		for(Fighter F : f.getFighters(team)) {
			if(F.getPersonnage() == null || F.getPersonnage().getAccount().getClient() == null || F.getPersonnage().getId() == guid)
				continue;
			send(F.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_FIGHT_SHOW_CASE(ArrayList<Player> PWs, int guid, int cellID) {
		String packet = "Gf"+guid+"|"+cellID;
		for (Player pw : PWs) {
			send(pw, packet);
		}
	}

	public static void GAME_SEND_GS_PACKET(Player out) {
		String packet = "GS";
		send(out,packet);
	}

	public static void GAME_SEND_GTL_PACKET(Player out, Fight fight) {
		String packet = fight.getGTL();
		send(out,packet);
	}

	public static void GAME_SEND_GAMETURNSTART_PACKET(Player P, int guid, int time) {
		String packet = "GTS"+guid+"|"+time;
		send(P,packet);
	}

	public static void GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(Player p, Map map, int arg1, int guid1, int guid2, int cell1, String str1, int cell2, String str2) {
		send(p, "Gc+" + guid1 + ";" + arg1 + "|" + guid1 + ";" + cell1 + ";" + str1 + "|" + guid2 + ";" + cell2 + ";" + str2);
	}

	public static void GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(Player p, Map map, int teamID, Fighter perso) {
		send(p, "Gt" + teamID + "|+" + perso.getGUID() + ";" + perso.getPacketsName() + ";" + perso.get_lvl());
	}

	public static void GAME_SEND_MAP_START_DUEL_TO_MAP(Map map, int guid, int guid2) {
		String packet = "GA;901;"+guid+";"+guid2;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_CANCEL_DUEL_TO_MAP(Map map, int guid, int guid2) {
		String packet = "GA;902;"+guid+";"+guid2;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_GDF_PACKET_TO_MAP(Map map, Case cell) {
		int cellID = cell.getID();
		InteractiveObject object = cell.getObject();
		String packet = "GDF|"+cellID+";"+object.getState()+";"+(object.isInteractive()?"1":"0");
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(Fight fight,int teams, int guid, boolean b) {
		String packet = "GR"+(b?"1":"0")+guid;
		if(fight.get_state() != 2)
			return;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			if(f.hasLeft())continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT(Player out, Map map) {
		String packet = "fC"+map.getNbrFight();
		send(out,packet);
	}

	public static void GAME_SEND_MAP_FIGHT_COUNT0(Player out) {
		String packet = "fC0";
		send(out,packet);
	}

	public static void SEND_GA903_FIGHT_ERROR(Player out, char c) {
		String packet = "GA;903;;" + c;
		send(out, packet);
	}

	public static void GAME_SEND_BN(Player out) {
		String packet = "BN";
		send(out, packet);
	}

	public static void GAME_SEND_cMK_PACKET_TO_FIGHT(Fight fight,int teams,String suffix,int guid,String name,String msg) {
		String packet = "cMK"+suffix+"|"+guid+"|"+name+"|"+msg;
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_ON_EQUIP_ITEM_FIGHT(Player _perso, Fighter f, Fight F) {
		String packet = _perso.parseToOa();
		for(Fighter z : F.getFighters(f.getTeam2()))  {
			if(z.getPersonnage() == null)
				continue;
			send(z.getPersonnage(),packet);
		}
		for(Fighter z : F.getFighters(f.getOtherTeam())) {
			if(z.getPersonnage() == null)
				continue;
			send(z.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_SPELL_UPGRADE_SUCCEED(Player _out, int spellID, int level) {
		String packet = "SUK"+spellID+"~"+level;
		send(_out,packet);
	}

	public static void GAME_SEND_SPELL_UPGRADE_FAILED(Player out) {
		String packet = "SUE";
		send(out,packet);
	}

	public static void GAME_SEND_SPELL_LIST(Player perso) {
		String packet = perso.parseSpellList();
		send(perso,packet);
	}

	public static void GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(final Fight fight, final int teams) {
		String packet = "GA;0";
		for(final Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft() || f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(), packet);
		}
	}

	public static void GAME_SEND_FIGHT_GIE_TO_FIGHT(Fight fight, int teams,int mType,int cible,int value,String mParam2,String mParam3,String mParam4, int turn,int spellID) {
		StringBuilder packet = new StringBuilder();
		packet.append("GIE").append(mType).append(";").append(cible).append(";").append(value).append(";").append(mParam2).append(";").append(mParam3).append(";").append(mParam4).append(";").append(turn).append(";").append(spellID);
		for(Fighter f : fight.getFighters(teams)) {
			if(f.hasLeft() || f.getPersonnage() == null)
				continue;
			if(f.getPersonnage().getOnline())
				send(f.getPersonnage(),packet.toString());
		}
	}

	public static void GAME_SEND_SERVER_HOUR(Player out) {
		String packet = getServerTime();
		send(out,packet);
	}

	public static void GAME_SEND_SERVER_DATE(Player out) {
		String packet = getServerDate();
		send(out,packet);
	}

	public static String getServerTime() {
		Date actDate = new Date();
		return "BT"+(actDate.getTime()+3600000);
	}

	public static String getServerDate() {
		Date actDate = new Date();
		DateFormat dateFormat = new SimpleDateFormat("dd");
		StringBuilder jour = new StringBuilder(Integer.parseInt(dateFormat.format(actDate)) + "");
		while(jour.length() <2) {
			jour.insert(0, "0");
		}
		dateFormat = new SimpleDateFormat("MM");
		String mois = (Integer.parseInt(dateFormat.format(actDate))-1)+"";
		while(mois.length() <2) {
			mois = "0"+mois;
		}
		dateFormat = new SimpleDateFormat("yyyy");
		String annee = (Integer.parseInt(dateFormat.format(actDate))-1370)+"";
		return "BD"+annee+"|"+mois+"|"+jour;
	}

	public static void GAME_SEND_MESSAGE(Fight fight, String txt1, String mess, String txt2, String color) {
		String packet = "cs<font color=''><b>"+txt1+"</b></font> lost <font color='"+color+"'><b>"+mess+"</b></font>"+txt2;
		for(Fighter f : fight.getFighters(7)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_MESSAGE2(Fight fight, String msg, String msg2, String msg3, String msg4) {
		String packet = "cs<font color=''><b>"+msg+"</b></font> "+msg2+"<font color='#FF0000'><b>"+msg3+"</b></font>"+msg4;
		for(Fighter f : fight.getFighters(7)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_SPECTATOR(Fight fight, String msg, String msg2) {
		String packet = "cs<font color='#FF8000'><b>Spectator: </b></font> <b>"+msg+"</b>: "+msg2;
		for(Player persoc: fight._spec.values()) {
			send(persoc, packet);
		}
	}

	public static void GAME_SEND_MESSAGE3(Fight fight, String color) {
		String packet = "cs<font color='"+color+"'><b>------ Casting Spell ------</b></font>";
		for(Fighter f : fight.getFighters(7)) {
			if(f.hasLeft())
				continue;
			if(f.getPersonnage() == null || !f.getPersonnage().getOnline())
				continue;
			send(f.getPersonnage(),packet);
		}
	}

	public static void GAME_SEND_GDO_PACKET(Player p, char c, int cell, int itm, int i) {
		String packet = "GDO"+c+cell+";"+itm+";"+i;
		send(p,packet);
	}

	public static void GAME_SEND_GDO_PACKET_TO_MAP(Map map, char c, int cell, int itm, int i) {
		String packet = "GDO"+c+cell+";"+itm+";"+i;
		for(Player z : map.getPersos())
			send(z,packet);
	}

	public static void GAME_SEND_FIGHT_DETAILS(Player out, Fight fight) {
		if(fight == null)
			return;
		StringBuilder packet = new StringBuilder();
		packet.append("fD").append(fight.get_id()).append("|");
		for(Fighter f : fight.getFighters(1))
			packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
		packet.append("|");
		for(Fighter f : fight.getFighters(2))
			packet.append(f.getPacketsName()).append("~").append(f.get_lvl()).append(";");
		send(out,packet.toString());
	}

	public static void GAME_SEND_gK_PACKET(Player p, String str) {
		String packet = "gK" + str;
		send(p, packet);

	}

	public IoSession getIoSession() {
		return ioSession;
	}

	public void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}

	public Account getAccount() {
		return compte;
	}

	public void setAccount(Account account) {
		this.compte = account;
	}
}