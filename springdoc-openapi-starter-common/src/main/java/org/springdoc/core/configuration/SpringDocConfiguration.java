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


package org.springdoc.core.configuration;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.api.ErrorMessage;
import org.springdoc.api.OpenApiResourceNotFoundException;
import org.springdoc.core.conditions.CacheOrGroupedOpenApiCondition;
import org.springdoc.core.conditions.MultipleOpenApiSupportCondition;
import org.springdoc.core.configurer.SpringdocActuatorBeanFactoryConfigurer;
import org.springdoc.core.configurer.SpringdocBeanFactoryConfigurer;
import org.springdoc.core.converters.AdditionalModelsConverter;
import org.springdoc.core.converters.FileSupportConverter;
import org.springdoc.core.converters.ModelConverterRegistrar;
import org.springdoc.core.converters.OAS31ModelConverter;
import org.springdoc.core.converters.PolymorphicModelConverter;
import org.springdoc.core.converters.PropertyCustomizingConverter;
import org.springdoc.core.converters.ResponseSupportConverter;
import org.springdoc.core.converters.SchemaPropertyDeprecatingConverter;
import org.springdoc.core.converters.WebFluxSupportConverter;
import org.springdoc.core.customizers.ActuatorOperationCustomizer;
import org.springdoc.core.customizers.DataRestRouterOperationCustomizer;
import org.springdoc.core.customizers.DelegatingMethodParameterCustomizer;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OperationIdCustomizer;
import org.springdoc.core.customizers.ParameterObjectNamingStrategyCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springdoc.core.customizers.QuerydslPredicateOperationCustomizer;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springdoc.core.customizers.SpringDocCustomizers;
import org.springdoc.core.discoverer.SpringDocParameterNameDiscoverer;
import org.springdoc.core.filters.GlobalOpenApiMethodFilter;
import org.springdoc.core.filters.OpenApiMethodFilter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ActuatorProvider;
import org.springdoc.core.providers.CloudFunctionProvider;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.providers.RepositoryRestConfigurationProvider;
import org.springdoc.core.providers.RepositoryRestResourceProvider;
import org.springdoc.core.providers.RouterFunctionProvider;
import org.springdoc.core.providers.SecurityOAuth2Provider;
import org.springdoc.core.providers.SpringDataWebPropertiesProvider;
import org.springdoc.core.providers.SpringDocProviders;
import org.springdoc.core.providers.SpringWebProvider;
import org.springdoc.core.providers.WebConversionServiceProvider;
import org.springdoc.core.service.GenericParameterService;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.OperationService;
import org.springdoc.core.service.RequestBodyService;
import org.springdoc.core.service.SecurityService;
import org.springdoc.core.utils.PropertyResolverUtils;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.DeferredResult;

import static org.springdoc.core.utils.Constants.SPRINGDOC_DEPRECATING_CONVERTER_ENABLED;
import static org.springdoc.core.utils.Constants.SPRINGDOC_ENABLED;
import static org.springdoc.core.utils.Constants.SPRINGDOC_POLYMORPHIC_CONVERTER_ENABLED;
import static org.springdoc.core.utils.Constants.SPRINGDOC_SCHEMA_RESOLVE_PROPERTIES;
import static org.springdoc.core.utils.Constants.SPRINGDOC_SHOW_ACTUATOR;
import static org.springdoc.core.utils.SpringDocUtils.getConfig;

/**
 * The type Spring doc configuration.
 *
 * @author bnasslahsen
 */
@Lazy(false)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = SPRINGDOC_ENABLED, matchIfMissing = true)
@ConditionalOnWebApplication
public class SpringDocConfiguration {

	/**
	 * The constant BINDRESULT_CLASS.
	 */
	private static final String BINDRESULT_CLASS = "org.springframework.boot.context.properties.bind.BindResult";

	static {
		getConfig().replaceWithSchema(ObjectNode.class, new ObjectSchema())
				.replaceWithClass(Charset.class, String.class)
				.addResponseWrapperToIgnore(DeferredResult.class);
	}

	/**
	 * Springdoc bean factory post processor bean factory post processor.
	 *
	 * @return the bean factory post processor
	 */
	@Bean
	@Conditional(CacheOrGroupedOpenApiCondition.class)
	@ConditionalOnClass(name = BINDRESULT_CLASS)
	@Lazy(false)
	static BeanFactoryPostProcessor springdocBeanFactoryPostProcessor() {
		return new SpringdocBeanFactoryConfigurer();
	}

