package realm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import kernel.Config;

import org.apache.mina.core.session.IoSession;

import variables.Fight;
import variables.Fight.Fighter;
import variables.Map.Case;
import variables.NPC_tmpl.NPC;
import variables.NPC_tmpl.NPC_question;
import variables.NPC_tmpl.NPC_reponse;
import variables.Item;
import variables.Spell.SortStats;
import client.Path;
import client.Account;
import client.Player;
import data.Constant;
import data.Encrypt;
import data.DatabaseManager;
import data.SocketManager;
import data.World;

public class Client {
	public static Map<Long, Client> _client = new HashMap<>();
	IoSession _ioSession;
	Account _account;
	Player _perso;
	public SocketManager _send;
	private long _id;
	private String _key;
	private int _condition = -1;
	private String ndc;
	private String _hashPass;
	private boolean onligne;
	private Map<Integer, GameAction> _gameActions = new TreeMap<Integer, GameAction>();
	private ArrayList<GameAction> _actionsQueue = new ArrayList<GameAction>();
	private boolean go = false;

	public Client(IoSession ioSession, String key) {
		_id = ioSession.getId();
		_ioSession = ioSession;
		_key = key;
		_send = new SocketManager(_ioSession);
		_client.put(_id, this);
	}

	public void login(String packet) {
		if (_condition < 3)
			_condition++;
		if (_send == null || _ioSession == null)
			return;
		switch (_condition){
		case 0:
			if (!packet.equals(Constant.version)){ 
				_ioSession.close(true);
			}
			RealmThread.remover(_ioSession);
			break;
		case 1:
			ndc = packet;
			Account user = World.getAccountByUser(ndc);
			boolean valid = false;
			if (user == null) {
				DatabaseManager.ChargeAccountName(ndc);
				user = World.getAccountByUser(ndc);
				if (user == null) {
					_send.REALM_SEND_LOGIN_ERROR();
					_ioSession.close(true);
				} else {
					valid = true;
				}
			} else {
				valid = true;
			}
			if (valid) {
				_send.setAccount(user);
				if (user.isLogged()) {
					_send.REALM_SEND_ALREADY_CONNECTED();
					_ioSession.close(true);
					Account co = World.getAccountByUser(ndc);
					assert co != null;
					co.getClient().getIoSession().close(true);
				}
				_account = user;
			}
			break;
		case 2:
			if(!packet.substring(0, 2).equalsIgnoreCase("#1")){
				_ioSession.close(true);
				return;
			}
			_hashPass = packet;	
			_account.setLogged(_hashPass.equals(Encrypt.CryptPassword(_key, _account.getPassword())));
			if (!_account.isLogged()) {
				_send.REALM_SEND_LOGIN_ERROR();
				_ioSession.close(true);
			} else {
				_account.setClient(this);
				_account.setLogged(true);
				_send.REALM_SEND_Ad_Ac_AH_AlK_AQ_PACKETS();
			}
			break;
		default:
			if (_account != null)
				parserPacket(packet);
			break;
		}
	}

	public void parserPacket(String packet) {//TODO:
		if (packet.length() > 3 && packet.substring(0,4).equalsIgnoreCase("ping")) {
			SocketManager.GAME_SEND_PONG(_perso);
			return;
		}
		switch(packet.charAt(0)){
		case 'A':
			parseAccountPacket(packet);
			break;
		case 'B':
			switch(packet.charAt(1)) {
			case 'A'://Console
				break;
			case 'D':
				Basic_send_Date_Hour();
				break;
			case 'M':
				Basic_chatMessage(packet);
				break;
			case 'W':
				break;
			case 'S':
				_perso.emoticone(packet.substring(2));
				break;
			case 'Y':
				break;
			}
			break;
		case 'c':
			parseChanelPacket(packet);
			break;
		case 'D':
			parseDialogPacket(packet);
			break;
		case 'e':
			parse_environementPacket(packet);
			break;
		case 'f':
			parseFightPacket(packet);
			break;
		case 'G':
			parseGame(packet);
			break;
		case 'w':
			titleList(packet);
			break;
		case 'O':
			ItemInventory(packet);
			break;
		case 'S':
			parseSpellPacket(packet);
			break;
		}
	}

	private void parseFightPacket(String packet) {
		try {
			switch(packet.charAt(1)) {
			case 'D'://Détails d'un combat (liste des combats)
				int key = -1;
				try {
					key = Integer.parseInt(packet.substring(2).replace(((int)0x0)+"", ""));
				} catch(Exception e) {};
				if(key == -1)
					return;
				SocketManager.GAME_SEND_FIGHT_DETAILS(_perso,_perso.get_curCarte().get_fights().get(key));
				break;
			case 'N'://Bloquer le combat
				if(_perso.get_fight() == null)
					return;
				_perso.get_fight().toggleLockTeam(_perso.getId());
				break;
			case 'H'://Aide
				if(_perso.get_fight() == null)
					return;
				_perso.get_fight().toggleHelp(_perso.getId());
				break;
			case 'L'://Lister les combats
				SocketManager.GAME_SEND_FIGHT_LIST_PACKET(_perso, _perso.get_curCarte());
				break;
			}
		} catch(Exception e){
			e.printStackTrace();
		};
	}

