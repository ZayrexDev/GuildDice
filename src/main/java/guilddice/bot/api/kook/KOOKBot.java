package guilddice.bot.api.kook;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.ChannelSession;
import guilddice.bot.api.kook.network.WSClient;
import guilddice.bot.api.universal.Bot;
import guilddice.bot.api.universal.ID;
import guilddice.bot.api.universal.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

public class KOOKBot extends Bot {
    protected static final Logger LOG = LogManager.getLogger(KOOKBot.class);
    private static final String gatewayUrl = "https://www.kookapp.cn/api/v3/gateway/index";
    private static final String apiRoot = "https://www.kookapp.cn";
    private static final String sendMessage = apiRoot.concat("/api/v3/message/create");
    private static final String changeNickname = apiRoot.concat("/api/v3/guild/nickname");
    private final String authStr;
    private final HashMap<String, ChannelSession> sessions = new HashMap<>();
    private WSClient wsClient;

    public KOOKBot(String token) {
        this.authStr = "Bot " + token;
    }

    @Override
    public void sendMessage(Message message, String content) {
        try {
            final KOOKMessage kookMessage = (KOOKMessage) message;
            final JSONObject obj = new JSONObject();
            obj.put("type", 9);
            obj.put("target_id", kookMessage.getTargetId());
            obj.put("content", content);
            final JSONObject reply = JSONObject.parseObject(
                    Jsoup.connect(sendMessage)
                            .method(Connection.Method.POST)
                            .header("Authorization", authStr)
                            .requestBody(obj.toString())
                            .header("Content-type", "application/json")
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute().body());

            if (reply.getInteger("code") != 0) {
                throw new RuntimeException("返回码非0(" + reply + ")");
            }
        } catch (Exception e) {
            LOG.error("发送消息失败！", e);
        }
    }

    @Override
    public void changeNickname(String groupId, ID userId, String nickname) {
        try {
            final JSONObject obj = new JSONObject();
            obj.put("guild_id", groupId);
            obj.put("nickname", nickname);
            obj.put("user_id", userId.id());
            final JSONObject reply = JSONObject.parseObject(
                    Jsoup.connect(changeNickname)
                            .method(Connection.Method.POST)
                            .header("Authorization", authStr)
                            .requestBody(obj.toString())
                            .header("Content-type", "application/json")
                            .ignoreContentType(true)
                            .ignoreHttpErrors(true)
                            .execute().body());

            if (reply.getInteger("code") != 0) {
                throw new RuntimeException("返回码非0(" + reply + ")");
            }
        } catch (Exception e) {
            LOG.error("修改昵称失败！", e);
        }
    }

    @Override
    public void connect() {
        LOG.info("正在获取网关连接地址...");
        String gateway;
        try {
            final var s = Jsoup.connect(gatewayUrl)
                    .header("Authorization", authStr)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get().body().ownText();
            gateway = JSONObject.parseObject(s).getJSONObject("data").getString("url");
            LOG.info("获取成功！");
        } catch (IOException e) {
            LOG.error("网关连接地址获取失败！", e);
            return;
        }

        LOG.info("正在连接到网关...");
        wsClient = new WSClient(URI.create(gateway), this);
        wsClient.connect();
    }

    public void reconnect() {
        LOG.info("重连中...");
        if (wsClient.isOpen()) {
            try {
                wsClient.closeBlocking();
            } catch (InterruptedException ignored) {
            }
        }

        LOG.info("正在 5 秒后重连...");

        new Thread(() -> {
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException ignored) {
            }
            connect();
        }).start();
    }

    public void onMessage(JSONObject data) {
        final KOOKMessage message = data.to(KOOKMessage.class);
        if (!message.getChannelType().equals("GROUP")) return; // 忽略非群组消息
        if (message.getAuthorId().equals("1")) return; // 忽略系统消息

        final String channelId = message.getTargetId();
        if (!sessions.containsKey(channelId)) {
            sessions.put(channelId, new ChannelSession(this, channelId));
        }
        sessions.get(channelId).handleMessage(message);
    }
}
