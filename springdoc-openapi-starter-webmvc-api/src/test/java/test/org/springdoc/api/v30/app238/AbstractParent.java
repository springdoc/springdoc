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
public abstract sealed class AbstractParent {
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}

final class ChildOfAbstract1 extends AbstractParent {
	private String abstrachChild1Param;

	public String getAbstrachChild1Param() {
		return abstrachChild1Param;
	}

	public void setAbstrachChild1Param(String abstrachChild1Param) {
		this.abstrachChild1Param = abstrachChild1Param;
	}
}

final class ChildOfAbstract2 extends AbstractParent {
	private String abstractChild2Param;

	public String getAbstractChild2Param() {
		return abstractChild2Param;
	}

	public void setAbstractChild2Param(String abstractChild2Param) {
		this.abstractChild2Param = abstractChild2Param;
	}
}
