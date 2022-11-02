package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;

import kernel.Cmd;
import variables.Map;
import variables.NPC_tmpl;
import variables.NPC_tmpl.NPC_question;
import variables.NPC_tmpl.NPC_reponse;
import variables.Item;
import variables.Item.ObjTemplate;
import variables.Spell;
import client.Account;
import client.Player;

public class World {
	private static java.util.Map<Integer, Account> Account = new TreeMap<>();
	private static java.util.Map<Integer, Player> Personnages = new TreeMap<>();
	private static java.util.Map<Short, Map> Cartes = new TreeMap<>();
	private static java.util.Map<Integer, Item> Items = new TreeMap<>();
	private static java.util.Map<Integer, ObjTemplate> ItemTemplates = new TreeMap<>();
	private static java.util.Map<Integer, ExpLevel> ExpLevels = new TreeMap<>();
	private static java.util.Map<Integer,NPC_tmpl> NPCTemplates = new TreeMap<>();
	private static java.util.Map<Integer,NPC_question> NPCQuestions = new TreeMap<>();
	private static java.util.Map<Integer,NPC_reponse> NPCReponses = new TreeMap<>();
	private static java.util.Map<Integer,IOTemplate> IOTemplate = new TreeMap<>();
	private static java.util.Map<Integer, Spell> Sorts = new TreeMap<>();
	private static int nextObjetID;
	private static int nextPlayerID;

	public static void LoadWorld() {
		/*boolean junto = false;
		String LEVEL = "100, 2, null, null, 0, 0, 0d0+2".replace(" ", "");
		String LEVEL2 = "100, 3, null, null, 0, 0, 0d0+3".replace(" ", "");
		
		String finals0 = "";
		String finals1 = "";

		String[] str = LEVEL.split(",");
		String str0 = str[0];
		if (str0.contains("null"))
			str0 = "-1";
		String str1 = str[1];
		if (str1.contains("null"))
			str1 = "-1";
		String str2 = str[2];
		if (str2.contains("null"))
			str2 = "-1";
		String str3 = str[3];
		if (str3.contains("null"))
			str3 = "-1";
		String str4 = str[4];
		if (str4.contains("null"))
			str4 = "-1";
		String str5 = str[5];
		if (str5.contains("null"))
			str5 = "-1";
		String str6 = str[6];
		if (str6.contains("null"))
			str6 = "-1";
		finals0 = str0+";"+str1+";"+str2+";"+str3+";"+str4+";"+str5+";"+str6;

		if (LEVEL2 != "") {
			String[] str7 = LEVEL2.split(",");
			String str8 = str7[0];
			if (str8.contains("null"))
				str8 = "-1";
			String str9 = str7[1];
			if (str9.contains("null"))
				str9 = "-1";
			String str10 = str7[2];
			if (str10.contains("null"))
				str10 = "-1";
			String str11 = str7[3];
			if (str11.contains("null"))
				str11 = "-1";
			String str12 = str7[4];
			if (str12.contains("null"))
				str12 = "-1";
			String str13 = str7[5];
			if (str13.contains("null"))
				str13 = "-1";
			String str14 = str7[6];
			if (str14.contains("null"))
				str14 = "-1";
			finals1 = str8+";"+str9+";"+str10+";"+str11+";"+str12+";"+str13+";"+str14;
		}
		String finalfinal = "";
		finalfinal = finals0;
		if (LEVEL2 != "") {
			if (junto)
				finalfinal += "|";
			else
				finalfinal += ",";
		}
		if (finals1 != "")
			finalfinal += finals1;
		System.out.println(finalfinal);
		*/
		Cmd.println("Database loading...");
		Cmd.println("Spell loading...");
		DatabaseManager.LOAD_SORTS();
		Cmd.println("Experience database loading...");
		DatabaseManager.LOAD_EXP();
		Cmd.println("Maps loading...");
		DatabaseManager.LOAD_MAPS();
		Cmd.println("Itemtemplates loading...");
		DatabaseManager.LOAD_OBJ_TEMPLATE();
		Cmd.println("NPCs loading...");
		DatabaseManager.LOAD_NPC_TEMPLATE();
		DatabaseManager.LOAD_NPC_QUESTIONS();
		DatabaseManager.LOAD_NPC_ANSWERS();
		DatabaseManager.LOAD_IOTEMPLATE();
		DatabaseManager.LOAD_NPCS();
		Cmd.println("<< All account and character will be loaded automaticaly >>");
		nextObjetID = DatabaseManager.getNextObjetID();
		nextPlayerID = DatabaseManager.getNextPlayerID();
		 
		/*
		String text = "2_5";
		String[] split = text.split("_");
		int locd = 0;
		while(locd < split.length)
		{
			System.out.println("LOL "+text.split("_")[locd]);
			locd += 1;
		}*/
	}

	public static Spell getSort(int id) {
		return Sorts.get(id);
	}

	public static void addSort(Spell spell) {
		Sorts.put(spell.getSpellID(), spell);
	}

	public static void addIOTemplate(IOTemplate IOT) {
		IOTemplate.put(IOT.getId(), IOT);
	}

