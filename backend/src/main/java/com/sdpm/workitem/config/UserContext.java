package com.sdpm.workitem.config;

public class UserContext {

    private static final ThreadLocal<String> OPERATOR = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setOperator(String operator) {
        OPERATOR.set(operator);
    }

    public static String getOperator() {
        String op = OPERATOR.get();
        return op != null ? op : "anonymous";
    }

    public static void clear() {
        OPERATOR.remove();
    }
}