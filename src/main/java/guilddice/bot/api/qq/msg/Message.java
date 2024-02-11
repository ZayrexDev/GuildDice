package guilddice.bot.api.qq.msg;

import guilddice.bot.api.qq.Member;
import guilddice.bot.api.qq.User;

public record Message(String id, String channel_id, String guild_id, String content, String timestamp,
                      String edited_timestamp, boolean mention_everyone, User author, MessageAttachment attachments,
                      MessageEmbed embeds, User mentions, Member member, MessageArk ark, int seq, String seq_in_channel,
                      MessageReference message_reference) {
}
