package io.lumen.context;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

final class PackageScanner {

    private PackageScanner() {}

    public static List<Class<?>> scan(String... basePackages) {
        List<Class<?>> allClasses = new ArrayList<>();
        for (String pkg : basePackages) {
            allClasses.addAll(scanRecursive(pkg));
        }
        return allClasses;
    }

    private static List<Class<?>> scanRecursive(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    File directory = new File(resource.getFile().replace("%20", " "));
                    if (directory.exists() && directory.isDirectory()) {
                        scanDirectory(basePackage, directory, classes);
                    }
                } else if ("jar".equals(protocol)) {
                    scanJar(resource, basePackage, classes);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan package: " + basePackage, e);
        }
        return classes;
    }

    private static void scanDirectory(String packageName, File dir, List<Class<?>> classes) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Keep the dot notation consistent
                scanDirectory(packageName + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    // Ignore or log; sometimes inner classes or synthetic classes cause issues
                }
            }
        }
    }

    private static void scanJar(URL resource, String basePackage, List<Class<?>> classes) throws IOException {
        // Extract the jar path from the URL (jar:file:/path/to/jar.jar!/package)
        String jarPath = resource.getFile().substring(5, resource.getFile().indexOf("!"));
        String packagePath = basePackage.replace('.', '/');

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.replace("%20", " "))) {
            Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.hasMoreElements() ? entries.nextElement() : null;
                if (entry == null) break;

                String name = entry.getName();
                if (name.startsWith(packagePath) && name.endsWith(".class")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        classes.add(Class.forName(className));
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        }
    }
}