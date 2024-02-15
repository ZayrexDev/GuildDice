package guilddice.util;

import lombok.Getter;

import java.util.LinkedList;
import java.util.UUID;

@Getter
public class BindRequest {
    @Getter
    private static final LinkedList<BindRequest> bindRequests = new LinkedList<>();
    private final UUID authorUuid;
    private final UUID targetUuid;
    private final long startTime;

    public BindRequest(UUID authorUuid, UUID targetUuid) {
        this.authorUuid = authorUuid;
        this.targetUuid = targetUuid;
        startTime = System.currentTimeMillis();
    }

    public static void clearExpired() {
        bindRequests.removeIf(br -> System.currentTimeMillis() - br.getStartTime() > 10 * 60 * 1000);

    }

    public static void startRequest(UUID authorUuid, UUID targetUuid) {
        bindRequests.removeIf(br -> br.getAuthorUuid().equals(authorUuid) || br.getTargetUuid().equals(authorUuid) || System.currentTimeMillis() - br.getStartTime() > 10 * 60 * 1000);
        bindRequests.add(new BindRequest(authorUuid, targetUuid));
    }
}
