package data;

public class ExpLevel {
    public long perso;
    public int metier;
    /*public int dinde;*/
    public int pvp;
    public long guilde;

    public ExpLevel(long c, int m, int p) {
        perso = c;
        metier = m;
        pvp = p;
        guilde = perso*10;
    }
}
