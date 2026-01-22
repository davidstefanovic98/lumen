package io.lumen.web.filter;

import jakarta.servlet.Filter;

public interface LumenFilter extends Filter {

    default int getOrder() {
        return 0;
    }
}
