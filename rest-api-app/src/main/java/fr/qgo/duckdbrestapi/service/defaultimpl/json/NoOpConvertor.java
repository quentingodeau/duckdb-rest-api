package fr.qgo.duckdbrestapi.service.defaultimpl.json;

import fr.qgo.duckdbrestapi.service.JsonConvertor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public final class NoOpConvertor implements JsonConvertor<String> {
    @Override
    public String toJsonStr(String data, Map<String, Object> userQueryParams) {
        return data;
    }
}
