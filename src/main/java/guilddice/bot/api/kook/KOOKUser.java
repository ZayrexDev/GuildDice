package guilddice.bot.api.kook;

import com.alibaba.fastjson2.annotation.JSONField;
import guilddice.bot.api.universal.ID;
import guilddice.bot.api.universal.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KOOKUser extends User {
    private String username;
    private String id;
    @JSONField(name = "identify_num")
    private String identifyNum;
    private boolean online;
    private String os;
    private int status;
    private String avatar;
    private String nickname;
    @JSONField(name = "is_vip")
    private boolean vip;
    @JSONField(name = "bot")
    private boolean bot;
    @JSONField(name = "is_sys")
    private boolean sys;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public ID getID() {
        return new ID(KOOKUser.class.toString(), id);
    }
}
