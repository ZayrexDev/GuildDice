package guilddice;

import guilddice.bot.Bot;
import guilddice.util.Config;

public class Main {
    public static Config CONFIG = new Config();
    public static String APP_ID = "";
    public static String APP_SECRET = "";
    public static void main(String[] args) {
        Bot bot = new Bot(APP_ID, APP_SECRET);
        bot.connect();
    }

    public static Config getConfig() {
        return CONFIG;
    }
}
