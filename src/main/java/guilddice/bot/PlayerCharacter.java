package guilddice.bot;

import com.alibaba.fastjson2.JSONArray;
import guilddice.Main;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class PlayerCharacter {
    private final String name;
    private final LinkedHashMap<String, Integer> attr;

    public PlayerCharacter(String name) {
        this.name = name;
        this.attr = new LinkedHashMap<>();

        if (Main.DEFAULT_ATTR != null) {
            for (Map.Entry<String, Object> stringObjectEntry : Main.DEFAULT_ATTR.entrySet()) {
                attr.put(stringObjectEntry.getKey(), (Integer) stringObjectEntry.getValue());
            }
        }
    }

    public static String getStandardName(String orig) {
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
}
