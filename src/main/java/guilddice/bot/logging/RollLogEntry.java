package guilddice.bot.logging;

import guilddice.bot.OutputType;
import lombok.Getter;

@Getter
public class RollLogEntry extends LogEntry {
    private final String diceExpr;
    private final String reason;
    private final String result;
    private final String outcome;

    public RollLogEntry(String content, String senderName, String senderNick, String timestamp, String reason, String diceExpr, String result, String outcome) {
        super(content, senderName, senderNick, timestamp);
        this.diceExpr = diceExpr;
        this.reason = reason;
        this.result = result;
        this.outcome = outcome;
    }

    @Override
    public String toString(OutputType type) {
        if (type.equals(OutputType.HuoZi)) {
            final StringBuilder s = new StringBuilder();
            s.append("【骰子】(").append(senderNick).append(" ").append(reason).append(")");
            if (outcome != null && !outcome.isEmpty()) s.append("[").append(outcome).append("]");
            s.append(diceExpr).append("=").append(result);
            return s.toString();
        }

        return senderNick + " " + timestamp + "\n" +
                "投掷 " + reason + ":" + diceExpr + "=" + result + " " + outcome;
    }
}
