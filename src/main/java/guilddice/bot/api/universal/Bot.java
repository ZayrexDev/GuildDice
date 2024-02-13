package guilddice.bot.api.universal;

public abstract class Bot {
    public abstract void sendMessage(Message message, String content);
    public abstract void changeNickname(ID id, String nickname);
    public abstract void connect();
}
