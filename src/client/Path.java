package client;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import variables.Fight;
import variables.Fight.Fighter;
import variables.Map;
import variables.Map.Case;
import data.Encrypt;

public class Path {

	private static Integer _nSteps = new Integer(0);

	public static int isValidPath(Map map, int cellID, AtomicReference<String> pathRef, Fight fight) {
		synchronized(_nSteps) {
			_nSteps = 0;
			int newPos = cellID;
			int Steps = 0;
			String path = pathRef.get();
			StringBuilder newPath = new StringBuilder("");
			for (int i = 0; i < path.length(); i += 3)  {
				String SmallPath = path.substring(i, i+3);
				char dir = SmallPath.charAt(0);
				int dirCaseID = Encrypt.cellCode_To_ID(SmallPath.substring(1));
				_nSteps = 0;
				if(fight != null && i != 0 && getEnemyFighterArround(newPos, map, fight) != null) {
					pathRef.set(newPath.toString());
					return Steps;
				}
				String[] aPathInfos = ValidSinglePath(newPos, SmallPath, map, fight).split(":");
				if(aPathInfos[0].equalsIgnoreCase("stop")) {
					newPos = Integer.parseInt(aPathInfos[1]);
					Steps += _nSteps;
					newPath.append(dir+ Encrypt.cellID_To_Code(newPos));
					pathRef.set(newPath.toString());
					return -Steps;
				} else if (aPathInfos[0].equalsIgnoreCase("ok")) {
					newPos = dirCaseID;
					Steps += _nSteps;
				} else {
					pathRef.set(newPath.toString());
					return -1000;
				}
				newPath.append(dir+ Encrypt.cellID_To_Code(newPos));
			}
			pathRef.set(newPath.toString());
			return Steps;
		}
	}

	public static String ValidSinglePath(int CurrentPos, String Path, Map map, Fight fight) {
		_nSteps = 0;
		char dir = Path.charAt(0);
		int dirCaseID = Encrypt.cellCode_To_ID(Path.substring(1));
		if(fight != null && fight.isOccuped(dirCaseID))
			return "no:";
		int lastPos = CurrentPos;
		for (_nSteps = 1; _nSteps <= 64; _nSteps++)  {
			if (GetCaseIDFromDirection(lastPos, dir, map, fight!=null) == dirCaseID) {
				if(fight != null && fight.isOccuped(dirCaseID))
					return "stop:"+lastPos;

				if(map.getCase(dirCaseID).isWalkable(true))
					return "ok:";
				else {
					_nSteps--;
					return ("stop:"+lastPos);
				}
			}  else 
				lastPos = GetCaseIDFromDirection(lastPos, dir, map, fight!=null);

			if(fight != null && fight.isOccuped(lastPos))  {
				return "no:";
			}
			if(fight != null)  {
				if(getEnemyFighterArround(lastPos, map, fight) != null)
				{
					return "stop:"+lastPos;
				}
			}

		}
		return "no:";
	}

	public static ArrayList<Fighter> getEnemyFighterArround(int cellID, Map map, Fight fight) {
		char[] dirs = {'b','d','f','h'};
		ArrayList<Fighter> enemy = new ArrayList<Fighter>();
		for(char dir : dirs) {
			Fighter f = map.getCase(GetCaseIDFromDirection(cellID, dir, map, false)).getFirstFighter();
			if(f != null) {
				if(f.getTeam() != fight.getCurFighter().getTeam())
					enemy.add(f);
			}
		}
		if(enemy.size() == 0 || enemy.size() == 4) 
			return null;
		return enemy;
	}

	public static Fighter getEnemyAround(int cellID, Map map, Fight fight) {
		char[] dirs = { 'b', 'd', 'f', 'h' };
		for (char dir : dirs) {
			Case sigCell = map.getCase(GetCaseIDFromDirection(cellID, dir, map, false));
			if (sigCell == null)
				continue;
			Fighter f = sigCell.getFirstFighter();
			if (f != null) {
				if (f.getTeam() != fight.getCurFighter().getTeam())
					return f;
			}
		}
		return null;
	}

	public static int GetCaseIDFromDirection(int CaseID, char Direction, Map map, boolean Combat) {
		switch (Direction) {
		case 'a':
			return Combat ? -1 : CaseID + 1;
		case 'b':
			return CaseID + map.get_w();
		case 'c':
			return Combat ? -1 : CaseID + (map.get_w() * 2 - 1);
		case 'd':
			return  CaseID + (map.get_w() - 1);
		case 'e':
			return Combat ? -1 : CaseID - 1;
		case 'f':
			return CaseID - map.get_w();
		case 'g':
			return Combat ? -1 : CaseID - (map.get_w() * 2 - 1);
		case 'h':
			return  CaseID - map.get_w() + 1;
		}
		return -1; 
	}

