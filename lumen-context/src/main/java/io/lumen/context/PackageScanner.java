package io.lumen.context;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Very simple package scanner.
 * Lists all classes in a package (works if classes are in the file system, not in jars).
 */
public final class PackageScanner {

    private PackageScanner() {}

    /**
     * Scan multiple base packages recursively.
     * @param basePackages list of packages to scan
     * @return list of found classes
     */
    public static List<Class<?>> scan(String... basePackages) {
        List<Class<?>> allClasses = new ArrayList<>();
        for (String pkg : basePackages) {
            allClasses.addAll(scanRecursive(pkg));
        }
        return allClasses;
    }

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

    /**
     * Recursively scan a single package.
     * @param basePackage package name (dot notation)
     * @return list of classes in the package and subpackages
     */
    private static List<Class<?>> scanRecursive(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        if (resource == null) return classes;

        File directory = new File(resource.getFile());
        if (!directory.exists() || !directory.isDirectory()) return classes;

        scanDirectory(basePackage, directory, classes);
        return classes;
    }

    /**
     * Helper method to scan a directory recursively.
     */
    private static void scanDirectory(String packageName, File dir, List<Class<?>> classes) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(packageName + "." + file.getName(), file, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replaceAll("\\.class$", "");
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Failed to load class: " + className, e);
                }
            }
        }
    }
}
