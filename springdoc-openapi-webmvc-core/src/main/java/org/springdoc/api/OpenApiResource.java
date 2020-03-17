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

package org.springdoc.api;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.PathUtils;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.AbstractRequestBuilder;
import org.springdoc.core.GenericResponseBuilder;
import org.springdoc.core.OpenAPIBuilder;
import org.springdoc.core.OperationBuilder;
import org.springdoc.core.SecurityOAuth2Provider;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.customizers.OpenApiCustomiser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springdoc.core.Constants.API_DOCS_URL;
import static org.springdoc.core.Constants.APPLICATION_OPENAPI_YAML;
import static org.springdoc.core.Constants.DEFAULT_API_DOCS_URL_YAML;
import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

@RestController
@ConditionalOnMissingBean(name = "openApiResource")
public class OpenApiResource extends AbstractOpenApiResource {

	private final RequestMappingInfoHandlerMapping requestMappingHandlerMapping;

	private final Optional<ActuatorProvider> servletContextProvider;

	public OpenApiResource(String groupName, OpenAPIBuilder openAPIBuilder, AbstractRequestBuilder requestBuilder,
			GenericResponseBuilder responseBuilder, OperationBuilder operationParser,
			RequestMappingInfoHandlerMapping requestMappingHandlerMapping, Optional<ActuatorProvider> servletContextProvider,
			Optional<List<OpenApiCustomiser>> openApiCustomisers, SpringDocConfigProperties springDocConfigProperties) {
		super(groupName, openAPIBuilder, requestBuilder, responseBuilder, operationParser, openApiCustomisers, springDocConfigProperties);
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
		this.servletContextProvider = servletContextProvider;
	}

	@Operation(hidden = true)
	@GetMapping(value = API_DOCS_URL, produces = MediaType.APPLICATION_JSON_VALUE)
	public String openapiJson(HttpServletRequest request)
			throws JsonProcessingException {
		calculateServerUrl(request);
		OpenAPI openAPI = this.getOpenApi();
		return Json.mapper().writeValueAsString(openAPI);
	}

	@Operation(hidden = true)
	@GetMapping(value = DEFAULT_API_DOCS_URL_YAML, produces = APPLICATION_OPENAPI_YAML)
	public String openapiYaml(HttpServletRequest request)
			throws JsonProcessingException {
		calculateServerUrl(request);
		OpenAPI openAPI = this.getOpenApi();
		return Yaml.mapper().writeValueAsString(openAPI);
	}

	@Override
	protected void getPaths(Map<String, Object> restControllers) {
		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
		calculatePath(restControllers, map, Optional.empty());

		if (servletContextProvider.isPresent()) {
			map = servletContextProvider.get().getMethods();
			this.openAPIBuilder.addTag(new HashSet<>(map.values()), servletContextProvider.get().getTag());
			calculatePath(restControllers, map, servletContextProvider);
		}
		Optional<SecurityOAuth2Provider> securityOAuth2ProviderOptional = openAPIBuilder.getSpringSecurityOAuth2Provider();
		if (securityOAuth2ProviderOptional.isPresent()) {
			SecurityOAuth2Provider securityOAuth2Provider = securityOAuth2ProviderOptional.get();
			Map<RequestMappingInfo, HandlerMethod> mapOauth = securityOAuth2Provider.getHandlerMethods();
			Map<String, Object> requestMappingMapSec = securityOAuth2Provider.getFrameworkEndpoints();
			Class[] additionalRestClasses = requestMappingMapSec.values().stream().map(Object::getClass).toArray(Class[]::new);
			AbstractOpenApiResource.addRestControllers(additionalRestClasses);
			calculatePath(requestMappingMapSec, mapOauth, Optional.empty());
		}
	}

	private void calculatePath(Map<String, Object> restControllers, Map<RequestMappingInfo, HandlerMethod> map, Optional<ActuatorProvider> actuatorProvider) {
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
			RequestMappingInfo requestMappingInfo = entry.getKey();
			HandlerMethod handlerMethod = entry.getValue();
			PatternsRequestCondition patternsRequestCondition = requestMappingInfo.getPatternsCondition();
			Set<String> patterns = patternsRequestCondition.getPatterns();
			Map<String, String> regexMap = new LinkedHashMap<>();
			for (String pattern : patterns) {
				String operationPath = PathUtils.parsePath(pattern, regexMap);
				if (((actuatorProvider.isPresent() && actuatorProvider.get().isRestController(operationPath))
						|| isRestController(restControllers, handlerMethod, operationPath))
						&& isPackageToScan(handlerMethod.getBeanType().getPackage().getName())
						&& isPathToMatch(operationPath)) {
					Set<RequestMethod> requestMethods = requestMappingInfo.getMethodsCondition().getMethods();
					// default allowed requestmethods
					if (requestMethods.isEmpty())
						requestMethods = this.getDefaultAllowedHttpMethods();
					calculatePath(handlerMethod, operationPath, requestMethods);
				}
			}
		}
	}

	private boolean isRestController(Map<String, Object> restControllers, HandlerMethod handlerMethod,
			String operationPath) {
		ResponseBody responseBodyAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
		if (responseBodyAnnotation == null)
			responseBodyAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ResponseBody.class);

		return (responseBodyAnnotation != null && restControllers.containsKey(handlerMethod.getBean().toString()) || isAdditionalRestController(handlerMethod.getBeanType()))
				&& operationPath.startsWith(DEFAULT_PATH_SEPARATOR)
				&& (springDocConfigProperties.isModelAndViewAllowed()|| !ModelAndView.class.isAssignableFrom(handlerMethod.getMethod().getReturnType()));
	}

	private void calculateServerUrl(HttpServletRequest request) {
		HttpRequest httpRequest = new ServletServerHttpRequest(request);
		UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(httpRequest).build();

		String scheme = uriComponents.getScheme();
		String serverName = uriComponents.getHost();
		int serverPort = uriComponents.getPort();

		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://");
		url.append(serverName);

		if (serverPort != 80 && serverPort != 443 && serverPort != -1) {
			url.append(":").append(serverPort);
		}

		openAPIBuilder.setServerBaseUrl(url.toString());
	}
}
