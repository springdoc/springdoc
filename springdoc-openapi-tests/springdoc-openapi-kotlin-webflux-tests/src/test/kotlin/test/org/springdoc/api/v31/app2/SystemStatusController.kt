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

package test.org.springdoc.api.v31.app2

import jakarta.validation.Valid
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

enum class SystemStatus(val status: String) {
	OK("OK")
}

data class SystemStatusResponse(
	val status: SystemStatus
)

@RestController
@RequestMapping("/status")
class SystemStatusController {
	@GetMapping
	suspend fun index() = SystemStatusResponse(SystemStatus.OK)

	@GetMapping("/foo")
	fun foo() = mono {
		SystemStatusResponse(SystemStatus.OK)
	}

	@PostMapping("{token}/activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	suspend fun activateUser(
		@PathVariable token: String,
		@RequestBody @Valid request: String
	) {
		// do something
	}
}