package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.Timer;

import kernel.Main;
import variables.Fight;
import variables.Map;
import variables.Map.Case;
import variables.Item;
import variables.Spell.SortStats;
import variables.SpellEffect;
import data.Constant;
import data.DatabaseManager;
import data.SocketManager;
import data.World;

public class Player {
	private int _id;
	private String _nombre;
	private Account _account;
	private int _level = 1;
	private int _sexe = 1;
	private int _gfxID = 10;
	private int _color1 = -1;
	private int _color2 = -1;
	private int _color3 = -1;
	private int _color4 = -1;
	private int _color5 = -1;
	private long _exp = 0;;
	private int _orientation;
	private int _size;
	private int _title;
	private String _titles;
	private Map _mapPj;
	private Case _cellPj;
	private String _channel;
	private int _accessories;
	private java.util.Map<Integer, Item> _items = new TreeMap<Integer, Item>();
	private java.util.Map<Integer,SortStats> _sorts = new TreeMap<Integer,SortStats>();
	private java.util.Map<Integer,Character> _sortsPlaces = new TreeMap<Integer,Character>();
	private java.util.Map<Integer,SpellEffect> _buffs = new TreeMap<Integer,SpellEffect>();
	public Stats _baseStats;
	private boolean _online = false;
	private int _emoteActive = 0;
	private boolean _sitted;
	private Timer _sitTimer;
	private int _PDV;
	private int _PDVMAX;
	private int _exPdv;
	private int _isTradingWith = 0;
	private int _isTalkingWith = 0;
	private Fight _fight;
	private boolean _ready = false;
	private boolean _away;
	private int _duelID = -1;
	private int _spellPts;
	private boolean _isForgetingSpell = false;

