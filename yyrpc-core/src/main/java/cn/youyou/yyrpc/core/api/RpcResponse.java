package cn.youyou.yyrpc.core.api;

import cn.youyou.yyrpc.core.RpcException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定义RPC传输的返回结构体
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponse<T>{

    boolean status;
    T data;
    RpcException ex;

}
