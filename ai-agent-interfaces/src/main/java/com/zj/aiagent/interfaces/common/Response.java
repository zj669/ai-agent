package com.zj.aiagent.interfaces.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一响应对象 - 接口层
 * 
 * 用于HTTP REST API的统一响应格式
 * 注意：此类属于接口层，不应在领域层或应用层使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 7000723935764546321L;

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应信息
     */
    private String info;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应码常量
     */
    public static class Code {
        public static final String SUCCESS = "0000";
        public static final String BUSINESS_ERROR = "0001";
        public static final String SYSTEM_ERROR = "0002";
        public static final String ILLEGAL_PARAMETER = "0003";
        public static final String UNAUTHORIZED = "0401";
        public static final String FORBIDDEN = "0403";
        public static final String NOT_FOUND = "0404";
    }

    /**
     * 创建成功的响应
     * 
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(Code.SUCCESS, "操作成功", data);
    }

    /**
     * 创建成功的响应（无数据）
     * 
     * @return 成功响应
     */
    public static <T> Response<T> success() {
        return new Response<>(Code.SUCCESS, "操作成功", null);
    }

    /**
     * 创建成功的响应（自定义消息）
     * 
     * @param message 成功消息
     * @param data    响应数据
     * @return 成功响应
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(Code.SUCCESS, message, data);
    }

    /**
     * 创建失败的响应
     * 
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> Response<T> fail(String message) {
        return new Response<>(Code.BUSINESS_ERROR, message, null);
    }

    /**
     * 创建失败的响应
     * 
     * @param code    响应码
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> Response<T> fail(String code, String message) {
        return new Response<>(code, message, null);
    }

    /**
     * 创建非法参数的响应
     * 
     * @param message 错误信息
     * @return 非法参数响应
     */
    public static <T> Response<T> illegalArgument(String message) {
        return new Response<>(Code.ILLEGAL_PARAMETER, message, null);
    }

    /**
     * 创建未授权的响应
     * 
     * @param message 错误信息
     * @return 未授权响应
     */
    public static <T> Response<T> unauthorized(String message) {
        return new Response<>(Code.UNAUTHORIZED, message, null);
    }

    /**
     * 创建系统错误的响应
     * 
     * @param message 错误信息
     * @return 系统错误响应
     */
    public static <T> Response<T> systemError(String message) {
        return new Response<>(Code.SYSTEM_ERROR, message, null);
    }

    /**
     * 判断响应是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return Code.SUCCESS.equals(this.code);
    }
}
