/*
 *
 *  *
 *  *  *
 *  *  *  * Copyright 2019-2022 the original author or authors.
 *  *  *  *
 *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  * You may obtain a copy of the License at
 *  *  *  *
 *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *
 *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  * See the License for the specific language governing permissions and
 *  *  *  * limitations under the License.
 *  *  *
 *  *
 *
 */

package org.springdoc.webflux.core;

import java.util.List;
import java.util.Optional;

import org.springdoc.core.AbstractRequestService;
import org.springdoc.core.GenericParameterService;
import org.springdoc.core.GenericResponseService;
import org.springdoc.core.OpenAPIService;
import org.springdoc.core.OperationService;
import org.springdoc.core.PropertyResolverUtils;
import org.springdoc.core.RequestBodyService;
import org.springdoc.core.ReturnTypeParser;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocProviders;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.ParameterCustomizer;
import org.springdoc.core.customizers.RouterOperationCustomizer;
import org.springdoc.core.filters.OpenApiMethodFilter;
import org.springdoc.core.providers.ActuatorProvider;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.providers.SpringWebProvider;
import org.springdoc.webflux.api.OpenApiActuatorResource;
import org.springdoc.webflux.api.OpenApiWebfluxResource;
import org.springdoc.webflux.core.converters.WebFluxSupportConverter;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.web.reactive.ControllerEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;

import static org.springdoc.core.Constants.SPRINGDOC_ENABLED;
import static org.springdoc.core.Constants.SPRINGDOC_USE_MANAGEMENT_PORT;

/**
 * The type Spring doc web flux configuration.
 * @author bnasslahsen
 */
@Lazy(false)
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = SPRINGDOC_ENABLED, matchIfMissing = true)
public class SpringDocWebFluxConfiguration {

	/**
	 * Open api resource open api resource.
	 *
	 * @param openAPIBuilderObjectFactory the open api builder object factory
	 * @param requestBuilder the request builder
	 * @param responseBuilder the response builder
	 * @param operationParser the operation parser
	 * @param operationCustomizers the operation customizers
	 * @param openApiCustomisers the open api customisers
	 * @param routerOperationCustomizers the router operation customizers
	 * @param methodFilters the method filters
	 * @param springDocConfigProperties the spring doc config properties
	 * @param springDocProviders the spring doc providers
	 * @return the open api resource
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_USE_MANAGEMENT_PORT, havingValue = "false", matchIfMissing = true)
	@Lazy(false)
	OpenApiWebfluxResource openApiResource(ObjectFactory<OpenAPIService> openAPIBuilderObjectFactory, AbstractRequestService requestBuilder,
			GenericResponseService responseBuilder, OperationService operationParser,
			Optional<List<OperationCustomizer>> operationCustomizers,
			Optional<List<OpenApiCustomiser>> openApiCustomisers,
			Optional<List<RouterOperationCustomizer>> routerOperationCustomizers,
			Optional<List<OpenApiMethodFilter>> methodFilters,
			SpringDocConfigProperties springDocConfigProperties,
			SpringDocProviders springDocProviders) {
		return new OpenApiWebfluxResource(openAPIBuilderObjectFactory, requestBuilder,
				responseBuilder, operationParser, operationCustomizers,
				openApiCustomisers, routerOperationCustomizers, methodFilters, springDocConfigProperties, springDocProviders);
	}

	/**
	 * Request builder request builder.
	 *
	 * @param parameterBuilder the parameter builder
	 * @param requestBodyService the request body builder
	 * @param operationService the operation builder
	 * @param parameterCustomizers the parameter customizers
	 * @param localSpringDocParameterNameDiscoverer the local spring doc parameter name discoverer
	 * @return the request builder
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	RequestService requestBuilder(GenericParameterService parameterBuilder, RequestBodyService requestBodyService,
			OperationService operationService,
			Optional<List<ParameterCustomizer>> parameterCustomizers,
			LocalVariableTableParameterNameDiscoverer localSpringDocParameterNameDiscoverer) {
		return new RequestService(parameterBuilder, requestBodyService,
				operationService, parameterCustomizers, localSpringDocParameterNameDiscoverer);
	}

	/**
	 * Response builder generic response builder.
	 *
	 * @param operationService the operation builder
	 * @param returnTypeParsers the return type parsers
	 * @param springDocConfigProperties the spring doc config properties
	 * @param propertyResolverUtils the property resolver utils
	 * @return the generic response builder
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	GenericResponseService responseBuilder(OperationService operationService, List<ReturnTypeParser> returnTypeParsers, SpringDocConfigProperties springDocConfigProperties, PropertyResolverUtils propertyResolverUtils) {
		return new GenericResponseService(operationService, returnTypeParsers, springDocConfigProperties, propertyResolverUtils);
	}

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

	/**
	 * Spring web provider spring web provider.
	 *
	 * @return the spring web provider
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SpringWebProvider springWebProvider(){
		return new SpringWebFluxProvider();
	}

	/**
	 * The type Spring doc web flux actuator configuration.
	 * @author bnasslahsen
	 */
	@ConditionalOnClass(WebFluxEndpointHandlerMapping.class)
	static class SpringDocWebFluxActuatorConfiguration {

