package cn.ialley.unihalo.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.enums.CandidateType;
import cn.ialley.unihalo.services.AuditDataService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;

/**
 * 审核模式公开接口（app 端/小程序端，匿名可访问）。
 *
 * 联动审核模式开关（{@code AuditDataConfig.spec.enabled}）：enabled=true 时返回剔除
 * 失效引用后的选中列表（小程序端据此过滤真实数据展示），并附带 {@code categoryDetails}
 * 分类完整快照（剔除失效、按配置顺序），供 app 端审核模式下免请求映射 ICategory；
 * 开关关闭时返回 {@code {enabled:false}}。匿名放行由 role-template-anonymous.yaml 全局规则覆盖。
 *
 * @author 小莫唐尼
 */
@Component
public class AuditDataPublicEndpoint implements CustomEndpoint {

    private final AuditDataService auditDataService;

    public AuditDataPublicEndpoint(AuditDataService auditDataService) {
        this.auditDataService = auditDataService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.AUDIT_DATA_API_BASE_PATH, this::getAuditData)
                .build();
    }

    private Mono<ServerResponse> getAuditData(ServerRequest request) {
        return auditDataService.getDetail().flatMap(detail -> {
            boolean enabled = detail.config() != null
                    && detail.config().getSpec() != null
                    && Boolean.TRUE.equals(detail.config().getSpec().getEnabled());
            Map<String, Object> body = new HashMap<>();
            body.put("enabled", enabled);
            if (!enabled) {
                return ServerResponse.ok().bodyValue(body);
            }
            return auditDataService.getEffective()
                    .flatMap(effective -> {
                        body.put("spec",
                                effective.getSpec() == null ? Map.of() : effective.getSpec());
                        // 分类完整快照（剔除失效、按配置顺序，app 端审核模式免请求映射 ICategory）
                        var categoryDetails = detail.selections()
                                .get(CandidateType.category);
                        if (categoryDetails != null && !categoryDetails.isEmpty()) {
                            body.put("categoryDetails", categoryDetails);
                        }
                        return ServerResponse.ok().bodyValue(body);
                    });
        });
    }
}
