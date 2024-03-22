package cn.youyou.yyrpc.core.consumer;

import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;

public interface HttpInvoker {

    RpcResponse<?> post(RpcRequest rpcRequest, String url);

}