		/**
		 * Actuator provider actuator provider.
		 *
		 * @param serverProperties the server properties
		 * @param springDocConfigProperties the spring doc config properties
		 * @param managementServerProperties the management server properties
		 * @param webEndpointProperties the web endpoint properties
		 * @param webFluxEndpointHandlerMapping the web flux endpoint handler mapping
		 * @param controllerEndpointHandlerMapping the controller endpoint handler mapping
		 * @return the actuator provider
		 */
		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnExpression("${springdoc.show-actuator:false} or ${springdoc.use-management-port:false}")
		@Lazy(false)
		ActuatorProvider actuatorProvider(ServerProperties serverProperties,
				SpringDocConfigProperties springDocConfigProperties,
				Optional<ManagementServerProperties> managementServerProperties,
				Optional<WebEndpointProperties> webEndpointProperties,
				Optional<WebFluxEndpointHandlerMapping> webFluxEndpointHandlerMapping,
				Optional<ControllerEndpointHandlerMapping> controllerEndpointHandlerMapping) {
			return new ActuatorWebFluxProvider(serverProperties,
					springDocConfigProperties,
					managementServerProperties,
					webEndpointProperties,
					webFluxEndpointHandlerMapping,
					controllerEndpointHandlerMapping);
		}

		/**
		 * Actuator open api resource open api actuator resource.
		 *
		 * @param openAPIBuilderObjectFactory the open api builder object factory
		 * @param requestBuilder the request builder
		 * @param responseBuilder the response builder
		 * @param operationParser the operation parser
		 * @param operationCustomizers the operation customizers
		 * @param openApiCustomisers the open api customisers
		 * @param routerOperationCustomizers the router operation customizers
		 * @param methodFilters the method filters
		 * @param springDocConfigProperties the spring doc config properties
		 * @param springDocProviders the spring doc providers
		 * @return the open api actuator resource
		 */
		@Bean
		@ConditionalOnMissingBean(MultipleOpenApiSupportConfiguration.class)
		@ConditionalOnProperty(SPRINGDOC_USE_MANAGEMENT_PORT)
		@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
		@Lazy(false)
		OpenApiActuatorResource actuatorOpenApiResource(ObjectFactory<OpenAPIService> openAPIBuilderObjectFactory, AbstractRequestService requestBuilder,
				GenericResponseService responseBuilder, OperationService operationParser,
				Optional<List<OperationCustomizer>> operationCustomizers,
				Optional<List<OpenApiCustomiser>> openApiCustomisers,
				Optional<List<RouterOperationCustomizer>> routerOperationCustomizers,
				Optional<List<OpenApiMethodFilter>> methodFilters,
				SpringDocConfigProperties springDocConfigProperties,
				SpringDocProviders springDocProviders) {
			return new OpenApiActuatorResource(openAPIBuilderObjectFactory, requestBuilder,
					responseBuilder, operationParser,operationCustomizers,
					openApiCustomisers, routerOperationCustomizers, methodFilters,  springDocConfigProperties, springDocProviders);
		}
	}
}
