package client;

import java.util.ArrayList;
import java.util.List;

import realm.Client;
import data.DatabaseManager;
import data.SocketManager;

public class Account {
	private int _guid;
	private String _account;
	private String _password;
	private String _pseudo;
	private boolean _logged;
	private List<Player> persos = new ArrayList<Player>();
	private Client _client;
	private Player _perso = null;
	private String _question;
	private int _pointsVIP = 0;

	public Account(int guid, String account, String password, String pseudo, String aQuestion, int points){
		_guid = guid;
		_account = account;
		_password = password;
		_pseudo = pseudo;
		_logged = false;
		_perso = null;
		_question = aQuestion;
		_pointsVIP = points;
	}

	public int getPointsVIP() {
		return _pointsVIP;
	}

	public void setPointsVIP(int points) {
		_pointsVIP = points;
	}

	public String get_question() {
		return _question;
	}

	public String getPassword() {
		return _password;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String getAccount() {
		return _account;
	}

	public void setAccount(String account) {
		this._account = account;
	}

	public int getGuid() {
		return _guid;
	}

	public void setGuid(int guid) {
		this._guid = guid;
	}

	public boolean isLogged() {
		return _logged;
	}

	public void setLogged(boolean logged) {
		_logged = logged;
	}

	public Client getClient() {
		return _client;
	}

	public void setClient(Client client) {
		_client = client;
	}

	public String getPseudo() {
		return _pseudo;
	}

	public void setPseudo(String pseudo) {
		this._pseudo = pseudo;
	}

	public List<Player> getPersos() {
		return persos;
	}

	public void setPersos(List<Player> persos) {
		this.persos = persos;
	}

	public Player getPerso() {
		return _perso;
	}

	public void setPerso(Player perso) {
		this._perso = perso;
	}

	public void addPerso(Player perso){
		this.persos.add(perso);
	}

	public Player getPersoByGuid(int index){
		for (Player perso : persos){
			if (perso.getId()==index)
				return perso;
		}
		return null;
	}

	public void delPerso(Player perso){
		int a = 0;
		for (Player player : persos){
			if (player.getId()==perso.getId()){
				persos.remove(a);
			}
			a++;
		}
	}

	public void deconnexion() {
		resetAllChars();
	}

	public void resetAllChars() {
		for (Player P : persos) {
			if (P.getOnline()) {
				P.setOnline(false);
				if (P.get_fight() != null) {
					P.get_fight().leftFight(P, null);
				} else {
					P.get_curCell().removePlayer(P.getId());
					SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(P.get_curCarte(), P.getId());
				}
				DatabaseManager.SAVE_PERSONNAGE(P);
				P.resetVars();
			}
		}
		_perso = null;
		_client = null;
	}
}