package guilddice.bot.api.universal;

import guilddice.util.Storage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ID(String clazz, String id) {
    public UUID getUniversalID() {
        for (Map.Entry<UUID, LinkedList<ID>> entry : Storage.getUniversalIDs().entrySet()) {
            if (entry.getValue().contains(this)) return entry.getKey();
        }
        final UUID key = UUID.randomUUID();
        Storage.getUniversalIDs().put(key, new LinkedList<>(List.of(this)));

        try {
            Storage.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return key;
    }
}
