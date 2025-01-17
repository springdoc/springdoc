/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  * Copyright 2025 the original author or authors.
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

package test.org.springdoc.api.v30.app238;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("class-hierarchy")
public class Controller {
	@PostMapping("abstract-parent")
	public Response abstractParent(@RequestBody AbstractParent payload) {
		return null;
	}

	@PostMapping("concrete-parent")
	public Response concreteParent(@RequestBody ConcreteParent payload) {
		return null;
	}
}

class Response {
	AbstractParent abstractParent;

	List<ConcreteParent> concreteParents;

	public AbstractParent getAbstractParent() {
		return abstractParent;
	}

	public void setAbstractParent(AbstractParent abstractParent) {
		this.abstractParent = abstractParent;
	}

	public List<ConcreteParent> getConcreteParents() {
		return concreteParents;
	}

	public void setConcreteParents(List<ConcreteParent> concreteParents) {
		this.concreteParents = concreteParents;
	}
}
