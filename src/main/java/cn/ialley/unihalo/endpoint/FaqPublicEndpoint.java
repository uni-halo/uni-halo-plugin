package cn.ialley.unihalo.endpoint;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.services.FaqService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import static cn.ialley.unihalo.utils.QueryParams.parseInt;

/**
 * 常见问题公开接口（app 端，匿名可访问）。
 *
 * 列表仅返回 visible=true 条目，size<=0 返回全量（app 端一次取全部）；
 * 公开接口已被 role-template-anonymous.yaml 的 api.unihalo.ialley.cn 全资源规则覆盖。
 *
 * @author 小莫唐尼
 */
@Component
public class FaqPublicEndpoint implements CustomEndpoint {

    private final FaqService faqService;

    public FaqPublicEndpoint(FaqService faqService) {
        this.faqService = faqService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.PUBLIC_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.FAQ_API_BASE_PATH, this::listFaqs)
                .GET(Constants.FAQ_API_BASE_PATH + "/{name}", this::getFaq)
                .build();
    }

    /**
     * 公开列表（keyword 模糊匹配标题/摘要/分类，category 非空时按分类过滤；
     * size 缺省 0 = 返回全量；返回 Halo Extension 原始 ListResult 结构）。
     */
    private Mono<ServerResponse> listFaqs(ServerRequest request) {
        String keyword = request.queryParam("keyword").orElse("");
        String category = request.queryParam("category").orElse("");
        int page = parseInt(request.queryParam("page"), 1);
        int size = parseInt(request.queryParam("size"), 0);
        return faqService.listPublic(keyword, category, page, size)
                .flatMap(body -> ServerResponse.ok().bodyValue(body));
    }

    /**
     * 公开详情（仅 visible=true，隐藏条目按不存在处理返回 404；
     * 返回 Halo Extension 原始结构）。
     */
    private Mono<ServerResponse> getFaq(ServerRequest request) {
        String name = request.pathVariable("name");
        return faqService.getPublicByName(name)
                .flatMap(body -> ServerResponse.ok().bodyValue(body))
                .onErrorResume(cn.ialley.unihalo.exception.NotFoundException.class,
                        e -> ServerResponse.notFound().build());
    }
}
