package guilddice.bot.api.kook.network;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.kook.KOOKBot;
import guilddice.util.ZLibUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;

public class WSClient extends WebSocketClient {
    protected static final Logger LOG = LogManager.getLogger(WSClient.class);
    protected final KOOKBot bot;
    protected int sn;
    protected long heartbeatTime;
    protected boolean heartbeatReplied = false;
    private Thread heartBeatThread;
    private long openTime;

    public WSClient(URI serverUri, KOOKBot bot) {
        super(serverUri);
        this.bot = bot;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        openTime = System.currentTimeMillis();
        LOG.info("正在建立连接 {}: {}", serverHandshake.getHttpStatus(), serverHandshake.getHttpStatusMessage());
    }

    @Override
    public void onMessage(String s) {
        if (s == null || s.isBlank()) return;

        final JSONObject obj = JSONObject.parseObject(s);

        switch (obj.getInteger("s")) {
            case Signals.EVENT -> {
                this.sn = obj.getInteger("sn");
                bot.onMessage(obj.getJSONObject("d"));
            }
            case Signals.HELLO -> {
                LOG.info("收到服务器回复，用时 {} 毫秒。", System.currentTimeMillis() - openTime);
                heartBeatThread = new HeartBeatThread(this);
                heartBeatThread.start();
                LOG.info("连接已建立。");
            }
            case Signals.PONG -> {
                LOG.debug("收到心跳回复，用时 {} 毫秒。", System.currentTimeMillis() - heartbeatTime);
                heartbeatReplied = true;
            }
            case Signals.RECONNECT -> {
                LOG.info("服务器要求重连。");
                bot.reconnect();
            }
            case Signals.RESUME_ACK -> LOG.info("消息恢复成功，离线消息已经全部接受。");
            default -> LOG.warn("收到不明信息: {}", s);
        }
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            this.onMessage(ZLibUtil.decompressString(bytes.array()));
        } catch (DataFormatException | IOException e) {
            LOG.error("解压缩消息失败！", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean byRemote) {
        LOG.info("连接已断开。响应码：{}，理由：{}，由远程关闭：{}", code, reason, byRemote);
        heartBeatThread.interrupt();
        if (byRemote) {
            LOG.info("正在尝试重连...");
            bot.reconnect();
        }
    }

    @Override
    public void onError(Exception e) {
        LOG.error("连接遇到错误！", e);
    }
}

final class HeartBeatThread extends Thread {
    private final WSClient client;

    public HeartBeatThread(WSClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                //noinspection BusyWait
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                return;
            }
            JSONObject obj = new JSONObject();
            obj.put("s", Signals.PING);
            obj.put("sn", client.sn);
            if (isInterrupted()) return;

            client.heartbeatTime = System.currentTimeMillis();
            client.heartbeatReplied = false;

            client.send(obj.toString());

            WSClient.LOG.debug("已发送心跳。");
        }
    }
}
