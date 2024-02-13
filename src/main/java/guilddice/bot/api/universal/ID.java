package guilddice.bot.api.universal;

import guilddice.util.Storage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ID(String clazz, String id) {
    private static final Logger LOG = LogManager.getLogger(ID.class);
    public UUID asUuid() {
        for (Map.Entry<UUID, LinkedList<ID>> entry : Storage.getUniversalIDs().entrySet()) {
            if (entry.getValue().contains(this)) return entry.getKey();
        }
        UUID key;
        do {
            key = UUID.randomUUID();
        } while (Storage.getUniversalIDs().containsKey(key));
        LOG.info("为id为 {} 的用户创建了新的UUID {}", this, key);
        Storage.getUniversalIDs().put(key, new LinkedList<>(List.of(this)));

        try {
            Storage.save();
        } catch (IOException e) {
            LOG.error("无法保存ID信息", e);
        }
        return key;
    }
}
