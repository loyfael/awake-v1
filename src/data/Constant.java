package data;

import java.util.TreeMap;

import variables.Spell.SortStats;
import client.Player;

public class Constant {
	public static String version = "1.12.0s";

	public static final int ELEMENT_NULL		=	-1;
	public static final int ELEMENT_NEUTRE		= 	0;
	public static final int ELEMENT_TERRE		= 	1;
	public static final int ELEMENT_EAU			= 	2;
	public static final int ELEMENT_FEU			= 	3;
	public static final int ELEMENT_AIR			= 	4;
	public static final int[] ON_HIT_BUFFS		=	{9,79,107,788};
	public static final int[] BEGIN_TURN_BUFF	=	{91,92,93,94,95,96,97,98,99,100,108};
	public static final int MAX_EFFECTS_ID 		=	1500;
	public static final int ITEM_POS_NO_EQUIPED 	= -1;
	public static final int ITEM_POS_AMULETTE		= 0;
	public static final int ITEM_POS_ARME			= 1;
	public static final int ITEM_POS_ANNEAU1		= 2;
	public static final int ITEM_POS_CEINTURE		= 3;
	public static final int ITEM_POS_ANNEAU2		= 4;
	public static final int ITEM_POS_BOTTES			= 5;
	public static final int ITEM_POS_COIFFE		 	= 6;
	public static final int ITEM_POS_CAPE			= 7;
	public static final int ITEM_POS_FAMILIER		= 8;
/*	public static final int ITEM_POS_DOFUS1			= 9;
	public static final int ITEM_POS_DOFUS2			= 10;
	public static final int ITEM_POS_DOFUS3			= 11;
	public static final int ITEM_POS_DOFUS4			= 12;
	public static final int ITEM_POS_DOFUS5			= 13;
	public static final int ITEM_POS_DOFUS6			= 14;*/
	public static final int ITEM_POS_BOUCLIER		= 15;
	// public static final int ITEM_POS_DRAGODINDE     = 16;
	public static final int ITEM_TYPE_DOFUS			= 23;
	public static final int FIGHT_TYPE_CHALLENGE 	= 0;//Défies
	public static final int FIGHT_TYPE_AGRESSION 	= 1;//Aggros
	public static final int FIGHT_TYPE_CONQUETE		= 2;//Conquista
/*	public static final int FIGHT_TYPE_DOPEUL		= 3;//Dopuls
	public static final int FIGHT_TYPE_PVM			= 4;//PvM
	public static final int FIGHT_TYPE_PVT			= 5;//Percepteur
	public static final int FIGHT_TYPE_KOLI		    = 6;//koli
	public static final int FIGHT_STATE_INIT		= 1;*/
	public static final int FIGHT_STATE_PLACE		= 2;
	public static final int FIGHT_STATE_ACTIVE 		= 3;
	public static final int FIGHT_STATE_FINISHED	= 4;
	public static final int EFFECT_PASS_TURN		= 140;
	public static final int IOBJECT_STATE_FULL		= 1;
/*	public static final int IOBJECT_STATE_EMPTYING	= 2;
	public static final int IOBJECT_STATE_EMPTY		= 3;
	public static final int IOBJECT_STATE_EMPTY2	= 4;
	public static final int IOBJECT_STATE_FULLING	= 5;*/
	public static final int STATS_ADD_PM2			= 	78;
	public static final int STATS_REM_PA			= 	101;
	public static final int STATS_ADD_VIE			= 	110;
	public static final int STATS_ADD_PA			= 	111;
	public static final int STATS_MULTIPLY_DOMMAGE	=	114;
	public static final int STATS_ADD_CC			=	115;
	public static final int STATS_REM_PO			= 	116;
	public static final int STATS_ADD_PO			= 	117;
	public static final int STATS_ADD_FORC			= 	118;
	public static final int STATS_ADD_AGIL			= 	119;
	public static final int STATS_ADD_PA2			=	120;
	public static final int STATS_ADD_DOMA			=	112;
	public static final int STATS_ADD_EC			=	122;
	public static final int STATS_ADD_CHAN			= 	123;
	public static final int STATS_ADD_SAGE			= 	124;
	public static final int STATS_ADD_VITA			= 	125;
	public static final int STATS_ADD_INTE			= 	126;
	public static final int STATS_REM_PM			= 	127;
	public static final int STATS_ADD_PM			= 	128;
	public static final int STATS_ADD_PERDOM		=	138;
	public static final int STATS_ADD_PDOM			=	142;
	public static final int STATS_REM_DOMA			= 	145;
	public static final int STATS_REM_CHAN			= 	152;
	public static final int STATS_REM_VITA			= 	153;
	public static final int STATS_REM_AGIL			= 	154;
	public static final int STATS_REM_INTE			= 	155;
	public static final int STATS_REM_SAGE			= 	156;
	public static final int STATS_REM_FORC			= 	157;
	public static final int STATS_ADD_PODS			= 	158;
	public static final int STATS_REM_PODS			= 	159;
	public static final int STATS_ADD_AFLEE			=	160;
	public static final int STATS_ADD_MFLEE			=	161;
	public static final int STATS_REM_AFLEE			=	162;
	public static final int STATS_REM_MFLEE			=	163;
	public static final int STATS_ADD_MAITRISE		=	165;
	public static final int STATS_REM_PA2			=	168;
	public static final int STATS_REM_PM2			=	169;
	public static final int STATS_REM_CC			=	171;
	public static final int STATS_ADD_INIT			= 	174;
	public static final int STATS_REM_INIT			= 	175;
	public static final int STATS_ADD_PROS			= 	176;
	public static final int STATS_REM_PROS			= 	177;
	public static final int STATS_ADD_SOIN			= 	178;
	public static final int STATS_REM_SOIN			= 	179;
	public static final int STATS_CREATURE			= 	182;
	public static final int STATS_ADD_RP_TER		=	210;
	public static final int STATS_ADD_RP_EAU 		=	211;
	public static final int STATS_ADD_RP_AIR		=	212;
	public static final int STATS_ADD_RP_FEU 		=	213;
	public static final int STATS_ADD_RP_NEU		= 	214;
	public static final int STATS_REM_RP_TER		=	215;
	public static final int STATS_REM_RP_EAU 		=	216;
	public static final int STATS_REM_RP_AIR		=	217;
	public static final int STATS_REM_RP_FEU 		=	218;
	public static final int STATS_REM_RP_NEU		= 	219;
	public static final int STATS_RETDOM			=	220;
	public static final int STATS_TRAPDOM			=	225;
	public static final int STATS_TRAPPER			=	226;
	public static final int STATS_ADD_R_FEU 		= 	240;
	public static final int STATS_ADD_R_NEU			=	241;
	public static final int STATS_ADD_R_TER			=	242;
	public static final int STATS_ADD_R_EAU			=	243;
	public static final int STATS_ADD_R_AIR			=	244;
	public static final int STATS_REM_R_FEU 		= 	245;
	public static final int STATS_REM_R_NEU			=	246;
	public static final int STATS_REM_R_TER			=	247;
	public static final int STATS_REM_R_EAU			=	248;
	public static final int STATS_REM_R_AIR			=	249;
	public static final int STATS_ADD_RP_PVP_TER	=	250;
	public static final int STATS_ADD_RP_PVP_EAU	=	251;
	public static final int STATS_ADD_RP_PVP_AIR	=	252;
	public static final int STATS_ADD_RP_PVP_FEU	=	253;
	public static final int STATS_ADD_RP_PVP_NEU	=	254;
	public static final int STATS_REM_RP_PVP_TER	=	255;
	public static final int STATS_REM_RP_PVP_EAU	=	256;
	public static final int STATS_REM_RP_PVP_AIR	=	257;
	public static final int STATS_REM_RP_PVP_FEU	=	258;
	public static final int STATS_REM_RP_PVP_NEU	=	259;
	public static final int STATS_ADD_R_PVP_TER		=	260;
	public static final int STATS_ADD_R_PVP_EAU		=	261;
	public static final int STATS_ADD_R_PVP_AIR		=	262;
	public static final int STATS_ADD_R_PVP_FEU		=	263;
	public static final int STATS_ADD_R_PVP_NEU		=	264;

