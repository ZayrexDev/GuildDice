package guilddice.bot;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.qq.msg.Message;
import guilddice.bot.api.qq.network.WSClient;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Bot {
    protected static final Logger LOG = LogManager.getLogger(Bot.class);
    public final Timer accessTokenRefreshTimer = new Timer();
    private final HashMap<String, ChannelSession> sessions = new HashMap<>();
    private final String appID;
    private final String appSecret;
    @Getter
    private String accessToken = null;
    private WSClient wsClient;

    public Bot(String appID, String appSecret) {
        this.appID = appID;
        this.appSecret = appSecret;
    }

    public void connect() {
        LOG.info("即将在5秒后连接...");
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException ignored) {
        }
        LOG.info("正在获取 AccessToken ...");
        refreshAccessToken();

        if (wsClient == null) {
            LOG.info("正在连接至服务器...");
            wsClient = new WSClient(this);
            wsClient.connect();
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(5 * 1000);
                    if (wsClient.isOpen()) wsClient.closeBlocking();
                } catch (InterruptedException ignored) {
                }
                wsClient.reconnect();
            }).start();
        }
    }

    private void refreshAccessToken() {
        int tries = 5;
        while (tries-- >= 0) {
            try {
                final JSONObject args = new JSONObject();
                args.put("appId", appID);
                args.put("clientSecret", appSecret);
                final Connection.Response r = Jsoup.connect("https://bots.qq.com/app/getAppAccessToken")
                        .method(Connection.Method.POST)
                        .requestBody(args.toString())
                        .header("content-type", "application/json")
                        .ignoreContentType(true)
                        .execute();

                final JSONObject object = JSONObject.parseObject(r.body());
                accessToken = "QQBot " + object.getString("access_token");
                final int expires = object.getInteger("expires_in");
                accessTokenRefreshTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        connect();
                    }
                }, expires * 1000L);

                LOG.info("已获取 AccessToken，将在{}秒后过期并重新获取。", expires);
                return;
            } catch (IOException e) {
                LOG.warn("获取 AccessToken 失败，正在尝试重新获取...", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }

        throw new RuntimeException("无法获取 AccessToken!");
    }

    private String buildString(String endpoint) {
        return "https://api.sgroup.qq.com" + endpoint;
    }

    public void sendMessage(String content, String msgId, String channelId) {
        try {
            final JSONObject args = new JSONObject();
            args.put("content", content);
            args.put("msg_id", msgId);
            Jsoup.connect(buildString("/channels/" + channelId + "/messages"))
                    .method(Connection.Method.POST)
                    .header("Authorization", accessToken)
                    .header("X-Union-Appid", appID)
                    .requestBody(args.toString())
                    .header("content-type", "application/json")
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
        } catch (IOException e) {
            LOG.error("发送消息失败！", e);
        }
    }

    public void reply(Message message, String content) {
        sendMessage(content, message.id(), message.channel_id());
    }

    public void onMessage(JSONObject d, boolean b) {
        final String channelId = d.getString("channel_id");
        if (!sessions.containsKey(channelId)) {
            sessions.put(channelId, new ChannelSession(this, channelId));
        }
        sessions.get(channelId).handleMessage(d.to(Message.class));
    }
}
