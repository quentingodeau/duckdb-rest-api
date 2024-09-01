package fr.qgo.duckdbrestapi.bean;

import fr.qgo.duckdbrestapi.config.AppConfig;
import fr.qgo.duckdbrestapi.config.QueryConfig;
import fr.qgo.duckdbrestapi.config.QueryConfig.ExpositionVerb;
import fr.qgo.duckdbrestapi.config.QueryDoc;
import fr.qgo.duckdbrestapi.controller.BaseQueryController;
import fr.qgo.duckdbrestapi.controller.PojoCreationError;
import fr.qgo.duckdbrestapi.service.PojoClassGeneratorService;
import fr.qgo.duckdbrestapi.service.RequestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeDescription.Generic;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;


/**
 * Developer note this class has been inspired by the following
 * <a href="https://github.com/tsarenkotxt/poc-spring-boot-dynamic-controller">Github repository</a>
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class QueryControllerRegister {
    private final ByteBuddy byteBuddy;
    private final AppConfig appConfig;
    private final RequestExecutionService requestExecutionService;
    private final PojoClassGeneratorService pojoClassGeneratorService;

    private Class<? extends BaseQueryController> createQueriesControllerClass() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, PojoCreationError {
        if (appConfig.getQueries().isEmpty()) {
            throw new IllegalArgumentException("No queries defined");
        }

        DynamicType.Builder<BaseQueryController> typeBuilder = byteBuddy
                .subclass(BaseQueryController.class)
                .name("QueryController")
                .annotateType(
                        AnnotationDescription.Builder.ofType(RestController.class).build(),
                        AnnotationDescription.Builder.ofType(RequestMapping.class).defineArray("path", "/api/v1/queries").build()
                );

        val streamResponse = Generic.Builder.parameterizedType(ResponseEntity.class, StreamingResponseBody.class).build();
        val runQuery = RequestExecutionService.class.getDeclaredMethod("runQuery", String.class, Object.class);

        for (val queryEntry : appConfig.getQueries().entrySet()) {
            val queryId = queryEntry.getKey();
            val capitalizeQueryId = StringUtils.capitalize(queryId);
            val query = queryEntry.getValue();

            DynamicType.Builder.MethodDefinition.ParameterDefinition<BaseQueryController> defineMethod = typeBuilder.defineMethod("query" + capitalizeQueryId, streamResponse, Modifier.PUBLIC);

            var payloadClass = query.getPayloadClass();
            if (query.getPayloadStruct() != null) {
                String className = capitalizeQueryId + "Payload";
                payloadClass = pojoClassGeneratorService.create(className, query.getPayloadStruct());
            }

            // Add body if there is one
            boolean hasPayload = payloadClass != null && payloadClass != void.class && payloadClass != Void.class;
            if (hasPayload) {
                defineMethod = defineMethod
                        .withParameter(payloadClass, "params")
                        .annotateParameter(
                                AnnotationDescription.Builder.ofType(RequestBody.class).build(),
                                AnnotationDescription.Builder.ofType(Valid.class).build()
                        );
            }

            val methodAnnotations = new ArrayList<AnnotationDescription>();
            AnnotationDescription springAnnotation = createSpringMethodAnnotation(queryId, query, hasPayload);
            methodAnnotations.add(springAnnotation);
            if (query.getDoc() != null) {
                AnnotationDescription swaggerMethodAnnotation = createSwaggerMethodAnnotation(queryId, query.getDoc());
                methodAnnotations.add(swaggerMethodAnnotation);
            }

            typeBuilder = defineMethod
                    .throwing(Exception.class)
                    .intercept(MethodCall
                            .invoke(runQuery)
                            .onField("requestExecutionService")
                            .with(queryId)
                            .withAllArguments())
                    .annotateMethod(methodAnnotations);

        }

        DynamicType.Unloaded<BaseQueryController> dynamicType = typeBuilder.make();
//        try {
//            dynamicType.saveIn(new File("."));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return dynamicType
                .load(getClass().getClassLoader())
                .getLoaded();
    }

    private AnnotationDescription createSwaggerMethodAnnotation(String queryId, QueryDoc queryDoc) throws PojoCreationError {
        AnnotationDescription.Builder builder = AnnotationDescription.Builder.ofType(Operation.class);
        val tags = queryDoc.getTags();
        if (tags != null && !tags.isEmpty()) {
            builder = builder.defineArray("tags", tags.toArray(String[]::new));
        }
        val summary = queryDoc.getSummary();
        if (summary != null) {
            builder = builder.define("summary", summary);
        }
        val description = queryDoc.getDescription();
        if (description != null) {
            builder = builder.define("description", description);
        }
        var returnedType = queryDoc.getReturnedType();
        val returnedStruct = queryDoc.getReturnedStruct();
        if (returnedType == null && returnedStruct != null && !returnedStruct.isEmpty()) {
            returnedType = pojoClassGeneratorService.create(StringUtils.capitalize(queryId) + "Response", returnedStruct);
        }
        if (returnedType != null) {
            AnnotationDescription.Builder responceBuilder = AnnotationDescription.Builder.ofType(ApiResponse.class)
                    .define("responseCode", "200")
                    .defineAnnotationArray("content", TypeDescription.ForLoadedType.of(Content.class), AnnotationDescription.Builder.ofType(Content.class)
                            .define("mediaType", MediaType.APPLICATION_NDJSON_VALUE)
                            .define("array", AnnotationDescription.Builder.ofType(ArraySchema.class)
                                    .define("schema", AnnotationDescription.Builder.ofType(Schema.class)
                                            .define("type", returnedType.getName())
                                            .build()
                                    )
                                    .build())
                            .build()
                    );
            builder.defineAnnotationArray("responses", TypeDescription.ForLoadedType.of(ApiResponse.class), responceBuilder.build());
        }

        return builder.build();
    }

    private static AnnotationDescription createSpringMethodAnnotation(String queryId, QueryConfig query, boolean hasPayload) {
        AnnotationDescription.Builder methodAnnotation = AnnotationDescription.Builder.ofType(PostMapping.class)
                .defineArray("value", '/' + queryId)
                .defineArray("produces", MediaType.APPLICATION_NDJSON_VALUE);

        if (query.getVerb() == ExpositionVerb.POST && hasPayload) {
            methodAnnotation.defineArray("consumes", MediaType.APPLICATION_JSON_VALUE);
        }
        return methodAnnotation.build();
    }

    @Bean
    public BaseQueryController queriesController() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<? extends BaseQueryController> queriesControllerClass;
        try {
            queriesControllerClass = createQueriesControllerClass();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | PojoCreationError e) {
            throw new BeanCreationException("queriesController", "Failed to create the dynamic query controller", e);
        }
        return queriesControllerClass.getDeclaredConstructor(RequestExecutionService.class)
                .newInstance(requestExecutionService);
    }
}
