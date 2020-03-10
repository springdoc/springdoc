/*
 *
 *  * Copyright 2019-2020 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springdoc.core;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("rawtypes")
public class GenericParameterBuilder {

	private final LocalVariableTableParameterNameDiscoverer localSpringDocParameterNameDiscoverer;
	private final IgnoredParameterAnnotations ignoredParameterAnnotations;
	private final PropertyResolverUtils propertyResolverUtils;
	private static final List<Class<?>> FILE_TYPES = new ArrayList<>();

	static {
		FILE_TYPES.add(MultipartFile.class);
	}

	public GenericParameterBuilder(LocalVariableTableParameterNameDiscoverer localSpringDocParameterNameDiscoverer,
								   IgnoredParameterAnnotations ignoredParameterAnnotations,
								   PropertyResolverUtils propertyResolverUtils) {
		this.localSpringDocParameterNameDiscoverer = localSpringDocParameterNameDiscoverer;
		this.ignoredParameterAnnotations = ignoredParameterAnnotations;
		this.propertyResolverUtils = propertyResolverUtils;
	}

	Parameter mergeParameter(List<Parameter> existingParamDoc, Parameter paramCalcul) {
		Parameter result = paramCalcul;
		if (paramCalcul != null && paramCalcul.getName() != null) {
			final String name = paramCalcul.getName();
			Parameter paramDoc = existingParamDoc.stream().filter(p -> name.equals(p.getName())).findAny().orElse(null);
			if (paramDoc != null) {
				mergeParameter(paramCalcul, paramDoc);
				result = paramDoc;
			}
			else
				existingParamDoc.add(result);
		}
		return result;
	}

	private void mergeParameter(Parameter paramCalcul, Parameter paramDoc) {
		if (StringUtils.isBlank(paramDoc.getDescription()))
			paramDoc.setDescription(paramCalcul.getDescription());

		if (StringUtils.isBlank(paramDoc.getIn()))
			paramDoc.setIn(paramCalcul.getIn());

		if (paramDoc.getExample() == null)
			paramDoc.setExample(paramCalcul.getExample());

		if (paramDoc.getDeprecated() == null)
			paramDoc.setDeprecated(paramCalcul.getDeprecated());

		if (paramDoc.getRequired() == null)
			paramDoc.setRequired(paramCalcul.getRequired());

		if (paramDoc.getAllowEmptyValue() == null)
			paramDoc.setAllowEmptyValue(paramCalcul.getAllowEmptyValue());

		if (paramDoc.getAllowReserved() == null)
			paramDoc.setAllowReserved(paramCalcul.getAllowReserved());

		if (StringUtils.isBlank(paramDoc.get$ref()))
			paramDoc.set$ref(paramDoc.get$ref());

		if (paramDoc.getSchema() == null)
			paramDoc.setSchema(paramCalcul.getSchema());

		if (paramDoc.getExamples() == null)
			paramDoc.setExamples(paramCalcul.getExamples());

		if (paramDoc.getExtensions() == null)
			paramDoc.setExtensions(paramCalcul.getExtensions());

		if (paramDoc.getStyle() == null)
			paramDoc.setStyle(paramCalcul.getStyle());

		if (paramDoc.getExplode() == null)
			paramDoc.setExplode(paramCalcul.getExplode());
	}

	Parameter buildParameterFromDoc(io.swagger.v3.oas.annotations.Parameter parameterDoc,
			Components components, JsonView jsonView) {
		Parameter parameter = new Parameter();
		if (StringUtils.isNotBlank(parameterDoc.description())) {
			parameter.setDescription(propertyResolverUtils.resolve(parameterDoc.description()));
		}
		if (StringUtils.isNotBlank(parameterDoc.name())) {
			parameter.setName(propertyResolverUtils.resolve(parameterDoc.name()));
		}
		if (StringUtils.isNotBlank(parameterDoc.in().toString())) {
			parameter.setIn(parameterDoc.in().toString());
		}
		if (StringUtils.isNotBlank(parameterDoc.example())) {
			try {
				parameter.setExample(Json.mapper().readTree(parameterDoc.example()));
			}
			catch (IOException e) {
				parameter.setExample(parameterDoc.example());
			}
		}
		if (parameterDoc.deprecated()) {
			parameter.setDeprecated(parameterDoc.deprecated());
		}
		if (parameterDoc.required()) {
			parameter.setRequired(parameterDoc.required());
		}
		if (parameterDoc.allowEmptyValue()) {
			parameter.setAllowEmptyValue(parameterDoc.allowEmptyValue());
		}
		if (parameterDoc.allowReserved()) {
			parameter.setAllowReserved(parameterDoc.allowReserved());
		}
		setSchema(parameterDoc, components, jsonView, parameter);
		setExamples(parameterDoc, parameter);
		setExtensions(parameterDoc, parameter);
		setParameterStyle(parameter, parameterDoc);
		setParameterExplode(parameter, parameterDoc);

		return parameter;
	}

	private void setSchema(io.swagger.v3.oas.annotations.Parameter parameterDoc, Components components, JsonView jsonView, Parameter parameter) {
		if (StringUtils.isNotBlank(parameterDoc.ref())) {
			parameter.$ref(parameterDoc.ref());
		}
		else {
			Schema schema = AnnotationsUtils.getSchemaFromAnnotation(parameterDoc.schema(), components, jsonView)
					.orElse(null);
			if (schema == null) {
				if (parameterDoc.content().length > 0) {
					if (AnnotationsUtils.hasSchemaAnnotation(parameterDoc.content()[0].schema())) {
						schema = AnnotationsUtils.getSchemaFromAnnotation(parameterDoc.content()[0].schema(), components, jsonView).orElse(null);
					}
					else if (AnnotationsUtils.hasArrayAnnotation(parameterDoc.content()[0].array())) {
						schema = AnnotationsUtils.getArraySchema(parameterDoc.content()[0].array(), components, jsonView).orElse(null);
					}
				}
				else {
					schema = AnnotationsUtils.getArraySchema(parameterDoc.array(), components, jsonView).orElse(null);
				}
			}
			parameter.setSchema(schema);
		}
	}

	Schema calculateSchema(Components components, ParameterInfo parameterInfo,
			RequestBodyInfo requestBodyInfo, JsonView jsonView) {
		Schema schemaN;
		Class<?> schemaImplementation = null;
		Type returnType = null;
		JavaType ct = null;
		String paramName = parameterInfo.getpName();
		java.lang.reflect.Parameter parameter = parameterInfo.getParameter();
		if (parameter != null) {
			returnType = parameter.getParameterizedType();
			ct = constructType(parameter.getType());
			schemaImplementation = parameter.getType();
		}

		if (parameterInfo.getParameterModel() == null || parameterInfo.getParameterModel().getSchema() == null) {
			if (isFile(ct)) {
				schemaN = getFileSchema(requestBodyInfo);
				schemaN.addProperties(paramName, new FileSchema());
				return schemaN;
			} else if (returnType instanceof ParameterizedType) {
				ParameterizedType parameterizedType = (ParameterizedType) returnType;
				if (isFile(parameterizedType)) {
					return extractFileSchema(paramName, requestBodyInfo);
				}
				schemaN = calculateSchemaFromParameterizedType(components, returnType, jsonView);
			}
			else {
				schemaN = SpringDocAnnotationsUtils.resolveSchemaFromType(schemaImplementation, components, jsonView, parameter != null ? parameter.getAnnotations() : null);
			}
		}
		else {
			schemaN = parameterInfo.getParameterModel().getSchema();
		}

		if (requestBodyInfo != null) {
			if (requestBodyInfo.getMergedSchema() != null) {
				requestBodyInfo.getMergedSchema().addProperties(paramName, schemaN);
				schemaN = requestBodyInfo.getMergedSchema();
			}
			else
				requestBodyInfo.addProperties(paramName, schemaN);
		}

		return schemaN;
	}

	public LocalVariableTableParameterNameDiscoverer getLocalSpringDocParameterNameDiscoverer() {
		return localSpringDocParameterNameDiscoverer;
	}

	private Schema calculateSchemaFromParameterizedType(Components components, Type paramType, JsonView jsonView) {
		return SpringDocAnnotationsUtils.extractSchema(components, paramType, jsonView,null);
	}

	private Schema extractFileSchema(String paramName, RequestBodyInfo requestBodyInfo) {
		Schema schemaN = getFileSchema(requestBodyInfo);
		ArraySchema schemaFile = new ArraySchema();
		schemaFile.items(new FileSchema());
		schemaN.addProperties(paramName, new ArraySchema().items(new FileSchema()));
		return schemaN;
	}

	private Schema getFileSchema(RequestBodyInfo requestBodyInfo) {
		Schema schemaN;
		if (requestBodyInfo.getMergedSchema() != null)
			schemaN = requestBodyInfo.getMergedSchema();
		else {
			schemaN = new ObjectSchema();
			requestBodyInfo.setMergedSchema(schemaN);
		}
		return schemaN;
	}

	private boolean isFile(ParameterizedType parameterizedType) {
		Type type = parameterizedType.getActualTypeArguments()[0];
		if (MultipartFile.class.getName().equals(type.getTypeName())
				|| FilePart.class.getName().equals(type.getTypeName())) {
			return true;
		}
		else if (type instanceof WildcardType) {
			WildcardType wildcardType = (WildcardType) type;
			Type[] upperBounds = wildcardType.getUpperBounds();
			return MultipartFile.class.getName().equals(upperBounds[0].getTypeName());
		}
		return false;
	}

	private boolean isFile(JavaType ct) {
		return FILE_TYPES.stream().anyMatch(clazz -> clazz.isAssignableFrom(ct.getRawClass()));
	}

	public boolean isFile(java.lang.reflect.Parameter parameter) {
		boolean result = false;
		Type type = parameter.getParameterizedType();
		JavaType javaType = this.constructType(type);
		if (isFile(javaType)) {
			result = true;
		}
		else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			if (isFile(parameterizedType)) {
				result = true;
			}
		}
		return result;
	}

	<A extends Annotation> A getParameterAnnotation(HandlerMethod handlerMethod,
			java.lang.reflect.Parameter parameter, int i, Class<A> annotationType) {
		A parameterDoc = AnnotationUtils.getAnnotation(parameter, annotationType);
		if (parameterDoc == null) {
			Set<Method> methods = MethodUtils.getOverrideHierarchy(handlerMethod.getMethod(),
					ClassUtils.Interfaces.INCLUDE);
			for (Method methodOverriden : methods) {
				parameterDoc = AnnotationUtils.getAnnotation(methodOverriden.getParameters()[i], annotationType);
				if (parameterDoc != null)
					break;
			}
		}
		return parameterDoc;
	}

	private void setExamples(io.swagger.v3.oas.annotations.Parameter parameterDoc, Parameter parameter) {
		Map<String, Example> exampleMap = new HashMap<>();
		if (parameterDoc.examples().length == 1 && StringUtils.isBlank(parameterDoc.examples()[0].name())) {
			Optional<Example> exampleOptional = AnnotationsUtils.getExample(parameterDoc.examples()[0]);
			exampleOptional.ifPresent(parameter::setExample);
		}
		else {
			for (ExampleObject exampleObject : parameterDoc.examples()) {
				AnnotationsUtils.getExample(exampleObject)
						.ifPresent(example -> exampleMap.put(exampleObject.name(), example));
			}
		}
		if (exampleMap.size() > 0) {
			parameter.setExamples(exampleMap);
		}
	}

	private void setExtensions(io.swagger.v3.oas.annotations.Parameter parameterDoc, Parameter parameter) {
		if (parameterDoc.extensions().length > 0) {
			Map<String, Object> extensionMap = AnnotationsUtils.getExtensions(parameterDoc.extensions());
			extensionMap.forEach(parameter::addExtension);
		}
	}

	private void setParameterExplode(Parameter parameter, io.swagger.v3.oas.annotations.Parameter p) {
		if (isExplodable(p)) {
			if (Explode.TRUE.equals(p.explode())) {
				parameter.setExplode(Boolean.TRUE);
			}
			else if (Explode.FALSE.equals(p.explode())) {
				parameter.setExplode(Boolean.FALSE);
			}
		}
	}

	private void setParameterStyle(Parameter parameter, io.swagger.v3.oas.annotations.Parameter p) {
		if (StringUtils.isNotBlank(p.style().toString())) {
			parameter.setStyle(Parameter.StyleEnum.valueOf(p.style().toString().toUpperCase()));
		}
	}

	private boolean isExplodable(io.swagger.v3.oas.annotations.Parameter p) {
		io.swagger.v3.oas.annotations.media.Schema schema = p.schema();
		boolean explode = true;
		Class<?> implementation = schema.implementation();
		if (implementation == Void.class && !schema.type().equals("object") && !schema.type().equals("array")) {
			explode = false;
		}
		return explode;
	}

	private JavaType constructType(Type type) {
		return TypeFactory.defaultInstance().constructType(type);
	}

	public boolean isAnnotationToIgnore(java.lang.reflect.Parameter parameter) {
		return ignoredParameterAnnotations.isAnnotationToIgnore(parameter);
	}

	public static void addFileType(Class<?>... classes) {
		FILE_TYPES.addAll(Arrays.asList(classes));
	}
}
