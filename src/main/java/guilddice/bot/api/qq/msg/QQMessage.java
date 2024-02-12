package guilddice.bot.api.qq.msg;

import guilddice.bot.api.universal.Message;
import guilddice.bot.api.qq.QQMember;
import guilddice.bot.api.qq.QQUser;
import guilddice.bot.api.universal.User;
import guilddice.bot.api.qq.msg.extra.MessageArk;
import guilddice.bot.api.qq.msg.extra.MessageAttachment;
import guilddice.bot.api.qq.msg.extra.MessageEmbed;
import guilddice.bot.api.qq.msg.extra.MessageReference;

import java.util.Objects;

public final class QQMessage extends Message {
    private final String id;
    private final String channel_id;
    private final String guild_id;
    private final String content;
    private final String timestamp;
    private final String edited_timestamp;
    private final boolean mention_everyone;
    private final QQUser author;
    private final MessageAttachment attachments;
    private final MessageEmbed embeds;
    private final QQUser mentions;
    private final QQMember member;
    private final MessageArk ark;
    private final int seq;
    private final String seq_in_channel;
    private final MessageReference message_reference;

    public QQMessage(String id, String channel_id, String guild_id, String content, String timestamp,
                     String edited_timestamp, boolean mention_everyone, QQUser author, MessageAttachment attachments,
                     MessageEmbed embeds, QQUser mentions, QQMember member, MessageArk ark, int seq, String seq_in_channel,
                     MessageReference message_reference) {
        this.id = id;
        this.channel_id = channel_id;
        this.guild_id = guild_id;
        this.content = content;
        this.timestamp = timestamp;
        this.edited_timestamp = edited_timestamp;
        this.mention_everyone = mention_everyone;
        this.author = author;
        this.attachments = attachments;
        this.embeds = embeds;
        this.mentions = mentions;
        this.member = member;
        this.ark = ark;
        this.seq = seq;
        this.seq_in_channel = seq_in_channel;
        this.message_reference = message_reference;
    }

    public String id() {
        return id;
    }

    public String channel_id() {
        return channel_id;
    }

    public String guild_id() {
        return guild_id;
    }

    public String content() {
        return content;
    }

    public String timestamp() {
        return timestamp;
    }

    public String edited_timestamp() {
        return edited_timestamp;
    }

    public boolean mention_everyone() {
        return mention_everyone;
    }

    public QQUser author() {
        return author;
    }

    public MessageAttachment attachments() {
        return attachments;
    }

    public MessageEmbed embeds() {
        return embeds;
    }

    public QQUser mentions() {
        return mentions;
    }

    public QQMember member() {
        return member;
    }

    public MessageArk ark() {
        return ark;
    }

    public int seq() {
        return seq;
    }

    public String seq_in_channel() {
        return seq_in_channel;
    }

    public MessageReference message_reference() {
        return message_reference;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (QQMessage) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.channel_id, that.channel_id) &&
                Objects.equals(this.guild_id, that.guild_id) &&
                Objects.equals(this.content, that.content) &&
                Objects.equals(this.timestamp, that.timestamp) &&
                Objects.equals(this.edited_timestamp, that.edited_timestamp) &&
                this.mention_everyone == that.mention_everyone &&
                Objects.equals(this.author, that.author) &&
                Objects.equals(this.attachments, that.attachments) &&
                Objects.equals(this.embeds, that.embeds) &&
                Objects.equals(this.mentions, that.mentions) &&
                Objects.equals(this.member, that.member) &&
                Objects.equals(this.ark, that.ark) &&
                this.seq == that.seq &&
                Objects.equals(this.seq_in_channel, that.seq_in_channel) &&
                Objects.equals(this.message_reference, that.message_reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channel_id, guild_id, content, timestamp, edited_timestamp, mention_everyone, author, attachments, embeds, mentions, member, ark, seq, seq_in_channel, message_reference);
    }

    @Override
    public String toString() {
        return "QQMessage[" +
                "id=" + id + ", " +
                "channel_id=" + channel_id + ", " +
                "guild_id=" + guild_id + ", " +
                "content=" + content + ", " +
                "timestamp=" + timestamp + ", " +
                "edited_timestamp=" + edited_timestamp + ", " +
                "mention_everyone=" + mention_everyone + ", " +
                "author=" + author + ", " +
                "attachments=" + attachments + ", " +
                "embeds=" + embeds + ", " +
                "mentions=" + mentions + ", " +
                "member=" + member + ", " +
                "ark=" + ark + ", " +
                "seq=" + seq + ", " +
                "seq_in_channel=" + seq_in_channel + ", " +
                "message_reference=" + message_reference + ']';
    }

    @Override
    public boolean isBotMessage() {
        return author.bot();
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public User getAuthor() {
        return author;
    }

    @Override
    public String atAuthor() {
        return "<@!" + author.id() + ">";
    }

    @Override
    public String getTimestamp() {
        return timestamp;
    }
}
