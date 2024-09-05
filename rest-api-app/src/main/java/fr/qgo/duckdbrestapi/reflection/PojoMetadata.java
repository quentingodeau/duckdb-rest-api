package fr.qgo.duckdbrestapi.reflection;

import fr.qgo.duckdbrestapi.exception.InvalidPojo;
import jakarta.persistence.Column;
import lombok.Getter;
import org.sql2o.tools.UnderscoreToCamelCase;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PojoMetadata {

    private static final Map<Class<?>, Map<String, Field>> caseSensitiveFalse = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, Field>> caseSensitiveTrue = new ConcurrentHashMap<>();

    @Getter
    private final Class<?> clazz;
    @Getter
    private final boolean caseSensitive;
    @Getter
    private final boolean autoDeriveColumnNames;
    @Getter
    private final Map<String, String> columnMappings;

    private final Map<String, Field> propertyInfo;

    public PojoMetadata(Class<?> clazz, boolean caseSensitive, boolean autoDeriveColumnNames, Map<String, String> columnMappings) {
        this.clazz = clazz;
        this.caseSensitive = caseSensitive;
        this.autoDeriveColumnNames = autoDeriveColumnNames;
        this.columnMappings = columnMappings == null ? Collections.emptyMap() : columnMappings;

        this.propertyInfo = getFields(clazz, caseSensitive);

    }

    public Object newInstance() {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException e) {
            throw new InvalidPojo("Is " + clazz + " has a default constructor and is not abstract", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new InvalidPojo("Cannot create new instance of " + clazz, e);
        }
    }

    private static Map<String, Field> getFields(Class<?> clazz, boolean caseSensitive) {
        return (caseSensitive
                ? caseSensitiveTrue
                : caseSensitiveFalse)
                .computeIfAbsent(clazz, (c) -> initializePropertyInfo(c, caseSensitive));
    }

    private static Map<String, Field> initializePropertyInfo(Class<?> clazz, boolean caseSensitive) {
        Map<String, Field> fields = new HashMap<>();

        boolean isJpaColumnInClasspath = false;
        try {
            Class.forName("jakarta.persistence.Column");
            isJpaColumnInClasspath = true;
        } catch (ClassNotFoundException e) {
            // jakarta.persistence.Column is not in the classpath
        }

        Class<?> theClass = clazz;
        do {
            for (Field f : theClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                String propertyName = readAnnotatedColumnName(f, isJpaColumnInClasspath);
                if (propertyName == null) {
                    propertyName = f.getName();
                }
                propertyName = caseSensitive ? propertyName : propertyName.toLowerCase();

                f.setAccessible(true);
                fields.put(propertyName, f);
            }
            theClass = theClass.getSuperclass();
        } while (!theClass.equals(Object.class));

        return fields;
    }


    public Field getFieldIfExists(String propertyName) {

        String name = this.caseSensitive ? propertyName : propertyName.toLowerCase();

        if (this.columnMappings.containsKey(name)) {
            name = this.columnMappings.get(name);
        }

        if (autoDeriveColumnNames) {
            name = UnderscoreToCamelCase.convert(name);
            if (!this.caseSensitive) name = name.toLowerCase();
        }

        return propertyInfo.get(name);
    }

    /**
     * Try to read the {@link jakarta.persistence.Column} annotation and return the name of the column.
     * Returns null if no {@link jakarta.persistence.Column} annotation is present or if the name of the column is empty
     */
    private static String readAnnotatedColumnName(AnnotatedElement classMember, boolean isJpaColumnInClasspath) {
        if (isJpaColumnInClasspath) {
            Column columnInformation = classMember.getAnnotation(Column.class);
            if (columnInformation != null && columnInformation.name() != null && !columnInformation.name().isEmpty()) {
                return columnInformation.name();
            }
        }
        return null;
    }
}
