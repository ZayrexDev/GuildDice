package guilddice.bot.api.kook;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import guilddice.bot.api.universal.Message;
import guilddice.bot.api.universal.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KOOKMessage extends Message {
    @JSONField(name = "channel_type")
    private String channelType;
    private int type;
    @JSONField(name = "target_id")
    private String targetId;
    @JSONField(name = "author_id")
    private String authorId;
    private String content;
    @JSONField(name = "msg_id")
    private String msgId;
    @JSONField(name = "msg_timestamp")
    private int msgTimestamp;
    private String nonce;
    private JSONObject extra;

    public KOOKMessage() {
        super();
    }

    @Override
    public boolean isBotMessage() {
        return extra.getJSONObject("author").to(KOOKUser.class).isBot();
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public User getAuthor() {
        return extra.getJSONObject("author").to(KOOKUser.class);
    }

    @Override
    public String atAuthor() {
        return "(met)" + authorId + "(met)";
    }

    @Override
    public String getTimestamp() {
        return String.valueOf(msgTimestamp);
    }

    @Override
    public String getGroupId() {
        return extra.getString("guild_id");
    }
}
