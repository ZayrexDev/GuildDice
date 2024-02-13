package guilddice.bot.api.qq.user;

public record QQMember(QQUser user, String nick, String[] roles, String joined_at) {
}
