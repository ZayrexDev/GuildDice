package guilddice.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import guilddice.Main;
import guilddice.bot.api.universal.ID;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public class Storage {
    @Getter
    private static final LinkedHashMap<UUID, LinkedList<ID>> universalIDs = new LinkedHashMap<>();
    private static final Path ID_ROOT = Main.DATA_ROOT.resolve("id");

    public static void load() throws IOException {
        if (!Files.exists(ID_ROOT)) return;

        Files.list(ID_ROOT).forEach(p -> {
            try {
                final String UUIDStr = p.getFileName().toString();
                final UUID uuid = UUID.fromString(UUIDStr.substring(0, UUIDStr.lastIndexOf(".json")));
                final LinkedList<ID> ids = new LinkedList<>();

                final JSONArray jsonArray = JSON.parseArray(Files.readString(p));
                for (Object o : jsonArray) {
                    final JSONObject obj = (JSONObject) o;
                    final String typeClazz = obj.getString("type");
                    final String idStr = obj.getString("id");
                    ids.add(new ID(typeClazz, idStr));
                }

                universalIDs.put(uuid, ids);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void save() throws IOException {
        if (!Files.exists(ID_ROOT)) Files.createDirectories(ID_ROOT);

        for (Map.Entry<UUID, LinkedList<ID>> r : universalIDs.entrySet()) {
            final Path p = ID_ROOT.resolve(r.getKey().toString() + ".json");

            final JSONArray arr = new JSONArray();

            for (ID id : r.getValue()) {
                final JSONObject obj = new JSONObject();
                obj.put("type", id.clazz());
                obj.put("id", id.id());
                arr.add(obj);
            }

            Files.writeString(p, arr.toString());
        }

        Files.list(ID_ROOT).forEach(p -> {
            try {
                final String UUIDStr = p.getFileName().toString();
                final UUID uuid = UUID.fromString(UUIDStr.substring(0, UUIDStr.lastIndexOf(".json")));
                final LinkedList<ID> ids = new LinkedList<>();

                final JSONArray jsonArray = JSON.parseArray(Files.readString(p));
                for (Object o : jsonArray) {
                    final JSONObject obj = (JSONObject) o;
                    final String typeClazz = obj.getString("type");
                    final String idStr = obj.getString("id");
                    ids.add(new ID(typeClazz, idStr));
                }

                universalIDs.put(uuid, ids);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
