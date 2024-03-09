package cn.youyou.yyrpc.demo.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.demo.api.User;
import cn.youyou.yyrpc.demo.api.UserService;
import org.springframework.stereotype.Component;

@Component
@YYProvider
public class UserServiceImpl implements UserService {
    @Override
    public User findById(int id) {
        return new User(id, "YY-" + System.currentTimeMillis());
    }
}
