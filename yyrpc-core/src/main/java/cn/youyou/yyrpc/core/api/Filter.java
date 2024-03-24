package cn.youyou.yyrpc.core.api;

/**
 * todo：这里面方法的入参定义我觉得还有待商榷，改进，对filter在消费端的使用逻辑也有待优化
 */
public interface Filter {

    Object preFilter(RpcRequest request);

    Object postFilter(RpcRequest request, RpcResponse response, Object result);

    Filter DEFAULT = new Filter() {
        @Override
        public Object preFilter(RpcRequest request) {
            return null;
        }

        @Override
        public Object postFilter(RpcRequest request, RpcResponse response, Object result) {
            return null;
        }
    };

}
