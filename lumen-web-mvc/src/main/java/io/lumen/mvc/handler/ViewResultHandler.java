package io.lumen.mvc.handler;

import io.lumen.web.Route;
import io.lumen.web.flash.FlashMapManager;
import io.lumen.web.handler.RouteResultHandler;
import io.lumen.mvc.argument.ModelAndView;
import io.lumen.web.view.ViewResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public class ViewResultHandler implements RouteResultHandler {
    private final ViewResolver viewResolver;

    public ViewResultHandler(ViewResolver viewResolver) {
        this.viewResolver = viewResolver;
    }

    @Override
    public boolean supports(Object returnValue, Route route) {
        return !route.isRest();
    }

    @Override
    public void handle(Object result, Object[] args, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        ModelAndView mavToUse = null;

        for (Object arg : args) {
            if (arg instanceof ModelAndView mav) {
                mavToUse = mav;
                break;
            }
        }

        String viewName = null;
        if (result instanceof String s) {
            viewName = s;
        } else if (result instanceof ModelAndView mav) {
            mavToUse = mav;
            viewName = mav.getViewName();
        }

        if (viewName != null && viewName.startsWith("redirect:")) {
            if (mavToUse != null && !mavToUse.getModel().isEmpty()) {
                Map<String, Object> modelToFlash = mavToUse.getModel();
                if (!modelToFlash.isEmpty()) {
                    FlashMapManager.save(req, modelToFlash);
                }
            }
            String target = viewName.substring(9).trim();
            if (target.startsWith("/")) {
                resp.sendRedirect(req.getContextPath() + target);
            } else {
                resp.sendRedirect(target);
            }
            return;
        }

        if (viewName != null && viewName.startsWith("forward:")) {
            req.getRequestDispatcher(viewName.substring(8).trim()).forward(req, resp);
            return;
        }

        if (mavToUse == null) mavToUse = new ModelAndView();
        if (viewName != null) mavToUse.setViewName(viewName);

        viewResolver.resolve(mavToUse.getViewName(), mavToUse.getModel(), req, resp);
    }
}
