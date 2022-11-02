package variables;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import kernel.Config;
import kernel.Main;
import realm.Client.GameAction;
import variables.Map.Case;
import variables.Spell.SortStats;
import client.Path;
import client.Formules;
import client.Player;
import client.Player.Stats;
import data.Constant;
import data.Encrypt;
import data.SocketManager;
import data.World;

public class Fight {
	private int _id;
	private java.util.Map<Integer,Fighter> _team0 = new TreeMap<Integer,Fighter>();
	private java.util.Map<Integer,Fighter> _team1 = new TreeMap<Integer,Fighter>();
	private java.util.Map<Integer,Fighter> deadList = new TreeMap<Integer,Fighter>();
	public java.util.Map<Integer, Player> _spec  = new TreeMap<Integer, Player>();
	private Map _map;
	private Map _mapOld;
	private Fighter _init0;
	private Fighter _init1;
	private ArrayList<Case> _start0 = new ArrayList<Case>();
	private ArrayList<Case> _start1 = new ArrayList<Case>();
	private int _state = 0;
	private int _type = -1;
	private boolean locked0 = false;
	private boolean locked1 = false;
	private boolean specOk = true;
	private boolean help1 = false;
	private boolean help2 = false;
	private int _st2;
	private int _st1;
	private int _curPlayer;
	private int _curFighterPA;
	private int _curFighterPM;
	private int _curFighterUsedPA;
	private int _curFighterUsedPM;
	private String _curAction = "";
	private List<Fighter> _ordreJeu = new ArrayList<Fighter>();
	private long _tiempoInicio = 0L;
	private long _tiempoInicioTurno = 0L;
	private TimerTask actTimerTask = null;
	private boolean FightStarted = false;

	public boolean getSpect(int id) {
		for(Player persoc: _spec.values()) {
			if (persoc.getId() == id)
				return true;
		}
		return false;
	}

	private synchronized void startTimer(int time, final boolean isTurn) {
		Main.TimersPelea.schedule(actTimerTask = new TimerTask(){
			public void run(){
				try {
					if (isTurn) {
						endTurn();
					} else {
						if (!FightStarted) {
							startFight();
						}
					}
					cancel();
					return;
				} catch (Exception e){
					cancel();
					System.out.println(e.getMessage()+"");
					return;
				}
			}
		}, time);
	}

	private void setFightStarted(boolean fightStarted) {
		FightStarted = fightStarted;
	}

	private void setActTimerTask(TimerTask actTimerTasks) {
		actTimerTask = actTimerTasks;
	}

	private TimerTask getActTimerTask() {
		return actTimerTask;
	}