	public static ArrayList<Case> getCellListFromAreaString(Map map, int cellID, int castCellID, String zoneStr, int PONum, boolean isCC) {
		ArrayList<Case> cases = new ArrayList<Case>();
		int c = PONum;
		if(map.getCase(cellID) == null)
			return cases;
		cases.add(map.getCase(cellID));
		int taille = Encrypt.getIntByHashedValue(zoneStr.charAt(c+1));
		switch(zoneStr.charAt(c)) {
		case 'C'://Cercle
			for(int a = 0; a < taille;a++) {
				char[] dirs = {'b','d','f','h'};
				ArrayList<Case> cases2 = new ArrayList<Case>();
				cases2.addAll(cases);
				for(Case aCell : cases2) {
					for(char d : dirs) {
						Case cell = map.getCase(GetCaseIDFromDirection(aCell.getID(), d, map, true));
						if(cell == null)
							continue;
						if(!cases.contains(cell))
							cases.add(cell);
					}
				}
			}
			break;
		case 'X'://Croix
			char[] dirs = {'b','d','f','h'};
			for(char d : dirs) {
				int cID = cellID;
				for(int a = 0; a< taille; a++) {
					cases.add(map.getCase(GetCaseIDFromDirection(cID, d, map, true)));
					cID = GetCaseIDFromDirection(cID, d, map, true);
				}
			}
			break;
		case 'L'://Ligne
			char dir = getDirBetweenTwoCase(castCellID, cellID, map,true);
			for(int a = 0; a< taille; a++) {
				cases.add(map.getCase(GetCaseIDFromDirection(cellID, dir, map, true)));
				cellID = GetCaseIDFromDirection(cellID, dir, map, true);
			}
			break;
		case 'P'://Player?
			break;
		default:
			break;
		}
		return cases;
	}

	public static char getDirBetweenTwoCase(int cell1ID, int cell2ID, Map map, boolean Combat) {
		ArrayList<Character> dirs = new ArrayList<>();
		dirs.add('b');
		dirs.add('d');
		dirs.add('f');
		dirs.add('h');
		if(!Combat) {
			dirs.add('a');
			dirs.add('b');
			dirs.add('c');
			dirs.add('d');
		}
		for(char c : dirs) {
			int cell = cell1ID;
			for(int i = 0; i <= 64; i++) {
				if(GetCaseIDFromDirection(cell, c, map, Combat) == cell2ID)
					return c;
				cell = GetCaseIDFromDirection(cell, c, map, Combat);
			}
		}
		return 0;
	}

	public static boolean casesAreInSameLine(Map map, int c1, int c2, char dir) {
		if(c1 == c2)
			return true;
		if(dir != 'z') {
			for(int a = 0;a<70;a++) {
				if(GetCaseIDFromDirection(c1, dir, map, true) == c2)
					return true;
				if(GetCaseIDFromDirection(c1, dir, map, true) == -1)
					break;
				c1 = GetCaseIDFromDirection(c1, dir, map, true);
			}
		} else {
			char[] dirs = {'b','d','f','h'};
			for(char d : dirs) {
				int c = c1;
				for(int a = 0;a<70;a++) {
					if(GetCaseIDFromDirection(c, d, map, true) == c2)
						return true;
					c = GetCaseIDFromDirection(c, d, map, true);
				}
			}
		}
		return false;
	}

	public static boolean checkLoS(Map map, int cell1, int cell2, Fighter fighter) {
		if(fighter.getPersonnage() != null)
			return true;
		int dist = getDistanceBetween(map,cell1,cell2);
		ArrayList <Integer> los = new ArrayList<>();
		if(dist > 2)
			los = getLoS(cell1,cell2);
		if(los != null && dist > 2) {
			for(int i : los) {
				if(i != cell1 && i != cell2 && !map.getCase(i).blockLoS() )
					return false;
			}
		}
		if(dist > 2) {
			int cell = getNearestCellAround(map,cell2,cell1,null);
			if(cell != -1 && !map.getCase(cell).blockLoS())
				return false;
		}
		return true;
	}

