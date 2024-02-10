package guilddice.bot.logging;

import guilddice.bot.OutputType;
import lombok.Getter;

@Getter
public class CheckLogEntry extends LogEntry {
    private final int require;
    private final int result;
    private final String reason;
    private final String outcome;

    public CheckLogEntry(String content, String senderName, String senderNick, String timestamp, int require, int result, String reason, String outcome) {
        super(content, senderName, senderNick, timestamp);
        this.require = require;
        this.result = result;
        this.reason = reason;
        this.outcome = outcome;
    }

    @Override
    public String toString(OutputType type) {
        if (type.equals(OutputType.HuoZi)) {
            return "【投掷】(" + senderNick + " " + reason + ")1d100=" + result + "/" + require;
        }

        return senderNick + " " + timestamp + "\n" +
                "投掷 " + reason + ":" + "1d100" + "=" + result + "/" + require + "," + outcome;
    }
}
