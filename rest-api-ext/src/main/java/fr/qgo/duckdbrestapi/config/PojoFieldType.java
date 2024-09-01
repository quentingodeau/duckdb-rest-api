package fr.qgo.duckdbrestapi.config;

public record PojoFieldType(String value) {
    @Override
    public String toString() {
        return value;
    }
}
