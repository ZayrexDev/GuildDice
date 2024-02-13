package guilddice.bot.api.kook;

import com.alibaba.fastjson2.JSONObject;
import guilddice.bot.api.universal.Bot;
import guilddice.bot.api.universal.ID;
import guilddice.bot.api.universal.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import java.io.IOException;

public class KOOKBot extends Bot {
    protected static final Logger LOG = LogManager.getLogger(KOOKBot.class);
    private static final String gatewayUrl = "https://www.kookapp.cn/api/v3/gateway/index";
    private final String clientId;
    private final String clientSecret;
    private final String token;
    private final String authStr;
    private String gateway;

    public KOOKBot(String clientId, String clientSecret, String token) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.token = token;

        this.authStr = "Bot " + token;
    }

    @Override
    public void sendMessage(Message message, String content) {

    }

    @Override
    public void changeNickname(ID id, String nickname) {

    }

    @Override
    public void connect() {
        LOG.info("正在获取网关连接地址...");
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


    }
}
