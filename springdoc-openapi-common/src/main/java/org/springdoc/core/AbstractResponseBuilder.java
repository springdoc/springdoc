package org.springdoc.core;

import static org.springdoc.core.Constants.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.ReflectionUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@SuppressWarnings("rawtypes")
public abstract class AbstractResponseBuilder {

	private Map<String, ApiResponse> genericMapResponse = new LinkedHashMap<>();

	private OperationBuilder operationBuilder;

	protected AbstractResponseBuilder(OperationBuilder operationBuilder) {
		super();
		this.operationBuilder = operationBuilder;
	}

	public ApiResponses build(Components components, HandlerMethod handlerMethod, Operation operation,
			MethodAttributes methodAttributes) {
		ApiResponses apiResponses = operation.getResponses();
		if (apiResponses == null)
			apiResponses = new ApiResponses();

		// for each one build ApiResponse and add it to existing responses
		for (Entry<String, ApiResponse> entry : genericMapResponse.entrySet()) {
			apiResponses.addApiResponse(entry.getKey(), entry.getValue());
		}
		// Fill api Responses
		computeResponse(components, handlerMethod.getMethod(), apiResponses, methodAttributes, false);

		return apiResponses;
	}

	public void buildGenericResponse(Components components, Map<String, Object> findControllerAdvice) {
		// ControllerAdvice
		List<Method> methods = getMethods(findControllerAdvice);

		// for each one build ApiResponse and add it to existing responses
		for (Method method : methods) {
			if (!operationBuilder.isHidden(method)) {
				RequestMapping reqMappringMethod = ReflectionUtils.getAnnotation(method, RequestMapping.class);
				String[] methodProduces = { MediaType.ALL_VALUE };
				if (reqMappringMethod != null) {
					methodProduces = reqMappringMethod.produces();
				}
				Map<String, ApiResponse> apiResponses = computeResponse(components, method, new ApiResponses(),
						new MethodAttributes(methodProduces), true);
				for (Map.Entry<String, ApiResponse> entry : apiResponses.entrySet()) {
					genericMapResponse.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	protected abstract Schema calculateSchemaFromParameterizedType(Components components, ParameterizedType returnType);

	protected Schema calculateSchemaParameterizedType(Components components, ParameterizedType parameterizedType) {
		Schema schemaN = null;
		Type type = parameterizedType.getActualTypeArguments()[0];
		if ((type instanceof Class || type instanceof ParameterizedType) && !Void.class.equals(type)) {
			schemaN = calculateSchema(components, parameterizedType);
		} else if (Void.class.equals(type)) {
			// if void, no content
			schemaN = AnnotationsUtils.resolveSchemaFromType(String.class, null, null);
		}
		return schemaN;
	}

	private List<Method> getMethods(Map<String, Object> findControllerAdvice) {
		List<Method> methods = new ArrayList<>();
		for (Map.Entry<String, Object> entry : findControllerAdvice.entrySet()) {
			Object controllerAdvice = entry.getValue();
			// get all methods with annotation @ExceptionHandler
			Class<?> objClz = controllerAdvice.getClass();
			if (org.springframework.aop.support.AopUtils.isAopProxy(controllerAdvice)) {
				objClz = org.springframework.aop.support.AopUtils.getTargetClass(controllerAdvice);
			}
			for (Method m : objClz.getDeclaredMethods()) {
				if (m.isAnnotationPresent(ExceptionHandler.class)) {
					methods.add(m);
				}
			}
		}
		return methods;
	}

	private Map<String, ApiResponse> computeResponse(Components components, Method method, ApiResponses apiResponsesOp,
			MethodAttributes methodAttributes, boolean isGeneric) {
		// Parsing documentation, if present
		Set<io.swagger.v3.oas.annotations.responses.ApiResponse> apiResponseAnnotations = getApiResponseAnnotations(method);
		if (!apiResponseAnnotations.isEmpty()) {
			methodAttributes.setWithApiResponseDoc(true);
			for (io.swagger.v3.oas.annotations.responses.ApiResponse apiResponseAnnotation : apiResponseAnnotations) {
				ApiResponse apiResponse = new ApiResponse();
				if (StringUtils.isNotBlank(apiResponseAnnotation.ref())) {
					apiResponse.$ref(apiResponseAnnotation.ref());
					apiResponsesOp.addApiResponse(apiResponseAnnotation.responseCode(), apiResponse);
					continue;
				}

				apiResponse.setDescription(apiResponseAnnotation.description());
				io.swagger.v3.oas.annotations.media.Content[] contentdoc = apiResponseAnnotation.content();
				Optional<Content> optionalContent = SpringDocAnnotationsUtils.getContent(contentdoc, new String[0],
						methodAttributes.getAllProduces(), null, components, null);

				if (apiResponsesOp.containsKey(apiResponseAnnotation.responseCode())) {
					// Merge with the existing content
					Content existingContent = apiResponsesOp.get(apiResponseAnnotation.responseCode()).getContent();
					if (optionalContent.isPresent() && existingContent != null) {
						Content newContent = optionalContent.get();
						for (String mediaTypeStr : methodAttributes.getAllProduces()) {
							io.swagger.v3.oas.models.media.MediaType mediaType = newContent.get(mediaTypeStr);
							mergeSchema(existingContent, mediaType.getSchema(), mediaTypeStr);
						}
						apiResponse.content(existingContent);
					}
				} else {
					optionalContent.ifPresent(apiResponse::content);
				}

				AnnotationsUtils.getHeaders(apiResponseAnnotation.headers(), null).ifPresent(apiResponse::headers);
				apiResponsesOp.addApiResponse(apiResponseAnnotation.responseCode(), apiResponse);
			}
		}

		if (!CollectionUtils.isEmpty(apiResponsesOp) && (apiResponsesOp.size() != genericMapResponse.size())) {
			// API Responses at operation and @ApiResponse annotation
			for (Map.Entry<String, ApiResponse> entry : apiResponsesOp.entrySet()) {
				String httpCode = entry.getKey();
				ApiResponse apiResponse = entry.getValue();
				buildApiResponses(components, method, apiResponsesOp, methodAttributes, httpCode, apiResponse,
						isGeneric);
			}
		} else {
			// Use response parameters with no description filled - No documentation
			// available
			String httpCode = evaluateResponseStatus(method, method.getClass(), isGeneric);
			ApiResponse apiResponse = genericMapResponse.containsKey(httpCode) ? genericMapResponse.get(httpCode)
					: new ApiResponse();
			if (httpCode != null)
				buildApiResponses(components, method, apiResponsesOp, methodAttributes, httpCode, apiResponse,
						isGeneric);
		}
		return apiResponsesOp;
	}

	private Set<io.swagger.v3.oas.annotations.responses.ApiResponse> getApiResponseAnnotations(Method method) {
		Class<?> declaringClass = method.getDeclaringClass();

		Set<io.swagger.v3.oas.annotations.responses.ApiResponses> apiResponsesDoc = AnnotatedElementUtils
				.findAllMergedAnnotations(method, io.swagger.v3.oas.annotations.responses.ApiResponses.class);
		Set<io.swagger.v3.oas.annotations.responses.ApiResponse> responses = apiResponsesDoc.stream()
				.flatMap(x -> Stream.of(x.value())).collect(Collectors.toSet());

		Set<io.swagger.v3.oas.annotations.responses.ApiResponses> apiResponsesDocDeclaringClass = AnnotatedElementUtils
				.findAllMergedAnnotations(method, io.swagger.v3.oas.annotations.responses.ApiResponses.class);
		responses.addAll(
				apiResponsesDocDeclaringClass.stream().flatMap(x -> Stream.of(x.value())).collect(Collectors.toSet()));

		Set<io.swagger.v3.oas.annotations.responses.ApiResponse> apiResponseDoc = AnnotatedElementUtils
				.findMergedRepeatableAnnotations(method, io.swagger.v3.oas.annotations.responses.ApiResponse.class);
		responses.addAll(apiResponseDoc);

		Set<io.swagger.v3.oas.annotations.responses.ApiResponse> apiResponseDocDeclaringClass = AnnotatedElementUtils
				.findMergedRepeatableAnnotations(declaringClass,
						io.swagger.v3.oas.annotations.responses.ApiResponse.class);
		responses.addAll(apiResponseDocDeclaringClass);

		return responses;
	}

	private Content buildContent(Components components, Method method, String[] methodProduces) {
		Content content = new Content();
		Type returnType = method.getGenericReturnType();
		if (isVoid(returnType)) {
			// if void, no content
			content = null;
		} else if (ArrayUtils.isNotEmpty(methodProduces)) {
			Schema<?> schemaN = calculateSchema(components, returnType);
			if (schemaN != null) {
				io.swagger.v3.oas.models.media.MediaType mediaType = new io.swagger.v3.oas.models.media.MediaType();
				mediaType.setSchema(schemaN);
				// Fill the content
				setContent(methodProduces, content, mediaType);
			}
		}
		return content;
	}

	private Schema<?> calculateSchema(Components components, Type returnType) {
		Schema<?> schemaN = null;
		if (isVoid(returnType)) {
			// if void, no content
			return schemaN;
		}
		if (returnType instanceof ParameterizedType) {
			schemaN = calculateSchemaFromParameterizedType(components, (ParameterizedType) returnType);
		} else if (ResponseEntity.class.getName().equals(returnType.getTypeName())) {
			schemaN = AnnotationsUtils.resolveSchemaFromType(String.class, null, null);
		}
		if (schemaN == null) {
			schemaN = SpringDocAnnotationsUtils.extractSchema(components, returnType);
		}
		if (schemaN == null && returnType instanceof Class) {
			schemaN = AnnotationsUtils.resolveSchemaFromType((Class) returnType, null, null);
		}
		return schemaN;
	}

	private void setContent(String[] methodProduces, Content content,
			io.swagger.v3.oas.models.media.MediaType mediaType) {
		for (String mediaTypeStr : methodProduces) {
			content.addMediaType(mediaTypeStr, mediaType);
		}
	}

	private Schema calculateSchema(Components components, ParameterizedType parameterizedType) {
		Schema schemaN;
		schemaN = SpringDocAnnotationsUtils.extractSchema(components, parameterizedType.getActualTypeArguments()[0]);
		if (schemaN.getType() == null) {
			schemaN = SpringDocAnnotationsUtils.extractSchema(components,
					parameterizedType.getActualTypeArguments()[0]);
		}
		return schemaN;
	}

	private void buildApiResponses(Components components, Method method, ApiResponses apiResponsesOp,
			MethodAttributes methodAttributes, String httpCode, ApiResponse apiResponse, boolean isGeneric) {
		// No documentation
		if (StringUtils.isBlank(apiResponse.get$ref())) {
			if (apiResponse.getContent() == null) {
				Content content = buildContent(components, method, methodAttributes.getAllProduces());
				apiResponse.setContent(content);
			}
			if (StringUtils.isBlank(apiResponse.getDescription())) {
				apiResponse.setDescription(DEFAULT_DESCRIPTION);
			}
		}

		if (apiResponse.getContent() != null
				&& (isGeneric || (methodAttributes.isMethodOverloaded() && methodAttributes.isNoApiResponseDoc()))) {
			// Merge with existing schema
			Content existingContent = apiResponse.getContent();
			Schema<?> schemaN = calculateSchema(components, method.getGenericReturnType());
			if (schemaN != null && ArrayUtils.isNotEmpty(methodAttributes.getAllProduces())) {
				for (String mediaTypeStr : methodAttributes.getAllProduces()) {
					mergeSchema(existingContent, schemaN, mediaTypeStr);
				}
			}
		}

		apiResponsesOp.addApiResponse(httpCode, apiResponse);
	}

	private void mergeSchema(Content existingContent, Schema<?> schemaN, String mediaTypeStr) {
		if (existingContent.containsKey(mediaTypeStr)) {
			io.swagger.v3.oas.models.media.MediaType mediaType = existingContent.get(mediaTypeStr);
			if (!schemaN.equals(mediaType.getSchema())) {
				// Merge the two schemas for the same mediaType
				Schema firstSchema = mediaType.getSchema();
				ComposedSchema schemaObject;
				if (firstSchema instanceof ComposedSchema) {
					schemaObject = (ComposedSchema) firstSchema;
					List<Schema> listOneOf = schemaObject.getOneOf();
					if (!CollectionUtils.isEmpty(listOneOf) && !listOneOf.contains(schemaN))
						schemaObject.addOneOfItem(schemaN);
				} else {
					schemaObject = new ComposedSchema();
					schemaObject.addOneOfItem(schemaN);
					schemaObject.addOneOfItem(firstSchema);
				}
				mediaType.setSchema(schemaObject);
				existingContent.addMediaType(mediaTypeStr, mediaType);
			}
		} else {
			// Add the new schema for a different mediaType
			existingContent.addMediaType(mediaTypeStr, new io.swagger.v3.oas.models.media.MediaType().schema(schemaN));
		}
	}

	private String evaluateResponseStatus(Method method, Class<?> beanType, boolean isGeneric) {
		String responseStatus = null;
		ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(method, ResponseStatus.class);
		if (annotation == null && beanType != null) {
			annotation = AnnotatedElementUtils.findMergedAnnotation(beanType, ResponseStatus.class);
		}
		if (annotation != null) {
			responseStatus = String.valueOf(annotation.code().value());
		}
		if (annotation == null && !isGeneric) {
			responseStatus = String.valueOf(HttpStatus.OK.value());
		}
		return responseStatus;
	}

	private boolean isVoid(Type returnType) {
		return Void.TYPE.equals(returnType) || (returnType instanceof ParameterizedType
				&& Void.class.equals(((ParameterizedType) returnType).getActualTypeArguments()[0]));
	}
}
