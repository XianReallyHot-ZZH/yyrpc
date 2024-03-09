package cn.youyou.yyrpc.core.api;

import lombok.Data;

/**
 * 定义RPC传输的请求结构体
 */
@Data
public class RpcRequest {

    private String service;     // 接口：cn.kimmking.kkrpc.demo.api.UserService
    private String method;      // 方法：findById
    private Object[] args;      // 参数： 100

}
