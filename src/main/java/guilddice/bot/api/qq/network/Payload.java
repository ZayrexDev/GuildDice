package guilddice.bot.api.qq.network;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter @AllArgsConstructor
public class Payload {
    private int operationCode;

    private JSONObject data;

    private Integer serialNum;

    private String eventType;

    public Payload(int operationCode, JSONObject data) {
        this.operationCode = operationCode;
        this.data = data;
        this.serialNum = null;
        this.eventType = null;
    }

    public JSONObject toJSONObject() {
        JSONObject j = new JSONObject();
        j.put("op", operationCode);
        j.put("d", data);
        if(serialNum != null)
            j.put("d", serialNum);
        if(eventType != null)
            j.put("t", eventType);
        return j;
    }

    @Override
    public String toString() {
        return toJSONObject().toString(JSONWriter.Feature.PrettyFormat);
    }
}
