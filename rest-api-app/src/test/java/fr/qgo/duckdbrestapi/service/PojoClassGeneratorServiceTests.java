package fr.qgo.duckdbrestapi.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import fr.qgo.duckdbrestapi.config.PojoFieldType;
import fr.qgo.duckdbrestapi.controller.PojoCreationError;
import net.bytebuddy.ByteBuddy;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PojoClassGeneratorServiceTests {
    private final PojoClassGeneratorService generatorService = new PojoClassGeneratorService(new ByteBuddy(), new ObjectMapper());

    @Test
    void simpleString() throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> clazz = generatorService.create("TestClass1", Map.of(
                "field1", new PojoFieldType("string")
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        set(newInstance, "field1", "hello world", String.class);
        assertThat((Object) get(newInstance, "field1")).isEqualTo("hello world");
    }


    @Test
    void simpleStringDefault() throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PojoFieldType pojoFieldType = new PojoFieldType("string");
        pojoFieldType.setDefaultValue("greetings");
        Class<?> clazz = generatorService.create("TestClass2", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((Object) get(newInstance, "field1")).isEqualTo("greetings");
    }

    @Test
    void listString()throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException  {
        PojoFieldType pojoFieldType = new PojoFieldType("list<string>");
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
        PojoFieldType pojoFieldType = new PojoFieldType("list<string>");
        pojoFieldType.setDefaultValue("[]");
        Class<?> clazz = generatorService.create("TestClass4", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((List<?>) get(newInstance, "field1")).isNotNull().isEmpty();
    }

    @Test
    void listStringWithDefault2()throws PojoCreationError, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException  {
        PojoFieldType pojoFieldType = new PojoFieldType("list<string>");
        pojoFieldType.setDefaultValue("[\"greetings\"]");
        Class<?> clazz = generatorService.create("TestClass5", Map.of(
                "field1", pojoFieldType
        ));

        Object newInstance = clazz.getDeclaredConstructor().newInstance();
        assertThat((List<String>) get(newInstance, "field1"))
                .isNotNull()
                .isNotEmpty()
                .containsExactly("greetings");
    }

    @SuppressWarnings("Unchecked")
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
