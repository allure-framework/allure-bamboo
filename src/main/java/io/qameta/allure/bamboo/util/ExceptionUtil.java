package io.qameta.allure.bamboo.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public enum ExceptionUtil {
    ;

    public static String stackTraceToString(Throwable e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
