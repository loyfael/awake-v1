package client;

import java.util.Random;

import variables.Fight;
import variables.Fight.Fighter;
import data.Constant;
import data.SocketManager;

public class Formules {
	public static final int[] ARMES_EFFECT_IDS	=	{91,92,93,94,95,96,97,98,99,100,101};
	public static final int[] NO_BOOST_CC_IDS	=	{101};

	public static int getRandomValue(int i1,int i2) {
		Random rand = new Random();
		return (rand.nextInt((i2-i1)+1))+i1;
	}

	public static int getRandomJet(String jet) {
		try {
			int num = 0;
			int des = Integer.parseInt(jet.split("d")[0]);
			int faces = Integer.parseInt(jet.split("d")[1].split("\\+")[0]);
			int add = Integer.parseInt(jet.split("d")[1].split("\\+")[1]);
			for(int a=0;a<des;a++) {
				num += getRandomValue(1,faces);
			}
			num += add;
			return num;
		} catch(NumberFormatException e) {
			return -1;
		}
	}

	public static int spellCost(int nb) {
		int total = 0;
		for (int i = 1; i < nb ; i++) {
			total += i;
		}
		return total;
	}

	public static int getTackleChance(Fighter fight, Fighter fighter) {
		int agiTR = getRandomValue(10,200);
		int agiT = getRandomValue(10,200);
		int a = agiTR + 25;
		int b = agiTR + agiT + 50;
		if (b <= 0)
			b = 1;
		int chan = (int) ((long) (300L * a / b) - 100);
		if (chan < 10)
			chan = 10;
		if (chan > 90)
			chan = 90;
		return chan;
	}

	public static int getPointsLost(char z, int value, Fighter caster,Fighter target) {
		float esquiveC = z=='a'?caster.getTotalStats().getEffect(Constant.STATS_ADD_AFLEE):caster.getTotalStats().getEffect(Constant.STATS_ADD_MFLEE);
		float esquiveT = z=='a'?target.getTotalStats().getEffect(Constant.STATS_ADD_AFLEE):target.getTotalStats().getEffect(Constant.STATS_ADD_MFLEE);
		float ptsMax = z=='a'?target.getTotalStatsLessBuff().getEffect(Constant.STATS_ADD_PA):target.getTotalStatsLessBuff().getEffect(Constant.STATS_ADD_PM);
		int retrait = 0;
		for(int i = 0; i < value;i++) {
			float pts = z =='a'?target.getPA():target.getPM();
			float ptsAct = pts - retrait;
			if(esquiveT == 0)
				esquiveT=1;
			if(esquiveC == 0)
				esquiveC=1;
			float a = (float)(esquiveC/esquiveT);
			float b = (ptsAct/ptsMax);
			float pourcentage = (float)(a*b*50);
			int chance = (int)Math.ceil(pourcentage);
			if(chance <0)
				chance = 0;
			if(chance >100)
				chance = 100;
			int jet = getRandomValue(0, 99);
			if(jet<chance) {
				retrait++;
			}
		}
		return retrait;
	}

