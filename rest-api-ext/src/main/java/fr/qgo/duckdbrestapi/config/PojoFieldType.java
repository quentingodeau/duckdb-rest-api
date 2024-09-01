package fr.qgo.duckdbrestapi.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PojoFieldType {
    private String type;
    private String defaultValue = null;

    public PojoFieldType(String type) {
        this.type = type;
    }
}
