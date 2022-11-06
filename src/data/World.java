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