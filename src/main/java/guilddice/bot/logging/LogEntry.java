package guilddice.bot.logging;

import guilddice.bot.OutputType;
import lombok.Getter;

@Getter
public abstract class LogEntry {
    protected final String content;
    protected final String senderNick;
    protected final String senderName;
    protected final String timestamp;

    public LogEntry(String content, String senderName, String senderNick, String timestamp) {
        this.senderNick = senderNick;
        this.senderName = senderName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public abstract String toString(OutputType type);
}


