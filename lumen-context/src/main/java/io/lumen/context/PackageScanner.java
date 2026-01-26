package io.lumen.context;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class PackageScanner {

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
        String jarPath = resource.getFile().substring(5, resource.getFile().indexOf("!"));
        String packagePath = basePackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try (JarFile jar = new JarFile(jarPath.replace("%20", " "))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(packagePath) && name.endsWith(".class")) {
                    String className = name.replace('/', '.').replace(".class", "");
                    try {
                        classes.add(Class.forName(className, false, loader));
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        }
    }
}