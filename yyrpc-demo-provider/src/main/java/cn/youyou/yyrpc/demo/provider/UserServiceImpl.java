package cn.youyou.yyrpc.demo.provider;

import cn.youyou.yyrpc.core.annotation.YYProvider;
import cn.youyou.yyrpc.core.api.RpcContext;
import cn.youyou.yyrpc.demo.api.User;
import cn.youyou.yyrpc.demo.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@YYProvider
public class UserServiceImpl implements UserService {

    @Autowired
    Environment environment;

    private String timeoutPorts = "8081,8094";;

    @Override
    public User findById(int id) {
        return new User(id, "YY-V1" + environment.getProperty("server.port") + "_" + System.currentTimeMillis());
    }

    @Override
    public User findById(int id, String name) {
        return new User(id, "YY-" + name + "_" + System.currentTimeMillis());
    }

    @Override
    public long getId(long id) {
        return id;
    }

    @Override
    public long getId(User user) {
        return user.getId().longValue();
    }

    @Override
    public long getId(float id) {
        return 1L;
    }

    @Override
    public String getName() {
        return "YY2022";
    }

    @Override
    public String getName(int id) {
        return "YouYou-" + id;
    }

    @Override
    public int[] getIds() {
        return new int[] {100,200,300};
    }

    @Override
    public long[] getLongIds() {
        return new long[]{1,2,3};
    }

    @Override
    public int[] getIds(int[] ids) {
        return ids;
    }

    @Override
    public User[] findUsers(User[] users) {
        return users;
    }

    @Override
    public List<User> getList(List<User> userList) {
        User[] users = userList.toArray(new User[0]);
        System.out.println(" ==> userList.toArray()[] = ");
        Arrays.stream(users).forEach(System.out::println);
        userList.add(new User(2024, "YY2024"));
        return userList;
    }

    @Override
    public Map<String, User> getMap(Map<String, User> userMap) {
        userMap.values().forEach(x -> System.out.println(x.getClass()));
        User[] users = userMap.values().toArray(new User[userMap.size()]);
        System.out.println(" ==> userMap.values().toArray()[] = ");
        Arrays.stream(users).forEach(System.out::println);
        userMap.put("A2024", new User(2024, "KK2024"));
        return userMap;
    }

    @Override
    public Boolean getFlag(boolean flag) {
        return !flag;
    }

    @Override
    public User findById(long id) {
        return new User(Long.valueOf(id).intValue(), "YY");
    }

    @Override
    public User ex(boolean flag) {
        if(flag) throw new RuntimeException("just throw an exception(Mock)");
        return new User(100, "YY100");
    }

    @Override
    public User find(int timeout) {
        String port = environment.getProperty("server.port");
        if (Arrays.stream(timeoutPorts.split(",")).anyMatch(port::equals)) {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return new User(1001, "YY1001-" + port);
    }

    @Override
    public void setTimeoutPorts(String timeoutPorts) {
        this.timeoutPorts = timeoutPorts;
    }

    /**
     * 获取打印跨节点传参的指定参数，并顺便打印一下
     * @param key
     * @return
     */
    @Override
    public String echoParameter(String key) {
        System.out.println(" ====>> RpcContext.ContextParameters: ");
        RpcContext.ContextParameters.get().forEach((k, v) -> System.out.println(k + " -> " + v));
        return RpcContext.ContextParameters.get().get(key);
    }
}
