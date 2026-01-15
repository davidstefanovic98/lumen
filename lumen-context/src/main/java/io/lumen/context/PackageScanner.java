package io.lumen.context;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Very simple package scanner.
 * Lists all classes in a package (works if classes are in the file system, not in jars).
 */
public class PackageScanner {

    public static List<Class<?>> scan(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        if (resource == null) return classes;

        File directory = new File(resource.getFile());
        if (!directory.exists()) return classes;

        for (File file : directory.listFiles()) {
            String name = file.getName();
            if (name.endsWith(".class")) {
                String className = basePackage + "." + name.substring(0, name.length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class: " + className, e);
                }
            }
        }

        return classes;
    }
}
