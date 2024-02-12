package guilddice.bot.api.universal;

public abstract class Message {
    public abstract boolean isBotMessage();
    public abstract String getContent();
    public abstract User getAuthor();
    public abstract String atAuthor();
    public abstract String getTimestamp();
}
