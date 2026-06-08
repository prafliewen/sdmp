package com.sdpm.workitem;

import com.sdpm.workitem.common.ErrorCode;
import com.sdpm.workitem.common.Result;
import com.sdpm.workitem.config.GlobalExceptionHandler;
import com.sdpm.workitem.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("处理BizException → 返回对应错误码")
    void shouldHandleBizException() {
        BizException ex = new BizException(ErrorCode.BIZ_NOT_FOUND);

        Result<Void> result = handler.handleBizException(ex);

        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMessage());
    }

    @Test
    @DisplayName("处理BizException带自定义消息")
    void shouldHandleBizExceptionWithDetail() {
        BizException ex = new BizException(ErrorCode.BIZ_DONE_IMMUTABLE, "已完成不可变更");

        Result<Void> result = handler.handleBizException(ex);

        assertEquals(409, result.getCode());
        assertEquals("已完成不可变更", result.getMessage());
    }

    @Test
    @DisplayName("处理参数校验异常 → 返回400")
    void shouldHandleValidationException() {
        // 用一个有 title 属性的对象
        TestDto target = new TestDto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dto");
        bindingResult.addError(new FieldError("dto", "title", "标题不能为空"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        Result<Void> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertTrue(result.getMessage().contains("title"));
    }

    @Test
    @DisplayName("处理请求体不可读 → 返回400")
    void shouldHandleNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad json");

        Result<Void> result = handler.handleNotReadable(ex);

        assertEquals(400, result.getCode());
        assertEquals("请求体格式不正确", result.getMessage());
    }

    @Test
    @DisplayName("处理未知异常 → 返回500")
    void shouldHandleUnknownException() {
        Exception ex = new RuntimeException("未知错误");

        Result<Void> result = handler.handleException(ex);

        assertEquals(500, result.getCode());
        assertEquals("系统繁忙，请稍后再试", result.getMessage());
    }

    static class TestDto {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}