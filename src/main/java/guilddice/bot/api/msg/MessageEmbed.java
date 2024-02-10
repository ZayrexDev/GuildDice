package guilddice.bot.api.msg;

public record MessageEmbed(String title, String prompt, MessageEmbedThumbnail thumbnail, MessageEmbedField fields) {
}
