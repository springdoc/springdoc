/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  * Copyright 2019-2022 the original author or authors.
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

package test.org.springdoc.api.v30.app114;

import java.math.BigDecimal;

import javax.money.MonetaryAmount;

import org.springdoc.core.utils.Constants;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.test.context.TestPropertySource;
import test.org.springdoc.api.v30.AbstractSpringDocV30Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@TestPropertySource(properties = Constants.SPRINGDOC_SAME_MODEL_NAME_CONVERTER_ENABLED + "=false")
public class SpringDocApp114Test extends AbstractSpringDocV30Test {

	static {
		SpringDocUtils.getConfig()
				.removeFromSchemaMap(BigDecimal.class)
				.replaceWithClass(MonetaryAmount.class, org.springdoc.core.converters.models.MonetaryAmount.class);
	}

	@SpringBootApplication
	static class SpringDocTestApp {}
}
