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

package test.org.springdoc.api.v31.app53;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The type Hello controller with global api response.
 */
@RestController
@RequestMapping(path = "/global")
class HelloControllerWithGlobalApiResponse {

	/**
	 * List with no api response list.
	 *
	 * @return the list
	 */
	@Operation(description = "Some operation", responses = {
			@ApiResponse(responseCode = "204", description = "Explicit description for this response") })
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@GetMapping(value = "/hello1", produces = MediaType.APPLICATION_JSON_VALUE)
	List<String> listWithNoApiResponse() {
		return null;
	}

	/**
	 * List with default response status list.
	 *
	 * @return the list
	 */
	@Operation(description = "Some operation")
	@ApiResponse(responseCode = "200", description = "Explicit description for this response")
	@GetMapping(value = "/hello2", produces = MediaType.APPLICATION_JSON_VALUE)
	List<String> listWithDefaultResponseStatus() {
		return null;
	}
}