	public static IOTemplate getIOTemplate(int id) {
		return IOTemplate.get(id);
	}

	public static void addNPCreponse(NPC_reponse rep) {
		NPCReponses.put(rep.get_id(), rep);
	}

	public static NPC_reponse getNPCreponse(int guid) {
		return NPCReponses.get(guid);
	}

	public static void addNPCQuestion(NPC_question quest) {
		NPCQuestions.put(quest.get_id(), quest);
	}

	public static NPC_question getNPCQuestion(int guid) {
		return NPCQuestions.get(guid);
	}

	public static NPC_tmpl getNPCTemplate(int guid) {
		return NPCTemplates.get(guid);
	}

	public static void addNpcTemplate(NPC_tmpl temp) {
		NPCTemplates.put(temp.get_id(), temp);
	}

	public synchronized static int getNewItemGuid() {
		nextObjetID += 1;
		return nextObjetID;
	}

	public static Player getPersonnage(int guid) {
		return Personnages.get(guid);
	}

	public static void unloadPerso(int g) {
		Player toRem = Personnages.get(g);
		if(!toRem.getItems().isEmpty()) {
			for(Entry<Integer, Item> curObj : toRem.getItems().entrySet()) {
				System.out.println("ITEM ERASED: "+curObj.getKey());
				Items.remove(curObj.getKey());
			}
		}
		toRem = null;
	}

	public static void addObjet(Item item, boolean saveSQL) {
		Items.put(item.getGuid(), item);
		System.out.println("LOADED ITEM: "+item.getGuid());
		if(saveSQL)
			DatabaseManager.SAVE_NEW_ITEM(item);
	}

	public static Item getObjet(int guid) {
		return Items.get(guid);
	}

	public static void removeItem(int guid) {
		Items.remove(guid);
		DatabaseManager.DELETE_ITEM(guid);
	}

	public static void addObjTemplate(ObjTemplate obj) {
		ItemTemplates.put(obj.getID(), obj);
	}

	public static ObjTemplate getObjTemplate(int id) {
		return ItemTemplates.get(id);
	}

	public static Collection<ObjTemplate> getItemTemplates() {
		return ItemTemplates.values();
	}

	public static Item newObjet(int Guid, int template, int qua, int pos) {
		if(World.getObjTemplate(template) == null)  {
			System.out.println("ItemTemplate "+template+" not found in table `items`:"+Guid);
			System.exit(0);
			return null;
		} 
		return new Item(Guid, template, qua, pos);
	}

	public static Map getCarte(short id) {
		return Cartes.get(id);
	}

	public static  void addCarte(Map map) {
		if(!Cartes.containsKey(map.get_id()))
			Cartes.put(map.get_id(),map);
	}

	public static void delCarte(Map map)  {
		if (Cartes.containsKey(map.get_id()))
			Cartes.remove(map.get_id());
	}

	public static void delPlayer(int guid){
		Personnages.remove(guid);
	}

	public static boolean NamePlayerExist(String name){
		for (Entry<Integer, Player> perso : Personnages.entrySet()){
			if(perso.getValue().getName().equals(name))
				return true;
		}
		return false;
	}

	public static int getExpLevelSize() {
		return ExpLevels.size();
	}

	public static void addExpLevel(int lvl,ExpLevel exp) {
		ExpLevels.put(lvl, exp);
	}

	public static long getPersoXpMin(int _lvl) {
		if(_lvl > getExpLevelSize()) 	
			_lvl = getExpLevelSize();
		if(_lvl < 1) 	
			_lvl = 1;
		return ExpLevels.get(_lvl).perso;
	}

	public static long getPersoXpMax(int _lvl) {
		if(_lvl >= getExpLevelSize()) 	
			_lvl = (getExpLevelSize()-1);
		if(_lvl <= 1)	 	
			_lvl = 1;
		return ExpLevels.get(_lvl+1).perso;
	}

	public static ExpLevel getExpLevel(int lvl) {
		return ExpLevels.get(lvl);
	}

	public static int GetPlayerGuidDispo() {
		nextPlayerID += 1;
		return nextPlayerID;
	}

	public static Account getAccountByUser(String user){
		ArrayList<Account> acc = new ArrayList<>();
		acc.addAll(Account.values());
		for(Account P : acc)
			if(P.getAccount().equals(user))
				return P;
		return null;
	}

	public static Account getAccountByGuid(int guid){
		return Account.get(guid);
	}

	public static void addAccount(int guid, Account compte){
		Account.put(guid, compte);
	}

	public static void addPlayer(int guid, Player perso){
		Personnages.put(guid, perso);
	}

	public static Player getPersoByName(String name) {
		ArrayList<Player> Ps = new ArrayList<Player>();
		Ps.addAll(Personnages.values());
		for(Player P : Ps)
			if(P.getName().equalsIgnoreCase(name))
				return P;
		return null;
	}

	public static class Couple<L,R> {
		public L first;
		public R second;

		public Couple(L s, R i)  {
			this.first = s;
			this.second = i;
		}
	}
}