package net.chiaai.bot.conf.web;

import lombok.extern.slf4j.Slf4j;
import net.chiaai.bot.common.exception.BizException;
import net.chiaai.bot.common.web.BizResponse;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = Exception.class)
    public BizResponse exceptionHandler(Exception e) {
        log.error("系统异常", e);
        return BizResponse.systemError(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = BizException.class)
    public BizResponse bizExceptionHandler(BizException e) {
        return BizResponse.operationFailed(e.getErrorMessage());
    }

    @ResponseBody
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public BizResponse validExceptionHandler(MethodArgumentNotValidException e) {
        BeanPropertyBindingResult beanPropertyBindingResult = (BeanPropertyBindingResult) e.getBindingResult();
        List<ObjectError> errors = beanPropertyBindingResult.getAllErrors();
        if (errors.size() > 0) {
            return BizResponse.paramError(errors.get(0).getDefaultMessage());
        }
        return BizResponse.paramError();
    }

}
