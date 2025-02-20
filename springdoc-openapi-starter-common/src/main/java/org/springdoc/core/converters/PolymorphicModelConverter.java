/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  *
 *  *  *  *  *  * Copyright 2019-2025 the original author or authors.
 *  *  *  *  *  *
 *  *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *  *
 *  *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *  *
 *  *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  *  * See the License for the specific language governing permissions and
 *  *  *  *  *  * limitations under the License.
 *  *  *  *  *
 *  *  *  *
 *  *  *
 *  *
 *
 */

package org.springdoc.core.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.ArrayUtils;
import org.springdoc.core.providers.ObjectMapperProvider;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * The type Polymorphic model converter.
 *
 * @author bnasslahsen
 */
public class PolymorphicModelConverter implements ModelConverter {

	/**
	 * The Spring doc object mapper.
	 */
	private final ObjectMapperProvider springDocObjectMapper;

	/**
	 * The constant PARENT_TYPES_TO_IGNORE.
	 */
	private static final Set<String> PARENT_TYPES_TO_IGNORE = Collections.synchronizedSet(new HashSet<>());

	/**
	 * The constant PARENT_TYPES_TO_IGNORE.
	 */
	private static final Set<String> TYPES_TO_SKIP = Collections.synchronizedSet(new HashSet<>());

	static {
		PARENT_TYPES_TO_IGNORE.add("JsonSchema");
		PARENT_TYPES_TO_IGNORE.add("Pageable");
		PARENT_TYPES_TO_IGNORE.add("EntityModel");
	}

	/**
	 * Instantiates a new Polymorphic model converter.
	 *
	 * @param springDocObjectMapper the spring doc object mapper
	 */
	public PolymorphicModelConverter(ObjectMapperProvider springDocObjectMapper) {
		this.springDocObjectMapper = springDocObjectMapper;
	}

	/**
	 * Add parent type.
	 *
	 * @param parentTypes the parent types
	 */
	public static void addParentType(String... parentTypes) {
		PARENT_TYPES_TO_IGNORE.addAll(List.of(parentTypes));
	}

	/**
	 * Gets resolved schema.
	 *
	 * @param javaType       the java type
	 * @param resolvedSchema the resolved schema
	 * @return the resolved schema
	 */
	private Schema<?> getResolvedSchema(JavaType javaType, Schema<?> resolvedSchema) {
		if (resolvedSchema instanceof ObjectSchema && resolvedSchema.getProperties() != null) {
			if (resolvedSchema.getProperties().containsKey(javaType.getRawClass().getName())) {
				resolvedSchema = resolvedSchema.getProperties().get(javaType.getRawClass().getName());
			}
			else if (resolvedSchema.getProperties().containsKey(javaType.getRawClass().getSimpleName())) {
				resolvedSchema = resolvedSchema.getProperties().get(javaType.getRawClass().getSimpleName());
			}
		}
		return resolvedSchema;
	}

