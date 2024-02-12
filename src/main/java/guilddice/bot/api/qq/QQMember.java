package guilddice.bot.api.qq;

import java.util.Arrays;

public record QQMember(QQUser user, String nick, String[] roles, String joined_at) {
    public boolean hasRole(String roleId) {
        return Arrays.asList(roles).contains(roleId);
    }
}
