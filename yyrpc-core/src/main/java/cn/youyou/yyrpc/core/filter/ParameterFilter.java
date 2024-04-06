package cn.youyou.yyrpc.core.filter;

import cn.youyou.yyrpc.core.api.Filter;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.core.api.RpcRequest;
import cn.youyou.yyrpc.core.api.RpcResponse;

import java.io.File;
import java.util.Map;

/**
 * 对跨节点传输的信息拼装到请求中
 */
public class ParameterFilter implements Filter {
    @Override
    public Object preFilter(RpcRequest request) {
        Map<String, String> params = RpcContext.ContextParameters.get();
        if (!params.isEmpty()) {
            request.getParams().putAll(params);
        }
        return null;
    }

    @Override
    public Object postFilter(RpcRequest request, RpcResponse response, Object result) {
        // 这个要释放，因为是和线程绑定的，不然下次的内容和上次的就杂糅在一起，不准确了
        RpcContext.ContextParameters.get().clear();
        return null;
    }
}
