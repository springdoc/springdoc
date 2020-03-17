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

package test.org.springdoc.api.app99;


import org.junit.jupiter.api.Test;
import org.springdoc.core.Constants;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import test.org.springdoc.api.AbstractSpringDocTest;

public class SpringDocApp99Test extends AbstractSpringDocTest {

	@SpringBootApplication
	@ComponentScan(basePackages = { "org.springdoc", "test.org.springdoc.api.app99" })
	static class SpringDocTestApp {}

	@Test
	@Override
	public void testApp() throws Exception {
		String expected = getContent("results/app99.json");

		webTestClient.get()
				.uri(Constants.DEFAULT_API_DOCS_URL + groupName)
				.header("X-Forwarded-Proto", "https")
				.header("X-Forwarded-Host", "loadbalancer.example.com")
				.header("X-Forwarded-Port", "8443")
				.exchange()
				.expectStatus().isOk()
				.expectBody().json(expected);
	}
}