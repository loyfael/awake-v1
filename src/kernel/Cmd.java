package kernel;

import java.io.PrintStream;
import org.fusesource.jansi.AnsiConsole;

public class Cmd {
	public enum Color
	{
		//FONT
		BOLD(1),
		UNDERLINE(4),
		BLINK(5),
		HIDDEN(8),
		BLACK(30),
		RED(31),
		GREEN(32),
		YELLOW(33),
		BLUE(34),
		MAGENTA(35),
		CYAN(36),
		WHITE(37),
		//BG
		BG_BLACK(40),
		BG_RED(41),
		BG_GREEN(42),
		BG_YELLOW(43),
		BG_BLUE(44),
		BG_MAGENTA(45),
		BG_CYAN(46),
		BG_WHITE(47),
		//SPECIAL
		BLACK_AND_BG_WHITE(7),
		RESET(0);
		private int color;
		private Color(int color)
		{
			this.color = color;
		}
		public int get()
		{
			return color;
		}
	}
	
	public static void clear() {
        AnsiConsole.out.print("\033[H\033[2J");
    }
    
    public static void setTitle(String title) {
        AnsiConsole.out.append("\033]0;").append(title).append("\007");
    }
    public static void print(String message)
    {
    	AnsiConsole.out.print(message);
    }
    public static void println(String message)
    {
    	AnsiConsole.out.println(message);
    }
    public static PrintStream out()
    {
    	return AnsiConsole.out;
    }
    
    public static void println(String msg, Color color)
    {
    	AnsiConsole.out.println("\033[" + color.get() + "m" + msg + "\033[" + Color.RESET.get() + "m");
    }
    public static void print(String msg, Color color)
    {
    	AnsiConsole.out.print("\033[" + color.get() + "m" + msg + "\033[" + Color.RESET.get() + "m");
    }
    public static void print(String msg, Color color, Color Background)
    {
    	AnsiConsole.out.print("\033[" + color.get() + "m" + "\033[" + Background.get() + "m" + msg + "\033[" + Color.RESET.get() + "m");
    }
    public static void println(String msg, Color color, Color Background)
    {
    	AnsiConsole.out.println("\033[" + color.get() + "m" + "\033[" + Background.get() + "m" + msg + "\033[" + Color.RESET.get() + "m");
    }
}
