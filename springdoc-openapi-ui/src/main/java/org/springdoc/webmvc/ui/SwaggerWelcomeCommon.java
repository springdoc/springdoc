package org.springdoc.webmvc.ui;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SwaggerUiConfigParameters;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.ui.AbstractSwaggerWelcome;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The type Swagger welcome common.
 * @author bnasslashen
 */
public abstract class SwaggerWelcomeCommon extends AbstractSwaggerWelcome {
	/**
	 * Instantiates a new Abstract swagger welcome.
	 * @param swaggerUiConfig the swagger ui config
	 * @param springDocConfigProperties the spring doc config properties
	 * @param swaggerUiConfigParameters the swagger ui config parameters
	 */
	public SwaggerWelcomeCommon(SwaggerUiConfigProperties swaggerUiConfig, SpringDocConfigProperties springDocConfigProperties,
			SwaggerUiConfigParameters swaggerUiConfigParameters) {
		super(swaggerUiConfig, springDocConfigProperties, swaggerUiConfigParameters);
	}

	/**
	 * Redirect to ui response entity.
	 *
	 * @param request the request
	 * @return the response entity
	 */
	protected ResponseEntity<Void> redirectToUi(HttpServletRequest request) {
		buildFromCurrentContextPath(request);
		String sbUrl = contextPath + swaggerUiConfigParameters.getUiRootPath() + getSwaggerUiUrl();
		UriComponentsBuilder uriBuilder = getUriComponentsBuilder(sbUrl);

		// forward all queryParams from original request
		request.getParameterMap().forEach(uriBuilder::queryParam);

		return ResponseEntity.status(HttpStatus.FOUND)
				.location(uriBuilder.build().encode().toUri())
				.build();
	}

	/**
	 * Openapi json map.
	 *
	 * @param request the request
	 * @return the map
	 */
	protected Map<String, Object> openapiJson(HttpServletRequest request) {
		buildFromCurrentContextPath(request);
		return swaggerUiConfigParameters.getConfigParameters();
	}

	@Override
	protected void calculateOauth2RedirectUrl(UriComponentsBuilder uriComponentsBuilder) {
		if (StringUtils.isBlank(swaggerUiConfig.getOauth2RedirectUrl()) || !swaggerUiConfigParameters.isValidUrl(swaggerUiConfig.getOauth2RedirectUrl()) )
			swaggerUiConfigParameters.setOauth2RedirectUrl(uriComponentsBuilder
					.path(swaggerUiConfigParameters.getUiRootPath())
					.path(getOauth2RedirectUrl()).build().toString());
	}

	/**
	 * From current context path.
	 *
	 * @param request the request
	 */
	void buildFromCurrentContextPath(HttpServletRequest request) {
		super.init();
		contextPath = request.getContextPath();
		String servletPath = request.getServletPath();
		if (StringUtils.isNotBlank(servletPath)) {
			contextPath += servletPath;
		}
		buildConfigUrl(ServletUriComponentsBuilder.fromCurrentContextPath());
	}
}