	/**
	 * Springdoc bean factory post processor 2 bean factory post processor.
	 *
	 * @return the bean factory post processor
	 */
// For spring-boot-1 compatibility
	@Bean
	@Conditional(CacheOrGroupedOpenApiCondition.class)
	@ConditionalOnMissingClass(value = BINDRESULT_CLASS)
	@Lazy(false)
	static BeanFactoryPostProcessor springdocBeanFactoryPostProcessor2() {
		return SpringdocBeanFactoryConfigurer::initBeanFactoryPostProcessor;
	}

	/**
	 * Local spring doc parameter name discoverer local variable table parameter name discoverer.
	 *
	 * @return the local variable table parameter name discoverer
	 */
	@Bean
	@Lazy(false)
	SpringDocParameterNameDiscoverer localSpringDocParameterNameDiscoverer() {
		return new SpringDocParameterNameDiscoverer();
	}

	/**
	 * Additional models converter additional models converter.
	 *
	 * @param objectMapperProvider the object mapper provider
	 * @return the additional models converter
	 */
	@Bean
	@Lazy(false)
	AdditionalModelsConverter additionalModelsConverter(ObjectMapperProvider objectMapperProvider) {
		return new AdditionalModelsConverter(objectMapperProvider);
	}

	/**
	 * Property customizing converter property customizing converter.
	 *
	 * @param customizers the customizers
	 * @return the property customizing converter
	 */
	@Bean
	@Lazy(false)
	@ConditionalOnBean(PropertyCustomizer.class)
	PropertyCustomizingConverter propertyCustomizingConverter(Optional<List<PropertyCustomizer>> customizers) {
		return new PropertyCustomizingConverter(customizers);
	}

	/**
	 * File support converter file support converter.
	 *
	 * @param objectMapperProvider the object mapper provider
	 * @return the file support converter
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	FileSupportConverter fileSupportConverter(ObjectMapperProvider objectMapperProvider) {
		return new FileSupportConverter(objectMapperProvider);
	}

	/**
	 * Response support converter response support converter.
	 *
	 * @param objectMapperProvider the object mapper provider
	 * @return the response support converter
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	ResponseSupportConverter responseSupportConverter(ObjectMapperProvider objectMapperProvider) {
		return new ResponseSupportConverter(objectMapperProvider);
	}

	/**
	 * Schema property deprecating converter schema property deprecating converter.
	 *
	 * @return the schema property deprecating converter
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_DEPRECATING_CONVERTER_ENABLED, matchIfMissing = true)
	@Lazy(false)
	SchemaPropertyDeprecatingConverter schemaPropertyDeprecatingConverter() {
		return new SchemaPropertyDeprecatingConverter();
	}

	/**
	 * Polymorphic model converter polymorphic model converter.
	 *
	 * @param objectMapperProvider the object mapper provider
	 * @return the polymorphic model converter
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_POLYMORPHIC_CONVERTER_ENABLED, matchIfMissing = true)
	@Lazy(false)
	PolymorphicModelConverter polymorphicModelConverter(ObjectMapperProvider objectMapperProvider) {
		return new PolymorphicModelConverter(objectMapperProvider);
	}

	/**
	 * Open api builder open api builder.
	 *
	 * @param openAPI                   the open api
	 * @param securityParser            the security parser
	 * @param springDocConfigProperties the spring doc config properties
	 * @param propertyResolverUtils     the property resolver utils
	 * @param openApiBuilderCustomisers the open api builder customisers
	 * @param serverBaseUrlCustomisers  the server base url customisers
	 * @param javadocProvider           the javadoc provider
	 * @return the open api builder
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	OpenAPIService openAPIBuilder(Optional<OpenAPI> openAPI,
			SecurityService securityParser,
			SpringDocConfigProperties springDocConfigProperties, PropertyResolverUtils propertyResolverUtils,
			Optional<List<OpenApiBuilderCustomizer>> openApiBuilderCustomisers,
			Optional<List<ServerBaseUrlCustomizer>> serverBaseUrlCustomisers, Optional<JavadocProvider> javadocProvider) {
		return new OpenAPIService(openAPI, securityParser, springDocConfigProperties, propertyResolverUtils, openApiBuilderCustomisers, serverBaseUrlCustomisers, javadocProvider);
	}

	/**
	 * Model converter registrar model converter registrar.
	 *
	 * @param modelConverters           the model converters
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the model converter registrar
	 */
	@Bean
	@Lazy(false)
	ModelConverterRegistrar modelConverterRegistrar(Optional<List<ModelConverter>> modelConverters, SpringDocConfigProperties springDocConfigProperties) {
		return new ModelConverterRegistrar(modelConverters.orElse(Collections.emptyList()), springDocConfigProperties);
	}

