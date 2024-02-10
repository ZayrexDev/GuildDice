package guilddice.bot.api;

import java.util.Arrays;

public record Member(User user, String nick, String[] roles, String joined_at) {
    public boolean hasRole(String roleId) {
        return Arrays.asList(roles).contains(roleId);
    }
}
