package fr.qgo.duckdbrestapi.service.defaultimpl.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.qgo.duckdbrestapi.service.JsonConvertor;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public final class ObjectConvertor implements JsonConvertor<Object> {
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public String toJsonStr(Object data, Map<String, Object> userQueryParams) {
        return objectMapper.writeValueAsString(data);
    }
}
