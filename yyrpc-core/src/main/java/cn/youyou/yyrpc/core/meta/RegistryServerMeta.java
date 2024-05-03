package cn.youyou.yyrpc.core.meta;

import lombok.Data;

@Data
public class RegistryServerMeta {

    private String url;
    private boolean status;
    private boolean leader;
    private long version;

}
