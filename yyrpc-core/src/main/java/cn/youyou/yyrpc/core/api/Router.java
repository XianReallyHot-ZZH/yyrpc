package cn.youyou.yyrpc.core.api;

import java.util.List;

public interface Router<T> {

    List<T> route(List<T> providers);

    /**
     * 默认路由策略：不选择，直接全部返回
     */
    Router Default = providers -> providers;

}
