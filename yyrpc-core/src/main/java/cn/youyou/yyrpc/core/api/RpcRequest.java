package cn.youyou.yyrpc.core.api;

import lombok.Data;
import lombok.ToString;

/**
 * 定义RPC传输的请求结构体
 */
@Data
@ToString
public class RpcRequest {

    private String service;     // 接口：cn.kimmking.kkrpc.demo.api.UserService
    private String methodSign;      // 方法：findById
    private Object[] args;      // 参数： 100

}
