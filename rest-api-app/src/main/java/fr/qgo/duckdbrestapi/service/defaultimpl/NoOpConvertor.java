package fr.qgo.duckdbrestapi.service.defaultimpl;

import fr.qgo.duckdbrestapi.service.JsonConvertor;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public final class NoOpConvertor implements JsonConvertor<String> {
    @Override
    public String toJsonStr(String data, Properties userQueryParams) {
        return data;
    }
}
