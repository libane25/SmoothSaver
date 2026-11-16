package com.smoothsaver.xposed;

import com.smoothsaver.xposed.ModulePolicy.LineSink;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

/**
 * Reads framework class and method signatures once at startup.
 * It does not initialize classes and it does not install hooks.
 */
public final class FrameworkProbe {

    private static final int MAX_METHODS = 40;
    private static final int MAX_FIELDS = 40;

    private FrameworkProbe() {
    }

    public static void inspect(ClassLoader loader, List<String> classNames, LineSink sink) {
        if (loader == null) {
            sink.line("Framework inspection skipped: class loader was null.");
            return;
        }
        if (classNames == null) {
            sink.line("Framework inspection skipped: class list was null.");
            return;
        }
        for (String name : classNames) {
            inspectOne(loader, name, sink);
        }
    }

    static boolean isDiagnosticMethod(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("lowpower")
                || normalized.equals("onchange")
                || normalized.equals("observe")
                || normalized.equals("register")
                || normalized.equals("startobserving")
                || normalized.equals("updatevote")
                || normalized.equals("updateglobalvote")
                || normalized.equals("setvote")
                || normalized.contains("renderframe");
    }

    private static void inspectOne(ClassLoader loader, String name, LineSink sink) {
        try {
            Class<?> clazz = Class.forName(name, false, loader);
            describeClass(clazz, sink);
            for (Class<?> inner : clazz.getDeclaredClasses()) {
                describeClass(inner, sink);
            }
        } catch (ClassNotFoundException notFound) {
            sink.line("Detected framework class: " + name + " — absent");
        } catch (Throwable error) {
            sink.line("Inspection exception for " + name + ": "
                    + error.getClass().getName() + ": " + error.getMessage());
        }
    }

    private static void describeClass(Class<?> clazz, LineSink sink) {
        sink.line("Detected framework class: " + clazz.getName());
        logMethods(clazz, sink);
        if ("Vote".equals(clazz.getSimpleName())) {
            logPriorityFields(clazz, sink);
        }
    }

    private static void logMethods(Class<?> clazz, LineSink sink) {
        Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (Throwable error) {
            sink.line("Inspection exception for methods of " + clazz.getName() + ": "
                    + error.getClass().getName() + ": " + error.getMessage());
            return;
        }
        int shown = 0;
        int hidden = 0;
        for (Method method : methods) {
            if (method.isSynthetic() || method.isBridge()) {
                continue;
            }
            if (!isDiagnosticMethod(method.getName())) {
                continue;
            }
            if (shown >= MAX_METHODS) {
                hidden++;
                continue;
            }
            sink.line("Detected method: " + clazz.getName() + " " + signature(method));
            shown++;
        }
        if (hidden > 0) {
            sink.line("Detected method log for " + clazz.getName()
                    + " capped at " + MAX_METHODS + "; " + hidden + " more not logged.");
        }
    }

    private static void logPriorityFields(Class<?> clazz, LineSink sink) {
        int shown = 0;
        int hidden = 0;
        Field[] fields;
        try {
            fields = clazz.getDeclaredFields();
        } catch (Throwable error) {
            sink.line("Inspection exception for fields of " + clazz.getName() + ": "
                    + error.getClass().getName() + ": " + error.getMessage());
            return;
        }
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers()) || !field.getName().contains("PRIORITY")) {
                continue;
            }
            if (shown >= MAX_FIELDS) {
                hidden++;
                continue;
            }
            sink.line("Vote priority field present, value not read: " + clazz.getName()
                    + "." + field.getName() + " type=" + field.getType().getName());
            shown++;
        }
        if (hidden > 0) {
            sink.line("Vote priority field log capped at " + MAX_FIELDS
                    + "; " + hidden + " more not logged.");
        }
    }

    private static String signature(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(Modifier.toString(method.getModifiers()));
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(method.getReturnType().getName());
        builder.append(' ');
        builder.append(method.getName());
        builder.append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameters[i].getName());
        }
        builder.append(')');
        return builder.toString();
    }
}
