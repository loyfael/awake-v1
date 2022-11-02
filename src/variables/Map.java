package variables;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.Timer;

import data.*;
import variables.Fight.Fighter;
import variables.NPC_tmpl.NPC;
import client.Player;
import data.Encrypt;

public class Map {
	private short _id;
	private String _date;
	private byte _w;
	private byte _h;
	private String _key;
	private String _placesStr;
	private java.util.Map<Integer,Case> _cases = new TreeMap<Integer,Case>();
	private java.util.Map<Integer,Fight> _fights = new TreeMap<Integer,Fight>();
	private java.util.Map<Integer,NPC> _npcs = new TreeMap<Integer, NPC>();
	private int _maxTeam0 = 0;
	private int _maxTeam1 = 0;
	int _nextObjectID = -1;

	public Map(short id, String date, byte w, byte h, String key, String places, String dData, String cellsData) {
		_id = id;
		_date = date;
		_w = w;
		_h = h;
		_key = key;
		_placesStr = places;
		try {
			String[] split = places.split("\\|");
			_maxTeam0 = (split[0].length()/2);
			_maxTeam1 = (split[1].length()/2);
		} catch(Exception e) {}
		if (!dData.isEmpty()) {

			_cases = Encrypt.DecompileMapData(this, dData);

		} else {
			String[] cellsDataArray = cellsData.split("\\|");
			for (String o : cellsDataArray) {
				boolean Walkable = true;
				boolean LineOfSight = true;
				int Number = -1;
				int obj = -1;
				String[] cellInfos = o.split(",");
				try {
					Walkable = cellInfos[2].equals("1");
					LineOfSight = cellInfos[1].equals("1");
					Number = Integer.parseInt(cellInfos[0]);
					if(!cellInfos[3].trim().equals("")) {
						obj = Integer.parseInt(cellInfos[3]);
					}
				} catch(Exception d) {};
				if(Number == -1)
					continue;
				_cases.put(Number, new Case(this,Number,Walkable,LineOfSight,obj));
			}
		}
	}

	public Map(short id, String date, byte w, byte h, String key, String places) {
		_id = id;
		_date = date;
		_w = w;
		_h = h;
		_key = key;
		_placesStr = places;
		_cases = new TreeMap<Integer,Case>();
	}

	public Map getMapCopy() {
		java.util.Map<Integer,Case> cases = new TreeMap<Integer,Case>();
		Map map = new Map(_id,_date,_w,_h,_key,_placesStr);
		for(Entry<Integer,Case> entry : _cases.entrySet())
			cases.put(entry.getKey(), new Case(map, entry.getValue().getID(), entry.getValue().isWalkable(false), entry.getValue().isLoS(), (entry.getValue().getObject()==null?-1:entry.getValue().getObject().getID())));
		map.setCases(cases);
		return map;
	}

	public void onPlayerArriveOnCell(Player perso, int caseID, boolean hasEndingFight) {
		if(_cases.get(caseID) == null)
			return;
		Item obj = _cases.get(caseID).getDroppedItem();
		if(obj != null) {
			if(perso.addObjet(obj, true))
				World.addObjet(obj, true);
			SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(this,'-',caseID,0,0);
			SocketManager.GAME_SEND_Ow_PACKET(perso);
			_cases.get(caseID).clearDroppedItem();
		}
		//_cases.get(caseID).applyOnCellStopActions(perso);
		if(_placesStr.equalsIgnoreCase("|"))
			return;
		if(perso.get_curCarte().get_id() != _id)
			return;
	}

	public void sendFloorItems(Player perso) {
		for(Case c : _cases.values()) {
			if(c.getDroppedItem() != null)
				SocketManager.GAME_SEND_GDO_PACKET(perso,'+',c.getID(),c.getDroppedItem().getTemplate().getID(),0);
		}
	}

	private void setCases(java.util.Map<Integer, Case> cases) {
		_cases = cases;
	}

	public void removeFight(int id) {
		_fights.remove(id);
	}

	public int getNbrFight() {
		return _fights.size();
	}

