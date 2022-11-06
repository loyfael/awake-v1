package data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import variables.Map;
import variables.Map.Case;

public class Encrypt {
	private static final char[] HASH = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_' };

	public static String toUtf(String _in) {
		String _out = "";
		try {
			_out = new String(_in.getBytes(StandardCharsets.UTF_8));
		} catch(Exception e) {
			e.printStackTrace();
		}
		return _out;
	}

	public static char getHashedValueByInt(int c) {
		return HASH[c];
	}

	public static ArrayList<Case> parseStartCell(Map map, int num) {
		ArrayList<Case> list = null;
		String infos = null;
		if(!map.get_placesStr().equalsIgnoreCase("-1")) {
			infos = map.get_placesStr().split("\\|")[num];
			int a=0;
			list = new ArrayList<Case>();
			while( a < infos.length()) {
				list.add(map.getCase((getIntByHashedValue(infos.charAt(a))<<6) + getIntByHashedValue(infos.charAt (a+1))));
				a = a+2;
			}
		}
		System.out.println("LISTA "+list);
		return list;
	}

	public static int cellCode_To_ID(String cellCode) {
		char char1 = cellCode.charAt(0),char2 = cellCode.charAt(1);
		int code1= 0,code2= 0,a = 0;
		while (a < HASH.length) {
			if (HASH[a] == char1) {
				code1 = a * 64;
			}
			if (HASH[a] == char2) {
				code2 = a;
			}
			a++;
		}
		return (code1 + code2);
	}

	public static String CryptPassword(String Key, String Password) {
		StringBuilder _Crypted = new StringBuilder("#1");
		for (int i = 0; i < Password.length(); i++) {
			char PPass = Password.charAt(i);
			char PKey = Key.charAt(i);
			int APass = (int)PPass / 16;
			int AKey = (int)PPass % 16;
			int ANB = (APass + (int)PKey) % HASH.length;
			int ANB2 = (AKey + (int)PKey) % HASH.length;
			_Crypted.append(HASH[ANB]);
			_Crypted.append(HASH[ANB2]);
		}
		return _Crypted.toString();
	}

	public static String cellID_To_Code(int cellID) {
		int char1 = cellID / 64, char2 = cellID % 64;
		return HASH[char1] + "" + HASH[char2];
	}

	public static int getIntByHashedValue(char c) {
		for(int a = 0;a<HASH.length; a++) {
			if(HASH[a] == c) {
				return a;
			}
		}	
		return -1;
	}

	public static java.util.Map<Integer, Case> DecompileMapData(Map map, String dData)
	{
		java.util.Map<Integer, Case> cells = new TreeMap<Integer,Case>();
		for (int f = 0; f < dData.length(); f += 10)  {
			String CellData = dData.substring(f, f+10);
			List<Byte> CellInfo = new ArrayList<>();
			for (int i = 0; i < CellData.length(); i++)
				CellInfo.add((byte)getIntByHashedValue(CellData.charAt(i)));
			int Type = (CellInfo.get(2) & 56) >> 3;
			boolean IsSightBlocker = (CellInfo.get(0) & 1) != 0;
			int layerObject2 = ((CellInfo.get(0) & 2) << 12) + ((CellInfo.get(7) & 1) << 12) + (CellInfo.get(8) << 6) + CellInfo.get(9);
			boolean layerObject2Interactive = ((CellInfo.get(7) & 2) >> 1) != 0;
			int obj = (layerObject2Interactive?layerObject2:-1);
			cells.put(f/10, new Case(map, f/10, Type!=0, IsSightBlocker, obj));
		}
		return cells;
	}
}