package guilddice.bot.api.qq.network;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.qq.OPCode;
import guilddice.bot.api.qq.Payload;
import guilddice.bot.api.qq.QQBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Objects;

public class WSClient extends WebSocketClient {
    protected static final Logger LOG = LogManager.getLogger(WSClient.class);
    protected int lastS = 0;
    private HeartBeatThread heartBeatThread;

    public WSClient(QQBot bot) {
        super(URI.create("wss://api.sgroup.qq.com/websocket/"));
        this.bot = bot;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        LOG.info("已连接:{}", serverHandshake.getHttpStatus());
    }

    public void setHeartbeatInterval(int interval) {
        heartBeatThread = new HeartBeatThread(this, interval);
        heartBeatThread.setDaemon(true);
        heartBeatThread.start();
    }

    private JSONObject buildReconnectObject() {
        final JSONObject object = new JSONObject();
        object.put("token", bot.getAccessToken());
        object.put("session_id", session_id);
        object.put("seq", lastS);
        return object;
    }
    final QQBot bot;

    private String session_id = null;

    void sendPayload(Payload p) {
        final String string = p.toJSONObject().toString();
        send(string);
    }

    private JSONObject buildAuthObject() {
        final JSONObject object = new JSONObject();
        object.put("token", bot.getAccessToken());
        object.put("intents", 1 << 9 | 1 << 30);
        object.put("shards", new JSONArray(0, 1));
        return object;
    }

    @Override
    public void onMessage(String s) {
        final JSONObject object = JSONObject.parseObject(s);
        lastS = Objects.requireNonNullElse(object.getInteger("s"), 0);

        if (object.getInteger("op") == OPCode.HELLO) {
            if (session_id != null) {
                LOG.info("正在发送恢复信息...");
                sendPayload(new Payload(OPCode.RESUME, buildReconnectObject()));
            } else {
                LOG.info("正在发送鉴权信息...");
                sendPayload(new Payload(OPCode.IDENTIFY, buildAuthObject()));
            }
            setHeartbeatInterval(object.getJSONObject("d").getInteger("heartbeat_interval"));
        } else if (object.getInteger("op") == OPCode.DISPATCH) {
            if (object.getString("t").equals("MESSAGE_CREATE")) {
                bot.onMessage(object.getJSONObject("d"), false);
            } else if (object.getString("t").equals("AT_MESSAGE_CREATE")) {
                bot.onMessage(object.getJSONObject("d"), true);
            } else if (object.getString("t").equals("READY")) {
                session_id = object.getJSONObject("d").getString("session_id");
                LOG.info("连接已建立。");
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        LOG.info("连接已断开。响应码：{}，理由：{}，由远程关闭：{}", i, s, b);
        heartBeatThread.interrupt();
        LOG.info("正在尝试重连...");
        bot.connect();
    }

    @Override
    public void onError(Exception e) {
        LOG.error("连接发生错误。", e);
    }
}

final class HeartBeatThread extends Thread {
    private final WSClient client;
    private final int heartbeatInterval;

    public HeartBeatThread(WSClient client, int heartbeatInterval) {
        this.client = client;
        this.heartbeatInterval = heartbeatInterval;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(heartbeatInterval);
            } catch (InterruptedException e) {
                return;
            }
            JSONObject obj = new JSONObject();
            obj.put("op", OPCode.HEARTBEAT);
            obj.put("d", client.lastS);
            if (isInterrupted()) return;
            client.send(obj.toString());
            WSClient.LOG.debug("已发送心跳。");
        }
    }
}
