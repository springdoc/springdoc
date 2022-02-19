package org.springdoc.kotlin;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import kotlin.jvm.functions.Function1;
import kotlin.reflect.KClass;
import kotlinx.serialization.DeserializationStrategy;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.SerializationStrategy;
import kotlinx.serialization.SerializersKt;
import kotlinx.serialization.descriptors.PolymorphicKind;
import kotlinx.serialization.descriptors.PrimitiveKind;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.descriptors.SerialKind;
import kotlinx.serialization.descriptors.StructureKind;
import kotlinx.serialization.modules.SerializersModule;
import kotlinx.serialization.modules.SerializersModuleCollector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.swagger.v3.core.util.RefUtils.constructRef;

/**
 * ModelConverter implementation to integrate with kotlinx.serialization as JSON implementation.
 */
public class KotlinxSerializationTypeConverter implements ModelConverter {

    private static final Pattern polymorphicNamePattern = Pattern.compile("kotlinx\\.serialization\\.(Polymorphic|Sealed)<(.*)>");
    private final SerializersModule module;
    private final Map<String, List<SerialDescriptor>> classHierarchyMap;

    public KotlinxSerializationTypeConverter(@NotNull SerializersModule module) {
        // Inspect the module to collect all the subclasses for polymorphism
        Map<String, List<SerialDescriptor>> map = new HashMap<>();
        module.dumpTo(new SerializersModuleCollector() {
            @Override
            public <T> void contextual(@NotNull KClass<T> kClass, @NotNull KSerializer<T> kSerializer) {
            }

            @Override
            public <T> void contextual(@NotNull KClass<T> kClass, @NotNull Function1<? super List<? extends KSerializer<?>>, ? extends KSerializer<?>> function1) {
            }

            @Override
            public <Base, Sub extends Base> void polymorphic(@NotNull KClass<Base> baseClass, @NotNull KClass<Sub> subClass, @NotNull KSerializer<Sub> subSerializer) {
                KSerializer<Base> baseSerializer = SerializersKt.serializer(baseClass);
                map.computeIfAbsent(getName(baseSerializer.getDescriptor()), (k) -> new ArrayList<>()).add(subSerializer.getDescriptor());
            }

            @Override
            public <Base> void polymorphicDefault(@NotNull KClass<Base> kClass, @NotNull Function1<? super String, ? extends DeserializationStrategy<? extends Base>> function1) {
            }

            @Override
            public <Base> void polymorphicDefaultDeserializer(@NotNull KClass<Base> kClass, @NotNull Function1<? super String, ? extends DeserializationStrategy<? extends Base>> function1) {
            }

            @Override
            public <Base> void polymorphicDefaultSerializer(@NotNull KClass<Base> kClass, @NotNull Function1<? super Base, ? extends SerializationStrategy<? super Base>> function1) {
            }
        });
        this.module = module;
        this.classHierarchyMap = Collections.unmodifiableMap(map);
    }

