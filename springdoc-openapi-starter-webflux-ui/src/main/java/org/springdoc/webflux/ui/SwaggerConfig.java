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

package org.springdoc.webflux.ui;

import java.util.Optional;

import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ActuatorProvider;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.providers.SpringWebProvider;
import org.springdoc.webflux.core.providers.SpringWebFluxProvider;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.endpoint.web.reactive.WebFluxEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static org.springdoc.core.utils.Constants.SPRINGDOC_SWAGGER_UI_ENABLED;
import static org.springdoc.core.utils.Constants.SPRINGDOC_USE_MANAGEMENT_PORT;
import static org.springdoc.core.utils.Constants.SPRINGDOC_USE_ROOT_PATH;


/**
 * The type Swagger config.
 *
 * @author bnasslahsen
 */
@Lazy(false)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = SPRINGDOC_SWAGGER_UI_ENABLED, matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnBean(SpringDocConfiguration.class)
public class SwaggerConfig implements WebFluxConfigurer {

	/**
	 * Swagger welcome swagger welcome web flux.
	 *
	 * @param swaggerUiConfig           the swagger ui config
	 * @param springDocConfigProperties the spring doc config properties
	 * @param springWebProvider         the spring web provider
	 * @return the swagger welcome web flux
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_USE_MANAGEMENT_PORT, havingValue = "false", matchIfMissing = true)
	@Lazy(false)
	SwaggerWelcomeWebFlux swaggerWelcome(SwaggerUiConfigProperties swaggerUiConfig, SpringDocConfigProperties springDocConfigProperties, SpringWebProvider springWebProvider) {
		return new SwaggerWelcomeWebFlux(swaggerUiConfig, springDocConfigProperties,springWebProvider);
	}

	/**
	 * Swagger config resource swagger config resource.
	 *
	 * @param swaggerWelcomeCommon the swagger welcome common
	 * @return the swagger config resource
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_USE_MANAGEMENT_PORT, havingValue = "false", matchIfMissing = true)
	@Lazy(false)
	SwaggerConfigResource swaggerConfigResource(SwaggerWelcomeCommon swaggerWelcomeCommon) {
		return new SwaggerConfigResource(swaggerWelcomeCommon);
	}

	/**
	 * Swagger ui home swagger ui home.
	 *
	 * @param optionalWebFluxProperties the optional web flux properties
	 * @return the swagger ui home
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = SPRINGDOC_USE_ROOT_PATH, havingValue = "true")
	@Lazy(false)
	SwaggerUiHome swaggerUiHome(Optional<WebFluxProperties> optionalWebFluxProperties) {
		return new SwaggerUiHome(optionalWebFluxProperties);
	}

	/**
	 * Swagger web flux configurer swagger web flux configurer.
	 *
	 * @param swaggerUiConfigProperties the swagger ui calculated config
	 * @param swaggerIndexTransformer   the swagger index transformer
	 * @param actuatorProvider          the actuator provider
	 * @param swaggerResourceResolver   the swagger resource resolver
	 * @return the swagger web flux configurer
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SwaggerWebFluxConfigurer swaggerWebFluxConfigurer(SwaggerUiConfigProperties swaggerUiConfigProperties,
													  SwaggerIndexTransformer swaggerIndexTransformer,
			Optional<ActuatorProvider> actuatorProvider, SwaggerResourceResolver swaggerResourceResolver) {
		return new SwaggerWebFluxConfigurer(swaggerUiConfigProperties, swaggerIndexTransformer, actuatorProvider, swaggerResourceResolver);
	}

	/**
	 * Index page transformer swagger index transformer.
	 *
	 * @param swaggerUiConfig          the swagger ui config
	 * @param swaggerUiOAuthProperties the swagger ui o auth properties
	 * @param swaggerWelcomeCommon     the swagger welcome common
	 * @param objectMapperProvider     the object mapper provider
	 * @return the swagger index transformer
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SwaggerIndexTransformer indexPageTransformer(SwaggerUiConfigProperties swaggerUiConfig, SwaggerUiOAuthProperties swaggerUiOAuthProperties,
			SwaggerWelcomeCommon swaggerWelcomeCommon, ObjectMapperProvider objectMapperProvider) {
		return new SwaggerIndexPageTransformer(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
	}

	/**
	 * Spring web provider spring web provider.
	 *
	 * @return the spring web provider
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SpringWebProvider springWebProvider() {
		return new SpringWebFluxProvider();
	}

	/**
	 * Swagger resource resolver swagger resource resolver.
	 *
	 * @param swaggerUiConfigProperties the swagger ui config properties
	 * @return the swagger resource resolver
	 */

	/**
	 * Swagger resource resolver swagger resource resolver.
	 *
	 * @param swaggerUiConfigProperties the swagger ui config properties
	 * @return the swagger resource resolver
	 */
	@Bean
	@ConditionalOnMissingBean
	@Lazy(false)
	SwaggerResourceResolver swaggerResourceResolver(SwaggerUiConfigProperties swaggerUiConfigProperties) {
		return new SwaggerResourceResolver(swaggerUiConfigProperties);
	}

	/**
	 * The type Swagger actuator welcome configuration.
	 *
	 * @author bnasslashen
	 */
	@ConditionalOnProperty(SPRINGDOC_USE_MANAGEMENT_PORT)
	@ConditionalOnClass(WebFluxEndpointHandlerMapping.class)
	@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
	static class SwaggerActuatorWelcomeConfiguration {

		/**
		 * Swagger actuator welcome swagger welcome actuator.
		 *
		 * @param swaggerUiConfig            the swagger ui config
		 * @param springDocConfigProperties  the spring doc config properties
		 * @param webEndpointProperties      the web endpoint properties
		 * @param managementServerProperties the management server properties
		 * @return the swagger welcome actuator
		 */
		@Bean
		@ConditionalOnMissingBean
		@Lazy(false)
		SwaggerWelcomeActuator swaggerActuatorWelcome(SwaggerUiConfigProperties swaggerUiConfig, SpringDocConfigProperties springDocConfigProperties,
				WebEndpointProperties webEndpointProperties, ManagementServerProperties managementServerProperties) {
			return new SwaggerWelcomeActuator(swaggerUiConfig, springDocConfigProperties, webEndpointProperties, managementServerProperties);
		}
	}
}
