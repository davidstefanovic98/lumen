package io.lumen.web.argument;

import io.lumen.core.component.LightContainer;
import io.lumen.core.util.ParameterNameDiscoverer;
import io.lumen.web.annotation.RequestPart;
import io.lumen.web.exception.MultipartResolveException;
import io.lumen.web.http.MediaType;
import io.lumen.web.multipart.MultipartConfig;
import io.lumen.web.multipart.MultipartFile;
import io.lumen.web.multipart.ServletMultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MultipartArgumentResolver implements MethodArgumentResolver {

    private final LightContainer container;

    public MultipartArgumentResolver(LightContainer container) {
        this.container = container;
    }

    @Override
    public boolean supports(Parameter parameter) {
        Class<?> type = parameter.getType();
        return type == MultipartFile.class || isListOfMultipartFile(parameter)
                || parameter.isAnnotationPresent(RequestPart.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpServletRequest request, HttpServletResponse response, Map<String, String> pathVariables) {
        var config = container.internals().getLightsByType(MultipartConfig.class)
                .stream()
                .findFirst()
                .orElse(null);

        if (config == null) {
            throw new MultipartResolveException(
                    "Multipart support is not enabled. Please register a @Light of type MultipartConfig."
            );
        }

        String contentTypeHeader = request.getContentType();
        MediaType requestType = (contentTypeHeader != null) ? MediaType.parse(contentTypeHeader) : null;

        boolean isMultipart = MediaType.MULTIPART_FORM_DATA.isCompatibleWith(requestType);

        try {
            String partName = null;
            boolean required = true;

            if (parameter.isAnnotationPresent(RequestPart.class)) {
                RequestPart ann = parameter.getAnnotation(RequestPart.class);
                partName = ann.value();
                required = ann.required();
            }

            if (partName == null || partName.isEmpty()) {
                partName = ParameterNameDiscoverer.getParameterName(parameter);
            }

            Object resolvedValue = null;

            if (isMultipart) {
                if (List.class.isAssignableFrom(parameter.getType())) {
                    String finalPartName = partName;
                    List<MultipartFile> files = request.getParts().stream()
                            .filter(part -> part.getName().equals(finalPartName))
                            .filter(part -> part.getSubmittedFileName() != null)
                            .map(ServletMultipartFile::new)
                            .collect(Collectors.toList());

                    if (!files.isEmpty()) resolvedValue = files;
                } else {
                    Part part = request.getPart(partName);
                    if (part != null && part.getSubmittedFileName() != null) {
                        resolvedValue = new ServletMultipartFile(part);
                    }
                }
            }

            if (resolvedValue == null && required) {
                throw new MultipartResolveException("Required request part '" + partName + "' is not present");
            }
            return resolvedValue;
        } catch (MultipartResolveException e) {
            throw e;
        } catch (Exception e) {
            throw new MultipartResolveException("Failed to resolve multipart parameter [" + parameter.getName() + "]: " + e.getMessage());
        }
    }

    private boolean isListOfMultipartFile(Parameter parameter) {
        if (!List.class.isAssignableFrom(parameter.getType())) return false;
        Type genericType = parameter.getParameterizedType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            return args.length > 0 && args[0] == MultipartFile.class;
        }
        return false;
    }
}