package fr.qgo.duckdbrestapi.service;

import fr.qgo.duckdbrestapi.config.PojoFieldType;
import fr.qgo.duckdbrestapi.controller.PojoCreationError;
import lombok.AllArgsConstructor;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class PojoClassGeneratorService {
    private final ByteBuddy byteBuddy;

    public Class<?> create(String name, Map<String, PojoFieldType> definition) throws PojoCreationError {
        if (definition.isEmpty()) {
            throw new IllegalArgumentException("Cannot create POJO " + name + " without attributes");
        }

        DynamicType.Builder<Object> typeBuilder = byteBuddy
                .subclass(Object.class)
                .name(name);

        for (val pojoDef : definition.entrySet()) {
            val fieldName = pojoDef.getKey();
            val capitalizeFieldName = StringUtils.capitalize(fieldName);
            val fieldType = pojoDef.getValue();
            val type = toType(name, fieldName, fieldType);

            typeBuilder = typeBuilder.defineField(fieldName, type, Modifier.PRIVATE)
                    .defineMethod("get" + capitalizeFieldName, type, Modifier.PUBLIC)
                    .intercept(FieldAccessor.ofField(fieldName))
                    .defineMethod("set" + capitalizeFieldName, void.class, Modifier.PUBLIC)
                    .withParameter(type, "value")
                    .intercept(FieldAccessor.ofField(fieldName));
        }
        return typeBuilder.make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }

    private TypeDefinition toType(String className, String fieldName, PojoFieldType fieldType) throws PojoCreationError {
        return tryParseType(className, fieldName, fieldType, fieldType.value());
    }

    private TypeDefinition tryParseType(String className, String fieldName, PojoFieldType fieldType, String valueToParse) throws PojoCreationError {
        if (valueToParse.contains("<")) {
            return tryParseGenericType(className, fieldName, fieldType, valueToParse.trim());
        } else {
            return tryParseSimpleType(valueToParse.trim())
                    .orElseThrow(() -> PojoCreationError.failedToCreateField(className, fieldName, fieldType));
        }
    }


    private TypeDefinition tryParseGenericType(String className, String fieldName, PojoFieldType fieldType, String value) throws PojoCreationError {
        val startGeneric = value.indexOf('<');
        val endGeneric = value.lastIndexOf('>');
        val container = value.substring(0, startGeneric).toLowerCase(Locale.ENGLISH);
        val generic = value.substring(startGeneric + 1, endGeneric);

        return switch (container) {
            case "list" -> TypeDescription.Generic.Builder.parameterizedType(
                            TypeDescription.ForLoadedType.of(List.class),
                            tryParseType(className, fieldName, fieldType, generic)
                    )
                    .build();
            case "set" -> TypeDescription.Generic.Builder.parameterizedType(
                            TypeDescription.ForLoadedType.of(Set.class),
                            tryParseType(className, fieldName, fieldType, generic)
                    )
                    .build();
            case "map" -> {
                String[] split = generic.split(",");
                if (split.length != 2) throw PojoCreationError.failedToCreateField(className, fieldName, fieldType);

                yield TypeDescription.Generic.Builder.parameterizedType(
                                TypeDescription.ForLoadedType.of(Map.class),
                                tryParseType(className, fieldName, fieldType, split[0]),
                                tryParseType(className, fieldName, fieldType, split[1])
                        )
                        .build();
            }

            default -> throw PojoCreationError.failedToCreateField(className, fieldName, fieldType);
        };
    }

    private Optional<TypeDefinition> tryParseSimpleType(String value) {
        return Optional.ofNullable(switch (value) {
                    case "String", "string" -> String.class;

                    case "int" -> int.class;
                    case "Integer" -> Integer.class;

                    case "long" -> long.class;
                    case "Long" -> Long.class;

                    case "double" -> double.class;
                    case "Double" -> Double.class;

                    default -> null;
                })
                .map(TypeDescription.ForLoadedType::of);
    }


}
