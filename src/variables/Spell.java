package variables;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import variables.Fight.Fighter;
import variables.Map.Case;
import client.Path;
import client.Formules;
import data.Constant;
import data.World;

public class Spell {
	private String nombre;
	private int spellID;
	private int spriteID;
	private String spriteInfos;
	private Map<Integer, SortStats> sortStats = new TreeMap<Integer, SortStats>();
	private ArrayList<Integer> effectTargets = new ArrayList<Integer>();
	private ArrayList<Integer> CCeffectTargets = new ArrayList<Integer>();

	public Spell(int aspellID, String aNombre, int aspriteID, String aspriteInfos, String ET) {
		spellID = aspellID;
		nombre = aNombre;
		spriteID = aspriteID;
		spriteInfos = aspriteInfos;
		String nET = ET.split(":")[0];
		String ccET = "";
		if (ET.split(":").length > 1)
			ccET = ET.split(":")[1];
		for (String num : nET.split(";")) {
			try {
				effectTargets.add(Integer.parseInt(num));
			} catch (Exception e) {
				effectTargets.add(0);
				continue;
			}
		}
		for (String num : ccET.split(";")) {
			try {
				CCeffectTargets.add(Integer.parseInt(num));
			} catch (Exception e) {
				CCeffectTargets.add(0);
				continue;
			}
		}
	}

	public ArrayList<Integer> getEffectTargets() {
		return effectTargets;
	}

	public int getSpriteID() {
		return spriteID;
	}

	public String getSpriteInfos() {
		return spriteInfos;
	}

	public int getSpellID() {
		return spellID;
	}

	public SortStats getStatsByLevel(int lvl) {
		return sortStats.get(lvl);
	}

	public String getNombre() {
		return nombre;
	}

	public void addSortStats(Integer lvl, SortStats stats) {
		if (sortStats.get(lvl) != null)
			return;
		sortStats.put(lvl, stats);
	}

	public static class SortStats {
		private int spellID;
		private int level;
		private int PACost;
		private int minPO;
		private int maxPO;
		private int TauxCC;
		private int TauxEC;
		private boolean isLineLaunch;
		private boolean hasLDV;
		private boolean isEmptyCell;
		private boolean isModifPO;
		private int maxLaunchbyTurn;
		private int maxLaunchbyByTarget;
		private int coolDown;
		private int reqLevel;
		private boolean isEcEndTurn;
		private ArrayList<SpellEffect> effects;
		private ArrayList<SpellEffect> CCeffects;
		private String porteeType;

		public SortStats(int AspellID, int Alevel, int cost, int minPO, int maxPO, int tauxCC, int tauxEC, boolean isLineLaunch, boolean hasLDV, boolean isEmptyCell, boolean isModifPO, int maxLaunchbyTurn, int maxLaunchbyByTarget, int coolDown, int reqLevel, boolean isEcEndTurn, String effects, String ceffects, String typePortee) {
			this.spellID = AspellID;
			this.level = Alevel;
			this.PACost = cost;
			this.minPO = minPO;
			this.maxPO = maxPO;
			this.TauxCC = tauxCC;
			this.TauxEC = tauxEC;
			this.isLineLaunch = isLineLaunch;
			this.hasLDV = hasLDV;
			this.isEmptyCell = isEmptyCell;
			this.isModifPO = isModifPO;
			this.maxLaunchbyTurn = maxLaunchbyTurn;
			this.maxLaunchbyByTarget = maxLaunchbyByTarget;
			this.coolDown = coolDown;
			this.reqLevel = reqLevel;
			this.isEcEndTurn = isEcEndTurn;
			this.effects = parseEffect(effects);
			this.CCeffects = parseEffect(ceffects);
			this.porteeType = typePortee;
		}

		private ArrayList<SpellEffect> parseEffect(String e) {
			ArrayList<SpellEffect> effets = new ArrayList<SpellEffect>();
			String[] splt = e.split("\\|");
			for (String a : splt) {
				try {
					if (e.equals("-1"))
						continue;
					int id = Integer.parseInt(a.split(";", 2)[0]);
					String args = a.split(";", 2)[1];
					effets.add(new SpellEffect(id, args, spellID, level));
				} catch (Exception f) {
					f.printStackTrace();
					System.out.println(a);
					System.exit(1);
				}
			}
			return effets;
		}

		public int getSpellID() {
			return spellID;
		}

		public Spell getSpell() {
			return World.getSort(spellID);
		}