	public static boolean checkLoS(Map map, int cell1, int cell2, Fighter fighter, boolean isPeur) {
		if(fighter != null && fighter.getPersonnage() != null)
			return true;
		ArrayList<Integer> CellsToConsider;
		CellsToConsider = getLoSBotheringIDCases(map, cell1, cell2, true);
		if(CellsToConsider == null) {
			return true;
		}
		for(Integer cellID : CellsToConsider) {
			if(map.getCase(cellID) != null)
				if(!map.getCase(cellID).blockLoS() || ( !map.getCase(cellID).isWalkable(false) && isPeur )) {
					return false;
				}
		}
		return true;
	}

	private static ArrayList<Integer> getLoSBotheringIDCases(Map map, int cellID1, int cellID2, boolean Combat) {
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		int consideredCell1 = cellID1;
		int consideredCell2 = cellID2;
		char dir = 'b';
		int diffX = 0;
		int diffY = 0;
		int compteur = 0;
		ArrayList<Character> dirs = new ArrayList<Character>();
		dirs.add('b');
		dirs.add('d');
		dirs.add('f');
		dirs.add('h');
		while(getDistanceBetween(map, consideredCell1, consideredCell2) > 2 && compteur < 300) {
			diffX= getCellXCoord(map, consideredCell1) - getCellXCoord(map, consideredCell2);
			diffY= getCellYCoord(map, consideredCell1) - getCellYCoord(map, consideredCell2);
			if(Math.abs(diffX) > Math.abs(diffY)) {
				if(diffX > 0)
					dir = 'f';
				else dir = 'b';
				consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat);
				consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat);
				toReturn.add(consideredCell1);
				toReturn.add(consideredCell2);
			} else if(Math.abs(diffX) < Math.abs(diffY)) {
				if(diffY > 0)
					dir = 'h';
				else dir = 'd';
				consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat);
				consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat);
				toReturn.add(consideredCell1);
				toReturn.add(consideredCell2);
			} else {
				if(compteur == 0)
					return getLoSBotheringCasesInDiagonal(map, cellID1, cellID2, diffX, diffY);
				if(dir == 'f' || dir == 'b')
					if(diffY > 0)
						dir = 'h';
					else dir = 'd';
				else if(dir == 'h' || dir == 'd')
					if(diffX > 0)
						dir = 'f';
					else dir = 'b';
				consideredCell1 = GetCaseIDFromDirection(consideredCell1, dir, map, Combat);
				consideredCell2 = GetCaseIDFromDirection(consideredCell2, getOpositeDirection(dir), map, Combat);
				toReturn.add(consideredCell1);
				toReturn.add(consideredCell2);
			}
			compteur++;			
		}
		if(getDistanceBetween(map, consideredCell1, consideredCell2) == 2) {
			dir = 0;
			diffX= getCellXCoord(map, consideredCell1) - getCellXCoord(map, consideredCell2);
			diffY= getCellYCoord(map, consideredCell1) - getCellYCoord(map, consideredCell2);
			if(diffX == 0)
				if(diffY > 0)
					dir = 'h';
				else dir = 'd';
			if(diffY == 0)
				if(diffX > 0)
					dir = 'f';
				else dir = 'b';
			if(dir != 0)
				toReturn.add(GetCaseIDFromDirection(consideredCell1, dir, map, Combat));
		}
		return toReturn;
	}

	private static ArrayList<Integer> getLoSBotheringCasesInDiagonal(Map map, int cellID1, int cellID2, int diffX, int diffY) {
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		char dir = 'a';
		if(diffX > 0 && diffY > 0)
			dir = 'g';
		if(diffX > 0 && diffY < 0)
			dir = 'e';
		if(diffX < 0 && diffY > 0)
			dir = 'a';
		if(diffX < 0 && diffY < 0)
			dir = 'c';
		int consideredCell = cellID1, compteur = 0;
		while(consideredCell != -1 && compteur < 100) {
			consideredCell = GetCaseIDFromDirection(consideredCell, dir, map, true);
			if(consideredCell == cellID2)
				return toReturn;
			toReturn.add(consideredCell);
			compteur++;
		}
		return toReturn;
	}

	private static char getOpositeDirection(char c) {
		switch(c) {
		case 'a':
			return 'e';
		case 'b':
			return 'f';
		case 'c':
			return 'g';
		case 'd':
			return 'h';
		case 'e':
			return 'a';
		case 'f':
			return 'b';
		case 'g':
			return 'c';
		case 'h':
			return 'd';
		}
		return 0x00;
	}

	public static int getDistanceBetween(Map map, int id1, int id2) {
		if(id1 == id2)
			return 0;
		if(map == null)
			return 0;
		int diffX = Math.abs(getCellXCoord(map, id1) - getCellXCoord(map,id2));
		int diffY = Math.abs(getCellYCoord(map, id1) - getCellYCoord(map,id2));
		return (diffX + diffY);
	}

	public static int getCellXCoord(Map map, int cellID) {
		if(map == null) 
			return 0;
		int w = map.get_w();
		return ((cellID - (w -1) * getCellYCoord(map,cellID)) / w);
	}

	public static int getCellYCoord(Map map, int cellID) {
		int w = map.get_w();
		int loc5 = (int)(cellID/ ((w*2) -1));
		int loc6 = cellID - loc5 * ((w * 2) -1);
		int loc7 = loc6 % w;
		return (loc5 - loc7);
	}

	public static ArrayList<Integer> getLoS (int cell1, int cell2) {
		ArrayList<Integer> Los = new ArrayList<>();
		int cell = cell1;
		boolean next = false;
		int[] dir1 = {1,-1,29,-29,15,14,-15,-14};
		for(int i : dir1) {
			Los.clear();
			cell = cell1;
			Los.add(cell);
			next = false;
			while(!next) {
				cell += i;
				Los.add(cell);
				if(isBord2(cell) || isBord1(cell) || cell <= 0 || cell >= 480)
					next=true;
				if(cell == cell2) {
					return Los;
				}
			}
		}
		return null;
	}

	public static boolean isBord1(int id) {
		int[] bords = {1,30,59,88,117,146,175,204,233,262,291,320,349,378,407,436,465,15,44,73,102,131,160,189,218,247,276,305,334,363,392,421,450,479};
		ArrayList <Integer> test = new ArrayList <Integer>();
		for(int i : bords) {
			test.add(i);
		}
		if(test.contains(id))
			return true;
		else 
			return false;
	}

	public static boolean isBord2(int id) {
		int[] bords = {16,45,74,103,132,161,190,219,248,277,306,335,364,393,422,451,29,58,87,116,145,174,203,232,261,290,319,348,377,406,435,464};
		ArrayList <Integer> test = new ArrayList <Integer>();
		for(int i : bords) {
			test.add(i);
		}
		if(test.contains(id))
			return true;
		else 
			return false;
	}

	public static int getNearestCellAround(Map map, int startCell, int endCell, ArrayList<Case> forbidens) {
		int dist = 1000;
		int cellID = startCell;
		if(forbidens == null)forbidens = new ArrayList<Case>();
		char[] dirs = {'b','d','f','h'};
		for(char d : dirs) {
			int c = GetCaseIDFromDirection(startCell, d, map, true);
			int dis = getDistanceBetween(map, endCell, c);
			if(dis < dist && map.getCase(c).isWalkable(true) && map.getCase(c).getFirstFighter() == null && !forbidens.contains(map.getCase(c))) {
				dist = dis;
				cellID = c;
			}
		}
		return cellID==startCell?-1:cellID;
	}

	public static int newCaseAfterPush(Map map, Case CCase, Case TCase, int value) {
		if(CCase.getID() == TCase.getID())
			return 0;
		char c = getDirBetweenTwoCase(CCase.getID(), TCase.getID(), map, true);
		int id = TCase.getID();
		if(value <0) {
			c = getOpositeDirection(c);
			value = -value;
		}
		for(int a = 0; a<value;a++) {
			int nextCase = GetCaseIDFromDirection(id, c, map, true);
			if(map.getCase(nextCase) != null && map.getCase(nextCase).isWalkable(true) && map.getCase(nextCase).getFighters().isEmpty())
				id = nextCase;
			else
				return -(value-a);
		}
		if(id == TCase.getID())
			id = 0;
		return id;
	}
	
	public static int getNearestCellAroundHorsCombat(Map map, int startCell, int endCell, ArrayList<Case> forbidens) {
		int dist = 1000;
		int cellID = startCell;
		if(forbidens == null)
			forbidens = new ArrayList<Case>();
		char[] dirs = {'a','b','c','d','e','f','g','h'};
		for(char d : dirs) {
			int c = GetCaseIDFromDirection(startCell, d, map, false);
			int dis = getDistanceBetween(map, endCell, c);
			if(dis < dist && map.getCase(c).isWalkable(true) && !forbidens.contains(map.getCase(c))) {
				dist = dis;
				cellID = c;
			}
		}
		return cellID==startCell?-1:cellID;
	}
}