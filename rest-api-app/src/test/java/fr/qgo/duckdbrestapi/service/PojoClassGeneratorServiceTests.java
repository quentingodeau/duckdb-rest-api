package fr.qgo.duckdbrestapi.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import fr.qgo.duckdbrestapi.config.PojoFieldType;
import fr.qgo.duckdbrestapi.controller.PojoCreationError;
import net.bytebuddy.ByteBuddy;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PojoClassGeneratorServiceTests {
    private final PojoClassGeneratorService generatorService = new PojoClassGeneratorService(new ByteBuddy(), new ObjectMapper());

    @Test
    void simpleString() throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = generatorService.create("TestClass1", Map.of(
                "field1", makePojoField("string")
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        set(newInstance, "field1", "hello world", String.class);
        assertThat((Object) get(newInstance, "field1")).isEqualTo("hello world");
    }


    @Test
    void simpleStringDefault() throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PojoFieldType pojoFieldType = makePojoField("string", "greetings");
        Class<?> clazz = generatorService.create("TestClass2", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((Object) get(newInstance, "field1")).isEqualTo("greetings");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listString()throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException  {
        PojoFieldType pojoFieldType = makePojoField("list<string>");
        Class<?> clazz = generatorService.create("TestClass3", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((List<?>) get(newInstance, "field1")).isNull();
        set(newInstance, "field1", List.of("hello", "world"), List.class);
        assertThat((List<String>) get(newInstance, "field1")).containsExactly("hello", "world");
    }

    @Test
    void listStringWithDefault1()throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException  {
        PojoFieldType pojoFieldType = makePojoField("list<string>", "[]");
        Class<?> clazz = generatorService.create("TestClass4", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((List<?>) get(newInstance, "field1")).isNotNull().isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listStringWithDefault2()throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException  {
        PojoFieldType pojoFieldType = makePojoField("list<string>", "[\"greetings\"]");
        Class<?> clazz = generatorService.create("TestClass5", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((List<String>) get(newInstance, "field1"))
                .isNotNull()
                .isNotEmpty()
                .containsExactly("greetings");
    }


    @Test
    @SuppressWarnings("unchecked")
    void complexStruct() throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchFieldException {
        Map<String, PojoFieldType> definition = new java.util.HashMap<>();
        definition.put("pri1", makePojoField("Integer"));
        definition.put("pri2", makePojoField("int", "5"));
        definition.put("pri3", makePojoField("Long", "8"));
        definition.put("pri4", makePojoField("long"));
        definition.put("pri5", makePojoField("double", "8.56"));
        definition.put("pri6", makePojoField("Double"));

        definition.put("col1", makePojoField("list<string>"));
        definition.put("col2", makePojoField("list<string>", "[\"greetings\"]"));
        definition.put("col3", makePojoField("set<Integer>"));
        definition.put("col4", makePojoField("set<Integer>", "[1,2,3]"));
        definition.put("col5", makePojoField("map<String, Integer>"));
        definition.put("col6", makePojoField("map<String, Integer>", "{\"value\": 3}"));
        Class<?> clazz = generatorService.create("TestClass6", definition);

        Object newInstance = clazz.getDeclaredConstructor().newInstance();

        assertThat(fieldType(newInstance, "pri1")).isEqualTo("java.lang.Integer");
        assertThat((Integer) get(newInstance, "pri1")).isNull();

        assertThat(fieldType(newInstance, "pri2")).isEqualTo("int");
        assertThat((int) get(newInstance, "pri2")).isEqualTo(5);

        assertThat(fieldType(newInstance, "pri3")).isEqualTo("java.lang.Long");
        assertThat((Long) get(newInstance, "pri3")).isEqualTo(8);


        assertThat(fieldType(newInstance, "pri4")).isEqualTo("long");
        assertThat((long) get(newInstance, "pri4")).isEqualTo(0);


        assertThat(fieldType(newInstance, "pri5")).isEqualTo("double");
        assertThat((double) get(newInstance, "pri5")).isEqualTo(8.56);


        assertThat(fieldType(newInstance, "pri6")).isEqualTo("java.lang.Double");
        assertThat((Double) get(newInstance, "pri6")).isNull();

        assertThat(fieldType(newInstance, "col1")).isEqualTo("java.util.List<java.lang.String>");
        assertThat((List<String>) get(newInstance, "col1")).isNull();

        assertThat(fieldType(newInstance, "col2")).isEqualTo("java.util.List<java.lang.String>");
        assertThat((List<String>) get(newInstance, "col2")).isNotNull().containsExactly("greetings");

        assertThat(fieldType(newInstance, "col3")).isEqualTo("java.util.Set<java.lang.Integer>");
        assertThat((Set<Integer>) get(newInstance, "col3")).isNull();

        assertThat(fieldType(newInstance, "col4")).isEqualTo("java.util.Set<java.lang.Integer>");
        assertThat((Set<Integer>) get(newInstance, "col4")).isNotNull().containsExactly(1,2 ,3);

        assertThat(fieldType(newInstance, "col5")).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
        assertThat((Map<String, Integer>) get(newInstance, "col5")).isNull();

        assertThat(fieldType(newInstance, "col6")).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
        assertThat((Map<String, Integer>) get(newInstance, "col6")).isNotNull().isEqualTo(Map.of( "value", 3));
    }

    private static @NotNull PojoFieldType makePojoField(String type) {
        return makePojoField(type, null);
    }


    private static @NotNull PojoFieldType makePojoField(String type, String defaultValue) {
        PojoFieldType pojoFieldType = new PojoFieldType(type);
        pojoFieldType.setDefaultValue(defaultValue);
        return pojoFieldType;
    }

    private String fieldType(@NotNull Object o, @NotNull String fieldName) throws NoSuchFieldException {
        return o.getClass().getDeclaredField(fieldName).getGenericType().getTypeName();
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object o, String fieldName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (T) o.getClass()
                .getDeclaredMethod("get" + StringUtils.capitalize(fieldName))
                .invoke(o);
    }

    private static <T> void set(Object o, String fieldName, T value, Class<T> type) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        o.getClass()
                .getDeclaredMethod("set" + StringUtils.capitalize(fieldName), type)
                .invoke(o, value);
    }
}