    @Override
    public Schema<?> resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> iterator) {
        // Only process types that are annotated with @Serializable
        if (annotatedType.getType() instanceof Class<?>) {
            final Class<?> cls = (Class<?>)annotatedType.getType();
            if (Arrays.stream(cls.getAnnotations()).anyMatch(it -> it instanceof Serializable)) {
                KSerializer<?> serializer = SerializersKt.serializer(module, cls);
                SerialDescriptor serialDescriptor = serializer.getDescriptor();
                return resolveNullableSchema(context, serialDescriptor, null);
            }
        }
        if (iterator.hasNext()) {
            return iterator.next().resolve(annotatedType, context, iterator);
        } else {
            return null;
        }
    }

    private Schema<?> resolveNullableSchema(@NotNull ModelConverterContext context, @NotNull SerialDescriptor serialDescriptor, @Nullable Schema<?> baseSchema) {
        return resolveSchema(context, serialDescriptor, baseSchema).nullable(serialDescriptor.isNullable());
    }

    @NotNull
    private Schema<?> resolveSchema(@NotNull ModelConverterContext context, @NotNull SerialDescriptor serialDescriptor, @Nullable Schema<?> baseSchema) {
        final SerialKind kind = serialDescriptor.getKind();
        final Schema<?> resolved = resolveRef(context, serialDescriptor);
        if (resolved != null) {
            return resolved;
        } else if (PrimitiveKind.STRING.INSTANCE.equals(kind)) {
            return new StringSchema();
        } else if (PrimitiveKind.BOOLEAN.INSTANCE.equals(kind)) {
            return new BooleanSchema();
        } else if (PrimitiveKind.INT.INSTANCE.equals(kind) ||
                PrimitiveKind.LONG.INSTANCE.equals(kind) ||
                PrimitiveKind.SHORT.INSTANCE.equals(kind) ||
                PrimitiveKind.BYTE.INSTANCE.equals(kind) ||
                PrimitiveKind.CHAR.INSTANCE.equals(kind)) {
            return new IntegerSchema();
        } else if (PrimitiveKind.FLOAT.INSTANCE.equals(kind) ||
                PrimitiveKind.DOUBLE.INSTANCE.equals(kind)) {
            return new NumberSchema();
        } else if (StructureKind.CLASS.INSTANCE.equals(kind) || StructureKind.OBJECT.INSTANCE.equals(kind)) {
            final Schema<?> schema = createSchema(baseSchema, ObjectSchema::new);
            for (int i = 0; i < serialDescriptor.getElementsCount(); ++i) {
                final SerialDescriptor elementDescriptor = serialDescriptor.getElementDescriptor(i);
                final String elementName = serialDescriptor.getElementName(i);
                schema.addProperties(
                        elementName,
                        resolveNullableSchema(context, elementDescriptor, null)
                );
                if (!serialDescriptor.isElementOptional(i)) {
                    schema.addRequiredItem(elementName);
                }
            }
            return defineRef(context, serialDescriptor, schema);
        } else if (StructureKind.LIST.INSTANCE.equals(kind)) {
            final ArraySchema schema = new ArraySchema();
            final SerialDescriptor elementDescriptor = serialDescriptor.getElementDescriptor(0);
            schema.setItems(
                    resolveNullableSchema(context, elementDescriptor, null)
            );
            return schema;
        } else if (StructureKind.MAP.INSTANCE.equals(kind)) {
            if (serialDescriptor.getElementsCount() != 2) {
                throw new IllegalStateException("Expected exactly two elements for a Map serial descriptor");
            }
            // Key should always be a string
            if (!PrimitiveKind.STRING.INSTANCE.equals(serialDescriptor.getElementDescriptor(0).getKind())) {
                throw new IllegalStateException("Key type should be string for a Map serial descriptor to be able to support JSON mappings");
            }
            final Schema<?> schema = new ObjectSchema();
            final SerialDescriptor elementDescriptor = serialDescriptor.getElementDescriptor(1);
            final Schema<?> valueSchema = resolveNullableSchema(context, elementDescriptor, null);
            schema.additionalProperties(valueSchema);
            return schema;
        } else if (SerialKind.CONTEXTUAL.INSTANCE.equals(kind)) {
            throw new IllegalStateException("Contextual mappings are only allowed in the context of polymorphism");
        } else if (SerialKind.ENUM.INSTANCE.equals(kind)) {
            final @SuppressWarnings("unchecked") Schema<Object> schema =
                    (Schema<Object>)createSchema(baseSchema, StringSchema::new);
            for (int i = 0; i < serialDescriptor.getElementsCount(); ++i) {
                schema.addEnumItemObject(serialDescriptor.getElementName(i));
            }
            return defineRef(context, serialDescriptor, schema);
        } else if (PolymorphicKind.SEALED.INSTANCE.equals(kind) || PolymorphicKind.OPEN.INSTANCE.equals(kind)) {
            if (serialDescriptor.getElementsCount() < 2) {
                throw new IllegalStateException("Expected at least two fields for a polymorphic class descriptor");
            }
            final ComposedSchema composedSchema = new ComposedSchema();
            if (baseSchema != null) {
                composedSchema.addAllOfItem(baseSchema);
            }
            final Discriminator discriminator = new Discriminator().propertyName(serialDescriptor.getElementName(0));
            composedSchema.discriminator(discriminator);
            final Schema<?> refSchema = defineRef(context, serialDescriptor, composedSchema);
            for (int i = 0; i < serialDescriptor.getElementsCount(); ++i) {
                final String elementName = serialDescriptor.getElementName(i);
                final SerialDescriptor elementDescriptor = serialDescriptor.getElementDescriptor(i);
                if (elementDescriptor.getKind().equals(SerialKind.CONTEXTUAL.INSTANCE)) {
                    final Collection<SerialDescriptor> allKnownSubDescriptors = Optional.ofNullable(classHierarchyMap.get(getName(elementDescriptor)))
                            .orElse(Collections.emptyList());
                    for (SerialDescriptor subDescriptor : allKnownSubDescriptors) {
                        final Schema<?> subSchema = resolveNullableSchema(context, subDescriptor, refSchema);
                        discriminator.mapping(getName(subDescriptor), subSchema.get$ref());
                        composedSchema.addOneOfItem(subSchema);
                    }
                } else {
                    composedSchema.addProperties(
                            elementName,
                            resolveNullableSchema(context, elementDescriptor, null)
                    );
                    if (!serialDescriptor.isElementOptional(i)) {
                        composedSchema.addRequiredItem(elementName);
                    }
                }
            }
            return refSchema;
        }
        throw new IllegalStateException("Unsupported serialDescriptor: " + serialDescriptor);
    }

    private static Schema<?> createSchema(@Nullable Schema<?> baseSchema, Supplier<Schema<?>> schemaFactory) {
        if (baseSchema != null) {
            return new ComposedSchema().addAllOfItem(baseSchema);
        } else {
            return schemaFactory.get();
        }
    }

    private static Schema<?> resolveRef(ModelConverterContext context, SerialDescriptor serialDescriptor) {
        final String name = getName(serialDescriptor);
        if (context.getDefinedModels().containsKey(name)) {
            return new Schema<>().$ref(constructRef(name));
        }
        return null;
    }

    private static Schema<?> defineRef(ModelConverterContext context, SerialDescriptor serialDescriptor, Schema<?> schema) {
        // Store off the ref and add the enum as a top-level model
        final String name = getName(serialDescriptor);
        context.defineModel(name, schema.name(name));
        return new Schema<>().$ref(constructRef(name));
    }

    private static String getName(SerialDescriptor serialDescriptor) {
        String name = serialDescriptor.getSerialName().replace("?", "").trim();
        Matcher matcher = polymorphicNamePattern.matcher(name);
        if (matcher.matches()) {
            name = matcher.group(2);
        }
        return name;
    }
}

