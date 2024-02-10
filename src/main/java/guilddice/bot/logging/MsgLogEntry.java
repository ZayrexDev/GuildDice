package guilddice.bot.logging;

import guilddice.bot.OutputType;

import java.util.Objects;

public class MsgLogEntry extends LogEntry {

    public MsgLogEntry(String content,String senderName, String senderNick, String timestamp) {
        super(content,senderName, senderNick, timestamp);
    }

    @Override
    public String toString(OutputType type) {
        if (Objects.requireNonNull(type) == OutputType.HuoZi) {
            return "<" + senderNick + ">" + content;
        }
        return senderNick + " " + timestamp + "\n" +
                content;
    }

    public boolean isComment() {
        return content.startsWith("#");
    }
}
