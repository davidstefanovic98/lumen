package io.lumen;

import io.lumen.context.annotation.Service;

@Service
public class OptionalService {

    public String getMessage() {
        return "I'm optional!";
    }
}
