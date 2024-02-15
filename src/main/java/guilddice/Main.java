package guilddice;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.kook.KOOKBot;
import guilddice.bot.api.qq.QQBot;
import guilddice.bot.api.universal.Bot;
import guilddice.util.Config;
import guilddice.util.Storage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;

public class Main {
    public final static Path DATA_ROOT = Path.of("data");
    public final static Path CONFIG_ROOT = DATA_ROOT.resolve("config");
    public final static Path CONFIG_PATH = CONFIG_ROOT.resolve("config.yaml");
    public final static Path DEFAULT_ATTR_PATH = CONFIG_ROOT.resolve("default.json");
    public final static Path AKA_ATTR_PATH = CONFIG_ROOT.resolve("aka.json");
    public static final Path PC_ROOT = Main.DATA_ROOT.resolve("pc");
    public static final Path LOG_TEMP_ROOT = Main.DATA_ROOT.resolve("dice-logs-temp");
    public static final Path LOG_ROOT = Main.DATA_ROOT.resolve("dice-logs");
    public static final Logger LOG = LogManager.getLogger(Main.class);
    public static Config CONFIG = new Config();
    public static LinkedList<Bot> bots = new LinkedList<>();
    public static JSONObject DEFAULT_ATTR;
    public static JSONObject AKA_ATTR;

    public static void main(String[] args) {
        if (!Files.exists(CONFIG_ROOT)) {
            LOG.warn("未找到配置文件。");
            try {
                Files.createDirectories(CONFIG_ROOT);
                Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/config.yaml")), CONFIG_PATH);
                Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/aka.json")), AKA_ATTR_PATH);
                Files.copy(Objects.requireNonNull(Main.class.getResourceAsStream("/default.json")), DEFAULT_ATTR_PATH);
                LOG.info("已创建默认配置文件。请填写相关配置再允许程序~");
            } catch (IOException e) {
                LOG.error("无法创建默认配置文件！", e);
            }
            return;
        } else {
            try {
                loadConfig();
            } catch (IOException e) {
                LOG.error("无法读取配置文件，程序即将退出...", e);
                return;
            }
        }

        LOG.info("读取了 {} 个 Bot 配置。正在启动...", bots.size());

        for (Bot bot : bots) {
            new Thread(bot::connect).start();
        }

        LOG.info("启动完成。");
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static void loadConfig() throws IOException {
        CONFIG = new Config();

        Yaml yaml = new Yaml();
        final LinkedHashMap<String, Object> load = yaml.load(Files.newInputStream(CONFIG_PATH));
        CONFIG.setMasterId((String) load.get("master-id"));

        @SuppressWarnings("unchecked") final ArrayList<Object> confBots = (ArrayList<Object>) load.get("bots");
        for (var e : confBots) {
            @SuppressWarnings("unchecked") final LinkedHashMap<String, Object> bot = (LinkedHashMap<String, Object>) e;

            switch ((String) bot.get("type")) {
                case "qq" -> {
                    QQBot qqBot = new QQBot((String) bot.get("appId"), (String) bot.get("appSecret"));
                    bots.add(qqBot);
                }
                case "kook" -> {
                    KOOKBot kookBot = new KOOKBot((String) bot.get("token"));
                    bots.add(kookBot);
                }
            }
        }

        if (Files.exists(DEFAULT_ATTR_PATH)) {
            DEFAULT_ATTR = JSONObject.parseObject(Files.readString(DEFAULT_ATTR_PATH));
        } else {
            DEFAULT_ATTR = new JSONObject();
        }

        if (Files.exists(AKA_ATTR_PATH)) {
            AKA_ATTR = JSONObject.parseObject(Files.readString(AKA_ATTR_PATH));
        } else {
            AKA_ATTR = new JSONObject();
        }

        Storage.load();
    }
}