	public static int calculFinalDamage(Fight fight, Fighter caster, Fighter target, int statID, int jet, boolean isHeal, boolean isCaC, int spellid) {
		float i = 0;//Bonus maitrise
		float j = 100; //Bonus de Classe
		float a = 1;//Calcul
		float num = 0;
		float statC = 0, domC = 0, perdomC = 0, resfT = 0, respT = 0;
		int multiplier = 0;
		if(!isHeal) {
			domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_DOMA);
			perdomC = caster.getTotalStats().getEffect(Constant.STATS_ADD_PERDOM);
			multiplier = caster.getTotalStats().getEffect(Constant.STATS_MULTIPLY_DOMMAGE);
		} else {
			domC = caster.getTotalStats().getEffect(Constant.STATS_ADD_SOIN);
		}
		switch(statID) {
		case Constant.ELEMENT_NULL://Fixe
			statC = 0;
			resfT = 0;
			respT = 0;
			respT = 0;
			break;
		case Constant.ELEMENT_NEUTRE://neutre
			statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
			resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_NEU);
			respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_NEU);
			if(caster.getPersonnage() != null) {
				respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_NEU);
				resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_NEU);
			}
			//on ajoute les dom Physique
			domC += caster.getTotalStats().getEffect(142);
			//Ajout de la resist Physique
			resfT = target.getTotalStats().getEffect(184);
			break;
		case Constant.ELEMENT_TERRE://force
			statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
			resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_TER);
			respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_TER);
			if(caster.getPersonnage() != null) {
				respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_TER);
				resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_TER);
			}
			//on ajout les dom Physique
			domC += caster.getTotalStats().getEffect(142);
			//Ajout de la resist Physique
			resfT = target.getTotalStats().getEffect(184);
			break;
		case Constant.ELEMENT_EAU://chance
			statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_CHAN);
			resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_EAU);
			respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_EAU);
			if(caster.getPersonnage() != null) {
				respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_EAU);
				resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_EAU);
			}
			//Ajout de la resist Magique
			resfT = target.getTotalStats().getEffect(183);
			break;
		case Constant.ELEMENT_FEU://intell
			statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
			resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_FEU);
			respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_FEU);
			if(caster.getPersonnage() != null) {
				respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_FEU);
				resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_FEU);
			}
			//Ajout de la resist Magique
			resfT = target.getTotalStats().getEffect(183);
			break;
		case Constant.ELEMENT_AIR://agilité
			statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
			resfT = target.getTotalStats().getEffect(Constant.STATS_ADD_R_AIR);
			respT = target.getTotalStats().getEffect(Constant.STATS_ADD_RP_AIR);
			if(caster.getPersonnage() != null) {
				respT += target.getTotalStats().getEffect(Constant.STATS_ADD_RP_PVP_AIR);
				resfT += target.getTotalStats().getEffect(Constant.STATS_ADD_R_PVP_AIR);
			}
			//Ajout de la resist Magique
			resfT = target.getTotalStats().getEffect(183);
			break;
		}
		//On bride la resistance a 50% si c'est un joueur 
		if(respT >50)
			respT = 50;
		if(statC<0)
			statC=0;
		if(caster.getPersonnage() != null && isCaC) {
			int ArmeType = caster.getPersonnage().getObjetByPos(1).getTemplate().getType();

			if((caster.getSpellValueBool(392) == true) && ArmeType == 2)//ARC
			{
				i = caster.getMaitriseDmg(392);
			}
			if((caster.getSpellValueBool(390) == true) && ArmeType == 4)//BATON
			{
				i = caster.getMaitriseDmg(390);
			}
			if((caster.getSpellValueBool(391) == true) && ArmeType == 6)//EPEE
			{
				i = caster.getMaitriseDmg(391);
			}
			if((caster.getSpellValueBool(393) == true) && ArmeType == 7)//MARTEAUX
			{
				i = caster.getMaitriseDmg(393);
			}
			if((caster.getSpellValueBool(394) == true) && ArmeType == 3)//BAGUETTE
			{
				i = caster.getMaitriseDmg(394);
			}
			if((caster.getSpellValueBool(395) == true) && ArmeType == 5)//DAGUES
			{
				i = caster.getMaitriseDmg(395);
			}
			if((caster.getSpellValueBool(396) == true) && ArmeType == 8)//PELLE
			{
				i = caster.getMaitriseDmg(396);
			}
			if((caster.getSpellValueBool(397) == true) && ArmeType == 19)//HACHE
			{
				i = caster.getMaitriseDmg(397);
			}
			a = (((100+i)/100)*(j/100));
		}

		num = a*(jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;//dégats bruts

		//Poisons
		if(spellid != -1)
		{
			switch(spellid)
			{

			case 66 : 
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_AGIL);
				num = (jet * ((100 + statC + perdomC + (multiplier * 100)) / 100 ))+ domC;
				if(target.hasBuff(105))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
					return 0;
				}
				if(target.hasBuff(184))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;

			case 71 :
			case 196:
			case 219:
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_FORC);
				num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;
				if(target.hasBuff(105))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
					return 0;
				}
				if(target.hasBuff(184))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;

			case 181:
			case 200:
				statC = caster.getTotalStats().getEffect(Constant.STATS_ADD_INTE);
				num = (jet * ((100 + statC + perdomC + (multiplier*100)) / 100 ))+ domC;
				if(target.hasBuff(105))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(105).getValue());
					return 0;
				}
				if(target.hasBuff(184))
				{
					SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 105, caster.getGUID()+"", target.getGUID()+","+target.getBuff(184).getValue());
					return 0;
				}
				return (int) num;
			}
		}
		//Renvoie
		int renvoie = target.getTotalStatsLessBuff().getEffect(Constant.STATS_RETDOM);
		if(renvoie >0 && !isHeal)
		{
			if(renvoie > num)renvoie = (int)num;
			num -= renvoie;
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 107, "-1", target.getGUID()+","+renvoie);
			if(renvoie>caster.getPDV())renvoie = caster.getPDV();
			if(num<1)num =0;
			caster.removePDV(renvoie);
			SocketManager.GAME_SEND_GA_PACKET_TO_FIGHT(fight, 7, 100, caster.getGUID()+"", caster.getGUID()+",-"+renvoie);
		}
		if(!isHeal)
			num -= resfT;//resis fixe
		int reduc =	(int)((num/(float)100)*respT);//Reduc %resis
		if(!isHeal)
			num -= reduc;
		//dégats finaux
		if(num < 1)
			num=0;
		if(target.getPersonnage()!= null) 
			target.removePDVMAX((int)Math.floor(num/10));
		return (int)num;
	}
}