	/**
	 * Operation builder operation service.
	 *
	 * @param parameterBuilder      the parameter builder
	 * @param requestBodyService    the request body service
	 * @param securityParser        the security parser
	 * @param propertyResolverUtils the property resolver utils
	 * @return the operation service
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	OperationService operationBuilder(GenericParameterService parameterBuilder, RequestBodyService requestBodyService,
			SecurityService securityParser, PropertyResolverUtils propertyResolverUtils) {
		return new OperationService(parameterBuilder, requestBodyService,
				securityParser, propertyResolverUtils);
	}

	/**
	 * Property resolver utils property resolver utils.
	 *
	 * @param factory                   the factory
	 * @param messageSource             the message source
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the property resolver utils
	 */
	@Bean
	@Lazy(false)
	PropertyResolverUtils propertyResolverUtils(ConfigurableBeanFactory factory, MessageSource messageSource, SpringDocConfigProperties springDocConfigProperties) {
		return new PropertyResolverUtils(factory, messageSource, springDocConfigProperties);
	}

	/**
	 * Request body builder request body builder.
	 *
	 * @param parameterBuilder      the parameter builder
	 * @param propertyResolverUtils the property resolver utils
	 * @return the request body builder
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	RequestBodyService requestBodyBuilder(GenericParameterService parameterBuilder, PropertyResolverUtils propertyResolverUtils) {
		return new RequestBodyService(parameterBuilder, propertyResolverUtils);
	}

	/**
	 * Security parser.
	 *
	 * @param propertyResolverUtils the property resolver utils
	 * @return the security parser
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SecurityService securityParser(PropertyResolverUtils propertyResolverUtils) {
		return new SecurityService(propertyResolverUtils);
	}

	/**
	 * Parameter builder generic parameter builder.
	 *
	 * @param propertyResolverUtils                        the property resolver utils
	 * @param optionalDelegatingMethodParameterCustomizers the optional list delegating method parameter customizer
	 * @param optionalWebConversionServiceProvider         the optional web conversion service provider
	 * @param objectMapperProvider                         the object mapper provider
	 * @param javadocProvider                              the javadoc provider
	 * @return the generic parameter builder
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	GenericParameterService parameterBuilder(PropertyResolverUtils propertyResolverUtils,
			Optional<List<DelegatingMethodParameterCustomizer>> optionalDelegatingMethodParameterCustomizers,
			Optional<WebConversionServiceProvider> optionalWebConversionServiceProvider, ObjectMapperProvider objectMapperProvider, Optional<JavadocProvider> javadocProvider) {
		return new GenericParameterService(propertyResolverUtils, optionalDelegatingMethodParameterCustomizers,
				optionalWebConversionServiceProvider, objectMapperProvider, javadocProvider);
	}

	/**
	 * Properties resolver for schema open api customiser.
	 *
	 * @param openAPIService the open api builder
	 * @return the open api customiser
	 */
	@Bean
	@ConditionalOnProperty(SPRINGDOC_SCHEMA_RESOLVE_PROPERTIES)
	@Lazy(false)
	GlobalOpenApiCustomizer propertiesResolverForSchema(OpenAPIService openAPIService) {
		return openApi -> {
			Components components = openApi.getComponents();
			Map<String, Schema> schemas = components.getSchemas();
			if (!CollectionUtils.isEmpty(schemas))
				schemas.values().forEach(schema -> openAPIService.resolveProperties(schema, Locale.getDefault()));
		};
	}

