/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  * Copyright 2019-2024 the original author or authors.
 *  *  *  *  *
 *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *
 *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *
 *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  * See the License for the specific language governing permissions and
 *  *  *  *  * limitations under the License.
 *  *  *  *
 *  *  *
 *  *
 *
 */
package org.springdoc.webmvc.ui.oauth.proxy;

import org.springdoc.core.utils.Constants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import java.io.Serializable;
import java.net.URI;
import static org.springdoc.core.utils.Constants.SPRINGDOC_ENABLED;

@Validated
@ConfigurationProperties(prefix = Constants.SPRINGDOC_SWAGGER_UI_OAUTH_PROXY_PREFIX)
public class SwaggerOauthProxyProperties implements Validator {

  private boolean enabled;
  private URI path;
  private URI oauthTokenUri;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public URI getPath() {
    return path;
  }

  public void setPath(URI path) {
    this.path = path;
  }

  public URI getOauthTokenUri() {
    return oauthTokenUri;
  }

  public void setOauthTokenUri(URI oauthTokenUri) {
    this.oauthTokenUri = oauthTokenUri;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return SwaggerOauthProxyProperties.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    if (enabled) {
      if (path == null) {
        errors.rejectValue("path", "field.required");
      }
      if (oauthTokenUri == null) {
        errors.rejectValue("oauthTokenUri", "field.required");
      }
    }
  }
}
