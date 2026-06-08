package com.sdpm.workitem;

import com.sdpm.workitem.config.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("设置和获取操作人")
    void shouldSetAndGetOperator() {
        UserContext.setOperator("张三");
        assertEquals("张三", UserContext.getOperator());
    }

    @Test
    @DisplayName("未设置时返回 anonymous")
    void shouldReturnAnonymousWhenNotSet() {
        assertEquals("anonymous", UserContext.getOperator());
    }

    @Test
    @DisplayName("clear后恢复默认值")
    void shouldClearOperator() {
        UserContext.setOperator("李四");
        UserContext.clear();
        assertEquals("anonymous", UserContext.getOperator());
    }

    @Test
    @DisplayName("同一线程内隔离")
    void shouldBeThreadLocal() {
        UserContext.setOperator("线程A");
        assertEquals("线程A", UserContext.getOperator());
    }
}