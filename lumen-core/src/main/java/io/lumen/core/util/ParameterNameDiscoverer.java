package io.lumen.core.util;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers actual parameter names from bytecode using ASM.
 * Falls back to reflection-based names (arg0, arg1, etc.) if bytecode reading fails.
 */
public class ParameterNameDiscoverer {

    private static final Map<Executable, String[]> CACHE = new ConcurrentHashMap<>();

    /**
     * Get parameter names for a method or constructor.
     */
    public static String[] getParameterNames(Executable executable) {
        return CACHE.computeIfAbsent(executable, ParameterNameDiscoverer::discoverNames);
    }

    /**
     * Get the name of a specific parameter.
     */
    public static String getParameterName(Parameter parameter) {
        Executable executable = parameter.getDeclaringExecutable();
        String[] names = getParameterNames(executable);

        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(parameter)) {
                return names[i];
            }
        }

        return parameter.getName(); // Fallback
    }

    private static String[] discoverNames(Executable executable) {
        try {
            String[] names = readFromBytecode(executable);
            if (names != null && !hasPlaceholderNames(names)) {
                return names;
            }
        } catch (Exception e) {
            // Fall through to reflection fallback
        }

        return getReflectionNames(executable);
    }

    private static String[] readFromBytecode(Executable executable) throws IOException {
        Class<?> declaringClass = executable.getDeclaringClass();
        String className = declaringClass.getSimpleName() + ".class";

        try (InputStream is = declaringClass.getResourceAsStream(className)) {
            if (is == null) {
                return null;
            }

            ClassReader reader = new ClassReader(is);
            ParameterNameClassVisitor visitor = new ParameterNameClassVisitor(executable);
            reader.accept(visitor, 0);

            return visitor.getParameterNames();
        }
    }

    private static String[] getReflectionNames(Executable executable) {
        Parameter[] parameters = executable.getParameters();
        String[] names = new String[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            names[i] = parameters[i].getName();
        }

        return names;
    }

    private static boolean hasPlaceholderNames(String[] names) {
        for (String name : names) {
            if (name != null && name.matches("arg\\d+")) {
                return true;
            }
        }
        return false;
    }

    /**
     * ASM ClassVisitor to extract parameter names from bytecode.
     */
    private static class ParameterNameClassVisitor extends ClassVisitor {

        private final Executable executable;
        private String[] parameterNames;

        public ParameterNameClassVisitor(Executable executable) {
            super(Opcodes.ASM9);
            this.executable = executable;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {

            String executableName = executable instanceof Constructor
                    ? "<init>"
                    : executable.getName();

            if (!name.equals(executableName)) {
                return null;
            }

            return new ParameterNameMethodVisitor(access, executable.getParameterCount());
        }

        public String[] getParameterNames() {
            return parameterNames;
        }

        private class ParameterNameMethodVisitor extends MethodVisitor {

            private final boolean isStatic;
            private final int parameterCount;
            private final String[] names;

            public ParameterNameMethodVisitor(int access, int parameterCount) {
                super(Opcodes.ASM9);
                this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
                this.parameterCount = parameterCount;
                this.names = new String[parameterCount];
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature,
                                           Label start, Label end, int index) {
                int parameterIndex = isStatic ? index : index - 1;

                if (parameterIndex >= 0 && parameterIndex < parameterCount) {
                    names[parameterIndex] = name;
                }
            }

            @Override
            public void visitEnd() {
                parameterNames = names;
            }
        }
    }
}