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

package test.org.springdoc.api.app175;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import reactor.core.publisher.Flux;
import test.org.springdoc.api.AbstractSpringDocTest;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.function.web.mvc.ReactorAutoConfiguration;
import org.springframework.cloud.function.web.source.FunctionExporterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

@Import({ ReactorAutoConfiguration.class, FunctionExporterAutoConfiguration.class, ContextFunctionCatalogAutoConfiguration.class })
@TestPropertySource(properties="springdoc.show-spring-cloud-functions=false")
public class SpringDocApp175Test extends AbstractSpringDocTest {

	@SpringBootApplication
	static class SpringDocTestApp {
		@Bean
		public Function<String, String> reverseString() {
			return value -> new StringBuilder(value).reverse().toString();
		}

		@Bean
		public Function<String, String> uppercase() {
			return value -> value.toUpperCase();
		}

		@Bean
		@RouterOperations({
				@RouterOperation(method = RequestMethod.GET, operation = @Operation(description = "Say hello GET", operationId = "lowercaseGET", tags = "positions",
						responses = @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))))),
				@RouterOperation(method = RequestMethod.POST, operation = @Operation(description = "Say hello POST", operationId = "lowercasePOST", tags = "positions",
						responses = @ApiResponse(responseCode = "200", description = "new desc", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))))
		})
		public Function<Flux<String>, Flux<String>> lowercase() {
			return flux -> flux.map(value -> value.toLowerCase());
		}

		@Bean(name = "titi")
		@RouterOperation(operation = @Operation(description = "Say hello By Id", operationId = "hellome", tags = "persons",
				responses = @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PersonDTO.class)))))
		public Supplier<PersonDTO> helloSupplier() {
			return () -> new PersonDTO();
		}

		@Bean
		public Consumer<PersonDTO> helloConsumer() {
			return personDTO -> personDTO.getFirstName();
		}

		@Bean
		public Supplier<Flux<String>> words() {
			return () -> Flux.fromArray(new String[] { "foo", "bar" });
		}

	}

}