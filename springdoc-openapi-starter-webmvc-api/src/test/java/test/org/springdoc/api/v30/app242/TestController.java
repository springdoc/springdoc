package test.org.springdoc.api.v30.app242;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping
    public RootModel getRootModel() {
        return new RootModel();
    }
}
