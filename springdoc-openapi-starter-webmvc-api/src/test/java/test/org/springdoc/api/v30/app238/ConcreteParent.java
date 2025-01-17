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


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public sealed class ConcreteParent {
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}

final class ChildOfConcrete1 extends ConcreteParent {
	private String concreteChild1Param;

	public String getConcreteChild1Param() {
		return concreteChild1Param;
	}

	public void setConcreteChild1Param(String concreteChild1Param) {
		this.concreteChild1Param = concreteChild1Param;
	}
}

final class ChildOfConcrete2 extends ConcreteParent {
	private String concreteChild2Param;

	public String getConcreteChild2Param() {
		return concreteChild2Param;
	}

	public void setConcreteChild2Param(String concreteChild2Param) {
		this.concreteChild2Param = concreteChild2Param;
	}
}
