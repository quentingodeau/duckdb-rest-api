package fr.qgo.duckdbrestapi.service.defaultimpl;

import fr.qgo.duckdbrestapi.service.JsonConvertor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public final class JSONObjectConvertor implements JsonConvertor<JSONObject> {
    @Override
    public String toJsonStr(JSONObject data) {
        return data.toString();
    }
}
