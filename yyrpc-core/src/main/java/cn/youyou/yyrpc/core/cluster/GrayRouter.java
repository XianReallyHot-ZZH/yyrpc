package cn.youyou.yyrpc.core.cluster;

import cn.youyou.yyrpc.core.api.Router;
import cn.youyou.yyrpc.core.meta.InstanceMeta;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
@Slf4j
public class GrayRouter implements Router<InstanceMeta> {

    private int grayRatio;

    private final Random random = new Random();

    public GrayRouter(int grayRatio) {
        this.grayRatio = grayRatio;
    }

    @Override
    public List<InstanceMeta> route(List<InstanceMeta> providers) {
        if (providers == null || providers.size() <= 1) {
            return providers;
        }

        List<InstanceMeta> normalNodes = new ArrayList<>();
        List<InstanceMeta> grayNodes = new ArrayList<>();

        providers.forEach(p -> {
            if ("true".equals(p.getParameters().get("gray"))) {
                grayNodes.add(p);
            } else {
                normalNodes.add(p);
            }
        });

        log.debug(" grayRouter grayNodes/normalNodes, grayRatio ===> {}/{},{}", grayNodes.size(), normalNodes.size(), grayRatio);

        // 只有既有灰度又有普通节点时，才有进行灰度选择的必要
        if (normalNodes.isEmpty() || grayNodes.isEmpty()) {
            return providers;
        }

        if (grayRatio <= 0) {
            return normalNodes;
        } else if (grayRatio >= 100) {
            return grayNodes;
        }

        if (random.nextInt(100) < grayRatio) {
            log.debug(" grayRouter result to grayNodes ===> {}", grayNodes);
            return grayNodes;
        } else {
            log.debug(" grayRouter result to normalNodes ===> {}", normalNodes);
            return normalNodes;
        }
    }
}
