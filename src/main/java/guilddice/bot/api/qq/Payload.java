package guilddice.bot.api.qq;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.Getter;
import lombok.Setter;

@Setter @Getter
public class Payload {
    private int operationCode;

    private JSONObject data;

    private Integer serialNum;

    private String eventType;

    public Payload(int operationCode, JSONObject data, int serialNum, String eventType) {
        this.operationCode = operationCode;
        this.data = data;
        this.serialNum = serialNum;
        this.eventType = eventType;
    }

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
