package guilddice.bot.logging;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class Log {
    private static final Logger LOG = LogManager.getLogger(Log.class);
    private static final Path LOG_TEMP_ROOT = Path.of("dice-logs-temp");
    private final LinkedList<JSONObject> log = new LinkedList<>();
    private Path tempPath;

    public Log(String channelId) {
        try {
            Files.createDirectories(LOG_TEMP_ROOT);
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd.HH_mm_ss");
            final String format = simpleDateFormat.format(new Date());
            this.tempPath = LOG_TEMP_ROOT.resolve(channelId + "-tmp-" + format + ".dlog");
            if (!Files.exists(this.tempPath)) {
                Files.createFile(this.tempPath);
            }
        } catch (IOException e) {
            LOG.error("无法创建临时文件目录！", e);
            this.tempPath = null;
        }
    }

    public void attach(LogEntry logEntry) {
        if (logEntry == null) throw new RuntimeException("日志记录为空");
        final JSONObject obj = new JSONObject();
        switch (logEntry) {
            case MsgLogEntry ignored -> obj.put("type", "Msg");
            case RollLogEntry ignored -> obj.put("type", "Roll");
            case CheckLogEntry ignored -> obj.put("type", "Check");
            default -> {
                obj.put("type", "Unknown");
                LOG.warn("未知的日志类型:" + logEntry.getClass());
            }
        }

        obj.put("data", JSONObject.from(logEntry));

        log.add(obj);

        try {
            if (tempPath != null) {
                Files.writeString(tempPath, obj + ",\n", StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            LOG.error("日志临时文件保存失败！", e);
        }
    }

    public void save(Path path) throws IOException {
        JSONArray arr = new JSONArray();
        arr.addAll(log);
        Files.writeString(path, arr.toString());
    }
}