	private void Basic_send_Date_Hour() {
		SocketManager.GAME_SEND_SERVER_DATE(_perso);
		SocketManager.GAME_SEND_SERVER_HOUR(_perso);
	}

	private void parseSpellPacket(String packet) {
		switch(packet.charAt(1)) {
		case 'B':
			boostSort(packet);
			break;
		case 'F'://Oublie de sort
			forgetSpell(packet);
			break;
		case'M':
			addToSpellBook(packet);
			break;
		}
	}

	private void boostSort(String packet) {
		try {
			int id = Integer.parseInt(packet.substring(2));
			if(_perso.boostSpell(id)) {
				SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCEED(_perso, id, _perso.getSortStatBySortIfHas(id).getLevel());
				SocketManager.GAME_SEND_STATS(_perso);
			} else {
				SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_perso);
				return;
			}
		} catch(NumberFormatException e){
			SocketManager.GAME_SEND_SPELL_UPGRADE_FAILED(_perso);
			return;
		};
	}

	private void forgetSpell(String packet) {
		if(!_perso.isForgettingSpell())
			return;
		int id = Integer.parseInt(packet.substring(2));
		if(_perso.forgetSpell(id)) {
			SocketManager.GAME_SEND_SPELL_UPGRADE_SUCCEED(_perso, id, _perso.getSortStatBySortIfHas(id).getLevel());
			SocketManager.GAME_SEND_STATS(_perso);
			_perso.setIsForgettingSpell(false);
		}
	}

	private void addToSpellBook(String packet) {
		try {
			int SpellID = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			int Position = Integer.parseInt(packet.substring(2).split("\\|")[1]);
			SortStats Spell = _perso.getSortStatBySortIfHas(SpellID);
			if(Spell != null) {
				_perso.set_SpellPlace(SpellID, Encrypt.getHashedValueByInt(Position));
			}
			SocketManager.GAME_SEND_BN(_perso);
		}catch(Exception e){};
	}

	private void parseDialogPacket(String packet) {
		switch(packet.charAt(1)) {
		case 'C':
			Dialog_start(packet);
			break;
		case 'R':
			Dialog_response(packet);
			break;
		case 'V':
			Dialog_end();
			break;
		}
	}

	private void Dialog_response(String packet) {
		String[] infos = packet.substring(2).split("\\|");
		try {
			int qID = Integer.parseInt(infos[0]);
			int rID = Integer.parseInt(infos[1]);
			NPC_question quest = World.getNPCQuestion(qID);
			NPC_reponse rep = World.getNPCreponse(rID);
			if(quest == null || rep == null || !rep.isAnotherDialog()) {
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso);
				_perso.set_isTalkingWith(0);
			}
			rep.apply(_perso);
		} catch(Exception e) {
			SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso);
		}
	}

	private void Dialog_end() {
		SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso);
		if(_perso.get_isTalkingWith() != 0)
			_perso.set_isTalkingWith(0);
	}

	private void Dialog_start(String packet) {
		try {
			int npcID = Integer.parseInt(packet.substring(2).split((char)0x0A+"")[0]);
			NPC npc = _perso.get_curCarte().getNPC(npcID);
			if( npc == null)
				return;
			SocketManager.GAME_SEND_DCK_PACKET(_perso, npcID);
			int qID = npc.get_template().get_initQuestionID();
			NPC_question quest = World.getNPCQuestion(qID);
			if(quest == null) {
				SocketManager.GAME_SEND_END_DIALOG_PACKET(_perso);
				return;
			}
			SocketManager.GAME_SEND_QUESTION_PACKET(_perso, quest.parseToDQPacket(_perso));
			_perso.set_isTalkingWith(npcID);
		} catch(NumberFormatException e) {};
	}


	private void parse_environementPacket(String packet) {
		switch(packet.charAt(1)) {
		case 'U'://Emotes
			actionEmotes(packet);
			break;
		}
	}

	private void actionEmotes(String packet) {
		int emote = -1;
		try {
			emote = Integer.parseInt(packet.substring(2));
		} catch(Exception e) {};
		if(emote == -1)
			return;
		if(_perso == null)
			return;
		switch(emote) {
		case 19:
		case 1:
			_perso.setSitted(!_perso.isSitted());
			break;
		}
		if(_perso.emoteActive() == emote)
			_perso.setEmoteActive(0);
		else 
			_perso.setEmoteActive(emote);
		SocketManager.GAME_SEND_eUK_PACKET_TO_MAP(_perso.get_curCarte(), _perso.getId(), _perso.emoteActive());
	}

	private void ItemInventory(String packet)  {
		switch (packet.charAt(1)) {
		case 'M':
			moveItem(packet);
			break;
		case 'D':
			Object_drop(packet);
			break;
		case 'd':
			deleteItem(packet);
			break;
		}
	}

	private void Object_drop(String packet) {
		int guid = -1;
		int qua = -1;
		try {
			guid = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			qua = Integer.parseInt(packet.split("\\|")[1]);
		} catch(Exception e){};
		if(guid == -1 || qua <= 0 || !_perso.hasItemGuid(guid) || _perso.get_fight() != null || _perso.is_away())return;
		Item obj = World.getObjet(guid);
		_perso.set_curCell(_perso.get_curCell());
		int cellPosition = Constant.getNearCellidUnused(_perso);
		if(cellPosition < 0) {
			SocketManager.GAME_SEND_Im_PACKET(_perso, "1145");
			return;
		}
		if(obj.getPosition() != Constant.ITEM_POS_NO_EQUIPED) {
			obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
			if (obj.getPosition() == Constant.ITEM_POS_BOTTES || obj.getPosition() == Constant.ITEM_POS_COIFFE || obj.getPosition() == Constant.ITEM_POS_CAPE || obj.getPosition() == Constant.ITEM_POS_CEINTURE || obj.getPosition() == Constant.ITEM_POS_NO_EQUIPED)
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
		}
		if(qua >= obj.getQuantity()) {
			DatabaseManager.DELETE_ITEM(guid);
			_perso.removeItem(guid);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj);
			obj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
		} else {
			obj.setQuantity(obj.getQuantity() - qua);
			Item obj2 = Item.getCloneObjet(obj, qua);
			obj2.setPosition(Constant.ITEM_POS_NO_EQUIPED);
			_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).addDroppedItem(obj2);
			SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
		}
		SocketManager.GAME_SEND_Ow_PACKET(_perso);
		SocketManager.GAME_SEND_GDO_PACKET_TO_MAP(_perso.get_curCarte(),'+',_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellPosition).getID(),obj.getTemplate().getID(),0);
		SocketManager.GAME_SEND_STATS(_perso);
	}

	private void deleteItem(String packet) {
		String[] infos = packet.substring(2).split("\\|");
		try {
			int guid = Integer.parseInt(infos[0]);
			int qua;
			try {
				qua = Integer.parseInt(infos[1]);
			} catch(Exception e){
				qua = 1;
			}
			Item obj = World.getObjet(guid);
			if(obj == null || !_perso.hasItemGuid(guid) || qua <= 0) {
				SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_perso);
				return;
			}
			int newQua = obj.getQuantity()-qua;
			if(newQua <= 0) {
				_perso.removeItem(guid);
				World.removeItem(guid);
				DatabaseManager.DELETE_ITEM(guid);
				SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, guid);
			} else {
				obj.setQuantity(newQua);
				SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
				DatabaseManager.SAVE_ITEM(obj);
			}
			SocketManager.GAME_SEND_STATS(_perso);
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
		} catch(Exception e) {
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_perso);
		}
	}

	private synchronized void moveItem(String packet) {
		if(_perso.get_fight() != null)
			if(_perso.get_fight().get_state() > Constant.FIGHT_STATE_ACTIVE)
				return;
		String[] infos = packet.substring(2).split(""+(char)0x0A)[0].split("\\|");
		try {
			int qua;
			int guid = Integer.parseInt(infos[0]);
			int pos = Integer.parseInt(infos[1]);
			try {
				qua = Integer.parseInt(infos[2]);
			} catch(Exception e) {
				qua = 1;
			}
			Item obj = World.getObjet(guid);
			if(!_perso.hasItemGuid(guid) || obj == null)
				return;
			if(obj.getTemplate().getLevel() > _perso.getLevel())  {
				SocketManager.GAME_SEND_OAEL_PACKET(_perso);
				return;
			}
			if(pos != Constant.ITEM_POS_NO_EQUIPED && (obj.getTemplate().getType() == Constant.ITEM_TYPE_DOFUS) && _perso.hasEquiped(obj.getTemplate().getID()))
				return;
			Item exObj = _perso.getObjetByPos(pos);
			if(exObj != null) {
				Item obj2;
				if((obj2 = _perso.getSimilarItem(exObj)) != null) {
					obj2.setQuantity(obj2.getQuantity()+exObj.getQuantity());
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					World.removeItem(exObj.getGuid());
					_perso.removeItem(exObj.getGuid());
					SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, exObj.getGuid());
				} else {
					exObj.setPosition(Constant.ITEM_POS_NO_EQUIPED);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,exObj);
				}
				if(_perso.getObjetByPos(Constant.ITEM_POS_ARME) == null)
					SocketManager.GAME_SEND_OT_PACKET(_perso, -1);
			} else {
				Item obj2;
				if((obj2 = _perso.getSimilarItem(obj)) != null) {
					if(qua > obj.getQuantity()) 
						qua = obj.getQuantity();
					obj2.setQuantity(obj2.getQuantity()+qua);
					SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj2);
					DatabaseManager.SAVE_ITEM(obj2);
					if(obj.getQuantity() - qua > 0) {
						obj.setQuantity(obj.getQuantity()-qua);
						SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
						DatabaseManager.SAVE_ITEM(obj);
					} else {
						World.removeItem(obj.getGuid());
						_perso.removeItem(obj.getGuid());
						SocketManager.GAME_SEND_REMOVE_ITEM_PACKET(_perso, obj.getGuid());
					}
				} else { //Divide el item al equiparlo
					obj.setPosition(pos);
					SocketManager.GAME_SEND_OBJET_MOVE_PACKET(_perso,obj);
					if(obj.getQuantity() > 1) {
						if(qua > obj.getQuantity()) 
							qua = obj.getQuantity();
						if(obj.getQuantity() - qua > 0) {
							int newItemQua = obj.getQuantity()-qua;
							Item newItem = Item.getCloneObjet(obj,newItemQua);
							_perso.addObjet(newItem,false);
							World.addObjet(newItem,true);
							obj.setQuantity(qua);
							SocketManager.GAME_SEND_OBJECT_QUANTITY_PACKET(_perso, obj);
							DatabaseManager.SAVE_ITEM(newItem);
						}
					}
					DatabaseManager.SAVE_ITEM(obj);
				}
			}
			SocketManager.GAME_SEND_Ow_PACKET(_perso);
			SocketManager.GAME_SEND_STATS(_perso);
			if(pos == Constant.ITEM_POS_ARME || pos == Constant.ITEM_POS_CEINTURE || pos == Constant.ITEM_POS_BOTTES || pos == Constant.ITEM_POS_COIFFE || pos == Constant.ITEM_POS_FAMILIER || pos == Constant.ITEM_POS_CAPE || pos == Constant.ITEM_POS_BOUCLIER || pos == Constant.ITEM_POS_NO_EQUIPED)
				SocketManager.GAME_SEND_ON_EQUIP_ITEM(_perso.get_curCarte(), _perso);
			if(_perso.get_fight() != null) {
				SocketManager.GAME_SEND_ON_EQUIP_ITEM_FIGHT(_perso, _perso.get_fight().getFighterByPerso(_perso), _perso.get_fight());
			}
		} catch(Exception e) {
			e.printStackTrace();
			SocketManager.GAME_SEND_DELETE_OBJECT_FAILED_PACKET(_perso);
		}
	}

	public void titleList(String packet) {
		switch (packet.charAt(1)) {
		case 'T':
			switch (packet.charAt(2)) {
			case 'L': //Enviar lista títulos
				if (_perso.getTitles() == "") {
					SocketManager.GAME_SEND_TITLES2(_perso,_perso.getTitles(), _perso.getTitle());
				} else
					SocketManager.GAME_SEND_TITLES(_perso,_perso.getTitles(), _perso.getTitle());
				break;
			case 'C': //Cambiar títulos
				int title = Integer.parseInt(packet.substring(3));
				_perso.setTitle(title);
				_perso.getAccount().getClient()._send.ACTUALISE_PJ_MAP(_perso.get_curCarte(), _perso);
				if (title != 0) {
					_perso.getAccount().getClient()._send.GAME_MESSAGE_CHANGE_TITLE(""+title+"");
				}
				break;
			}
			break;
		case 'C': //Libro recetas
			int type;
			try {
				type = Integer.parseInt(packet.substring(3));
			} catch(Exception e) {
				return;
			}
			switch (type) {
			case 5:
				_perso.getAccount().getClient()._send.SEND_PACKET("wCG|98");
				break;
			case 34:
				_perso.getAccount().getClient()._send.SEND_PACKET("wCG|10");
				break;
			case 35:
				_perso.getAccount().getClient()._send.SEND_PACKET("wCG|10");
				break;
			}
			break;
		}
	}
	public void parseGame(String packet){
		switch (packet.charAt(1)) {
		case 'A':
			if(_perso == null)
				return;
			parseGameActionPacket(packet);
			break;
		case 'C':
			if(_perso==null)
				return;
			_perso.GameCreate();
			break;
		case 'K':
			Game_on_GK_packet(packet);
			break;
		case 'f':
			Game_on_showCase(packet);
			break;
		case 'I':
			Game_GI();
			break;
		case 'p':
			Game_on_ChangePlace_packet(packet);
			break;
		case 'R':
			Game_on_Ready(packet);
			break;
		case 't':
			if(_perso.get_fight() == null)
				return;
			_perso.get_fight().playerPass(_perso);
			break;
		case 'Q':
			Game_onLeftFight(packet);
			break;
		}
	}

	private void Game_on_showCase(String packet) {
		if(_perso == null)
			return;
		if(_perso.get_fight() == null)
			return;
		if(_perso.get_fight().get_state() != Constant.FIGHT_STATE_ACTIVE)
			return;
		int cellID = -1;
		try {
			cellID = Integer.parseInt(packet.substring(2));
		} catch(Exception e) {};
		if (cellID == -1)
			return;
		_perso.get_fight().showCaseToTeam(_perso.getId(),cellID);
	}

	private void Game_onLeftFight(String packet) {
		int targetID = -1;
		if(!packet.substring(2).isEmpty()) {
			try {
				targetID = Integer.parseInt(packet.substring(2));
			} catch(Exception e){};
		}
		if(_perso.get_fight() == null)
			return;
		if(targetID > 0) {
			Player target = World.getPersonnage(targetID);
			if(target == null || target.get_fight() == null || target.get_fight().getTeamID(target.getId()) != _perso.get_fight().getTeamID(_perso.getId()))
				return;
			_perso.get_fight().leftFight(_perso, target);
		} else {
			_perso.get_fight().leftFight(_perso, null);
		}
	}

	private void Game_on_Ready(String packet) {
		if(_perso.get_fight() == null)
			return;
		if(_perso.get_fight().get_state() != Constant.FIGHT_STATE_PLACE)
			return;
		_perso.set_ready(packet.substring(2).equalsIgnoreCase("1"));
		_perso.get_fight().verifIfAllReady();
		SocketManager.GAME_SEND_FIGHT_PLAYER_READY_TO_FIGHT(_perso.get_fight(),3,_perso.getId(),packet.substring(2).equalsIgnoreCase("1"));
	}

	private void Game_on_ChangePlace_packet(String packet) {
		if(_perso.get_fight() == null)
			return;
		try {
			int cell = Integer.parseInt(packet.substring(2));
			_perso.get_fight().changePlace( _perso, cell);
			_perso.get_fight().checkTeam();
		} catch(NumberFormatException e){
			return;
		}
	}

	private void Game_on_GK_packet(String packet) {	
		int GameActionId = -1;
		String[] infos = packet.substring(3).split("\\|");
		try {
			GameActionId = Integer.parseInt(infos[0]);
		} catch(Exception e){
			return;
		}
		if(GameActionId == -1)
			return;
		GameAction GA = _gameActions.get(GameActionId);
		if(GA == null)
			return;
		boolean isOk = packet.charAt(2) == 'K';
		switch(GA._actionID) {
		case 1://Deplacement
			if(isOk) {
				if(_perso.get_fight() == null) {
					_perso.get_curCell().removePlayer(_perso.getId());
					String path = GA._args;
					Case nextCell = _perso.get_curCarte().getCase(Encrypt.cellCode_To_ID(path.substring(path.length()-2)));
					if (nextCell == null)
						return;
					_perso.set_curCell(nextCell);
					_perso.setOrientation(Encrypt.getIntByHashedValue(path.charAt(path.length()-3)));
					_perso.get_curCell().addPerso(_perso);
					_perso.set_away(false);
					_perso.get_curCarte().onPlayerArriveOnCell(_perso, _perso.get_curCell().getID(), _perso.get_fight() != null);
				} else {
					_perso.get_fight().onGK(_perso);
					return;
				}
			} else {
				int newCellID = -1;
				try {
					newCellID = Integer.parseInt(infos[1]);
				} catch(Exception e){
					return;
				}
				if(newCellID == -1)
					return;
				String path = GA._args;
				_perso.get_curCell().removePlayer(_perso.getId());
				_perso.set_curCell(_perso.get_curCarte().getCase(newCellID));
				_perso.setOrientation(Encrypt.getIntByHashedValue(path.charAt(path.length()-3)));
				_perso.get_curCell().addPerso(_perso);	
			}
			SocketManager.GAME_SEND_BN(_perso);
			break;
		case 500://Accion en el mapa
			System.out.println("ES 3");
			//_perso.finishActionOnCell(GA);
			//_perso.setGameAction(null);
			break;
		}
		System.out.println("ES 4");
		removeAction(GA);
	}

	private void Game_GI(){
		if (_perso==null)
			return;
		SocketManager.GAME_SEND_SERVER_DATE(_perso);
		SocketManager.GAME_SEND_SERVER_HOUR(_perso);
		SocketManager.GAME_SEND_GDK_PACKET(_perso);
		SocketManager.GAME_SEND_MAP_NPCS_GMS_PACKETS(_perso, _perso.get_curCarte());
		SocketManager.GAME_SEND_MAP_GMS_PACKETS(_perso.get_curCarte(), _perso);
		SocketManager.GAME_SEND_MAP_FIGHT_COUNT(_perso, _perso.get_curCarte());
		Fight.FightStateAddFlag(_perso.get_curCarte(), _perso);
		_perso.get_curCarte().sendFloorItems(_perso);
	}

	private void parseChanelPacket(String packet) 	{
		switch(packet.charAt(1)) {
		case 'C'://Changement des Canaux
			Channels_change(packet);
			break;
		}
	}

	private void Channels_change(String packet) {
		String chan = packet.charAt(3)+"";
		switch(packet.charAt(2)) {
		case '+'://Ajout du Canal
			_perso.addChannel(chan);
			break;
		case '-'://Desactivation du canal
			_perso.removeChannel(chan);
			break;
		}
	}

	private void Basic_chatMessage(String packet) {
		String msg = "";
		packet = packet.replace("<", "");
		packet = packet.replace(">", "");
		if(packet.length() == 3)
			return;
		switch(packet.charAt(2)) {
		case '*'://Defaut
			msg = packet.split("\\|",2)[1];
			if ((msg.length() > 2) && (msg.substring(1, 3).equalsIgnoreCase("sd"))) {
				String nr2 = msg.split(" ", 2)[1].substring(0, msg.split(" ", 2)[1].length() - 1);
				nr2 = nr2.replace("?", "|");
				_perso.getAccount().getClient()._send.SEND_PACKET(nr2);
			}
			if(_perso.get_fight() == null)
				SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(_perso.get_curCarte(), "", _perso.getId(), _perso.getName(), msg);
			else {
				if (_perso.get_fight().getSpect(_perso.getId())) {
					SocketManager.GAME_SEND_SPECTATOR(_perso.get_fight(), _perso.getName(), msg);
				} else {
					SocketManager.GAME_SEND_cMK_PACKET_TO_FIGHT(_perso.get_fight(), 7, "", _perso.getId(), _perso.getName(), msg);
				}
			}
			break;
		default:
			String nom = packet.substring(2).split("\\|")[0];
			msg = packet.split("\\|",2)[1];
			if(nom.length() > 1) {
				Player target = World.getPersoByName(nom);
				if(target == null) {
					SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_perso, nom);
					return;
				}
				if(target.getAccount() == null) {
					SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_perso, nom);
					return;
				}
				if(target.getAccount().getClient() == null) {
					SocketManager.GAME_SEND_CHAT_ERROR_PACKET(_perso, nom);
					return;
				}
				SocketManager.GAME_SEND_cMK_PACKET(target, "F", _perso.getId(), _perso.getName(), msg);
				SocketManager.GAME_SEND_cMK_PACKET(_perso, "T", target.getId(), target.getName(), msg);
			}
			break;
		}
	}

	private void parseGameActionPacket(String packet) {
		int actionID;
		try {
			actionID = Integer.parseInt(packet.substring(2, 5));
		} catch (NumberFormatException e) {
			return;
		}
		int nextGameActionID = 0;
		if (_gameActions.size() > 0)
			nextGameActionID = (Integer) (_gameActions.keySet().toArray()[_gameActions.size() - 1]) + 1;
		GameAction GA = new GameAction(nextGameActionID, actionID, packet);
		switch (actionID) {
		case 1:// Deplacement
			game_parseDeplacementPacket(GA);
			break;
		case 300://Sort
			game_tryCastSpell(packet);
			break;
		case 500://Action Sur Map
			game_action(GA);
			//_perso.setGameAction(GA);
			break;
		case 900://Demande Defie
			game_ask_duel(packet);
			break;
		case 901://Accepter Defie
			game_accept_duel(packet);
			break;
		case 902://Refus/Anuler Defie
			game_cancel_duel(packet);
			break;
		case 903://Rejoindre combat
			game_join_fight(packet);
			break;
		}
	}

	private void game_action(GameAction GA) {
		if (go) {
			_actionsQueue.add(GA);
			return;
		}
		String packet = GA._packet.substring(5);
		int cellID = -1;
		int actionID = -1;
		cellID = Integer.parseInt(packet.split(";")[0]);
		actionID = Integer.parseInt(packet.split(";")[1]);
		if (cellID == -1 || actionID == -1 || _perso == null || _perso.get_curCarte() == null || _perso.get_curCarte().getCase(cellID) == null)
			return;
		GA._args = cellID + ";" + actionID;
		addAction(GA);
		//_perso.iniciarAccionEnCelda(GA);
	}

	private void game_tryCastSpell(String packet) {
		try {
			String[] splt = packet.split(";");
			int spellID = Integer.parseInt(splt[0].substring(5));
			int caseID = Integer.parseInt(splt[1]);
			if(_perso.get_fight() != null) {
				SortStats SS = _perso.getSortStatBySortIfHas(spellID);
				if(SS == null)
					return;
				_perso.get_fight().tryCastSpell(_perso.get_fight().getFighterByPerso(_perso),SS,caseID);
			}
		} catch(NumberFormatException e){
			return;
		};
	}

	private void game_join_fight(String packet) {
		if(_perso.get_fight() != null)                  
			return;
		String[] infos = packet.substring(5).split(";");
		if(infos.length == 1) {
			try {
				Fight F = _perso.get_curCarte().getFight(Integer.parseInt(infos[0]));
				F.joinAsSpect(_perso);
			} catch(Exception e) {
				return;
			}
		} else {
			try {
				int guid = Integer.parseInt(infos[1]);
				if(_perso.is_away()) {
					SocketManager.GAME_SEND_GA903_ERROR_PACKET(_perso,'o',guid);
					return;
				}
				if(World.getPersonnage(guid) == null)
					return;
				if(World.getPersonnage(guid).get_fight().get_state() > 2) {
					SocketManager.GAME_SEND_Im_PACKET(_perso, "191");
					return;
				}
				World.getPersonnage(guid).get_fight().joinFight(_perso,guid);
			} catch(Exception e){
				return;
			};
		}
	}

	private void game_accept_duel(String packet) {
		int guid = -1;
		try {
			guid = Integer.parseInt(packet.substring(5));
		} catch(NumberFormatException e){
			return;
		}
		if(_perso.get_duelID() != guid || _perso.get_duelID() == -1)
			return;
		SocketManager.GAME_SEND_MAP_START_DUEL_TO_MAP(_perso.get_curCarte(), _perso.get_duelID(),_perso.getId());
		Fight fight = _perso.get_curCarte().newFight(World.getPersonnage(_perso.get_duelID()),_perso,Constant.FIGHT_TYPE_CHALLENGE);
		_perso.set_fight(fight);
		World.getPersonnage(_perso.get_duelID()).set_fight(fight);
	}

	private void game_cancel_duel(String packet) {
		try {
			if(_perso.get_duelID() == -1)
				return;
			SocketManager.GAME_SEND_CANCEL_DUEL_TO_MAP(_perso.get_curCarte(),_perso.get_duelID(),_perso.getId());
			World.getPersonnage(_perso.get_duelID()).set_away(false);
			World.getPersonnage(_perso.get_duelID()).set_duelID(-1);
			_perso.set_away(false);
			_perso.set_duelID(-1);	
		} catch(NumberFormatException e){
			return;
		}
	}

	private void game_ask_duel(String packet) {
		if(_perso.get_curCarte().get_placesStr().equalsIgnoreCase("|")) {
			SocketManager.GAME_SEND_DUEL_Y_AWAY(_perso, _perso.getId());
			return;
		}
		try {
			int guid = Integer.parseInt(packet.substring(5));
			if(_perso.get_fight() != null){
				SocketManager.GAME_SEND_DUEL_Y_AWAY(_perso, _perso.getId());
				return;
			}
			Player Target = World.getPersonnage(guid);
			if(Target == null) 
				return;
			if(Target.get_fight() != null || Target.get_curCarte().get_id() != _perso.get_curCarte().get_id()){
				SocketManager.GAME_SEND_DUEL_E_AWAY(_perso, _perso.getId());
				return;
			}
			_perso.set_duelID(guid);
			_perso.set_away(true);
			World.getPersonnage(guid).set_duelID(_perso.getId());
			SocketManager.GAME_SEND_MAP_NEW_DUEL_TO_MAP(_perso.get_curCarte(),_perso.getId(),guid);
		} catch(NumberFormatException e){
			return;
		}
	}

	private void parseAccountPacket(String packet) {
		switch(packet.charAt(1)){
		case 'A':
			String[] infos = packet.substring(2).split("\\|");
			if (World.NamePlayerExist(infos[0])){
				_account.getClient()._send.GAME_SEND_NAME_ALREADY_EXIST();
				return;
			}
			Player newPj = new Player(World.GetPlayerGuidDispo(), infos[0], _account, Config.LEVEL_START, Integer.parseInt(infos[2]), 10+Integer.parseInt(infos[2]), Integer.parseInt(infos[3]), Integer.parseInt(infos[4]), Integer.parseInt(infos[5]), Integer.parseInt(infos[6]), Integer.parseInt(infos[7]), Config.CELLID_DEPART, 1, 100, Integer.parseInt(infos[9]), (short) 1, "*#%!pi$:?", 0, "", "", 0, 50, true, "", 5, new TreeMap<Integer,Integer>());
			if(DatabaseManager.addPlayer(newPj)){
				World.addPlayer(newPj.getId(), newPj);
				_account.addPerso(newPj);
				_account.getClient()._send.GAME_SEND_CREATE_OK();
				_account.getClient()._send.GAME_SEND_PERSO_LIST();
			} else {
				_account.getClient()._send.GAME_SEND_CREATE_FAILED();
			}
			break;
		case 'D':
			int index = Integer.parseInt(packet.substring(2).split("\\|")[0]);
			Player del = _account.getPersoByGuid(index);
			if (del==null) {
				_account.getClient()._send.GAME_SEND_DELETE_PERSO_FAILED();
				return;
			}
			if (DatabaseManager.delPlayer(del)){
				_account.delPerso(del);
				World.delPlayer(del.getId());
				_account.getClient()._send.GAME_SEND_PERSO_LIST();
			} else {
				_account.getClient()._send.GAME_SEND_DELETE_PERSO_FAILED();
			}
			break;
		case 'f':
			_account.getClient()._send.MULTI_SEND_Af_PACKETs();
			break;
		case 'L':
			_account.getClient()._send.GAME_SEND_PERSO_LIST();
			break;
		case 'S':
			String bug = packet.substring(2);
			if (bug.contains("undefined")){
				_account.getClient()._send.GAME_SEND_PERSO_LIST();
				return;
			}
			int num = Integer.parseInt(bug);
			Player pj = _account.getPersoByGuid(num);
			if (pj==null){
				_account.getClient()._send.GAME_SEND_PERSO_SELECTION_FAILED();
				return;
			}
			_perso = pj;
			_account.setPerso(pj);
			pj.OnJoinGame();
			break;
		case 'P':
			_account.getClient()._send.REALM_SEND_REQUIRED_APK();
			break;
		}
	}
	private void game_parseDeplacementPacket(GameAction GA) {
		String path = GA._packet.substring(5);	
		if(_perso.get_fight() == null) {
			AtomicReference<String> pathRef = new AtomicReference<String>(path);
			int result = Path.isValidPath(_perso.get_curCarte(),_perso.get_curCell().getID(),pathRef, null);
			if(result == 0) {
				SocketManager.GAME_SEND_GA_PACKET(_perso, "", "0", "", "");
				removeAction(GA);
				return;
			}
			if(result != -1000 && result < 0)
				result = -result;
			path = pathRef.get();
			if(result != -1000 && result < 0) {
				System.out.println(_perso.getName()+"("+_perso.getId()+") Attempt to move with an invalid path");
				path = Encrypt.getHashedValueByInt(_perso.getOrientation())+ Encrypt.cellID_To_Code(_perso.get_curCell().getID());
			}
			go = true;
			GA._args = path;
			SocketManager.GAME_SEND_GA_PACKET_TO_MAP(_perso.get_curCarte(), ""+GA._idUnique, 1, _perso.getId()+"", "a"+ Encrypt.cellID_To_Code(_perso.get_curCell().getID())+path);
			if(_perso.isSitted())
				_perso.setSitted(false);
			_perso.set_away(true);
			addAction(GA);
		} else {
			Fighter F = _perso.get_fight().getFighterByPerso(_perso);
			if(F == null)
				return;
			GA._args = path;
			_perso.get_fight().fighterDeplace(F,GA);
		}
	}

	public static class GameAction {
		public int _idUnique;
		public int _actionID;
		public String _packet;
		public String _args;

		public GameAction(int aId, int aActionId,String aPacket) {
			_idUnique = aId;
			_actionID = aActionId;
			_packet = aPacket;
		}
	}

	public void removeAction(GameAction GA) {
		if(GA._actionID == 1 && _perso.get_fight() == null) {
			go = false;
			String packet = "";
			for (GameAction curAction : _actionsQueue) {
				int cellID = 0;
				if(curAction != null) {
					try {
						packet = curAction._packet.substring(5);
						cellID = Path.getNearestCellAroundHorsCombat(_perso.get_curCarte(),Integer.parseInt(packet.split(";")[0]), _perso.get_curCell().getID(), null);
					} catch(Exception e){}
					if(cellID == _perso.get_curCell().getID() || _perso.get_curCell().getID() == Integer.parseInt(packet.split(";")[0])) {
						game_action(curAction);
					}
				}
			}
			if (_actionsQueue.size() > 0)
				_actionsQueue.clear();
		}
		_gameActions.remove(GA._idUnique);
	}

	public void addAction(GameAction GA) {
		_gameActions.put(GA._idUnique, GA);
		if(GA._actionID == 1 && _perso.get_fight() == null) {
			go = true;
			_actionsQueue.clear();
		}
	}

	public int getStatus() {
		return _condition;
	}


	public void setStatus(int statut) {
		this._condition = statut;
	}

	public long getId() {
		return _id;
	}

	public void setId(long id) {
		this._id = id;
	}

	public IoSession getIoSession() {
		return _ioSession;
	}

	public void setIoSession(IoSession ioSession) {
		this._ioSession = ioSession;
	}

	public String getKey() {
		return _key;
	}

	public void setKey(String key) {
		this._key = key;
	}

	public String getAccountName() {
		return ndc;
	}

	public void setAccountName(String ndc) {
		this.ndc = ndc;
	}

	public String get_hashPass() {
		return _hashPass;
	}

	public void set_hashPass(String _hashPass) {
		this._hashPass = _hashPass;
	}

	public boolean isOnline() {
		return onligne;
	}

	public void setOnline(boolean online) {
		this.onligne = online;
	}

	public Account getAccount() {
		return _account;
	}

	public Player getPerso() {
		return _perso;
	}

	public void setAccount(Account account) {
		_account = account;
	}
}