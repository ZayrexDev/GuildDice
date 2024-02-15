package guilddice.bot.api.universal;

public abstract class Bot {
    public abstract void sendMessage(Message message, String content);
    public abstract void changeNickname(String groupId, ID userId, String nickname);
    public abstract void connect();
}