		public int getSpriteID() {
			return getSpell().getSpriteID();
		}

		public String getSpriteInfos() {
			return getSpell().getSpriteInfos();
		}

		public int getLevel() {
			return level;
		}

		public int getPACost() {
			return PACost;
		}

		public int getMinPO() {
			return minPO;
		}

		public int getMaxPO() {
			return maxPO;
		}

		public int getTauxCC() {
			return TauxCC;
		}

		public int getTauxEC() {
			return TauxEC;
		}

		public boolean isLineLaunch() {
			return isLineLaunch;
		}

		public boolean hasLDV() {
			return hasLDV;
		}

		public boolean isEmptyCell() {
			return isEmptyCell;
		}

		public boolean isModifPO() {
			return isModifPO;
		}

		public int getMaxLaunchbyTurn() {
			return maxLaunchbyTurn;
		}

		public int getMaxLaunchbyByTarget() {
			return maxLaunchbyByTarget;
		}

		public int getCoolDown() {
			return coolDown;
		}

		public int getReqLevel() {
			return reqLevel;
		}

		public boolean isEcEndTurn() {
			return isEcEndTurn;
		}

		public ArrayList<SpellEffect> getEffects() {
			return effects;
		}

		public ArrayList<SpellEffect> getCCeffects() {
			return CCeffects;
		}

		public String getPorteeType() {
			return porteeType;
		}

		public void applySpellEffectToFight(Fight fight, Fighter perso, Case cell, ArrayList<Case> cells, boolean isCC) {
			ArrayList<SpellEffect> effets;
			if (isCC)
				effets = CCeffects;
			else
				effets = effects;
			int jetChance = Formules.getRandomValue(0, 99);
			int curMin = 0;
			for (SpellEffect SE : effets) {
				if (SE.getChance() != 0 && SE.getChance() != 100) {
					if (jetChance <= curMin || jetChance >= (SE.getChance() + curMin)) {
						curMin += SE.getChance();
						continue;
					}
					curMin += SE.getChance();
				}
				ArrayList<Fighter> cibles = SpellEffect.getTargets(SE, fight, cells);
				SE.applyToFight(fight, perso, cell, cibles);
			}
		}

		public void applySpellEffectToFight(Fight fight, Fighter perso, Case cell, boolean isCC) {
			ArrayList<SpellEffect> effets;
			if (isCC)
				effets = CCeffects;
			else
				effets = effects;
			int jetChance = Formules.getRandomValue(0, 99);
			int curMin = 0;
			int num = 0;
			for (SpellEffect SE : effets) {
				if (fight.get_state() >= Constant.FIGHT_STATE_FINISHED)
					return;
				if (SE.getChance() != 0 && SE.getChance() != 100) {
					if (jetChance <= curMin || jetChance >= (SE.getChance() + curMin)) {
						curMin += SE.getChance();
						continue;
					}
					curMin += SE.getChance();
				}
				int POnum = num * 2;
				if (isCC) {
					POnum += effects.size() * 2;
				}
				ArrayList<Case> cells = Path.getCellListFromAreaString(fight.get_map(), cell.getID(), perso.get_fightCell().getID(), porteeType, POnum, isCC);
				ArrayList<Case> finalCells = new ArrayList<Case>();
				int TE = 0;
				Spell S = World.getSort(spellID);
				if (S != null ? S.getEffectTargets().size() > num : false)
					TE = S.getEffectTargets().get(num);
				for (Case C : cells) {
					if (C == null)
						continue;
					Fighter F = C.getFirstFighter();
					if (F == null)
						continue;
					if (((TE & 1) == 1) && (F.getTeam() == perso.getTeam()))
						continue;
					if ((((TE >> 1) & 1) == 1) && (F.getGUID() == perso.getGUID()))
						continue;
					if ((((TE >> 2) & 1) == 1) && (F.getTeam() != perso.getTeam()))
						continue;
					if ((((TE >> 3) & 1) == 1))
						continue;
					if ((((TE >> 5) & 1) == 1) && (F.getGUID() != perso.getGUID()))
						continue;
					finalCells.add(C);
				}
				if (((TE >> 5) & 1) == 1)
					if (!finalCells.contains(perso.get_fightCell()))
						finalCells.add(perso.get_fightCell());
				ArrayList<Fighter> cibles = SpellEffect.getTargets(SE, fight, finalCells);
				SE.applyToFight(fight, perso, cell, cibles);
				num++;
			}
		}
	}
}