package net.chiaai.bot.common.web;

import com.alibaba.fastjson.JSON;
import net.chiaai.bot.common.enums.BizCodeEnum;
import net.chiaai.bot.common.enums.BizEnum;

import java.io.Serializable;

public class BizResponse<T> implements Serializable {
    private static final long serialVersionUID = 6657423316076866328L;
    private int code;
    private String message;
    private T data;

    public BizResponse() {
        this(BizCodeEnum.SUCCESS, BizCodeEnum.SUCCESS.getDesc());
    }

    public BizResponse(BizEnum errorCode) {
        this(errorCode, errorCode.getDesc());
    }

    public BizResponse(T data) {
        this(BizCodeEnum.SUCCESS, BizCodeEnum.SUCCESS.getDesc(), data);
    }

    public BizResponse(BizEnum errorCode, String message) {
        this(errorCode, message, null);
    }

    public BizResponse(BizEnum errorCode, String message, T data) {
        this.code = errorCode.getCode();
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return this.code == BizCodeEnum.SUCCESS.getCode();
    }

    public static BizResponse success() {
        return new BizResponse();
    }

    public static BizResponse success(String message) {
        return new BizResponse(BizCodeEnum.SUCCESS, message);
    }

    public static <T> BizResponse<T> success(T data) {
        return new BizResponse(data);
    }

    public static <T> BizResponse<T> success(String message, T data) {
        return new BizResponse(BizCodeEnum.SUCCESS, message, data);
    }

    public static BizResponse operationFailed() {
        return new BizResponse(BizCodeEnum.OPERATION_FAILED);
    }

    public static BizResponse operationFailed(String message) {
        return new BizResponse(BizCodeEnum.OPERATION_FAILED, message);
    }

    public static <T> BizResponse<T> operationFailed(T data) {
        return new BizResponse(BizCodeEnum.OPERATION_FAILED, BizCodeEnum.OPERATION_FAILED.getDesc(), data);
    }

    public static <T> BizResponse<T> operationFailed(String message, T data) {
        return new BizResponse(BizCodeEnum.OPERATION_FAILED, message, data);
    }

    public static BizResponse systemError() {
        return new BizResponse(BizCodeEnum.SYSTEM_ERROR);
    }

    public static BizResponse systemError(String message) {
        return new BizResponse(BizCodeEnum.SYSTEM_ERROR, message);
    }

    public static <T> BizResponse<T> systemError(T data) {
        return new BizResponse(BizCodeEnum.SYSTEM_ERROR, BizCodeEnum.SYSTEM_ERROR.getDesc(), data);
    }

    public static <T> BizResponse<T> systemError(String message, T data) {
        return new BizResponse(BizCodeEnum.SYSTEM_ERROR, message, data);
    }

    public static BizResponse paramError() {
        return new BizResponse(BizCodeEnum.PARAM_ERROR);
    }

    public static BizResponse paramError(String message) {
        return new BizResponse(BizCodeEnum.PARAM_ERROR, message);
    }

    public static <T> BizResponse<T> paramError(T data) {
        return new BizResponse(BizCodeEnum.PARAM_ERROR, BizCodeEnum.PARAM_ERROR.getDesc(), data);
    }

    public static <T> BizResponse<T> paramError(String message, T data) {
        return new BizResponse(BizCodeEnum.PARAM_ERROR, message, data);
    }

    public static BizResponse paramIsNull() {
        return new BizResponse(BizCodeEnum.PARAM_IS_NULL);
    }

    public static BizResponse paramIsNull(String message) {
        return new BizResponse(BizCodeEnum.PARAM_IS_NULL, message);
    }

    public static <T> BizResponse<T> paramIsNull(T data) {
        return new BizResponse(BizCodeEnum.PARAM_IS_NULL, BizCodeEnum.PARAM_IS_NULL.getDesc(), data);
    }

    public static <T> BizResponse<T> paramIsNull(String message, T data) {
        return new BizResponse(BizCodeEnum.PARAM_IS_NULL, message, data);
    }

    public String toString() {
        return JSON.toJSONString(this);
    }
}