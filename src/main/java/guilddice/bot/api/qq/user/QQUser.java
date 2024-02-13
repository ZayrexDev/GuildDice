package guilddice.bot.api.qq.user;

import guilddice.bot.api.universal.ID;
import guilddice.bot.api.universal.User;

import java.util.Objects;

@SuppressWarnings("unused")
public final class QQUser extends User {
    private final String id;
    private final String username;
    private final String avatar;
    private final boolean bot;

    public QQUser(String id, String username, String avatar, boolean bot) {
        this.id = id;
        this.username = username;
        this.avatar = avatar;
        this.bot = bot;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String avatar() {
        return avatar;
    }

    public boolean bot() {
        return bot;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QQUser) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.username, that.username) &&
                Objects.equals(this.avatar, that.avatar) &&
                this.bot == that.bot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, avatar, bot);
    }

    @Override
    public String toString() {
        return "QQUser[" +
                "id=" + id + ", " +
                "username=" + username + ", " +
                "avatar=" + avatar + ", " +
                "bot=" + bot + ']';
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public ID getID() {
        return new ID(QQUser.class.toString(), id);
    }
}