	public Player(int id, String name, Account compte, int level, int sexe, int gfxID, int color1, int color2, int color3, int color4, int color5, int cellID, int orientation, int size, int accesorries, short map, String channel, int title, String titles, String items, long exp, int pdvPer, boolean firstTime, String spell, int pts, java.util.Map<Integer,Integer> stats){
		_id = id;
		_nombre = name;
		_account = compte;
		_level = level;
		_sexe = sexe;
		_gfxID = gfxID;
		_color1 = color1;
		_color2 = color2;
		_color3 = color3;
		_color4 = color4;
		_color5 = color5;
		_orientation = orientation;
		_size = size;
		_accessories = accesorries;
		_spellPts = pts;
		_baseStats = new Stats(stats,true,this);
		_mapPj = World.getCarte(map);
		_cellPj = _mapPj.getCase(cellID);
		_channel = channel;
		_title = title;
		_titles = titles;
		_exp = exp;
		_PDVMAX = (_level -1)*5+50+getTotalStats().getEffect(Constant.STATS_ADD_VITA)+getTotalStats().getEffect(Constant.STATS_ADD_VIE);
		if (pdvPer > 100)
			_PDV = (_PDVMAX * 100 / 100);
		else
			_PDV = (_PDVMAX * pdvPer / 100);
		_sitTimer = new Timer(3000,new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				regenLife();
			}
		});
		parseSpells(spell);
		if(!items.equals("")) {
			if(items.charAt(items.length()-1) == '|')
				items = items.substring(0,items.length()-1);
			DatabaseManager.LOAD_ITEMS(items.replace("|",","));
		}
		for(String item : items.split("\\|")) {
			if(item.equals("") || item.isEmpty())
				continue;
			String[] infos = item.split(":");
			int guid = 0;
			try {
				guid = Integer.parseInt(infos[0]);
			} catch(Exception e) {
				continue;
			}
			Item obj = World.getObjet(guid);
			if(obj == null)
				continue;
			_items.put(obj.getGuid(), obj);
		}
		if (firstTime) {
			_sorts = Constant.getStartSorts();
			_sortsPlaces = Constant.getStartSortsPlaces();
		}
	}

	public Stats get_baseStats() {
		return _baseStats;
	}

	public Stats getDonsStats() {
		Stats stats = new Stats(false,null);
		return stats;
	}

	public java.util.Map<Integer, SpellEffect> get_buff() {
		return _buffs;
	}

	public Stats getBuffsStats() {
		Stats stats = new Stats(false,null);
		for(java.util.Map.Entry<Integer, SpellEffect> entry : _buffs.entrySet()) {
			stats.addOneStat(entry.getValue().getEffectID(), entry.getValue().getValue());
		}
		return stats;
	}

	public Stats getTotalStats() {
		Stats total = new Stats(false,null);
		total = Stats.cumulStat(total,_baseStats);
		total = Stats.cumulStat(total,getDonsStats());
		if(_fight == null)
			total = Stats.cumulStat(total,getBuffsStats());
		return total;
	}

	public void refreshStats() {
		double actPdvPer = (100*(double)_PDV)/(double)_PDVMAX;
		_PDVMAX = (_level -1)*5+50+getTotalStats().getEffect(Constant.STATS_ADD_VITA)+getTotalStats().getEffect(Constant.STATS_ADD_VIE);
		_PDV = (int) Math.round(_PDVMAX*actPdvPer/100);
	}

	public void addSpellPoint(int pts) {
		_spellPts += pts;
	}

	public int get_spellPts() {
		return _spellPts;
	}

	public void set_spellPts(int pts) {
		_spellPts = pts;
	}

	public void setSpells(java.util.Map<Integer, SortStats> spells) {
		_sorts.clear();
		_sortsPlaces.clear();
		_sorts = spells;
		_sortsPlaces = Constant.getStartSortsPlaces();
	}

	public String parseSpellToDB() {
		StringBuilder sorts = new StringBuilder();
		if(_sorts.isEmpty())
			return "";
		for(int key : _sorts.keySet()) {
			SortStats SS = _sorts.get(key);
			sorts.append(SS.getSpellID()).append(";").append(SS.getLevel()).append(";");
			if(_sortsPlaces.get(key)!=null)
				sorts.append(_sortsPlaces.get(key));
			else
				sorts.append("_");
			sorts.append(",");
		}
		return sorts.substring(0, sorts.length()-1).toString();
	}

	private void parseSpells(String str) {
		String[] spells = str.split(",");
		for(String e : spells) {
			try {
				int id = Integer.parseInt(e.split(";")[0]);
				int lvl = Integer.parseInt(e.split(";")[1]);
				char place = e.split(";")[2].charAt(0);
				learnSpell(id,lvl,false,false);
				_sortsPlaces.put(id, place);
			} catch(NumberFormatException e1){
				continue;
			}
		}
	}

	public boolean learnSpell(int spellID, int level, boolean save, boolean send) {
		if (World.getSort(spellID).getStatsByLevel(level) == null) {
			System.out.println("[ERROR]Sort " + spellID + " lvl " + level + " non trouve.");
			return false;
		}
		if ((spellID == 366) && (_sorts.containsKey(Integer.valueOf(spellID)))) {
			return false;
		}
		_sorts.put(Integer.valueOf(spellID), World.getSort(spellID).getStatsByLevel(level));
		if (send)  {
			SocketManager.GAME_SEND_SPELL_LIST(this);
			SocketManager.GAME_SEND_Im_PACKET(this, "03;" + spellID);
		}
		if (save) 
			DatabaseManager.SAVE_PERSONNAGE(this);
		return true;
	}

	public SortStats getSortStatBySortIfHas(int spellID) {
		return _sorts.get(spellID);
	}

	public boolean boostSpell(int spellID) {
		if(getSortStatBySortIfHas(spellID)== null) {
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if(AncLevel == 6)return false;
		if(_spellPts>=AncLevel && World.getSort(spellID).getStatsByLevel(AncLevel+1).getReqLevel() <= _level) {
			if(learnSpell(spellID,AncLevel+1,true,false)) {
				_spellPts -= AncLevel;
				DatabaseManager.SAVE_PERSONNAGE(this);
				return true;
			} else {
				return false;
			}
		} else {
			if(_spellPts<AncLevel)
				if(World.getSort(spellID).getStatsByLevel(AncLevel+1).getReqLevel() > _level)
					return false;
		}
		return _away;
	}
	public void set_SpellPlace(int SpellID, char Place) {
		replace_SpellInBook(Place);
		_sortsPlaces.remove(SpellID);	
		_sortsPlaces.put(SpellID, Place);
		DatabaseManager.SAVE_PERSONNAGE(this);
	}

	private void replace_SpellInBook(char Place) {
		for (int key : _sorts.keySet()) {
			if(_sortsPlaces.get(key)!=null) {
				if (_sortsPlaces.get(key).equals(Place)) {
					_sortsPlaces.remove(key);
				}
			}
		}
	}

	public String parseSpellList() {
		StringBuilder packet = new StringBuilder();
		packet.append("SL");
		for (SortStats SS : _sorts.values()) {
			packet.append(SS.getSpellID()).append("~").append(SS.getLevel()).append("~").append(_sortsPlaces.get(SS.getSpellID())).append(";");
		}
		return packet.toString();
	}

	public boolean forgetSpell(int spellID) {
		if(getSortStatBySortIfHas(spellID)== null) {
			return false;
		}
		int AncLevel = getSortStatBySortIfHas(spellID).getLevel();
		if(AncLevel <= 1)
			return false;
		if(learnSpell(spellID,1,true,false)) {
			_spellPts += Formules.spellCost(AncLevel);
			DatabaseManager.SAVE_PERSONNAGE(this);
			return true;
		} else {
			return false;
		}
	}

	public Collection<SortStats> getSorts() {
		return _sorts.values();
	}

	public void deleteAllSpells() {
		_sorts.clear();
		_sortsPlaces.clear();
	}

	public boolean hasSpell(int spellID) {
		return (getSortStatBySortIfHas(spellID) == null ? false : true);
	}

	public boolean is_away() {
		return _away;
	}

	public void set_away(boolean away) {
		_away = away;
	}

	public boolean is_ready() {
		return _ready;
	}

	public void set_ready(boolean ready) {
		_ready = ready;
	}

	public int get_isTalkingWith() {
		return _isTalkingWith;
	}

	public long getExp() {
		return _exp;
	}

	public void set_isTalkingWith(int talkingWith) {
		_isTalkingWith = talkingWith;
	}

	public int get_isTradingWith() {
		return _isTradingWith;
	}

	public void set_isTradingWith(int tradingWith) {
		_isTradingWith = tradingWith;
	}

	public void regenLife() {
		if(_mapPj == null)
			return;
		if(_fight != null)
			return;
		if(_PDV == _PDVMAX)
			return;
		_PDV += 3;
	}

	public void set_fight(Fight fight) {
		_fight = fight;

	}

	public Fight get_fight() {
		return _fight;
	}

	public int emoteActive() {
		return _emoteActive;
	}

	public boolean isSitted() {
		return _sitted;
	}

	public void setEmoteActive(int emoteActive) {
		_emoteActive = emoteActive;
	}

	public int getAccessories() {
		return _accessories;
	}

	public Case get_curCell() {
		return _cellPj;
	}

	public void addTitle(int title) {
		if (_titles.isEmpty()) {
			_titles += title;
		} else {
			_titles += "_" + title;
		}
	}

	public Map get_curCarte() {
		return _mapPj;
	}

	public void set_curCell(Map.Case cell) {
		_cellPj = cell;
	}

	public void setTitle(int titulo) {
		_title = titulo;
	}

	public int getTitle() {
		return _title;
	}

	public void setOnline(boolean online) {
		_online = online;
	}

	public boolean getOnline() {
		return _online;
	}

	public String getTitles() {
		return _titles;
	}

	public void OnJoinGame() {
		SocketManager.GAME_SEND_ASK(this);
		SocketManager.GAME_SEND_EMOTE_LIST(this, "7667711","0");
		SocketManager.GAME_SEND_ADD_CANAL(this, _channel +"^"+("@¤"));
		SocketManager.GAME_SEND_SPELL_LIST(this);
		_sitTimer.start();
		SocketManager.GAME_SEND_ILS_PACKET(this, 1000);
		SocketManager.GAME_SEND_Im_PACKET(this, "189");
	}

	public void fullPDV() {
		_PDV = _PDVMAX;
	}

	public void resetVars() {
		_emoteActive = 0;
		_isTradingWith = 0;
		_duelID = 0;
		_fight = null;
		_away = false;
		_isForgetingSpell = false;
		_ready = false;
		_sitted = false;
		_sitTimer.stop();
	}

	public void setIsForgettingSpell(boolean isForgettingSpell) {
		_isForgetingSpell = isForgettingSpell;
	}

	public boolean isForgettingSpell() {
		return _isForgetingSpell;
	}

	public void refreshMapAfterFight() {
		_mapPj.addPlayer(this);
		if(getAccount().getClient() != null) {
			SocketManager.GAME_SEND_STATS(this);
			SocketManager.GAME_SEND_ILS_PACKET(this, 1000);
			SocketManager.GAME_SEND_BN(this);
		}
		_fight = null;
		_away = false;
	}

	public String get_channel() {
		return _channel;
	}

	public void addChannel(String chan) {
		if(_channel.indexOf(chan) >=0)
			return;
		_channel += chan;
		SocketManager.GAME_SEND_cC_PACKET(this, '+', chan);
	}

	public void removeChannel(String chan) {
		_channel = _channel.replace(chan, "");
		SocketManager.GAME_SEND_cC_PACKET(this, '-', chan);
	}

	public void GameCreate(){
		SocketManager.GAME_SEND_GAME_CREATE(this, _nombre);
		SocketManager.GAME_SEND_STATS(this);
		SocketManager.GAME_SEND_MAPDATA(this, _mapPj.get_id(), _mapPj.get_date(), _mapPj.get_key());
		_mapPj.addPlayer(this);
		_online = true;
	}

	public String parseToGM() {
		StringBuilder str = new StringBuilder();
		str.append(get_curCell().getID()).append(";"); //Cellid
		str.append(_orientation).append(";"); //Orientation
		str.append(0).append(";");//BonusValue
		str.append(_id).append(";");
		str.append(_nombre).append(";");
		str.append(1).append((_title >0?(","+ _title +";"):(";"))); //Type
		str.append(_gfxID).append("^").append(100).append(";");//gfxID^size
		str.append(_sexe).append(";"); //Sexe
		str.append(_level).append(";"); //Level
		str.append(1).append(",");//1,0,0,4055064
		str.append("1,");//FIXME:?
		str.append(0).append(",");// Alignment
		str.append(0);
		str.append(",0");
		str.append(";");
		str.append((_color1 == -1 ? "-1" : Integer.toHexString(_color1))).append(";");
		str.append((_color2 == -1 ? "-1" : Integer.toHexString(_color2))).append(";");
		str.append((_color3 == -1 ? "-1" : Integer.toHexString(_color3))).append(";");
		str.append((_color4 == -1 ? "-1" : Integer.toHexString(_color4))).append(";");
		str.append((_color5 == -1 ? "-1" : Integer.toHexString(_color5))).append(";");
		str.append(getGMStuffString()).append(";");//Pelo,Sombrero,??, Camiseta,Pantalones,Botas
		str.append("0").append(";"); //Aura
		str.append(";").append(";"); //Emotes Emotes Timer
		str.append(";").append(";");
		str.append(";");
		return str.toString();
	}

	public String getGMStuffString() {
		StringBuilder str = new StringBuilder();
		str.append(getAccessories()).append(",");
		if(getObjetByPos(Constant.ITEM_POS_COIFFE) != null) {
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_COIFFE).getTemplate().getID()));
		} else {
			str.append("4");
		}
		str.append(",");
		str.append(","); // QUE ES???
		if(getObjetByPos(Constant.ITEM_POS_CAPE) != null) {
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_CAPE).getTemplate().getID()));
		} else {
			str.append("A");
		}
		str.append(",");
		if(getObjetByPos(Constant.ITEM_POS_CEINTURE) != null) {
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_CEINTURE).getTemplate().getID()));
		} else {
			str.append("C");
		}
		str.append(",");
		if(getObjetByPos(Constant.ITEM_POS_BOTTES) != null) {
			str.append(Integer.toHexString(getObjetByPos(Constant.ITEM_POS_BOTTES).getTemplate().getID()));
		}
		return str.toString();
	}

	public void setSitted(boolean b) {
		_sitted = b;
		if (_sitted) {
			_exPdv = _PDV;
		}
		int time = (b?1000:2000);
		if(_online) {
			if (!_sitted)
				SocketManager.GAME_SEND_ILF_PACKET(this, _PDV - _exPdv);
			SocketManager.GAME_SEND_ILS_PACKET(this, time);
		}
		_sitTimer.setDelay(time);
		if((_emoteActive == 1 || _emoteActive == 19) && b == false)
			_emoteActive = 0;
	}

	public void set_PDVMAX(int _pdvmax) {
		_PDVMAX = _pdvmax;
	}

	public int get_PDV() {
		return _PDV;
	}

	public void set_PDV(int _pdv) {
		_PDV = _pdv;
	}

	public int get_pdvper() {
		int pdvper = 100;
		pdvper = (100*_PDV)/_PDVMAX;
		if (pdvper > 100)
			return 100;
		return pdvper;
	}

	public int get_PDVMAX() {
		return _PDVMAX;
	}

	public int getId() {
		return _id;
	}

	public void setId(int id) {
		this._id = id;
	}

	public String getName() {
		return _nombre;
	}

	public void setName(String name) {
		this._nombre = name;
	}

	public Account getAccount() {
		return _account;
	}

	public void setAccount(Account account) {
		this._account = account;
	}

	public int getLevel() {
		return _level;
	}

	public void set_duelID(int _duelid) {
		_duelID = _duelid;
	}

	public int get_duelID() {
		return _duelID;
	}

	public void setLevel(int level) {
		_level = level;
	}

	public int getSexe() {
		return _sexe;
	}

	public void setSexe(int sexe) {
		this._sexe = sexe;
	}

	public int getGfxID() {
		return _gfxID;
	}

	public void setGfxID(int gfxID) {
		this._gfxID = gfxID;
	}

	public int getColor1() {
		return _color1;
	}

	public void setColor1(int color1) {
		this._color1 = color1;
	}

	public int getColor2() {
		return _color2;
	}

	public void setColor2(int color2) {
		this._color2 = color2;
	}

	public int getColor3() {
		return _color3;
	}

	public void setColor3(int color3) {
		this._color3 = color3;
	}

	public int getColor4() {
		return _color4;
	}

	public void setColor4(int color4) {
		this._color4 = color4;
	}

	public int getColor5() {
		return _color5;
	}

	public void setColor5(int color5) {
		this._color5 = color5;
	}

	public int getOrientation() {
		return _orientation;
	}

	public void setOrientation(int orientation) {
		this._orientation = orientation;
	}

	public int getSize() {
		return _size;
	}

	public void setSize(int size) {
		this._size = size;
	}

	public void emoticone(String str)  {
		try {
			int id = Integer.parseInt(str);
			Map map = _mapPj;
			if(_fight == null)
				SocketManager.GAME_SEND_EMOTICONE_TO_MAP(map, _id, id);
			else
				SocketManager.GAME_SEND_EMOTICONE_TO_FIGHT(_fight,7,_id,id);
		} catch(NumberFormatException e) {
			return;
		}
	}

	public void addObjet(Item newObj) {
		_items.put(newObj.getGuid(), newObj);
	}

	public String parseItemToASK() {
		StringBuilder str = new StringBuilder();
		if(_items.isEmpty())
			return "";
		for(Item obj : _items.values()) {
			str.append(obj.parseItem());
		}
		return str.toString();
	}

	public boolean hasItemGuid(int guid) {
		return _items.get(guid) != null?_items.get(guid).getQuantity()>0:false;
	}

	public boolean hasEquiped(int id) {
		for(Entry<Integer, Item> entry : _items.entrySet())
			if(entry.getValue().getTemplate().getID() == id && entry.getValue().getPosition() != Constant.ITEM_POS_NO_EQUIPED)
				return true;
		return false;
	}

	public Item getSimilarItem(Item exObj) {
		for(Entry<Integer, Item> entry : _items.entrySet()) {
			Item obj = entry.getValue();
			if(obj.getTemplate().getID() == exObj.getTemplate().getID() && obj.getGuid() != exObj.getGuid() && obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
				return obj;
		}
		return null;
	}

	public void removeItem(int guid) {
		_items.remove(guid);
	}

	public boolean addObjet(Item newObj, boolean stackIfSimilar) {
		for(Entry<Integer, Item> entry : _items.entrySet()) {
			Item obj = entry.getValue();
			if(obj.getTemplate().getID() == newObj.getTemplate().getID() && stackIfSimilar && newObj.getTemplate().getType() != 85 && obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED) {
				obj.setQuantity(obj.getQuantity()+newObj.getQuantity());
				DatabaseManager.SAVE_ITEM(obj);
				if (getOnline())
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(this,obj);
				return false;
			}
		}
		_items.put(newObj.getGuid(), newObj);
		if (getOnline()) {
			SocketManager.GAME_SEND_OAKO_PACKET(this,newObj);
		}
		return true;
	}

	public String parseToOa() {
		StringBuilder packetOa = new StringBuilder();
		packetOa.append("Oa").append(_id).append("|").append(getGMStuffString());
		return packetOa.toString();
	}

	public Item getObjetByPos(int pos) {
		if(pos == Constant.ITEM_POS_NO_EQUIPED)
			return null;
		for(Entry<Integer, Item> entry : _items.entrySet()) {
			Item obj = entry.getValue();
			if(obj.getPosition() == pos)
				return obj;
		}
		return null;
	}

	public String xpString(String c) {
		return _exp+c+ World.getPersoXpMin(_level)+c+ World.getPersoXpMax(_level);
	}

	public java.util.Map<Integer, Item> getItems() {
		return _items;
	}

	public boolean hasItemTemplate(int i, int q) {
		for(Item obj : _items.values()) {
			if(obj.getPosition() != -1)
				continue;
			if(obj.getTemplate().getID() != i)
				continue;
			if(obj.getQuantity() >= q)
				return true;
		}
		return false;
	}

	public int getInitiative() {
		int fact = 4;
		int pvmax = _PDVMAX;
		int pv = _PDV;
		double coef = pvmax/fact;
		coef += getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_CHAN);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_INTE);
		coef += getTotalStats().getEffect(Constant.STATS_ADD_FORC);
		int init = 1;
		if(pvmax != 0)
			init = (int)(coef*((double)pv/(double)pvmax));
		if(init <0)
			init = 0;
		return init;
	}
	public String getItemsIDSplitByChar(String splitter) {
		StringBuilder str = new StringBuilder();
		if(_items.isEmpty())
			return "";
		for(int entry : _items.keySet()) {
			if(str.length() != 0) 
				str.append(splitter);
			str.append(entry);
		}
		return str.toString();
	}

	public String parseObjetsToDB() {
		StringBuilder str = new StringBuilder();
		if(_items.isEmpty())
			return "";
		for(Entry<Integer, Item> entry : _items.entrySet()) {
			Item obj = entry.getValue();
			str.append(obj.getGuid()).append("|");
		}
		return str.toString();
	}

	public static class Stats {
		private java.util.Map<Integer,Integer> Effects = new TreeMap<Integer,Integer>();

		public Stats(boolean addBases, Player perso) {
			Effects = new TreeMap<Integer,Integer>();
			if(!addBases)
				return;
			Effects.put(Constant.STATS_ADD_PA,  perso.getLevel()<100?6:7);
			Effects.put(Constant.STATS_ADD_PM, 3);
			Effects.put(Constant.STATS_ADD_PROS, 100);
			Effects.put(Constant.STATS_ADD_PODS, 1000);
			Effects.put(Constant.STATS_CREATURE, 1);
			Effects.put(Constant.STATS_ADD_INIT, 1);
		}

		public Stats(java.util.Map<Integer, Integer> stats, boolean addBases, Player perso) {
			Effects = stats;
			if(!addBases)
				return;
			Effects.put(Constant.STATS_ADD_PA, perso.getLevel()<100?6:7);
			Effects.put(Constant.STATS_ADD_PM, 3);
			Effects.put(Constant.STATS_ADD_PROS, 100);
			Effects.put(Constant.STATS_ADD_PODS, 1000);
			Effects.put(Constant.STATS_CREATURE, 1);
			Effects.put(Constant.STATS_ADD_INIT, 1);
		}

		public Stats(java.util.Map<Integer, Integer> stats) {
			Effects = stats;
		}

		public Stats() {
			Effects = new TreeMap<>();
		}

		public int addOneStat(int id, int val) {
			if(Effects.get(id) == null || Effects.get(id) == 0)
				Effects.put(id,val);
			else {
				int newVal = (Effects.get(id)+val);
				Effects.put(id, newVal);
			}
			return Effects.get(id);
		}

		public boolean isSameStats(Stats other) {
			for(Entry<Integer,Integer> entry : Effects.entrySet()) {
				if(other.getMap().get(entry.getKey()) == null)
					return false;
				if(other.getMap().get(entry.getKey()) != entry.getValue())
					return false;	
			}
			for(Entry<Integer,Integer> entry : other.getMap().entrySet()) 	{
				if(Effects.get(entry.getKey()) == null)
					return false;
				if(Effects.get(entry.getKey()) != entry.getValue())
					return false;	
			}
			return true;
		}

		public int getEffect(int id) {
			int val;
			if(Effects.get(id) == null)
				val=0;
			else
				val = Effects.get(id);
			switch(id) {
			case Constant.STATS_ADD_AFLEE:
				if(Effects.get(Constant.STATS_REM_AFLEE)!= null)
					val -= (int)(getEffect(Constant.STATS_REM_AFLEE));
				if(Effects.get(Constant.STATS_ADD_SAGE) != null)
					val += (int)(getEffect(Constant.STATS_ADD_SAGE)/4);
				break;
			case Constant.STATS_ADD_MFLEE:
				if(Effects.get(Constant.STATS_REM_MFLEE)!= null)
					val -= (int)(getEffect(Constant.STATS_REM_MFLEE));
				if(Effects.get(Constant.STATS_ADD_SAGE) != null)
					val += (int)(getEffect(Constant.STATS_ADD_SAGE)/4);
				break;
			case Constant.STATS_ADD_INIT:
				if(Effects.get(Constant.STATS_REM_INIT)!= null)
					val -= Effects.get(Constant.STATS_REM_INIT);
				break;
			case Constant.STATS_ADD_AGIL:
				if(Effects.get(Constant.STATS_REM_AGIL)!= null)
					val -= Effects.get(Constant.STATS_REM_AGIL);
				break;
			case Constant.STATS_ADD_FORC:
				if(Effects.get(Constant.STATS_REM_FORC)!= null)
					val -= Effects.get(Constant.STATS_REM_FORC);
				break;
			case Constant.STATS_ADD_CHAN:
				if(Effects.get(Constant.STATS_REM_CHAN)!= null)
					val -= Effects.get(Constant.STATS_REM_CHAN);
				break;
			case Constant.STATS_ADD_INTE:
				if(Effects.get(Constant.STATS_REM_INTE)!= null)
					val -= Effects.get(Constant.STATS_REM_INTE);
				break;
			case Constant.STATS_ADD_PA:
				if(Effects.get(Constant.STATS_ADD_PA2)!= null)
					val += Effects.get(Constant.STATS_ADD_PA2);
				if(Effects.get(Constant.STATS_REM_PA)!= null)
					val -= Effects.get(Constant.STATS_REM_PA);
				if(Effects.get(Constant.STATS_REM_PA2)!= null)//Non esquivable
					val -= Effects.get(Constant.STATS_REM_PA2);
				break;
			case Constant.STATS_ADD_PM:
				if(Effects.get(Constant.STATS_ADD_PM2)!= null)
					val += Effects.get(Constant.STATS_ADD_PM2);
				if(Effects.get(Constant.STATS_REM_PM)!= null)
					val -= Effects.get(Constant.STATS_REM_PM);
				if(Effects.get(Constant.STATS_REM_PM2)!= null)//Non esquivable
					val -= Effects.get(Constant.STATS_REM_PM2);
				break;
			case Constant.STATS_ADD_PO:
				if(Effects.get(Constant.STATS_REM_PO)!= null)
					val -= Effects.get(Constant.STATS_REM_PO);
				break;
			case Constant.STATS_ADD_VITA:
				if(Effects.get(Constant.STATS_REM_VITA)!= null)
					val -= Effects.get(Constant.STATS_REM_VITA);
				break;
			case Constant.STATS_ADD_DOMA:
				if(Effects.get(Constant.STATS_REM_DOMA)!= null)
					val -= Effects.get(Constant.STATS_REM_DOMA);
				break;
			case Constant.STATS_ADD_PODS:
				if(Effects.get(Constant.STATS_REM_PODS)!= null)
					val -= Effects.get(Constant.STATS_REM_PODS);
				break;
			case Constant.STATS_ADD_PROS:
				if(Effects.get(Constant.STATS_REM_PROS)!= null)
					val -= Effects.get(Constant.STATS_REM_PROS);
				break;
			case Constant.STATS_ADD_R_TER:
				if(Effects.get(Constant.STATS_REM_R_TER)!= null)
					val -= Effects.get(Constant.STATS_REM_R_TER);
				break;
			case Constant.STATS_ADD_R_EAU:
				if(Effects.get(Constant.STATS_REM_R_EAU)!= null)
					val -= Effects.get(Constant.STATS_REM_R_EAU);
				break;
			case Constant.STATS_ADD_R_AIR:
				if(Effects.get(Constant.STATS_REM_R_AIR)!= null)
					val -= Effects.get(Constant.STATS_REM_R_AIR);
				break;
			case Constant.STATS_ADD_R_FEU:
				if(Effects.get(Constant.STATS_REM_R_FEU)!= null)
					val -= Effects.get(Constant.STATS_REM_R_FEU);
				break;
			case Constant.STATS_ADD_R_NEU:
				if(Effects.get(Constant.STATS_REM_R_NEU)!= null)
					val -= Effects.get(Constant.STATS_REM_R_NEU);
				break;
			case Constant.STATS_ADD_RP_TER:
				if(Effects.get(Constant.STATS_REM_RP_TER)!= null)
					val -= Effects.get(Constant.STATS_REM_RP_TER);
				break;
			case Constant.STATS_ADD_RP_EAU:
				if(Effects.get(Constant.STATS_REM_RP_EAU)!= null)
					val -= Effects.get(Constant.STATS_REM_RP_EAU);
				break;
			case Constant.STATS_ADD_RP_AIR:
				if(Effects.get(Constant.STATS_REM_RP_AIR)!= null)
					val -= Effects.get(Constant.STATS_REM_RP_AIR);
				break;
			case Constant.STATS_ADD_RP_FEU:
				if(Effects.get(Constant.STATS_REM_RP_FEU)!= null)
					val -= Effects.get(Constant.STATS_REM_RP_FEU);
				break;
			case Constant.STATS_ADD_RP_NEU:
				if(Effects.get(Constant.STATS_REM_RP_NEU)!= null)
					val -= Effects.get(Constant.STATS_REM_RP_NEU);
				break;
			case Constant.STATS_ADD_MAITRISE:
				if(Effects.get(Constant.STATS_ADD_MAITRISE)!= null)
					val = Effects.get(Constant.STATS_ADD_MAITRISE);
				break;
			}
			return val;
		}

		public static Stats cumulStat(Stats s1,Stats s2) {
			TreeMap<Integer,Integer> effets = new TreeMap<Integer,Integer>();
			for(int a = 0; a <= Constant.MAX_EFFECTS_ID; a++) {
				if((s1.Effects.get(a) == null  || s1.Effects.get(a) == 0) && (s2.Effects.get(a) == null || s2.Effects.get(a) == 0))
					continue;
				int som = 0;
				if(s1.Effects.get(a) != null)
					som += s1.Effects.get(a);

				if(s2.Effects.get(a) != null)
					som += s2.Effects.get(a);
				if(a==Constant.STATS_ADD_PA && som>Main.MAXPA)
					som=Main.MAXPA;
				if(a==Constant.STATS_ADD_PM && som>Main.MAXPM)
					som=Main.MAXPM;
				effets.put(a, som);
			}
			return new Stats(effets,false,null);
		}

		public static Stats cumulStatFight(Stats s1,Stats s2) {
			TreeMap<Integer,Integer> effets = new TreeMap<Integer,Integer>();
			for(int a = 0; a <= Constant.MAX_EFFECTS_ID; a++) {
				if((s1.Effects.get(a) == null  || s1.Effects.get(a) == 0) && (s2.Effects.get(a) == null || s2.Effects.get(a) == 0))
					continue;
				int som = 0;
				if(s1.Effects.get(a) != null)
					som += s1.Effects.get(a);

				if(s2.Effects.get(a) != null)
					som += s2.Effects.get(a);

				effets.put(a, som);
			}
			return new Stats(effets,false,null);
		}

		public java.util.Map<Integer, Integer> getMap() {
			return Effects;
		}

		public String parseToItemSetStats() {
			StringBuilder str = new StringBuilder();
			if(Effects.isEmpty())
				return "";
			for(Entry<Integer,Integer> entry : Effects.entrySet()) {
				if(str.length() >0)str.append(",");
				str.append(Integer.toHexString(entry.getKey())).append("#").append(Integer.toHexString(entry.getValue())).append("#0#0");
			}
			return str.toString();
		}
	}
}