	public static TreeMap<Integer, Character> getStartSortsPlaces() {
		TreeMap<Integer,Character> start = new TreeMap<Integer,Character>();
		start.put(1,'b');
		return start;
	}

	public static TreeMap<Integer,SortStats> getStartSorts() {
		TreeMap<Integer,SortStats> start = new TreeMap<Integer,SortStats>();
		start.put(1, World.getSort(1).getStatsByLevel(1));
		return start;
	}

	public static int getNearCellidUnused(Player _perso) {
		int cellFront = 0;
		int cellBack = 0;
		int cellRight = 0;
		int cellLeft = 0;
		cellFront = 15;
		cellBack = -15;
		cellRight = 14;
		cellLeft = -14;
		if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellFront).getDroppedItem() == null && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellFront).getPersos().isEmpty() && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellFront).isWalkable(false)) {
			return cellFront;
		} else if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()-cellBack).getDroppedItem() == null && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellBack).getPersos().isEmpty() && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellBack).isWalkable(false)) {
			return cellBack;
		} else if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()+cellRight).getDroppedItem() == null && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellRight).getPersos().isEmpty() && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()+cellRight).isWalkable(false)) {
			return cellRight;
		} else if(_perso.get_curCarte().getCase(_perso.get_curCell().getID()-cellLeft).getDroppedItem() == null && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellLeft).getPersos().isEmpty() && _perso.get_curCarte().GetCases().get(_perso.get_curCell().getID()-cellLeft).isWalkable(false)) {
			return cellLeft;
		}
		return -1;
	}
}