/*
 *
 *  *
 *  *  *
 *  *  *  * Copyright 2025 the original author or authors.
 *  *  *  *
 *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  * You may obtain a copy of the License at
 *  *  *  *
 *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *
 *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  * See the License for the specific language governing permissions and
 *  *  *  * limitations under the License.
 *  *  *
 *  *
 *
 */

package org.springdoc.core.configuration;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.swagger.v3.core.jackson.SwaggerAnnotationIntrospector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The type Spring doc sealed class module.
 *
 * @author sahil-ramagiri
 */
public class SpringDocSealedClassModule extends SimpleModule {

	@Override
	public void setupModule(SetupContext context) {
		context.insertAnnotationIntrospector(new RespectSealedClassAnnotationIntrospector());
	}

	/**
	 * The type sealed class annotation introspector.
	 */
	private static class RespectSealedClassAnnotationIntrospector extends SwaggerAnnotationIntrospector {

		@Override
		public List<NamedType> findSubtypes(Annotated annotated) {
			ArrayList<NamedType> subTypes = new ArrayList<>();

			if (annotated.getAnnotated() instanceof Class<?> clazz && clazz.isSealed()) {
				Class<?>[] permittedSubClasses = clazz.getPermittedSubclasses();
				if (permittedSubClasses.length > 0) {
					Arrays.stream(permittedSubClasses).map(NamedType::new).forEach(subTypes::add);
				}
			}

			return subTypes;
		}
	}
}