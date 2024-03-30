package cn.youyou.yyrpc.demo.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.demo.api.Order;
import cn.youyou.yyrpc.demo.api.OrderService;
import org.springframework.stereotype.Component;

@Component
@YYProvider
public class OrderServiceImple implements OrderService {
    @Override
    public Order findById(Integer id) {
        return new Order(id.longValue(), 99.9f);
    }
}
