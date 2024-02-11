package guilddice.bot.api.qq.msg;

public record MessageEmbed(String title, String prompt, MessageEmbedThumbnail thumbnail, MessageEmbedField fields) {
}
