package guilddice.util;

public class MessageBuilder {
    private final StringBuilder builder = new StringBuilder();
    public static MessageBuilder newInstance() {
        return new MessageBuilder();
    }

    public MessageBuilder append(String str) {
        builder.append(str);
        return this;
    }

    public MessageBuilder at(String id) {
        builder.append("<@!").append(id).append(">");
        return this;
    }

    public String build() {
        return builder.toString();
    }
}
