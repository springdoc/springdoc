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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import static org.springdoc.core.Constants.API_DOCS_URL;
import static org.springdoc.core.Constants.APPLICATION_OPENAPI_YAML;
import static org.springdoc.core.Constants.DEFAULT_API_DOCS_URL_YAML;
import static org.springframework.util.AntPathMatcher.DEFAULT_PATH_SEPARATOR;

@RestController
@ConditionalOnMissingBean(name = "openApiResource")
public class OpenApiResource extends AbstractOpenApiResource {

	private final RequestMappingInfoHandlerMapping requestMappingHandlerMapping;


	public OpenApiResource(String groupName, OpenAPIBuilder openAPIBuilder, AbstractRequestBuilder requestBuilder,
			GenericResponseBuilder responseBuilder, OperationBuilder operationParser,
			RequestMappingInfoHandlerMapping requestMappingHandlerMapping,
			Optional<List<OpenApiCustomiser>> openApiCustomisers, SpringDocConfigProperties springDocConfigProperties) {
		super(groupName, openAPIBuilder, requestBuilder, responseBuilder, operationParser, openApiCustomisers, springDocConfigProperties);
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
	}

	@Operation(hidden = true)
	@GetMapping(value = API_DOCS_URL, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> openapiJson(ServerHttpRequest serverHttpRequest)
			throws JsonProcessingException {
		calculateServerUrl(serverHttpRequest);
		OpenAPI openAPI = this.getOpenApi();
		return Mono.just(Json.mapper().writeValueAsString(openAPI));
	}

	@Operation(hidden = true)
	@GetMapping(value = DEFAULT_API_DOCS_URL_YAML, produces = APPLICATION_OPENAPI_YAML)
	public Mono<String> openapiYaml(ServerHttpRequest serverHttpRequest) throws JsonProcessingException {
		calculateServerUrl(serverHttpRequest);
		OpenAPI openAPI = this.getOpenApi();
		return Mono.just(Yaml.mapper().writeValueAsString(openAPI));
	}

	@Override
	protected void getPaths(Map<String, Object> restControllers) {
		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : map.entrySet()) {
			RequestMappingInfo requestMappingInfo = entry.getKey();
			HandlerMethod handlerMethod = entry.getValue();
			PatternsRequestCondition patternsRequestCondition = requestMappingInfo.getPatternsCondition();
			Set<PathPattern> patterns = patternsRequestCondition.getPatterns();
			for (PathPattern pathPattern : patterns) {
				String operationPath = pathPattern.getPatternString();
				Map<String, String> regexMap = new LinkedHashMap<>();
				operationPath = PathUtils.parsePath(operationPath, regexMap);
				if (operationPath.startsWith(DEFAULT_PATH_SEPARATOR)
						&& restControllers.containsKey(handlerMethod.getBean().toString()) && isPackageToScan(handlerMethod.getBeanType().getPackage().getName()) && isPathToMatch(operationPath)) {
					Set<RequestMethod> requestMethods = requestMappingInfo.getMethodsCondition().getMethods();
					// default allowed requestmethods
					if (requestMethods.isEmpty())
						requestMethods = this.getDefaultAllowedHttpMethods();
					calculatePath(handlerMethod, operationPath, requestMethods);
				}
			}
		}
	}

	private void calculateServerUrl(ServerHttpRequest serverHttpRequest) {
		UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(serverHttpRequest).build();

		String scheme = uriComponents.getScheme();
		String serverName = uriComponents.getHost();
		int serverPort = uriComponents.getPort();

		StringBuilder url = new StringBuilder();

		if (scheme != null && serverName != null) {
			url.append(scheme).append("://");
			url.append(serverName);

			if (serverPort != 80 && serverPort != 443 && serverPort != -1) {
				url.append(":").append(serverPort);
			}
		}

		openAPIBuilder.setServerBaseUrl(url.toString());

	}

}
