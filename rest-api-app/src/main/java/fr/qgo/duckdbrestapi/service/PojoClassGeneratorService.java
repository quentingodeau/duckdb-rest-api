package fr.qgo.duckdbrestapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.qgo.duckdbrestapi.config.PojoFieldType;
import fr.qgo.duckdbrestapi.controller.PojoCreationError;
import lombok.AllArgsConstructor;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class PojoClassGeneratorService {
    private final ByteBuddy byteBuddy;
    private final ObjectMapper objectMapper;

    public Class<?> create(String name, Map<String, PojoFieldType> definition) throws PojoCreationError {
        if (definition.isEmpty()) {
            throw new IllegalArgumentException("Cannot create POJO " + name + " without attributes");
        }

        DynamicType.Builder<Object> typeBuilder = byteBuddy
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(name);

        Implementation.Composable defaultConstructor = SuperMethodCall.INSTANCE;
        for (val pojoDef : definition.entrySet()) {
            val fieldName = pojoDef.getKey();
            val capitalizeFieldName = StringUtils.capitalize(fieldName);
            val fieldType = pojoDef.getValue();
            val pojoFieldDefinition = toType(name, fieldName, fieldType);
            val setterName = "set" + capitalizeFieldName;
            val getterName = "get" + capitalizeFieldName;

            typeBuilder = typeBuilder
                    // Field
                    .defineField(fieldName, pojoFieldDefinition.typeDefinition, Modifier.PRIVATE)
                    // Getter
                    .defineMethod(getterName, pojoFieldDefinition.typeDefinition, Modifier.PUBLIC)
                    .intercept(FieldAccessor.ofField(fieldName))
                    // Setter
                    .defineMethod(setterName, void.class, Modifier.PUBLIC)
                    .withParameter(pojoFieldDefinition.typeDefinition, "value")
                    .intercept(FieldAccessor.ofField(fieldName));

            if (pojoFieldDefinition.defaultValue != null) {
                defaultConstructor = defaultConstructor.andThen(
                        MethodCall.invoke(ElementMatchers.named(setterName)).with(pojoFieldDefinition.defaultValue)
                );
            }
        }

        typeBuilder = typeBuilder.defineConstructor(Modifier.PUBLIC)
                .intercept(defaultConstructor);

        DynamicType.Unloaded<Object> dynamicType = typeBuilder.make();
//        try {
//            dynamicType.saveIn(new File("."));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return dynamicType
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }

    private PojoFieldDefinition toType(String className, String fieldName, PojoFieldType fieldType) throws PojoCreationError {
        return tryParseType(className, fieldName, fieldType.getType(), fieldType.getDefaultValue());
    }

    private PojoFieldDefinition tryParseType(String className, String fieldName, String fieldType, String fieldDefaultValue) throws PojoCreationError {
        if (fieldType.contains("<")) {
            return tryParseGenericType(className, fieldName, fieldType.trim(), fieldDefaultValue);
        } else {
            return tryParseSimpleType(fieldType.trim(), fieldDefaultValue)
                    .orElseThrow(() -> PojoCreationError.failedToCreateField(className, fieldName, fieldType));
        }
    }


    private PojoFieldDefinition tryParseGenericType(String className, String fieldName, String fieldType, String fieldDefaultValue) throws PojoCreationError {
        val startGeneric = fieldType.indexOf('<');
        val endGeneric = fieldType.lastIndexOf('>');
        if (startGeneric < 0 || endGeneric < 0 || endGeneric < startGeneric) {
            throw new PojoCreationError(String.format("Wrong type definition in class %s field %s type: %s", className, fieldName, fieldType));
        }

        val container = fieldType.substring(0, startGeneric).toLowerCase(Locale.ENGLISH);
        val generic = fieldType.substring(startGeneric + 1, endGeneric);

        val hasDefaultValue = fieldDefaultValue != null && !fieldDefaultValue.isBlank();

        try {
            return switch (container) {
                case "list" -> new PojoFieldDefinition(
                        TypeDescription.Generic.Builder.parameterizedType(
                                        TypeDescription.ForLoadedType.of(List.class),
                                        tryParseType(className, fieldName, generic, null).typeDefinition
                                )
                                .build(),
                        hasDefaultValue ? objectMapper.readValue(fieldDefaultValue, ArrayList.class) : null
                );

                case "set" -> new PojoFieldDefinition(
                        TypeDescription.Generic.Builder.parameterizedType(
                                        TypeDescription.ForLoadedType.of(Set.class),
                                        tryParseType(className, fieldName, generic, null).typeDefinition
                                )
                                .build(),
                        hasDefaultValue ? objectMapper.readValue(fieldDefaultValue, HashSet.class) : null);

                case "map" -> {
                    String[] split = generic.split(",");
                    if (split.length != 2) throw PojoCreationError.failedToCreateField(className, fieldName, fieldType);

                    yield new PojoFieldDefinition(
                            TypeDescription.Generic.Builder.parameterizedType(
                                            TypeDescription.ForLoadedType.of(Map.class),
                                            tryParseType(className, fieldName, split[0], null).typeDefinition,
                                            tryParseType(className, fieldName, split[1], null).typeDefinition
                                    )
                                    .build(),
                            hasDefaultValue ? objectMapper.readValue(fieldDefaultValue, HashMap.class) : null);
                }

                default -> throw PojoCreationError.failedToCreateField(className, fieldName, fieldType);
            };
        } catch (JsonProcessingException e) {
            throw new PojoCreationError(String.format("Failed to create default value (%s) for field '%s' of type '%s' in class '%s'", fieldDefaultValue, fieldName, fieldType, className), e);
        }
    }


    private Optional<PojoFieldDefinition> tryParseSimpleType(String value, String defaultValue) {
        return Optional.ofNullable(switch (value) {
            case "String", "string" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(String.class),
                    defaultValue
            );

            case "int" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(int.class),
                    defaultValue == null ? null : Integer.parseInt(defaultValue)
            );
            case "Integer" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(Integer.class),
                    defaultValue == null ? null : Integer.parseInt(defaultValue)
            );

            case "long" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(long.class),
                    defaultValue == null ? null : Long.parseLong(defaultValue)
            );
            case "Long" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(Long.class),
                    defaultValue == null ? null : Long.parseLong(defaultValue)
            );

            case "double" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(double.class),
                    defaultValue == null ? null : Double.parseDouble(defaultValue)
            );
            case "Double" -> new PojoFieldDefinition(
                    TypeDescription.ForLoadedType.of(Double.class),
                    defaultValue == null ? null : Double.parseDouble(defaultValue)
            );

            default -> null;
        });
    }

    private record PojoFieldDefinition(TypeDefinition typeDefinition, Object defaultValue) {
    }
}
