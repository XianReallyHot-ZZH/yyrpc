package cn.youyou.yyrpc.core;

import lombok.Data;

@Data
public class RpcException extends RuntimeException{

    private String errorCode;

    public RpcException() {

    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }

    public RpcException(Throwable cause, String errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public RpcException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    // X => 技术类异常：
    // Y => 业务类异常：
    // Z => unknown, 搞不清楚，再归类到X或Y
    public static final String SocketTimeoutEx = "X001" + "-" + "http_invoke_timeout";
    public static final String NoSuchMethodEx = "X002" + "-" + "method_not_exists";
    public static final String ExceedLimitEx = "X003" + "-" + "tps_exceed_limit";

    public static final String UnknownEx = "Z001" + "-" + "unknown";

}
