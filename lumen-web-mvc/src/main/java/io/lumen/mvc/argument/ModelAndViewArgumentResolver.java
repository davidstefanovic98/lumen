package io.lumen.mvc.argument;

import io.lumen.web.argument.MethodArgumentResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Parameter;
import java.util.Map;

public class ModelAndViewArgumentResolver implements MethodArgumentResolver{

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType() == ModelAndView.class;
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        ModelAndView mav = new ModelAndView();

        /*
         * Retrieve flash attributes from the request and add them to the ModelAndView,
         * in order to make ModelAndView carry over with redirects
         */
        @SuppressWarnings("unchecked")
        Map<String, Object> flashAttributes = (Map<String, Object>) request.getAttribute("LUMEN_FLASH_ATTRIBUTES");

        if (flashAttributes != null) {
            flashAttributes.forEach(mav::addObject);
        }

        return mav;
    }
}

