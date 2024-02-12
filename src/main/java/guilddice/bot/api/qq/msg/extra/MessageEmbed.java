package guilddice.bot.api.qq.msg.extra;

public record MessageEmbed(String title, String prompt, MessageEmbedThumbnail thumbnail, MessageEmbedField fields) {
}
