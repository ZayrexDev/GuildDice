package guilddice;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.qq.QQBot;
import guilddice.bot.api.universal.Bot;
import guilddice.util.Config;
import guilddice.util.Storage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public static Config CONFIG = new Config();
    public static JSONObject DEFAULT_ATTR;
    public static JSONObject AKA_ATTR;
    public static final Logger LOG = LogManager.getLogger(Main.class);

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

        Bot bot = new QQBot(CONFIG.getAppId(), CONFIG.getAppSecret());

        bot.connect();
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static void loadConfig() throws IOException {
        Yaml yaml = new Yaml();
        InputStream is = Files.newInputStream(CONFIG_PATH);
        CONFIG = yaml.loadAs(is, Config.class);

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
