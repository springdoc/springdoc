package test.org.springdoc.api.app50;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HelloController {

	@Operation(description = "Some operation", responses = {
			@ApiResponse(responseCode = "401")
	})
	@ApiResponse(responseCode = "401", content = @Content())
	@GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
	List<String> list() {
		return null;
	}

}