	/**
	 * Spring doc providers spring doc providers.
	 *
	 * @param actuatorProvider               the actuator provider
	 * @param springCloudFunctionProvider    the spring cloud function provider
	 * @param springSecurityOAuth2Provider   the spring security o auth 2 provider
	 * @param repositoryRestResourceProvider the repository rest resource provider
	 * @param routerFunctionProvider         the router function provider
	 * @param springWebProvider              the spring web provider
	 * @param objectMapperProvider           the object mapper provider
	 * @return the spring doc providers
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SpringDocProviders springDocProviders(Optional<ActuatorProvider> actuatorProvider, Optional<CloudFunctionProvider> springCloudFunctionProvider, Optional<SecurityOAuth2Provider> springSecurityOAuth2Provider,
			Optional<RepositoryRestResourceProvider> repositoryRestResourceProvider, Optional<RouterFunctionProvider> routerFunctionProvider,
			Optional<SpringWebProvider> springWebProvider,
			ObjectMapperProvider objectMapperProvider) {
		objectMapperProvider.jsonMapper().registerModules(new SpringDocRequiredModule(), new SpringDocSealedClassModule());
		return new SpringDocProviders(actuatorProvider, springCloudFunctionProvider, springSecurityOAuth2Provider, repositoryRestResourceProvider, routerFunctionProvider, springWebProvider, objectMapperProvider);
	}

	/**
	 * Object mapper provider object mapper provider.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the object mapper provider
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	ObjectMapperProvider springdocObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties) {
		return new ObjectMapperProvider(springDocConfigProperties);
	}

	/**
	 * The type Spring doc web mvc actuator configuration.
	 *
	 * @author bnasslashen
	 */
	@ConditionalOnClass(WebEndpointProperties.class)
	@ConditionalOnProperty(SPRINGDOC_SHOW_ACTUATOR)
	static class SpringDocActuatorConfiguration {

		/**
		 * Springdoc bean factory post processor 3 bean factory post processor.
		 *
		 * @param groupedOpenApis the grouped open apis
		 * @return the bean factory post processor
		 */
		@Bean
		@Lazy(false)
		@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
		@Conditional(MultipleOpenApiSupportCondition.class)
		static BeanFactoryPostProcessor springdocBeanFactoryPostProcessor3(List<GroupedOpenApi> groupedOpenApis) {
			return new SpringdocActuatorBeanFactoryConfigurer(groupedOpenApis);
		}

		/**
		 * Actuator customizer operation customizer.
		 *
		 * @param springDocConfigProperties the spring doc config properties
		 * @return the operation customizer
		 */
		@Bean
		@Lazy(false)
		@ConditionalOnManagementPort(ManagementPortType.SAME)
		GlobalOperationCustomizer actuatorCustomizer(SpringDocConfigProperties springDocConfigProperties) {
			return new ActuatorOperationCustomizer(springDocConfigProperties);
		}

	}

	/**
	 * The type Web conversion service configuration.
	 *
	 * @author bnasslashen
	 */
	@ConditionalOnClass(WebConversionService.class)
	static class WebConversionServiceConfiguration {

		/**
		 * Web conversion service provider web conversion service provider.
		 *
		 * @return the web conversion service provider
		 */
		@Bean
		@Lazy(false)
		WebConversionServiceProvider webConversionServiceProvider() {
			return new WebConversionServiceProvider();
		}
	}

	/**
	 * The type Spring doc spring data web properties provider.
	 */
	@ConditionalOnClass(SpringDataWebProperties.class)
	static class SpringDocSpringDataWebPropertiesProvider {
		/**
		 * Spring data web properties provider spring data web properties provider.
		 *
		 * @param optionalSpringDataWebProperties the optional spring data web properties
		 * @return the spring data web properties provider
		 */
		@Bean
		@ConditionalOnMissingBean
		@Lazy(false)
		SpringDataWebPropertiesProvider springDataWebPropertiesProvider(Optional<SpringDataWebProperties> optionalSpringDataWebProperties) {
			return new SpringDataWebPropertiesProvider(optionalSpringDataWebProperties);
		}
	}

	/**
	 * The type Spring doc repository rest configuration.
	 */
	@ConditionalOnClass(RepositoryRestConfiguration.class)
	static class SpringDocRepositoryRestConfiguration {
		/**
		 * Repository rest configuration provider repository rest configuration provider.
		 *
		 * @param optionalRepositoryRestConfiguration the optional repository rest configuration
		 * @return the repository rest configuration provider
		 */
		@Bean
		@ConditionalOnMissingBean
		@Lazy(false)
		RepositoryRestConfigurationProvider repositoryRestConfigurationProvider(Optional<RepositoryRestConfiguration> optionalRepositoryRestConfiguration) {
			return new RepositoryRestConfigurationProvider(optionalRepositoryRestConfiguration);
		}
	}

	/**
	 * The type Spring doc web flux support configuration.
	 */
	@ConditionalOnClass(Flux.class)
	static class SpringDocWebFluxSupportConfiguration {