	public java.util.Map<Integer, Fight> get_fights() {
		return _fights;
	}

	public Fight getFight(int id) {
		return _fights.get(id);
	}

	public java.util.Map<Integer, NPC> get_npcs() {
		return _npcs;
	}

	public NPC addNpc(int npcID,int cellID, int dir) {
		NPC_tmpl temp = World.getNPCTemplate(npcID);
		if(temp == null)
			return null;
		if(getCase(cellID) == null)
			return null;
		NPC npc = new NPC(temp,_nextObjectID,cellID,(byte)dir);
		_npcs.put(_nextObjectID, npc);
		_nextObjectID--;
		return npc;
	}

	public NPC getNPC(int id) {
		return _npcs.get(id);
	}

	public NPC RemoveNPC(int id) {
		return _npcs.remove(id);
	}

	public String getNpcsGMsPackets() {
		if(_npcs.isEmpty())
			return "";
		StringBuilder packet = new StringBuilder();
		packet.append("GM|");
		boolean isFirst = true;
		for(Entry<Integer,NPC> entry : _npcs.entrySet()) {
			String GM = entry.getValue().parseGM();
			if(GM.equals(""))
				continue;
			if(!isFirst)
				packet.append("|");
			packet.append(GM);
			isFirst = false;
		}
		return packet.toString();
	}

	public void setPlaces(String place) {
		_placesStr = place;
	}

	public Case getCase(int id) {
		return _cases.get(id);
	}

	public Fight newFight(Player init1, Player init2, int type) {
		int id = 1;
		if(!_fights.isEmpty())
			id = ((Integer)(_fights.keySet().toArray()[_fights.size()-1]))+1;
		Fight f = new Fight(type,id,this,init1,init2);
		_fights.put(id,f);
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(this);
		return f;
	}

	public ArrayList<Player> getPersos() {
		ArrayList<Player> persos = new ArrayList<Player>();
		for (Case c : _cases.values()) {
			for (Player entry : c.getPersos().values()) {
				persos.add(entry);
			}
		}
		return persos;
	}

	public String getFightersGMsPackets() {
		StringBuilder packet = new StringBuilder();
		for(Entry<Integer,Case> cell : _cases.entrySet()) {
			for(Entry<Integer,Fighter> f : cell.getValue().getFighters().entrySet()) {
				packet.append(f.getValue().getGmPacket('+')).append('\u0000');
			}
		}
		return packet.toString();
	}

	public short get_id() {
		return _id;
	}

	public String get_date() {
		return _date;
	}

	public byte get_w() {
		return _w;
	}

	public byte get_h() {
		return _h;
	}

	public String get_key() {
		return _key;
	}

	public String get_placesStr() {
		return _placesStr;
	}

	public String getGMsPackets() {
		StringBuilder packet = new StringBuilder();
		for (Case cell : _cases.values())
			for (Player perso : cell.getPersos().values())
				packet.append("GM|+").append(perso.parseToGM()).append('\u0000');
		return packet.toString();
	}

	public java.util.Map<Integer, Case> GetCases() {
		return _cases;
	}

	public void addPlayer(Player perso) {
		SocketManager.GAME_SEND_ADD_PLAYER_TO_MAP(this,perso);
		perso.get_curCell().addPerso(perso);
	}

	public int get_maxTeam1() {
		return _maxTeam1;
	}

	public int get_maxTeam0() {
		return _maxTeam0;
	}

	public static class InteractiveObject {
		private int _id;
		private int _state;
		private Map _map;
		private Case _cell;
		private boolean _interactive = true;
		private Timer _respawnTimer;
		private IOTemplate _template;

