package guilddice.bot;

import com.alibaba.fastjson2.JSONArray;
import guilddice.Main;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PlayerCharacter {
    private final LinkedHashMap<String, Integer> attrs;
    private String name;

    public PlayerCharacter(String name) {
        this.name = name;
        this.attrs = new LinkedHashMap<>();

        if (Main.DEFAULT_ATTR != null) {
            for (Map.Entry<String, Object> stringObjectEntry : Main.DEFAULT_ATTR.entrySet()) {
                attrs.put(stringObjectEntry.getKey(), (Integer) stringObjectEntry.getValue());
            }
        }
    }

    public static String getStandardName(String orig) {
        if (orig == null) return null;
        orig = orig.toUpperCase();
        for (Map.Entry<String, Object> stringObjectEntry : Main.AKA_ATTR.entrySet()) {
            if (stringObjectEntry.getKey().equals(orig)) {
                return stringObjectEntry.getKey();
            }
            final JSONArray value = (JSONArray) stringObjectEntry.getValue();
            if (value.contains(orig)) {
                return stringObjectEntry.getKey();
            }
        }

        return orig;
    }

    public String listAttr(boolean tabbed) {
        final StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, Integer> en : this.attrs.entrySet()) {
            if (tabbed) {
                if (i != 1 && i % 2 == 1) {
                    sb.append("\n");
                }
            } else {
                sb.append(" ");
            }


            if (tabbed) {
                sb.append("\t");
            }

            sb.append(en.getKey()).append(":");

            if (tabbed) {
                sb.append("\t");
            }

            sb.append(en.getValue());

//            if (i % 2 != 0) {
//                sb.append("\t");
//            }

            i++;
        }

        return sb.toString();
    }

    public int getAttr(String attrName) {
        if (hasAttr(getStandardName(attrName))) {
            return getAttrs().get(getStandardName(attrName));
        } else {
            return -1;
        }
    }

    public void setAttr(String attrName, int value) {
        getAttrs().put(getStandardName(attrName), value);
    }

    public boolean hasAttr(String attrName) {
        return getAttrs().containsKey(getStandardName(attrName));
    }

    public void modAttr(String attrName, int mod) {
        getAttrs().put(getStandardName(attrName), getAttr(getStandardName(attrName)) + mod);
    }

    public String buildName() {
        if (!hasAttr("HP") || !hasAttr("SAN")) {
            return null;
        }
        final StringBuilder builder = new StringBuilder();
        builder.append(getName()).append(" ");

        if (hasAttr("HP")) {
            builder.append("HP:").append(getAttr("HP"));
        }

        if (getAttrs().containsKey(getStandardName("SIZ")) && getAttrs().containsKey(getStandardName("CON"))) {
            builder.append("/").append(((getAttr("SIZ") + getAttr("CON")) / 10));
        }

        builder.append(" ");

        if (hasAttr("SAN")) {
            builder.append("SAN:").append(getAttr("SAN"));
        }

        if (getAttrs().containsKey(getStandardName("克苏鲁神话"))) {
            builder.append("/").append(99 - getAttr("克苏鲁神话"));
        }

        return builder.toString();
    }
}
