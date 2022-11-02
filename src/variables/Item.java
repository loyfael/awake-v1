package variables;

import data.Constant;
import data.World;

public class Item {
	protected ObjTemplate template;
	protected int quantity = 1;
	protected int position = -1;
	protected int guid;
	protected int kamas;

	public Item(int Guid, int template, int qua, int pos) {
		this.guid = Guid;
		this.template = World.getObjTemplate(template);
		this.quantity = qua;
		this.position = pos;
	}

	public int getKamas(){
		return this.kamas;
	}

	public void setKamas(int k) {
		this.kamas = k;
	}

	public Item() {
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public ObjTemplate getTemplate() {
		return template;
	}

	public static Item getCloneObjet(Item obj, int qua) {
		Item ob = new Item(World.getNewItemGuid(), obj.getTemplate().getID(), qua, Constant.ITEM_POS_NO_EQUIPED);
		return ob;
	}

	public int getGuid() {
		return guid;
	}

	public String parseItem() {	
		String posi = position==-1?"":Integer.toHexString(position);
		return Integer.toHexString(guid)+"~"+Integer.toHexString(template.getID())+"~"+Integer.toHexString(quantity)+"~"+posi+";";
	}

	public static class ObjTemplate {
		private int ID;
		private String name;
		private	int type;
		private int level;
		private int pod;
		private int prix;
		private String conditions;
		private int points;

		public ObjTemplate(int id, String name, int type,int level, int pod, int prix, String conditions, int points) {
			this.ID = id;
			this.name = name;
			this.type = type;
			this.level = level;
			this.pod = pod;
			this.prix = prix;
			this.conditions = conditions;
			this.points = points;
		}

		public String parseItemTemplateStats() {
			String str = ID+";";
			return str;
		}

		public int getpoints() {
			return points;
		}

		public int getID() {
			return ID;
		}

		public String getName() {
			return name;
		}

		public int getType() {
			return type;
		}

		public int getLevel() {
			return level;
		}

		public int getPod() {
			return pod;
		}

		public int getPrix() {
			return prix;
		}

		public String getConditions() {
			return conditions;
		}

		public Item createNewItem(int qua, boolean useMax) {
			Item item = new Item(World.getNewItemGuid(), ID, qua,-1);
			return item;
		}
	}
}