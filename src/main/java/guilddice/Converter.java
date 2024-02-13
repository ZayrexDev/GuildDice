package guilddice;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.OutputType;
import guilddice.bot.logging.CheckLogEntry;
import guilddice.bot.logging.LogEntry;
import guilddice.bot.logging.MsgLogEntry;
import guilddice.bot.logging.RollLogEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Converter {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) return;
        final Path output = Path.of("output.txt");
        if (Files.exists(output)) Files.delete(output);
        Files.createFile(output);
        final OutputType type;

        if (args[1].equals("HuoZi")) {
            type = OutputType.HuoZi;
        } else {
            type = OutputType.PlainText;
        }

        final JSONArray arr = JSON.parseArray(Files.newInputStream(Path.of(args[0])));

        for (int i = 1, arrSize = arr.size(); i <= arrSize; i++) {
            System.out.println("正在转换:" + i + "/" + arrSize);
            Object o1 = arr.get(i - 1);
            final JSONObject obj = (JSONObject) o1;
            LogEntry logEntry;
            if (obj.getString("type").equals("Msg")) {
                logEntry = obj.getJSONObject("data").to(MsgLogEntry.class);
                if(((MsgLogEntry)logEntry).isComment()) continue;
            } else if (obj.getString("type").equals("Roll")) {
                logEntry = obj.getJSONObject("data").to(RollLogEntry.class);
            } else if (obj.getString("type").equals("Check")) {
                logEntry = obj.getJSONObject("data").to(CheckLogEntry.class);
            } else {
                logEntry = obj.getJSONObject("data").to(LogEntry.class);
            }

            Files.writeString(output, logEntry.toString(type).concat("\n"), StandardOpenOption.APPEND);
        }

        System.out.println("转换完成！");
    }
}