	@Override
	public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
		JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
		if (javaType != null) {
			for (BeanPropertyBiDefinition propertyDef : introspectBeanProperties(javaType)) {
				if (propertyDef.isAnyAnnotated(JsonUnwrapped.class)) {
					if (!TypeNameResolver.std.getUseFqn())
						PARENT_TYPES_TO_IGNORE.add(javaType.getRawClass().getSimpleName());
					else
						PARENT_TYPES_TO_IGNORE.add(javaType.getRawClass().getName());
				}
				else {
					io.swagger.v3.oas.annotations.media.Schema declaredSchema = propertyDef.getAnyAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
					if (declaredSchema != null &&
							(ArrayUtils.isNotEmpty(declaredSchema.oneOf()) || ArrayUtils.isNotEmpty(declaredSchema.allOf()))) {
						TYPES_TO_SKIP.add(propertyDef.getPrimaryType().getRawClass().getSimpleName());
					}
				}
			}
			if (chain.hasNext()) {
				if (!type.isResolveAsRef() && type.getParent() != null
						&& PARENT_TYPES_TO_IGNORE.stream().noneMatch(ignore -> type.getParent().getName().startsWith(ignore)))
					type.resolveAsRef(true);
				Schema<?> resolvedSchema = chain.next().resolve(type, context, chain);
				resolvedSchema = getResolvedSchema(javaType, resolvedSchema);
				if (resolvedSchema == null || resolvedSchema.get$ref() == null) {
					return resolvedSchema;
				}
				if (resolvedSchema.get$ref().contains(Components.COMPONENTS_SCHEMAS_REF)) {
					String schemaName = resolvedSchema.get$ref().substring(Components.COMPONENTS_SCHEMAS_REF.length());
					Schema existingSchema = context.getDefinedModels().get(schemaName);
					if (existingSchema != null && (existingSchema.getOneOf() != null || existingSchema.getAllOf() != null)) {
						return resolvedSchema;
					}
				}
				return composePolymorphicSchema(type, resolvedSchema, context.getDefinedModels().values());
			}
		}
		return null;
	}

	/**
	 * Compose polymorphic schema.
	 *
	 * @param type    the type
	 * @param schema  the schema
	 * @param schemas the schemas
	 * @return the schema
	 */
	private Schema composePolymorphicSchema(AnnotatedType type, Schema schema, Collection<Schema> schemas) {
		String ref = schema.get$ref();
		List<Schema> composedSchemas = findComposedSchemas(ref, schemas);
		if (composedSchemas.isEmpty()) return schema;
		ComposedSchema result = new ComposedSchema();
		if (isConcreteClass(type)) result.addOneOfItem(schema);
		JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
		Class<?> clazz = javaType.getRawClass();
		if (TYPES_TO_SKIP.stream().noneMatch(typeToSkip -> typeToSkip.equals(clazz.getSimpleName())))
			composedSchemas.forEach(result::addOneOfItem);
		return result;
	}

	/**
	 * Find composed schemas recursively.
	 *
	 * @param ref     the reference of the schema
	 * @param schemas the collection of schemas to search in
	 * @return the list of composed schemas
	 */
	private List<Schema> findComposedSchemas(String ref, Collection<Schema> schemas) {
		List<Schema> composedSchemas = schemas.stream()
				.filter(ComposedSchema.class::isInstance)
				.map(ComposedSchema.class::cast)
				.filter(s -> s.getAllOf() != null)
				.filter(s -> s.getAllOf().stream().anyMatch(s2 -> ref.equals(s2.get$ref())))
				.map(s -> new Schema().$ref(AnnotationsUtils.COMPONENTS_REF + s.getName()))
				.toList();

		List<Schema> resultSchemas = new ArrayList<>(composedSchemas);

		for (Schema childSchema : composedSchemas) {
			String childSchemaRef = childSchema.get$ref();
			resultSchemas.addAll(findComposedSchemas(childSchemaRef, schemas));
		}

		return resultSchemas;
	}

	/**
	 * Is concrete class boolean.
	 *
	 * @param type the type
	 * @return the boolean
	 */
	private boolean isConcreteClass(AnnotatedType type) {
		JavaType javaType = springDocObjectMapper.jsonMapper().constructType(type.getType());
		Class<?> clazz = javaType.getRawClass();
		return !Modifier.isAbstract(clazz.getModifiers()) && !clazz.isInterface();
	}

	/**
	 * Introspects the properties of the given Java type based on serialization and deserialization configurations.
	 * This method identifies properties present in both JSON serialization and deserialization views,
	 * and pairs them into a list of {@code BeanPropertyBiDefinition}.
	 */
	private List<BeanPropertyBiDefinition> introspectBeanProperties(JavaType javaType) {
		Map<String, BeanPropertyDefinition> forSerializationProps =
			springDocObjectMapper.jsonMapper()
				.getSerializationConfig()
				.introspect(javaType)
				.findProperties()
				.stream()
				.collect(toMap(BeanPropertyDefinition::getName, identity()));
		Map<String, BeanPropertyDefinition> forDeserializationProps =
			springDocObjectMapper.jsonMapper()
				.getDeserializationConfig()
				.introspect(javaType)
				.findProperties()
				.stream()
				.collect(toMap(BeanPropertyDefinition::getName, identity()));

		return forSerializationProps.keySet().stream()
			.map(key -> new BeanPropertyBiDefinition(forSerializationProps.get(key), forDeserializationProps.get(key)))
			.toList();
	}

	/**
	 * A record representing the bi-definition of a bean property, combining both
	 * serialization and deserialization property views.
	 */
	private record BeanPropertyBiDefinition(
		BeanPropertyDefinition forSerialization,
		BeanPropertyDefinition forDeserialization
	) {

		/**
		 * Retrieves an annotation of the specified type from either the serialization or
		 * deserialization property definition (field, getter, setter), returning the first available match.
		 */
		public <A extends Annotation> A getAnyAnnotation(Class<A> acls) {
			A anyForSerializationAnnotation = getAnyAnnotation(forSerialization, acls);
			A anyForDeserializationAnnotation = getAnyAnnotation(forDeserialization, acls);

			return anyForSerializationAnnotation != null ? anyForSerializationAnnotation : anyForDeserializationAnnotation;
		}

		/**
		 * Checks if any annotation of the specified type exists across serialization
		 * or deserialization property definitions.
		 */
		public <A extends Annotation> boolean isAnyAnnotated(Class<A> acls) {
			return getAnyAnnotation(acls) != null;
		}

		/**
		 * Type determined from the primary member for the property being built.
		 */
		public JavaType getPrimaryType() {
			JavaType forSerializationType = null;
			if (forSerialization != null) {
				forSerializationType = forSerialization.getPrimaryType();
			}

			JavaType forDeserializationType = null;
			if (forDeserialization != null) {
				forDeserializationType = forDeserialization.getPrimaryType();
			}

			if (forSerializationType != null && forDeserializationType != null && forSerializationType != forDeserializationType) {
				throw new IllegalStateException("The property " + forSerialization.getName() + " has different types for serialization and deserialization: "
					+ forSerializationType + " and " + forDeserializationType);
			}

			return forSerializationType != null ? forSerializationType : forDeserializationType;
		}

		private <A extends Annotation> A getAnyAnnotation(BeanPropertyDefinition prop, Class<A> acls) {
			if (prop == null) {
				return null;
			}

			if (prop.getField() != null) {
				return prop.getField().getAnnotation(acls);
			}
			if (prop.getGetter() != null) {
				return prop.getGetter().getAnnotation(acls);
			}
			if (prop.getSetter() != null) {
				return prop.getSetter().getAnnotation(acls);
			}

			return null;
		}
	}
}
