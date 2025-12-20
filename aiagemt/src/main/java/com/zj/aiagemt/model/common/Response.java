package com.zj.aiagemt.model.common;

import com.zj.aiagemt.model.enums.ResponseCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    private static final long serialVersionUID = 7000723935764546321L;

    private String code;
    private String info;
    private T data;


    /**
     * 创建成功的响应
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), data);
    }

    /**
     * 创建成功的响应（无数据）
     * @return 成功响应
     */
    public static <T> Response<T> success() {
        return new Response<T>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), null);
    }

    /**
     * 创建失败的响应
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> Response<T> fail(String message) {
        return new Response<T>(ResponseCode.UN_ERROR.getCode(), message, null);
    }

    /**
     * 创建失败的响应
     * @param code 响应码
     * @param message 错误信息
     * @return 失败响应
     */
    public static <T> Response<T> fail(String code, String message) {
        return new Response<T>(code, message, null);
    }

    /**
     * 创建非法参数的响应
     * @param message 错误信息
     * @return 非法参数响应
     */
    public static <T> Response<T> illegalArgument(String message) {
        return new Response<T>(ResponseCode.ILLEGAL_PARAMETER.getCode(), message, null);
    }

    /**
     * 判断响应是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return ResponseCode.SUCCESS.getCode().equals(this.code);
    }
}