	public Fight(int type, int id, Map map, Player init1, Player init2) {
		_type = type;
		_id = id;
		_map = map.getMapCopy();
		_mapOld = map;
		_init0 = new Fighter(this,init1);
		_init1 = new Fighter(this,init2);
		_team0.put(init1.getId(), _init0);
		_team1.put(init2.getId(), _init1);
		SocketManager.GAME_SEND_ILF_PACKET(init1, 0);
		SocketManager.GAME_SEND_ILF_PACKET(init2, 0);
		int cancelBtn = 1;
		int time = 45000;
		SocketManager.GAME_SEND_FIGHT_GJK_PACKET_TO_FIGHT(this,7,2, cancelBtn,1,0, time,_type);
		startTimer(time, false);
		Random teams = new Random();
		if(teams.nextBoolean()) {
			_start0 = parsePlaces(0);
			_start1 = parsePlaces(1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),0);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,2,_map.get_placesStr(),1);
			_st1 = 0;
			_st2 = 1;
		} else {
			_start0 = parsePlaces(1);
			_start1 = parsePlaces(0);
			_st1 = 1;
			_st2 = 0;
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,1,_map.get_placesStr(),1);
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET_TO_FIGHT(this,2,_map.get_placesStr(),0);
		}
		_init0.set_fightCell(getRandomCell(_start0));
		_init1.set_fightCell(getRandomCell(_start1));System.out.println("LLEGA5");
		_init0.getPersonnage().get_curCell().removePlayer(_init0.getGUID());
		_init1.getPersonnage().get_curCell().removePlayer(_init1.getGUID());
		_init0.get_fightCell().addFighter(_init0);
		_init1.get_fightCell().addFighter(_init1);
		_init0.getPersonnage().set_fight(this);
		_init0.setTeam(0);
		_init1.getPersonnage().set_fight(this);
		_init1.setTeam(1);System.out.println("LLEG6");
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init0.getPersonnage().get_curCarte(), _init0.getGUID());
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_init1.getPersonnage().get_curCarte(), _init1.getGUID());
		SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),0,_init0.getGUID(),_init1.getGUID(),_init0.getPersonnage().get_curCell().getID(),"0;-1", _init1.getPersonnage().get_curCell().getID(), "0;-1");
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID(), _init0);
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init1.getGUID(), _init1);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS_TO_FIGHT(this,7,_map);
		set_state(Constant.FIGHT_STATE_PLACE);
	}

	public Map get_map() {
		return _map;
	}

	private Case getRandomCell(List<Case> cells) {
		Random rand = new Random();
		Case cell;
		if(cells.isEmpty())
			return null;
		int limit = 0;
		do {
			int id = rand.nextInt(cells.size()-1);
			cell = cells.get(id);
			limit++;
		} while((cell == null || !cell.getFighters().isEmpty()) && limit < 80);
		if(limit == 80)
			return null;
		return cell;		
	}

	private ArrayList<Case> parsePlaces(int num) {
		return Encrypt.parseStartCell(_map, num);
	}

	public int get_id() {
		return _id;
	}

	public ArrayList<Fighter> getFighters(int teams) {
		ArrayList<Fighter> fighters = new ArrayList<Fighter>();
		if(teams - 4 >= 0) {
			for(Entry<Integer, Player> entry : _spec.entrySet()) {
				fighters.add(new Fighter(this,entry.getValue()));
			}
			teams -= 4;
		}
		if(teams -2 >= 0) {
			for(Entry<Integer,Fighter> entry : _team1.entrySet()) {
				fighters.add(entry.getValue());
			}
			teams -= 2;
		}
		if(teams -1 >=0) {	
			for(Entry<Integer,Fighter> entry : _team0.entrySet()) {
				fighters.add(entry.getValue());
			}
		}
		return fighters;
	}

	public synchronized void changePlace(Player perso, int cell) {
		Fighter fighter = getFighterByPerso(perso);
		int team = getTeamID(perso.getId()) -1;
		if(fighter == null)
			return;
		if(get_state() != 2 || isOccuped(cell) || perso.is_ready() || (team == 0 && !groupCellContains(_start0,cell)) || (team == 1 && !groupCellContains(_start1,cell)))
			return;
		fighter.get_fightCell().getFighters().clear();
		fighter.set_fightCell(_map.getCase(cell));
		_map.getCase(cell).addFighter(fighter);
		SocketManager.GAME_SEND_FIGHT_CHANGE_PLACE_PACKET_TO_FIGHT(this,3,_map,perso.getId(),cell);
	}

	public boolean isOccuped(int cell) {
		return _map.getCase(cell).getFighters().size() > 0;
	}

	private boolean groupCellContains(ArrayList<Case> cells, int cell) {
		for(int a = 0; a<cells.size();a++) {
			if(cells.get(a).getID() == cell)
				return true;
		}
		return false;
	}

	public void verifIfAllReady() {
		boolean val = true;
		for (int a = 0; a < _team0.size(); a++) {
			if (!_team0.get(_team0.keySet().toArray()[a]).getPersonnage().is_ready())
				val = false;
		}
		if (_type !=4 && _type != 5 && _type != Constant.FIGHT_TYPE_CONQUETE) {
			for (int a = 0; a < _team1.size(); a++) {
				if (!_team1.get(_team1.keySet().toArray()[a]).getPersonnage().is_ready())
					val = false;
			}
		}
		if (_type == 5 || _type == 2)
			val = false;
		if (val)
			startFight();
	}

	private void startFight() {
		if(_state >= Constant.FIGHT_STATE_ACTIVE)
			return;
		_tiempoInicio = System.currentTimeMillis();
		_tiempoInicioTurno = 0;
		_state = Constant.FIGHT_STATE_ACTIVE;
		SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(),_init0.getGUID());
		SocketManager.GAME_SEND_GIC_PACKETS_TO_FIGHT(this, 7);
		SocketManager.GAME_SEND_GS_PACKET_TO_FIGHT(this, 7);
		InitOrdreJeu();
		_curPlayer = -1;
		SocketManager.GAME_SEND_GTL_PACKET_TO_FIGHT(this,7);
		SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
		for(Fighter F : getFighters(3)) {
			Player perso = F.getPersonnage();
			if (perso == null)
				continue;
		}
		setFightStarted(true);
		if(getActTimerTask() != null)
			getActTimerTask().cancel();
		setActTimerTask(null);
		try {
			Thread.sleep(100);
		} catch(Exception e) {};
		startTurn();
	}

	private void startTurn() {
		if(!verifyStillInFight()) 
			verifIfTeamAllDead();
		if(_state >= Constant.FIGHT_STATE_FINISHED)
			return;
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		_curPlayer++;
		_curAction = "";
		if (_curPlayer < 0)
			return;
		if (_ordreJeu == null)
			return;
		if(_curPlayer >= _ordreJeu.size())
			_curPlayer = 0;
		_curFighterPA = _ordreJeu.get(_curPlayer).getPA();
		_curFighterPM = _ordreJeu.get(_curPlayer).getPM();
		_curFighterUsedPA = 0;
		_curFighterUsedPM = 0;
		if(_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead()) {
			System.out.println("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		_ordreJeu.get(_curPlayer).applyBeginningTurnBuff(this);
		if(_state == Constant.FIGHT_STATE_FINISHED)
			return;
		if(_ordreJeu.get(_curPlayer).getPDV()<=0)
			onFighterDie(_ordreJeu.get(_curPlayer), _init0);
		_ordreJeu.get(_curPlayer).ActualiseLaunchedSort();
		if(_ordreJeu == null)
			return;
		if(_ordreJeu.size() < _curPlayer)
			return;
		if(_ordreJeu.get(_curPlayer) == null)
			return;
		if(_ordreJeu.get(_curPlayer).isDead()) {
			System.out.println("("+_curPlayer+") Fighter ID=  "+_ordreJeu.get(_curPlayer).getGUID()+" est mort");
			endTurn();
			return;
		}
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null) {
			SocketManager.GAME_SEND_STATS(_ordreJeu.get(_curPlayer).getPersonnage());
		}
		if(_ordreJeu.get(_curPlayer).hasBuff(Constant.EFFECT_PASS_TURN)) {
			endTurn();
			return;
		}
		System.out.println("("+_curPlayer+")Debut du tour de Fighter ID = "+_ordreJeu.get(_curPlayer).getGUID());
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID(), 44000);
		_tiempoInicioTurno = System.currentTimeMillis();
		startTimer(45000, true);
		try {
			Thread.sleep(350);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		_ordreJeu.get(_curPlayer).setCanPlay(true);
	}

	public void endTurn() {
		try {
			if(_curPlayer == -1)
				return;
			if(_ordreJeu == null || _ordreJeu.get(_curPlayer) == null)
				return;
			if(_state >= Constant.FIGHT_STATE_FINISHED)
				return;
			if(_ordreJeu.get(_curPlayer).hasLeft() || _ordreJeu.get(_curPlayer).isDead()) {
				startTurn();
				return;
			}
			if (getActTimerTask() != null)
				getActTimerTask().cancel();
			setActTimerTask(null);
			_tiempoInicioTurno = 0L;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {e1.printStackTrace();}
			if(!_curAction.equals("") && _ordreJeu.get(_curPlayer).getPersonnage() != null) {
				while(!_curAction.isEmpty()){}
			}
			SocketManager.GAME_SEND_GAMETURNSTOP_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID());
			_ordreJeu.get(_curPlayer).setCanPlay(false);
			_curAction = "";
			for(SpellEffect SE : _ordreJeu.get(_curPlayer).getBuffsByEffectID(131)) {
				int pas = SE.getValue();
				int val = -1;
				try {
					val = Integer.parseInt(SE.getArgs().split(";")[1]);
				} catch(Exception e){};
				if(val == -1)
					continue;
				int nbr = (int) Math.floor((double)_curFighterUsedPA/(double)pas);
				int dgt = val * nbr;
				if(_ordreJeu.get(_curPlayer).hasBuff(184)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+_ordreJeu.get(_curPlayer).getBuff(184).getValue());
					dgt = dgt-_ordreJeu.get(_curPlayer).getBuff(184).getValue();//Réduction physique
				}
				if(_ordreJeu.get(_curPlayer).hasBuff(105)) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 105, _ordreJeu.get(_curPlayer).getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+_ordreJeu.get(_curPlayer).getBuff(105).getValue());
					dgt = dgt-_ordreJeu.get(_curPlayer).getBuff(105).getValue();//Immu
				}
				if(dgt <= 0)
					continue;
				if(dgt>_ordreJeu.get(_curPlayer).getPDV())
					dgt = _ordreJeu.get(_curPlayer).getPDV();//va mourrir
				_ordreJeu.get(_curPlayer).removePDV(dgt);
				dgt = -(dgt);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 100, SE.getCaster().getGUID()+"", _ordreJeu.get(_curPlayer).getGUID()+","+dgt);

			}
			if(_ordreJeu.get(_curPlayer).getPDV() <= 0)
				onFighterDie(_ordreJeu.get(_curPlayer), _init0);
			_curFighterUsedPA = 0;
			_curFighterUsedPM = 0;
			_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA);
			_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM);
			_ordreJeu.get(_curPlayer).refreshfightBuff();
			if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
				if(_ordreJeu.get(_curPlayer).getPersonnage().getOnline())
					SocketManager.GAME_SEND_STATS(_ordreJeu.get(_curPlayer).getPersonnage());
			SocketManager.GAME_SEND_GTM_PACKET_TO_FIGHT(this, 7);
			SocketManager.GAME_SEND_GTR_PACKET_TO_FIGHT(this, 7, _ordreJeu.get(_curPlayer==_ordreJeu.size()?0:_curPlayer).getGUID());
			System.out.println("("+_curPlayer+")Fin du tour de Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID());
			startTurn();
		} catch(NullPointerException e) {
			e.printStackTrace();
			endTurn();
		}
	}

	private void InitOrdreJeu() {
		int j = 0;
		int k = 0;
		int start0 = 0;
		int start1 = 0;
		int curMaxIni0 = 0;
		int curMaxIni1 = 0;
		Fighter curMax0 = null;
		Fighter curMax1 = null;
		boolean team1_ready = false;
		boolean team2_ready = false;
		do {
			if (!team1_ready) {
				team1_ready = true;
				java.util.Map<Integer, Fighter> _Team0 = _team0;
				for (Entry<Integer, Fighter> entry : _Team0.entrySet()) {
					if (_ordreJeu.contains(entry.getValue()))
						continue;
					team1_ready = false;
					if (entry.getValue().getInitiative() >= curMaxIni0) {
						curMaxIni0 = entry.getValue().getInitiative();
						curMax0 = entry.getValue();
					}
					if (curMaxIni0 > start0) {
						start0 = curMaxIni0;
					}
				}
			}
			if (!team2_ready) {
				team2_ready = true;
				for (Entry<Integer, Fighter> entry : _team1.entrySet()) {
					if (_ordreJeu.contains(entry.getValue()))
						continue;
					team2_ready = false;
					if (entry.getValue().getInitiative() >= curMaxIni1) {
						curMaxIni1 = entry.getValue().getInitiative();
						curMax1 = entry.getValue();
					}
					if (curMaxIni1 > start1) {
						start1 = curMaxIni1;
					}
				}
			}
			if ((curMax1 == null) && (curMax0 == null))
				return;
			if (start0 > start1) {
				if (getFighters(1).size() > j) {
					_ordreJeu.add(curMax0);
					j++;
				}
				if (getFighters(2).size() > k) {
					_ordreJeu.add(curMax1);
					k++;
				}
			} else {
				if (getFighters(2).size() > j) {
					_ordreJeu.add(curMax1);
					j++;
				}
				if (getFighters(1).size() > k) {
					_ordreJeu.add(curMax0);
					k++;
				}
			}
			curMaxIni0 = 0;
			curMaxIni1 = 0;
			curMax0 = null;
			curMax1 = null;
		} while (_ordreJeu.size() != getFighters(3).size());
	}

	public void joinFight(Player perso, int guid) {
		if (_tiempoInicio != 0L) {
			SocketManager.SEND_GA903_FIGHT_ERROR(perso, 'l');
			return;
		}
		long timeRestant = 44000 - (System.currentTimeMillis() - _tiempoInicioTurno);
		Fighter current_Join = null;
		if(_team0.containsKey(guid)) {
			Case cell = getRandomCell(_start0);
			if(cell == null)
				return;
			if(locked0) {
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso,'f',guid);
				return;
			}
			if(_type == Constant.FIGHT_TYPE_CHALLENGE) {
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,timeRestant,_type);
			} else {
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,timeRestant,_type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso, _map.get_placesStr(), _st1);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.getId());
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(0);
			_team0.put(perso.getId(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
			SocketManager.GAME_SEND_ILF_PACKET(perso, 0);
		} else if(_team1.containsKey(guid)) {
			Case cell = getRandomCell(_start1);
			if(cell == null)
				return;
			if(locked1) {
				SocketManager.GAME_SEND_GA903_ERROR_PACKET(perso,'f',guid);
				return;
			}
			if(_type == Constant.FIGHT_TYPE_CHALLENGE) {
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,1,1,0,0,_type);
			} else {
				SocketManager.GAME_SEND_GJK_PACKET(perso,2,0,1,0,0,_type);
			}
			SocketManager.GAME_SEND_FIGHT_PLACES_PACKET(perso, _map.get_placesStr(), _st2);
			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(perso.get_curCarte(), perso.getId());
			Fighter f = new Fighter(this, perso);
			current_Join = f;
			f.setTeam(1);
			_team1.put(perso.getId(), f);
			perso.set_fight(this);
			f.set_fightCell(cell);
			f.get_fightCell().addFighter(f);
		}
		perso.get_curCell().removePlayer(perso.getId());
		SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_MAP(perso.get_curCarte(),(current_Join.getTeam()==0?_init0:_init1).getGUID(), current_Join);
		SocketManager.GAME_SEND_FIGHT_PLAYER_JOIN(this,7,current_Join);
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this,_map,perso);
	}

	public void toggleLockTeam(int guid) {
		if(_init0 != null && _init0.getGUID() == guid) {
			locked0 = !locked0;
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init0.getPersonnage().get_curCarte(), locked0?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,1,locked0?"095":"096");
		} else if(_init1 != null && _init1.getGUID() == guid) {
			locked1 = !locked1;
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_init1.getPersonnage().get_curCarte(), locked1?'+':'-', 'A', guid);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this,2,locked1?"095":"096");
		}
	}

	public void toggleLockSpec(int guid) {
		if((_init0 != null && _init0.getGUID() == guid) || (_init1 != null &&  _init1.getGUID() == guid)) {
			specOk = !specOk;
			if(!specOk) {
				for(Entry<Integer, Player> spectateur : _spec.entrySet()) {
					Player perso = spectateur.getValue();
					SocketManager.GAME_SEND_GV_PACKET(perso);
					_spec.remove(perso.getId());
					perso.setSitted(false);
					perso.set_fight(null);
					perso.set_away(false);
				}
				if (_init0.getGUID() == guid)
					SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_mapOld, specOk?'+':'-', 'S', _init0.getGUID());
				else
					SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_mapOld, specOk?'+':'-', 'S', _init1.getGUID());
				SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7 ,specOk ? "039":"040");
			}
		}
	}

	public void toggleHelp(int id) {
		if ((_init0 != null) && (_init0.getGUID() == id)) {
			help1 = (!help1);
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_mapOld, help1 ? '+' : '-', 'H', id);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 1, help1 ? "0103" : "0104");
		} else if ((_init1 != null) && (_init1.getGUID() == id)) {
			help2 = (!help2);
			SocketManager.GAME_SEND_FIGHT_CHANGE_OPTION_PACKET_TO_MAP(_mapOld, help2 ? '+' : '-', 'H', id);
			SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 2, help2 ? "0103" : "0104");
		}
	}

	private void set_state(int _state) {
		this._state = _state;
	}

	public int get_state() {
		return _state;
	}

	public int get_type() {
		return _type;
	}

	public List<Fighter> get_ordreJeu() {
		return _ordreJeu;
	}

	public boolean fighterDeplace(Fighter f, GameAction GA) {
		String path = GA._args;
		if(path.equals("")) {
			System.out.println("Echec du deplacement: chemin vide");
			return false;
		}
		if(_ordreJeu.size() <= _curPlayer)
			return false;
		if(_ordreJeu.get(_curPlayer) == null)
			return false;
		System.out.println("Path: "+path);
		if(!_curAction.equals("")|| _ordreJeu.get(_curPlayer).getGUID() != f.getGUID() || _state != Constant.FIGHT_STATE_ACTIVE)
			return false;
		Fighter tacleador = Path.getEnemyAround(f.get_fightCell().getID(), _map, this);
		if (tacleador != null && !f.isState(6) && !f.isState(8)) {
			int porcEsquiva = Formules.getTackleChance(f, tacleador);
			int rand = Formules.getRandomValue(0, 99);
			if (rand > porcEsquiva) {
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._idUnique, "104", f.getGUID() + ";", "");
				int pierdePA = _curFighterPA * porcEsquiva / 100;
				if (pierdePA < 0)
					pierdePA = -pierdePA;
				if (_curFighterPM < 0)
					_curFighterPM = 0;
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._idUnique, "129", f.getGUID() + "", f.getGUID() + ",-" + _curFighterPM);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._idUnique, "102", f.getGUID() + "", f.getGUID() + ",-" + pierdePA);
				_curFighterPM = 0;
				_curFighterPA -= pierdePA;
				return false;
			}
		}
		AtomicReference<String> pathRef = new AtomicReference<String>(path);
		int nStep = Path.isValidPath(_map, f.get_fightCell().getID(), pathRef, this);
		String newPath = pathRef.get();
		if( nStep > _curFighterPM || nStep == -1000) {
			System.out.println("("+_curPlayer+") Fighter ID= "+_ordreJeu.get(_curPlayer).getGUID()+" a demander un chemin inaccessible ou trop loin");
			return false;
		}
		_curFighterPM -= nStep;
		_curFighterUsedPM += nStep;
		int nextCellID = Encrypt.cellCode_To_ID(newPath.substring(newPath.length() - 2));
		if(_ordreJeu.get(_curPlayer).getPersonnage() != null)
			SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this,7,_ordreJeu.get(_curPlayer).getGUID());
		SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, GA._idUnique, "1", _ordreJeu.get(_curPlayer).getGUID()+"", "a"+ Encrypt.cellID_To_Code(f.get_fightCell().getID())+newPath);
		_ordreJeu.get(_curPlayer).get_fightCell().getFighters().clear();
		_ordreJeu.get(_curPlayer).set_fightCell(_map.getCase(nextCellID));
		_ordreJeu.get(_curPlayer).get_fightCell().addFighter(_ordreJeu.get(_curPlayer));
		if(nStep < 0) {
			nStep = nStep*(-1);
		}
		_curAction = "GA;129;"+_ordreJeu.get(_curPlayer).getGUID()+";"+_ordreJeu.get(_curPlayer).getGUID()+",-"+nStep;
		if(f.getPersonnage() == null) {
			try {
				Thread.sleep(900+100*nStep);
			} catch (InterruptedException e) {};
			SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this,7,_curAction);
			_curAction = "";
			return true;
		}
		f.getPersonnage().getAccount().getClient().addAction(GA);
		return true;
	}

	public int tryCastSpell(Fighter fighter,SortStats Spell, int caseID) {
		if(!_curAction.equals(""))
			return 10;
		if(Spell == null)
			return 10;
		Case Cell = _map.getCase(caseID);
		_curAction = "casting";
		if(CanCastSpell(fighter,Spell,Cell, -1)) {
			List<String> list = new ArrayList<String>();
			list.add("#0000FF");
			list.add("#FF4000");
			list.add("#8A0829");
			list.add("#0B610B");
			list.add("#8B1EDE");
			list.add("#DFA209");
			String random = list.get(new Random().nextInt(list.size()));

			SocketManager.GAME_SEND_MESSAGE3(this, random);
			if (isOccuped(caseID)) {
				if(fighter.getPersonnage() != null)
					SocketManager.GAME_SEND_STATS(fighter.getPersonnage());
				SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID());
				boolean isEc = Spell.getTauxEC() != 0 && Formules.getRandomValue(1, Spell.getTauxEC()) == Spell.getTauxEC();
				if(isEc) {
					System.out.println(fighter.getPacketsName()+" Coup critique sur le sort "+Spell.getSpellID());
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 302, fighter.getGUID()+"", Spell.getSpellID()+"");
					SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
				} else {
					boolean isCC = fighter.testIfCC(Spell.getTauxCC(), Spell);
					String sort = Spell.getSpellID()+","+caseID+",0,0,0,"+Spell.getSpriteInfos();
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID()+"", sort);
					if(isCC) {
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 301, fighter.getGUID()+"", sort); // CC !
					}
					Spell.applySpellEffectToFight(this,fighter,Cell,isCC);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {};
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 101,fighter.getGUID()+"",fighter.getGUID()+",-"+Spell.getPACost());
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {};
				if(!isEc)
					fighter.addLaunchedSort(Cell.getFirstFighter(),Spell,fighter);
				if((isEc && Spell.isEcEndTurn())) {
					_curAction = "";
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {};
					endTurn();
					return 5;
				}
				verifIfTeamAllDead();
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			} else {
				_curFighterPA -= Spell.getPACost();
				_curFighterUsedPA += Spell.getPACost();
				String sort = Spell.getSpellID()+","+caseID+",0,0,0,"+Spell.getSpriteInfos();
				if(fighter.getPersonnage() != null)
					SocketManager.GAME_SEND_STATS(fighter.getPersonnage());
				SocketManager.GAME_SEND_GAS_PACKET_TO_FIGHT(this, 7, fighter.getGUID());
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 300, fighter.getGUID()+"", sort);
				SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(this, 7, 101,fighter.getGUID()+"",fighter.getGUID()+",-"+Spell.getPACost());
				SocketManager.GAME_SEND_MESSAGE2(this, fighter.getPersonnage().getName()," cast a spell in a cell that has ", "NO", " fighters");
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, fighter.getGUID());
			}
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {}
		_curAction = "";
		return 0;

	}

	public boolean CanCastSpell(Fighter caster, SortStats SS, Case cell, int launchCase) {
		int ValidlaunchCase;
		if (launchCase <= -1)
			ValidlaunchCase = caster.get_fightCell().getID();
		else
			ValidlaunchCase = launchCase;
		if (_ordreJeu == null)
			return false;
		if (_curPlayer < 0)
			return false;
		if (_curPlayer >= _ordreJeu.size())
			_curPlayer = 0;
		Fighter f = _ordreJeu.get(_curPlayer);
		Player perso = caster.getPersonnage();
		if (SS == null) {
			if (perso != null) {
				SocketManager.GAME_SEND_GA_CLEAR_PACKET_TO_FIGHT(this, 7);
				SocketManager.GAME_SEND_Im_PACKET(perso, "1169");
				SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 0, perso.getId());
			}
			return false;
		}
		if (f == null || f.getGUID() != caster.getGUID()) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1175");
			}
			return false;
		}
		int usedPA = 0;
		usedPA = SS.getPACost();
		if (_curFighterPA < usedPA) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1170;" + _curFighterPA + "~" + SS.getPACost());
			}
			return false;
		}
		if (cell == null) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1172");
			}
			return false;
		}
		if (SS.isLineLaunch() && !Path.casesAreInSameLine(_map, ValidlaunchCase, cell.getID(), 'z')) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1173");
			}
			return false;
		}
		char dir = Path.getDirBetweenTwoCase(ValidlaunchCase, cell.getID(), _map, true);
		if(SS.getSpellID() == 67)
			if(!Path.checkLoS(_map, Path.GetCaseIDFromDirection(ValidlaunchCase, dir, _map, true), cell.getID(), null, true)) {
				if(perso != null)
					SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
				return false;
			}
		if (SS.hasLDV() && !Path.checkLoS(_map, ValidlaunchCase, cell.getID(), caster)) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1174");
			}
			return false;
		}
		int dist = Path.getDistanceBetween(_map, ValidlaunchCase, cell.getID());
		int maxAlc = SS.getMaxPO();
		int minAlc = SS.getMinPO();
		if (SS.isModifPO()) {
			maxAlc += caster.getTotalStats().getEffect(117);
			if (maxAlc < minAlc)
				maxAlc = minAlc;
		}
		if (dist < minAlc || dist > maxAlc) {
			if (perso != null) {
				SocketManager.GAME_SEND_Im_PACKET(perso, "1171;" + minAlc + "~" + maxAlc + "~" + dist);
			}
			return false;
		}
		if (!LaunchedSort.coolDownGood(caster, SS.getSpellID()))
			return false;
		int numLunch = SS.getMaxLaunchbyTurn();
		if (numLunch - LaunchedSort.getNbLaunch(caster, SS.getSpellID()) <= 0 && numLunch > 0)
			return false;
		Fighter t = cell.getFirstFighter();
		int numLunchT = SS.getMaxLaunchbyTurn();
		if (numLunchT - LaunchedSort.getNbLaunchTarget(caster, t, SS.getSpellID()) <= 0 && numLunchT > 0)
			return false;
		return true;
	}

	public void onGK(Player perso) {
		if (_curPlayer < 0)
			return;
		if (_ordreJeu.size() == 0)
			return;
		if (_curPlayer >= _ordreJeu.size())
			_curPlayer = 0;
		int idLuchador = _ordreJeu.get(_curPlayer).getGUID();
		if (_curAction.equals("") || idLuchador != perso.getId() || _state != Constant.FIGHT_STATE_ACTIVE)
			return;
		SocketManager.GAME_SEND_GAMEACTION_TO_FIGHT(this, 7, _curAction);
		SocketManager.GAME_SEND_GAF_PACKET_TO_FIGHT(this, 7, 2, idLuchador);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {}
		_curAction = "";
	}


	public void playerPass(Player _perso) {
		Fighter f = getFighterByPerso(_perso);
		if(f == null)
			return;
		if(!f.canPlay())
			return;
		if(!_curAction.equals("")) 
			return;
		endTurn();
	}

	public boolean verifIfTeamIsDead() {
		boolean finish = true;
		for(Entry<Integer,Fighter> entry : _team1.entrySet()) {
			if(!entry.getValue().isDead()) {
				finish = false;
				break;
			}
		}
		return finish;
	}
	public void checkTeam() {
		System.out.println("ES: "+_team0.size()+" MAS "+_team1.size());

	}

	public void verifIfTeamAllDead() {
		if(_state >= Constant.FIGHT_STATE_FINISHED)
			return;
		boolean team0 = true;
		boolean team1 = true;
		for(Entry<Integer,Fighter> entry : _team0.entrySet()) {
			if(!entry.getValue().isDead()) {
				team0 = false;
				break;
			}
		}
		for(Entry<Integer,Fighter> entry : _team1.entrySet()) {
			if(!entry.getValue().isDead()) {
				team1 = false;
				break;
			}
		}
		if(team0 || team1 || !verifyStillInFight()) {
			_state = Constant.FIGHT_STATE_FINISHED;
			if(getActTimerTask() != null) 
				getActTimerTask().cancel();
			setActTimerTask(null);
			_tiempoInicioTurno = 0;
			setFightStarted(false);
			int winner = team0?2:1;
			_curPlayer = -1;
			SocketManager.GAME_SEND_FIGHT_GE_PACKET_TO_FIGHT(this,7,winner);
			Player perso = null;
			for (Fighter luchador : getFighters(7)) {
				perso = luchador.getPersonnage();
				if (perso != null) {
					perso.set_away(false);
					perso.set_duelID(-1);
					perso.set_ready(false);
					perso.set_fight(null);
				}
			}
			for(Player persoc: _spec.values()) {
				persoc.get_curCarte().addPlayer(persoc);
				persoc.refreshMapAfterFight();
			}
			World.getCarte(_map.get_id()).removeFight(_id);
			SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
			_map = null;
			_ordreJeu = null;
			ArrayList<Fighter> winTeam = new ArrayList<Fighter>();
			ArrayList<Fighter> looseTeam = new ArrayList<Fighter>();
			if(team0) {
				looseTeam.addAll(_team0.values());
				winTeam.addAll(_team1.values());
			} else {
				winTeam.addAll(_team0.values());
				looseTeam.addAll(_team1.values());
			}
			for(Fighter F : winTeam) {	
				Player player = F.getPersonnage();
				if(F.hasLeft())
					continue;
				if(player == null)
					continue;
				if(!player.getOnline())
					continue;
				if(_type != Constant.FIGHT_TYPE_CHALLENGE) {
					if(F.getPDV() <= 0) {
						player.set_PDV(1);
					} else {
						player.set_PDV(F.getPDV());	
					}
				}
				System.out.println("LLEGA PARA ACTUALIZAR MAPA: "+player.getName());
				player.refreshMapAfterFight();
				try {
					Thread.sleep(500);
				} catch(Exception E){};
				SocketManager.GAME_SEND_GV_PACKET(player);
			}
			for (Fighter F : looseTeam) {	
				Player player = F.getPersonnage();
				if(F.hasLeft())
					continue;
				if(player == null)
					continue;
				if(!F.getPersonnage().getOnline())
					continue;
				player.refreshMapAfterFight();
				try {
					Thread.sleep(500);
				} catch(Exception E){};
				SocketManager.GAME_SEND_GV_PACKET(player);
			}
		}
	}

	public void onFighterDie(Fighter target, Fighter caster)  {
		target.setIsDead(true);
		if(!target.hasLeft())
			deadList.put(target.getGUID(), target);
		SocketManager.GAME_SEND_FIGHT_PLAYER_DIE_TO_FIGHT(this,7,target.getGUID());
		target.get_fightCell().getFighters().clear();
		for(Fighter fighter : getFighters(3)) {
			ArrayList <SpellEffect> newBuffs = new ArrayList<SpellEffect>();
			for(SpellEffect entry : fighter.get_fightBuff()) {
				int casterId = entry.getCaster().getGUID();
				if(casterId == target.getGUID()) 
					continue;
				newBuffs.add(entry);
			}
			fighter.get_fightBuff().clear();
			fighter.get_fightBuff().addAll(newBuffs);
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {};
	}

	public int getTeamID(int guid) {
		if(_team0.containsKey(guid))
			return 1;
		if(_team1.containsKey(guid))
			return 2;
		if(_spec.containsKey(guid))
			return 4;
		return -1;
	}

	public int getOtherTeamID(int guid) {
		if(_team0.containsKey(guid))
			return 2;
		if(_team1.containsKey(guid))
			return 1;
		return -1;
	}

	public Fighter getFighterByPerso(Player perso) {
		Fighter fighter = null;
		if(_team0.get(perso.getId()) != null)
			fighter = _team0.get(perso.getId());
		if(_team1.get(perso.getId()) != null)
			fighter = _team1.get(perso.getId());
		return fighter;
	}

	public Fighter getCurFighter() {
		return _ordreJeu.get(_curPlayer);
	}

	public void refreshCurPlayerInfos() {
		_curFighterPA = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PA) - _curFighterUsedPA;
		_curFighterPM = _ordreJeu.get(_curPlayer).getTotalStats().getEffect(Constant.STATS_ADD_PM) - _curFighterUsedPM;
	}

	public void leftFight(Player perso, Player target) {
		if(perso == null)
			return;
		Fighter F = getFighterByPerso(perso);
		Fighter T = null;
		if (target != null) 
			T = getFighterByPerso(target);
		if (F != null) {
			switch(_type) {
			case Constant.FIGHT_TYPE_CHALLENGE://Desafio
				if(_state >= Constant.FIGHT_STATE_ACTIVE) { //EMPEZO_PELEA
					if(_ordreJeu.size() == 0){
						endTurn();
					} else if(_ordreJeu.get(_curPlayer).getGUID() == F.getGUID()) {
						endTurn();
					}
					F.setLeft(true);
					onFighterDie(F, T);
					boolean StillInFight = false;
					StillInFight = verifyStillInFightTeam(F.getGUID());
					Player Pd = F.getPersonnage();
					if (Pd != null) {
						Pd.set_duelID(-1);
						Pd.set_ready(false);
						Pd.set_fight(null);
						Pd.setSitted(false);
						Pd.set_away(false);
						Pd.fullPDV();
						if(Pd.getOnline()) {
							Pd.refreshMapAfterFight();
							try {
								Thread.sleep(200);
							} catch(Exception E){};
							SocketManager.GAME_SEND_GV_PACKET(Pd);
						} else {
							SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
						}
					}
					if(!StillInFight) {
						verifIfTeamAllDead();
						return;
					} else {
						Player P = F.getPersonnage();
						if (P != null) {
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);
							if (P.getOnline()) {
								P.refreshMapAfterFight();
								try {
									Thread.sleep(200);
								} catch(Exception E){};
								SocketManager.GAME_SEND_GV_PACKET(P);
							} else {
								SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(_map, F.getGUID());
							}
						}
					}
				} else if(_state == Constant.FIGHT_STATE_PLACE) {
					boolean esLuchInicioPelea = false;
					if(T != null) {
						if(_init0 != null &&_init0.getPersonnage() != null) {
							if(F.getPersonnage().getId() == _init0.getPersonnage().getId()) {
								esLuchInicioPelea = true;
							}
						}
						if(_init1 != null &&_init1.getPersonnage() != null) {
							if(F.getPersonnage().getId() == _init1.getPersonnage().getId()) {
								esLuchInicioPelea = true;
							}
						}
					}
					if(esLuchInicioPelea) {
						if((T.getTeam() == F.getTeam()) && (T.getGUID() != F.getGUID())) {
							SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().getId(), getTeamID(T.getGUID()));
							if(_type == Constant.FIGHT_TYPE_CHALLENGE) 
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, T.getPersonnage().getId(), getOtherTeamID(T.getGUID()));
							Player P = T.getPersonnage();
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);
							if(_team0.containsKey(T.getGUID())) {
								T._cell.removeFighter(T);
								_team0.remove(T.getGUID());
							} else if(_team1.containsKey(T.getGUID())) {
								T._cell.removeFighter(T);
								_team1.remove(T.getGUID());
							}
							for(Player z : _mapOld.getPersos())
								FightStateAddFlag(_mapOld, z);
							if(P.getOnline()) {
								try {
									Thread.sleep(200);
								} catch(Exception E){};
								SocketManager.GAME_SEND_GV_PACKET(P);
								P.refreshMapAfterFight();
							}
						}
					} else if (T == null) {
						boolean esLucInicPelea = false;
						if(_init0 != null &&_init0.getPersonnage() != null) {
							if(F.getPersonnage().getId() == _init0.getPersonnage().getId()) {
								esLucInicPelea = true;
							}
						}
						if(_init1 != null &&_init1.getPersonnage() != null) {
							if(F.getPersonnage().getId() == _init1.getPersonnage().getId()) {
								esLucInicPelea = true;
							}
						}
						if(esLucInicPelea) {
							for(Fighter f : getFighters(F.getTeam2())) {
								if (f.getPersonnage() == null)
									continue;
								Player P = f.getPersonnage();
								P.set_duelID(-1);
								P.set_ready(false);
								P.fullPDV();
								P.set_fight(null);
								P.setSitted(false);
								P.set_away(false);
								if(P.getOnline()) {	
									P.refreshMapAfterFight();
									try {
										Thread.sleep(200);
									} catch(Exception E){};
									SocketManager.GAME_SEND_GV_PACKET(P);
								}
							}
							if(_type == Constant.FIGHT_TYPE_CHALLENGE) {
								for(Fighter f : getFighters(F.getOtherTeam())) {
									if(f.getPersonnage() == null)
										continue;
									Player P = f.getPersonnage();
									P.set_duelID(-1);
									P.set_ready(false);
									P.fullPDV();
									P.set_fight(null);
									P.setSitted(false);
									P.set_away(false);
									if(P.getOnline()) {
										P.refreshMapAfterFight();
										try {
											Thread.sleep(200);
										} catch(Exception E){};
										SocketManager.GAME_SEND_GV_PACKET(P);
									}
								}
							}
							_state = 4;
							World.getCarte(_map.get_id()).removeFight(_id);
							SocketManager.GAME_SEND_MAP_FIGHT_COUNT_TO_MAP(World.getCarte(_map.get_id()));
							SocketManager.GAME_SEND_GAME_REMFLAG_PACKET_TO_MAP(this._mapOld,_init0.getGUID());
							if (getActTimerTask() != null)
								getActTimerTask().cancel();
							setActTimerTask(null);
							_tiempoInicioTurno = 0L;
							_map = null;
							_ordreJeu = null;
						} else { // si se retira asi mismo y no es lider
							System.out.println("ES: 16");
							SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().getId(), getTeamID(F.getGUID()));
							if(_type == Constant.FIGHT_TYPE_CHALLENGE) 
								SocketManager.GAME_SEND_ON_FIGHTER_KICK(this, F.getPersonnage().getId(), getOtherTeamID(F.getGUID()));
							Player P = F.getPersonnage();
							P.set_duelID(-1);
							P.set_ready(false);
							P.fullPDV();
							P.set_fight(null);
							P.setSitted(false);
							P.set_away(false);
							if(_team0.containsKey(F.getGUID())) {
								F._cell.removeFighter(F);
								_team0.remove(F.getGUID());
							} else if(_team1.containsKey(F.getGUID())) {
								F._cell.removeFighter(F);
								_team1.remove(F.getGUID());
							}
							for(Player z : _mapOld.getPersos())
								FightStateAddFlag(_mapOld, z);
							if(P.getOnline()) {
								try {
									Thread.sleep(200);
								} catch(Exception E){};
								SocketManager.GAME_SEND_GV_PACKET(P);
								P.refreshMapAfterFight();
							}
						}
					}
				}
				break;
			default:
				System.out.println("Type de combat non geree, type de combat:"+_type+" T:"+T+" F:"+F);
				break;
			}
		} else {
			SocketManager.GAME_SEND_GV_PACKET(perso);
			_spec.remove(perso.getId());
			perso.setSitted(false);
			perso.set_fight(null);
			perso.set_away(false);
		}
	}

	public String getGTL() {
		StringBuilder packet = new StringBuilder("GTL");
		for(Fighter f: get_ordreJeu()) {
			packet.append("|"+f.getGUID());
		}
		return packet.toString()+(char)0x00;
	}

	public int getNextLowerFighterGuid() {
		int g = -1;
		for(Fighter f : getFighters(3)) {
			if(f.getGUID() < g)
				g = f.getGUID();
		}
		g--;
		return g;
	}

	public void addFighterInTeam(Fighter f, int team) {
		if(team == 0)
			_team0.put(f.getGUID(), f);
		else if (team == 1)
			_team1.put(f.getGUID(), f);
	}

	public String parseFightInfos() {
		StringBuilder infos = new StringBuilder();
		infos.append(_id).append(";");
		Date actDate = new Date();
		long time = actDate.getTime() + 3600000L - (System.currentTimeMillis() - _tiempoInicio);
		infos.append((_tiempoInicio == 0 ? "-1" : time)).append(";");
		infos.append("0,");
		switch(_type) {
		case Constant.FIGHT_TYPE_CHALLENGE:
			infos.append("0,");
			infos.append(_team0.size()).append(";");
			//Team2
			infos.append("0,");
			infos.append("0,");
			infos.append(_team1.size()).append(";");
			break;
		}
		return infos.toString();
	}

	public void showCaseToTeam(int guid, int cellID) {
		int teams = getTeamID(guid)-1;
		if(teams == 4)
			return;
		ArrayList<Player> PWs = new ArrayList<Player>();
		if(teams == 0) {
			for(Entry<Integer,Fighter> e : _team0.entrySet()) {
				if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getClient() != null)
					PWs.add(e.getValue().getPersonnage());
			}
		} else if(teams == 1) {
			for(Entry<Integer,Fighter> e : _team1.entrySet()) {
				if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getClient() != null)
					PWs.add(e.getValue().getPersonnage());
			}
		}
		SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void showCaseToAll(int guid, int cellID) {
		ArrayList<Player> PWs = new ArrayList<Player>();
		for(Entry<Integer,Fighter> e : _team0.entrySet()) {
			if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getClient() != null)
				PWs.add(e.getValue().getPersonnage());
		}
		for(Entry<Integer,Fighter> e : _team1.entrySet()) {
			if(e.getValue().getPersonnage() != null && e.getValue().getPersonnage().getAccount().getClient() != null)
				PWs.add(e.getValue().getPersonnage());
		}
		for(Entry<Integer, Player> e : _spec.entrySet()) {
			PWs.add(e.getValue().getAccount().getPerso());
		}
		if (!PWs.isEmpty())
			SocketManager.GAME_SEND_FIGHT_SHOW_CASE(PWs, guid, cellID);
	}

	public void joinAsSpect(Player p) {
		if (p.get_fight() != null) 
			return;
		long time = System.currentTimeMillis() - _tiempoInicio;
		if (time - 1000 <= 0) {
			return;
		}
		if (_tiempoInicio == 0L || p == null) {
			return;
		}
		if(!specOk  || _state != Constant.FIGHT_STATE_ACTIVE) {
			SocketManager.GAME_SEND_Im_PACKET(p, "157");
			return;
		}
		int tiempoRestante = (int) ((44000) - (System.currentTimeMillis() - _tiempoInicioTurno));
		p.get_curCell().removePlayer(p.getId());
		SocketManager.GAME_SEND_GJK_PACKET(p, _state, 0, 0, 1, 0, _type);
		SocketManager.GAME_SEND_GS_PACKET(p);
		SocketManager.GAME_SEND_GTL_PACKET(p,this);
		SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(p.get_curCarte(), p.getId());
		SocketManager.GAME_SEND_MAP_FIGHT_GMS_PACKETS(this,_map,p);
		SocketManager.GAME_SEND_GAMETURNSTART_PACKET(p,_ordreJeu.get(_curPlayer).getGUID(), tiempoRestante);
		p.set_fight(this);
		_spec.put(p.getId(), p);
		SocketManager.GAME_SEND_Im_PACKET_TO_FIGHT(this, 7, "036;"+p.getName());
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT0(p);
	}

	public boolean verifyStillInFight() {
		for(Fighter f : _team0.values()) {
			if(f.isDead() || f.getPersonnage() == null || f.hasLeft()) {
				continue;
			}
			if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null && f.getPersonnage().get_fight().get_id() == this.get_id()) {
				return true;
			}
		}
		for(Fighter f : _team1.values()) {
			if(f.isDead() || f.getPersonnage() == null || f.hasLeft()) {
				continue;
			}
			if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null && f.getPersonnage().get_fight().get_id() == this.get_id()) {
				return true;
			}
		}
		return false;
	}

	public boolean verifyStillInFightTeam(int guid) {
		if(_team0.containsKey(guid)) {
			for(Fighter f : _team0.values()) {
				if(f.isDead() || f.getPersonnage() == null || f.hasLeft()) {
					System.out.println("null");
					continue;
				}
				if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null && f.getPersonnage().get_fight().get_id() == this.get_id()) {
					return true;
				}
			}
		} else if(_team1.containsKey(guid)) {
			for(Fighter f : _team1.values()) {
				if(f.isDead() || f.getPersonnage() == null || f.hasLeft()) {
					System.out.println("null2");
					continue;
				}
				if(f.getPersonnage() != null && f.getPersonnage().get_fight() != null && f.getPersonnage().get_fight().get_id() == this.get_id()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void FightStateAddFlag(Map _map, Player P) {
		for(Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
			if(fight.getValue()._state == Constant.FIGHT_STATE_PLACE) {
				if(fight.getValue()._type == Constant.FIGHT_TYPE_CHALLENGE) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),0,fight.getValue()._init0.getGUID(),fight.getValue()._init1.getGUID(),fight.getValue()._init0.getPersonnage().get_curCell().getID(),"0;-1", fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;-1");
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
						if(Config.DEBUG) System.out.println(F.getValue().getPersonnage().getName());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
						if(Config.DEBUG) System.out.println(F.getValue().getPersonnage().getName());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().get_curCarte(),fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				} else if(fight.getValue()._type == Constant.FIGHT_TYPE_AGRESSION) {
					SocketManager.GAME_SEND_GAME_ADDFLAG_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),0,fight.getValue()._init0.getGUID(),fight.getValue()._init1.getGUID(),fight.getValue()._init0.getPersonnage().get_curCell().getID(),"0;0", fight.getValue()._init1.getPersonnage().get_curCell().getID(), "0;0");
					for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
						if(Config.DEBUG) System.out.println(F.getValue().getPersonnage().getName());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init0.getPersonnage().get_curCarte(),fight.getValue()._init0.getGUID(), fight.getValue()._init0);
					}
					for(Entry<Integer, Fighter> F : fight.getValue()._team1.entrySet()) {
						if(Config.DEBUG) System.out.println(F.getValue().getPersonnage().getName());
						SocketManager.GAME_SEND_ADD_IN_TEAM_PACKET_TO_PLAYER(P, fight.getValue()._init1.getPersonnage().get_curCarte(),fight.getValue()._init1.getGUID(), fight.getValue()._init1);
					}
				}
			}
		}
	}

	public static int getFightIDByFighter(Map _map, int guid) {
		for(Entry<Integer, Fight> fight : _map.get_fights().entrySet()) {
			for(Entry<Integer, Fighter> F : fight.getValue()._team0.entrySet()) {
				if(F.getValue().getPersonnage() != null && F.getValue().getGUID() == guid) {
					return fight.getValue().get_id();
				}
			}
		}
		return 0;
	}

	public java.util.Map<Integer,Fighter> getDeadList() {
		return deadList;
	}	

	public void delOneDead(Fighter target) {
		deadList.remove(target.getGUID());
	}

	public int getFightType() {
		return _type;
	}

	public static class Fighter {
		private int _id = 0;
		private boolean _canPlay = false;
		private Fight _fight;
		private int _type = 0; // 1 : Personnage, 2 : Mob, 5 : Perco
		private Player _perso = null;
		private int _team = -2;
		private Case _cell;
		private int _orientation; 
		public int _nbInvoc = 0;
		private int _PDVMAX;
		private int _PDV;
		private boolean _isDead;
		private boolean _hasLeft;
		private int _gfxID;
		private Fighter _oldCible = null;
		private ArrayList<SpellEffect> _fightBuffs = new ArrayList<SpellEffect>();
		private ArrayList<LaunchedSort> _launchedSort = new ArrayList<LaunchedSort>();
		private java.util.Map<Integer,Integer> _state = new TreeMap<Integer,Integer>();

		public void setState(int id, int t) {
			_state.remove(id);
			if(t != 0)
				_state.put(id, t);
		}

		public boolean isState(int id) {
			if(_state.get(id) == null)
				return false;
			return _state.get(id) != 0;
		}

		public void decrementStates() {
			ArrayList<Entry<Integer,Integer>> entries = new ArrayList<Entry<Integer, Integer>>();
			entries.addAll(_state.entrySet());
			for(Entry<Integer,Integer> e : entries) {
				if(e.getKey() < 0)
					continue;
				_state.remove(e.getKey());
				int nVal = e.getValue()-1;
				if(nVal == 0) {
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, getGUID()+"", getGUID()+","+e.getKey()+",0");
					continue;
				}
				_state.put(e.getKey(), nVal);
			}
		}

		public boolean canLaunchSpell(int spellID) {
			if(!getPersonnage().hasSpell(spellID))
				return false;
			else 
				return LaunchedSort.coolDownGood(this,spellID);
		}

		public Fighter get_oldCible() {
			return _oldCible;
		}

		public void set_oldCible(Fighter cible) {
			_oldCible = cible;
		}

		public Fighter(Fight f, Player perso) {
			_fight = f;
			_type = 1;
			_perso = perso;
			_id = perso.getId();
			_PDVMAX = perso.get_PDVMAX();
			_PDV = perso.get_PDV();
			_gfxID = getDefaultGfx();
		}

		public int getGUID() {
			return _id;
		}

		public int get_gfxID() {
			return _gfxID;
		}

		public void set_gfxID(int gfxID) {
			_gfxID = gfxID;
		}

		public void set_fightCell(Case cell) {
			_cell = cell;
		}

		public Case get_fightCell() {		
			return _cell;
		}

		public void setTeam(int i) {
			_team = i;
		}

		public boolean isDead() {
			return _isDead;
		}

		public int getPA() {
			if(_type == 1)
				return getTotalStats().getEffect(Constant.STATS_ADD_PA);
			return 6;
		}

		public Stats getTotalStatsLessBuff() {
			Stats stats = new Stats(new TreeMap<Integer,Integer>());
			if(_type == 1)
				stats = _perso.getTotalStats();
			return stats;
		}

		public int getPM() {
			if(_type == 1)
				return getTotalStats().getEffect(Constant.STATS_ADD_PM);
			return 8;
		}

		public void setDead(boolean isDead) {
			_isDead = isDead;
		}

		public boolean hasLeft() {
			return _hasLeft;
		}

		public void setLeft(boolean hasLeft) {
			_hasLeft = hasLeft;
		}

		public Player getPersonnage() {
			if(_type == 1)
				return _perso;
			return null;
		}

		public boolean testIfCC(int tauxCC) {
			if(tauxCC < 2)
				return false;
			int agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
			if(agi <0)
				agi =0;
			tauxCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC);
			tauxCC = (int)((tauxCC * 2.9901) / Math.log(agi +12));
			if(tauxCC<2)
				tauxCC = 2;
			int jet = Formules.getRandomValue(1, tauxCC);
			return (jet == tauxCC);
		}

		public boolean testIfCC(int porcCC, SortStats sSort) {
			if (porcCC < 2)
				return false;
			int agi = getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
			if (agi < 0)
				agi = 0;
			porcCC -= getTotalStats().getEffect(Constant.STATS_ADD_CC);
			porcCC = (int) ( (porcCC * 2.9901) / Math.log(agi + 12));
			if (porcCC < 2)
				porcCC = 2;
			int jet = Formules.getRandomValue(1, porcCC);
			return (jet == porcCC);
		}

		public Stats getTotalStats() {
			Stats stats = new Stats(new TreeMap<Integer,Integer>());
			if(_type == 1)
				stats = _perso.getTotalStats();
			stats = Stats.cumulStatFight(stats,getFightBuffStats());
			return stats;
		}

		public String getGmPacket(char c) {
			StringBuilder str = new StringBuilder();
			str.append("GM|").append(c);
			str.append(_cell.getID()).append(";");
			_orientation = 1;
			str.append(_orientation).append(";");
			str.append("0;");
			str.append(getGUID()).append(";");
			str.append(getPacketsName()).append(";");
			switch(_type) {
			case 1://Perso
				str.append(_perso.getGfxID()).append(";");
				str.append(_perso.getGfxID()).append("^").append(_perso.getSize()).append(";");
				str.append(_perso.getSexe()).append(";");
				str.append(_perso.getLevel()).append(";");
				str.append(0).append(",");
				str.append("0").append(",");
				str.append("0").append(",");
				str.append(_perso.getLevel()+_perso.getId());
				str.append(";");
				str.append((_perso.getColor1()==-1?"-1":Integer.toHexString(_perso.getColor1()))).append(";");
				str.append((_perso.getColor2()==-1?"-1":Integer.toHexString(_perso.getColor2()))).append(";");
				str.append((_perso.getColor3()==-1?"-1":Integer.toHexString(_perso.getColor3()))).append(";");
				str.append((_perso.getColor4()==-1?"-1":Integer.toHexString(_perso.getColor4()))).append(";");
				str.append((_perso.getColor5()==-1?"-1":Integer.toHexString(_perso.getColor5()))).append(";");
				str.append(_perso.getGMStuffString()).append(";");
				str.append(getPDV()).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PA)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_PM)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_TER)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU)).append(";");	
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_AFLEE)).append(";");
				str.append(getTotalStats().getEffect(Constant.STATS_ADD_MFLEE)).append(";");
				str.append(_team).append(";");
				str.append(";");
				break;
			}
			return str.toString();
		}

		public int getType() {
			return _type;
		}

		public int getPDV() {
			int pdv = _PDV + getBuffValue(Constant.STATS_ADD_VITA);
			return pdv;
		}

		public void removePDV(int pdv) {
			_PDV -= pdv;
		}

		public int getPDVMAX() {
			return _PDVMAX + getBuffValue(Constant.STATS_ADD_VITA);
		}

		public int get_lvl() {
			if(_type == 1)
				return _perso.getLevel();
			return 0;
		}

		public int getInitiative() {
			if(_type == 1)
				return _perso.getInitiative();
			return 0;
		}

		public ArrayList<SpellEffect> getBuffsByEffectID(int effectID) {
			ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
			for(SpellEffect buff : _fightBuffs) {
				if(buff.getEffectID() == effectID)
					buffs.add(buff);
			}
			return buffs;
		}

		public void debuff() {
			ArrayList<SpellEffect> newBuffs = new ArrayList<SpellEffect>();
			for(SpellEffect SE : _fightBuffs) {
				if(!SE.isDebuffabe())
					newBuffs.add(SE);
				switch(SE.getEffectID()) {
				case Constant.STATS_ADD_PA:
				case Constant.STATS_ADD_PA2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 101,getGUID()+"",getGUID()+",-"+SE.getValue());
					break;

				case Constant.STATS_ADD_PM:
				case Constant.STATS_ADD_PM2:
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 127,getGUID()+"",getGUID()+",-"+SE.getValue());
					break;
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(newBuffs);
			if(_perso != null && !_hasLeft)
				SocketManager.GAME_SEND_STATS(_perso);
		}


		public String xpString(String str) {
			if(_perso != null) {
				int max = _perso.getLevel()+1;
				if(max> World.getExpLevelSize())
					max = World.getExpLevelSize();
				return World.getExpLevel(_perso.getLevel()).perso+str+_perso.getExp()+str+ World.getExpLevel(max).perso;
			}
			return "0"+str+"0"+str+"0";
		}

		public String getPacketsName() {
			if(_type == 1)
				return _perso.getName();

			return "";
		}

		public int getTeam() {
			return _team;
		}

		public int getTeam2() {
			return _fight.getTeamID(_id);
		}

		public int getOtherTeam() {
			return _fight.getOtherTeamID(_id);
		}

		public boolean canPlay() {
			return _canPlay;
		}

		public void setCanPlay(boolean b) {
			_canPlay = b;
		}

		public int getCurPA(Fight fight) {
			return fight._curFighterPA;
		}

		public int getCurPM(Fight fight) {
			return fight._curFighterPM;
		}

		public void setCurPM(Fight fight, int pm) {
			fight._curFighterPM += pm;
		}

		public void setCurPA(Fight fight, int pa) {
			fight._curFighterPA += pa;
		}

		public void fullPDV() {
			_PDV = _PDVMAX;
		}

		public void setIsDead(boolean b) {
			_isDead = b;
		}

		public int getPdvMaxOutFight() {
			if(_perso != null)
				return _perso.get_PDVMAX();
			return 0;
		}

		public int getDefaultGfx() {
			if(_perso != null)
				return _perso.getGfxID();
			return 0;
		}

		public void addPDV(int max)  {
			_PDVMAX = (_PDVMAX+max);
			_PDV = (_PDV+max);
		}

		public void removePDVMAX(int numb) {
			_PDVMAX = _PDVMAX-numb;
			if(_PDV > _PDVMAX) _PDV = _PDVMAX;
		}

		public ArrayList<LaunchedSort> getLaunchedSorts() {
			return _launchedSort;
		}

		public void ActualiseLaunchedSort() {
			ArrayList<LaunchedSort> copie = new ArrayList<LaunchedSort>();
			copie.addAll(_launchedSort);
			int i = 0;
			for(LaunchedSort S : copie) {
				S.ActuCooldown();
				if(S.getCooldown() <= 0) {
					_launchedSort.remove(i);
					i--;
				}
				i++;
			}
		}

		public void addLaunchedSort(Fighter target, SortStats sort, Fighter lanzador) {
			LaunchedSort launched = new LaunchedSort(target, sort, lanzador);
			_launchedSort.add(launched);
		}

		public ArrayList<SpellEffect> get_fightBuff() {
			return _fightBuffs;
		}

		public void initBuffStats() {
			if(_type == 1) {
				for(java.util.Map.Entry<Integer, SpellEffect> entry : _perso.get_buff().entrySet()) {
					_fightBuffs.add(entry.getValue());
				}
			}
		}

		private Stats getFightBuffStats() {
			Stats stats = new Stats();
			for(SpellEffect entry : _fightBuffs) {
				stats.addOneStat(entry.getEffectID(), entry.getValue());
			}
			return stats;
		}

		public void applyBeginningTurnBuff(Fight fight) {
			synchronized(_fightBuffs) {
				for(int effectID : Constant.BEGIN_TURN_BUFF) {
					ArrayList<SpellEffect> buffs = new ArrayList<SpellEffect>();
					buffs.addAll(_fightBuffs);
					for(SpellEffect entry : buffs) {
						if(entry.getEffectID() == effectID) {
							entry.applyBeginningBuff(fight, this);
						}
					}
				}
			}
		}

		public SpellEffect getBuff(int id) {
			for(SpellEffect entry : _fightBuffs) {
				if(entry.getEffectID() == id && entry.getDuration() >0) {
					return entry;
				}
			}
			return null;
		}

		public boolean hasBuff(int id) {
			for(SpellEffect entry : _fightBuffs) {
				if(entry.getEffectID() == id && entry.getDuration() > 0) {
					return true;
				}
			}
			return false;
		}

		public int getBuffValue(int id) {
			int value = 0;
			for(SpellEffect entry : _fightBuffs) {
				if(entry.getEffectID() == id)
					value += entry.getValue();
			}
			return value;
		}

		public int getMaitriseDmg(int id) {
			int value = 0;
			for(SpellEffect entry : _fightBuffs) {
				if(entry.getSpell() == id)
					value += entry.getValue();
			}
			return value;
		}

		public boolean getSpellValueBool(int id) {
			for(SpellEffect entry : _fightBuffs) {
				if(entry.getSpell() == id)
					return true;
			}
			return false;
		}

		public void refreshfightBuff() {
			ArrayList<SpellEffect> b = new ArrayList<SpellEffect>();
			for(SpellEffect entry : _fightBuffs) {
				if(entry.decrementDuration() != 0) {
					b.add(entry);
				} else {
					switch(entry.getEffectID()) {
					case 150://Invisibilité
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 150, entry.getCaster().getGUID()+"",getGUID()+",0");
						break;
					case 950:
						String args = entry.getArgs();
						int id = -1;
						try {
							id = Integer.parseInt(args.split(";")[2]);
						} catch(Exception e){}
						if(id == -1)
							return;
						setState(id,0);
						SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(_fight, 7, 950, entry.getCaster().getGUID()+"", entry.getCaster().getGUID()+","+id+",0");
						break;
					}
				}
			}
			_fightBuffs.clear();
			_fightBuffs.addAll(b);
		}

		public void addBuff(int id,int val,int duration,int turns,boolean debuff,int spellID,String args,Fighter caster) {
			_fightBuffs.add(new SpellEffect(id,val,(_canPlay?duration+1:duration),turns,debuff,caster,args,spellID));	
			switch(id) {
			case 6://Renvoie de sort
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), -1, val+"", "10", "", duration, spellID);
				break;
			case 79://Chance éca
				val = Integer.parseInt(args.split(";")[0]);
				String valMax = args.split(";")[1];
				String chance = args.split(";")[2];
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax, chance, "", duration, spellID);
				break;
			case 788://Fait apparaitre message le temps de buff sacri Chatiment de X sur Y tours
				val = Integer.parseInt(args.split(";")[1]);
				String valMax2 = args.split(";")[2];
				if(Integer.parseInt(args.split(";")[0]) == 108)
					return;
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, ""+val, ""+valMax2, "", duration, spellID);
				break;
			case 98://Poison insidieux
			case 107://Mot d'épine (2à3), Contre(3)
			case 100://Flèche Empoisonnée, Tout ou rien
			case 108://Mot de Régénération, Tout ou rien
			case 165://Maîtrises
			case 781://MAX
			case 782://MIN
				val = Integer.parseInt(args.split(";")[0]);
				String valMax1 = args.split(";")[1];
				if(valMax1.compareTo("-1") == 0 || spellID == 82 || spellID == 94) {
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration, spellID);
				} else if(valMax1.compareTo("-1") != 0) {
					SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, valMax1, "", "", duration, spellID);
				}
				break;
			default:
				SocketManager.GAME_SEND_FIGHT_GIE_TO_FIGHT(_fight, 7, id, getGUID(), val, "", "", "", duration, spellID);
				break;
			}
			if(_perso!=null) {
				SocketManager.GAME_SEND_STATS(_perso);
			}
		}
	}

	public static class LaunchedSort {
		private int _spellId = 0;
		private int _cooldown = 0;
		private Fighter _target = null;

		public LaunchedSort(Fighter t, SortStats SS, Fighter caster) {
			_target = t;
			_spellId = SS.getSpellID();
			_cooldown = SS.getCoolDown();
		}

		public void ActuCooldown() {
			_cooldown--;
		}

		public int getCooldown() {
			return _cooldown;
		}

		public int getId() {
			return _spellId;
		}

		public Fighter getTarget() {
			return _target;
		}

		public static boolean coolDownGood(Fighter fighter,int id) {
			for(LaunchedSort S : fighter.getLaunchedSorts()) {
				if(S._spellId == id && S.getCooldown() > 0)
					return false;
			}
			return true;
		}

		public static int getNbLaunch(Fighter fighter,int id) {
			int nb = 0;
			for(LaunchedSort S : fighter.getLaunchedSorts()) {
				if(S._spellId == id)
					nb++;
			}
			return nb;
		}

		public static int getNbLaunchTarget(Fighter fighter,Fighter target,int id) {
			int nb = 0;
			for(LaunchedSort S : fighter.getLaunchedSorts()) {
				if(S._target == null || target == null)
					continue;
				if(S._spellId == id && S._target.getGUID() == target.getGUID())
					nb++;
			}
			return nb;
		}
	}
}