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

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
public class SwaggerOauthProxyController {

  private final String GRANT_TYPE_KEY = "grant_type";
  private final String CLIENT_CREDENTIALS_VALUE = "client_credentials";
  private final String CLIENT_ID_KEY = "client_id";
  private final String CLIENT_SECRET_KEY = "client_secret";
  private final String AUTHENTICATION_SCHEME_BASIC = "Basic";

  private final RestClient restClient = RestClient.builder().build();
  private final SwaggerOauthProxyProperties swaggerOauthProxyProperties;

  @Autowired
  public SwaggerOauthProxyController(SwaggerOauthProxyProperties swaggerOauthProxyProperties) {
    this.swaggerOauthProxyProperties = swaggerOauthProxyProperties;
  }

  @Hidden
  @PostMapping(path = "${springdoc.swagger-ui.oauth-proxy.path}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String redirectSwaggerOauth(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {

    if (authorizationHeader != null && authorizationHeader.startsWith(AUTHENTICATION_SCHEME_BASIC)) {

      String base64Credentials = authorizationHeader.substring(AUTHENTICATION_SCHEME_BASIC.length()).trim();
      String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
      String[] clientDetails = credentials.split(":", 2);

      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add(GRANT_TYPE_KEY, CLIENT_CREDENTIALS_VALUE);
      body.add(CLIENT_ID_KEY, clientDetails[0]);
      body.add(CLIENT_SECRET_KEY, clientDetails[1]);

      ResponseEntity<String> response = restClient.post()
          .uri(swaggerOauthProxyProperties.getOauthTokenUri())
          .body(body)
          .retrieve()
          .toEntity(String.class);

      return response.getBody();

    } else {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header missing or not using Basic Auth");
    }
  }
}
