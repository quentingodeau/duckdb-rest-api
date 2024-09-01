package fr.qgo.duckdbrestapi.service.defaultimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.qgo.duckdbrestapi.service.JsonConvertor;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public final class JavaObjectConvertor implements JsonConvertor<Object> {
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows
    public String toJsonStr(Object data) {
        return objectMapper.writeValueAsString(data);
    }
}
