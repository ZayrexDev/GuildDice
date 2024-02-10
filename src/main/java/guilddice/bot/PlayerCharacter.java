package guilddice.bot;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
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

        final JSONObject object = JSON.parseObject(this.getClass().getResource("/default.json"));
        if (object != null) {
            for (Map.Entry<String, Object> stringObjectEntry : object.entrySet()) {
                attr.put(stringObjectEntry.getKey(), (Integer) stringObjectEntry.getValue());
            }
        }
    }
}