		/**
		 * Web flux support converter web flux support converter.
		 *
		 * @param objectMapperProvider the object mapper provider
		 * @return the web flux support converter
		 */
		@Bean
		@ConditionalOnMissingBean
		@Lazy(false)
		WebFluxSupportConverter webFluxSupportConverter(ObjectMapperProvider objectMapperProvider) {
			return new WebFluxSupportConverter(objectMapperProvider);
		}

	}

	/**
	 * The type Open api resource advice.
	 *
	 * @author bnasslashen
	 */
	@RestControllerAdvice
	@Hidden
	class OpenApiResourceAdvice {
		/**
		 * Handle no handler found response entity.
		 *
		 * @param e the e
		 * @return the response entity
		 */
		@ExceptionHandler(OpenApiResourceNotFoundException.class)
		@ResponseStatus(HttpStatus.NOT_FOUND)
		public ResponseEntity<ErrorMessage> handleNoHandlerFound(OpenApiResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage(e.getMessage()));
		}
	}

	/**
	 * Spring doc customizers spring doc customizers.
	 *
	 * @param openApiCustomizers                 the open api customizers
	 * @param operationCustomizers               the operation customizers
	 * @param routerOperationCustomizers         the router operation customizers
	 * @param dataRestRouterOperationCustomizers the data rest router operation customizers
	 * @param methodFilters                      the method filters
	 * @param globalOpenApiCustomizers           the global open api customizers
	 * @param globalOperationCustomizers         the global operation customizers
	 * @param globalOpenApiMethodFilters         the global open api method filters
	 * @return the spring doc customizers
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	public SpringDocCustomizers springDocCustomizers(Optional<Set<OpenApiCustomizer>> openApiCustomizers,
			Optional<Set<OperationCustomizer>> operationCustomizers,
			Optional<Set<RouterOperationCustomizer>> routerOperationCustomizers,
			Optional<Set<DataRestRouterOperationCustomizer>> dataRestRouterOperationCustomizers,
			Optional<Set<OpenApiMethodFilter>> methodFilters, Optional<Set<GlobalOpenApiCustomizer>> globalOpenApiCustomizers,
			Optional<Set<GlobalOperationCustomizer>> globalOperationCustomizers,
			Optional<Set<GlobalOpenApiMethodFilter>> globalOpenApiMethodFilters) {
		return new SpringDocCustomizers(openApiCustomizers,
				operationCustomizers,
				routerOperationCustomizers,
				dataRestRouterOperationCustomizers,
				methodFilters, globalOpenApiCustomizers, globalOperationCustomizers, globalOpenApiMethodFilters);
	}

	/**
	 * The type Querydsl provider.
	 *
	 * @author bnasslashen
	 */
	@ConditionalOnClass(value = QuerydslBindingsFactory.class)
	static class QuerydslProvider {

		/**
		 * Query dsl querydsl predicate operation customizer querydsl predicate operation customizer.
		 *
		 * @param querydslBindingsFactory   the querydsl bindings factory
		 * @param springDocConfigProperties the spring doc config properties
		 * @return the querydsl predicate operation customizer
		 */
		@Bean
		@ConditionalOnMissingBean
		@Lazy(false)
		QuerydslPredicateOperationCustomizer queryDslQuerydslPredicateOperationCustomizer(Optional<QuerydslBindingsFactory> querydslBindingsFactory,
				SpringDocConfigProperties springDocConfigProperties) {
			if (querydslBindingsFactory.isPresent()) {
				getConfig().addRequestWrapperToIgnore(Predicate.class);
				return new QuerydslPredicateOperationCustomizer(querydslBindingsFactory.get(), springDocConfigProperties);
			}
			return null;
		}
	}

	/**
	 * Parameter object naming strategy customizer delegating method parameter customizer.
	 *
	 * @return the delegating method parameter customizer
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	ParameterObjectNamingStrategyCustomizer parameterObjectNamingStrategyCustomizer() {
		return new ParameterObjectNamingStrategyCustomizer();
	}

	/**
	 * Global open api customizer global open api customizer.
	 *
	 * @return the global open api customizer
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	GlobalOpenApiCustomizer globalOpenApiCustomizer() {
		return new OperationIdCustomizer();
	}

	/**
	 * Oas 31 model converter oas 31 model converter.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the oas 31 model converter
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	OAS31ModelConverter oas31ModelConverter(SpringDocConfigProperties springDocConfigProperties) {
		return springDocConfigProperties.isOpenapi31() ? new OAS31ModelConverter() : null;
	}
}
