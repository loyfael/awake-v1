package variables;

import variables.NPC_tmpl.NPC_question;
import client.ConditionParser;
import client.Player;
import data.SocketManager;
import data.World;

public class Action {

	private int ID;
	private String args;
	private String cond;

	public Action(int id, String args, String cond) {
		this.ID = id;
		this.args = args;
		this.cond = cond;
	}

	public void apply(Player perso, Player target, int itemID, int cellid) {
		if(perso == null)
			return;
		if (perso.get_fight() != null) {
			SocketManager.GAME_SEND_MESSAGE(perso, "<b>Action impossible.</b> Vous êtes actuellement en combat.", "000000");
			return;
		}
		if(!cond.equalsIgnoreCase("") && !cond.equalsIgnoreCase("-1")&& !ConditionParser.validConditions(perso, cond)) {
			SocketManager.GAME_SEND_Im_PACKET(perso, "119");
			return;
		}
		if(perso.getAccount().getClient() == null)
			return;
		if (ID == 1) { //Discours NPC
			if (args.equalsIgnoreCase("DV")) {
				SocketManager.GAME_SEND_END_DIALOG_PACKET(perso);
				perso.set_isTalkingWith(0);
			} else {
				int qID = -1;
				try {
					qID = Integer.parseInt(args);
				} catch (NumberFormatException e) {
				}
				;

				NPC_question quest = World.getNPCQuestion(qID);
				if (quest == null) {
					SocketManager.GAME_SEND_END_DIALOG_PACKET(perso);
					perso.set_isTalkingWith(0);
					return;
				}
				SocketManager.GAME_SEND_QUESTION_PACKET(perso, quest.parseToDQPacket(perso));
			}
		}
	}

	public int getID(){
		return ID;
	}
}