		public InteractiveObject(Map a_map, Case a_cell, int a_id) {
			_id = a_id;
			_map = a_map;
			_cell = a_cell;
			_state = Constant.IOBJECT_STATE_FULL;
			int respawnTime = 10000;
			_template = World.getIOTemplate(_id);
			if(_template != null)
				respawnTime = _template.getRespawnTime();
			//définition du timer
			_respawnTimer = new Timer(respawnTime, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					_respawnTimer.stop();
					_state = 5;
					_interactive = true;
					SocketManager.GAME_SEND_GDF_PACKET_TO_MAP(_map, _cell);
					_state = 1;
				}
			});
		}

		public int getID() {
			return _id;
		}

		public boolean isInteractive() {
			return _interactive;
		}

		public void setInteractive(boolean b) {
			_interactive = b;
		}

		public int getState() {
			return _state;
		}

		public void setState(int state) {
			_state = state;
		}

		public int getUseDuration()	{
			int duration = 1500;
			if(_template != null) {
				duration = _template.getDuration();
			}
			return duration;
		}

		public void startTimer() {
			if(_respawnTimer == null)
				return;
			_state = 3;
			_respawnTimer.restart();
		}

		public int getUnknowValue() {
			int unk = 4;
			if(_template != null) {
				unk = _template.getUnk();
			}
			return unk;
		}

		public boolean isWalkable() {
			if(_template == null)
				return false;
			return _template.isWalkable() && _state == Constant.IOBJECT_STATE_FULL;
		}
	}

	public static class Case {
		private int _id;
		private java.util.Map<Integer, Player> _persos	= new TreeMap<Integer, Player>();
		private java.util.Map<Integer, Fight.Fighter> _fighters = new TreeMap<Integer, Fight.Fighter>();
		private boolean _LoS = true;
		@SuppressWarnings("unused")
		private short _map;
		private ArrayList<Action> _onCellStop;
		private boolean _Walkable = true;
		private Map.InteractiveObject _object;
		private Item _droppedItem;

		public Case(Map a_map, int id, boolean _walk, boolean LoS, int objID) {
			_map = a_map.get_id();
			_id = id;
			_Walkable = _walk;
			_LoS = LoS;
			if(objID == -1)
				return;
			_object = new Map.InteractiveObject(a_map,this,objID);
		}

		public Item getDroppedItem() {
			return _droppedItem;
		}

		public void addDroppedItem(Item obj) {
			_droppedItem = obj;
		}

		public void clearDroppedItem() {
			_droppedItem = null;
		}

		public int getID() {
			return _id;
		}

		public Map.InteractiveObject getObject() {
			return _object;
		}

		public void addFighter(Fight.Fighter fighter) {
			if(_fighters == null)
				_fighters = new TreeMap<Integer, Fight.Fighter>();
			_fighters.put(fighter.getGUID(),fighter);
		}

		public void removeFighter(Fight.Fighter fighter) {
			_fighters.remove(fighter.getGUID());
		}

		public boolean isWalkable(boolean useObject) {
			if(_object != null && useObject)
				return _Walkable && _object.isWalkable();
			return _Walkable;
		}

		public void addPerso(Player perso) {
			if(_persos == null)
				_persos = new TreeMap<Integer, Player>();
			_persos.put(perso.getId(), perso);
		}

		public boolean isLoS() {
			return _LoS;
		}

		public void removePlayer(int guid) {
			if(_persos == null)
				return;
			_persos.remove(guid);
			if(_persos.isEmpty())
				_persos = null;
		}

		public boolean blockLoS() {
			if(_fighters == null)
				return _LoS;
			boolean fighter = false;
			return _LoS && fighter;
		}

		public java.util.Map<Integer, Fight.Fighter> getFighters() {
			if(_fighters == null)
				return new TreeMap<Integer, Fight.Fighter>();
			return _fighters;
		}

		public Fight.Fighter getFirstFighter() {
			if(_fighters == null)
				return null;
			for(java.util.Map.Entry<Integer, Fight.Fighter> entry : _fighters.entrySet()) {
				return entry.getValue();
			}
			return null;
		}

		public java.util.Map<Integer, Player> getPersos() {
			if(_persos == null)
				return new TreeMap<Integer, Player>();
			return _persos;
		}

		public void addOnCellStopAction(int actionID, String actionsArgs, String conditions) {
			if (this._onCellStop == null)
				this._onCellStop = new ArrayList<Action>();

			this._onCellStop.add(new Action(actionID, actionsArgs, conditions));
		}
	}

}