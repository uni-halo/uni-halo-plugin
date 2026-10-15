package cn.ialley.unihalo.endpoint;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import cn.ialley.unihalo.constants.Constants;
import cn.ialley.unihalo.scheme.Faq;
import cn.ialley.unihalo.services.FaqService;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.extension.GroupVersion;
import static cn.ialley.unihalo.utils.QueryParams.parseInt;

/**
 * 常见问题接口（控制台，需登录）。
 *
 * @author 小莫唐尼
 */
@Component
public class FaqEndpoint implements CustomEndpoint {

    private final FaqService faqService;

    public FaqEndpoint(FaqService faqService) {
        this.faqService = faqService;
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion(Constants.CONSOLE_CUSTOM_API_GROUP_NAME);
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
                .GET(Constants.FAQ_API_BASE_PATH, this::listFaqs)
                .POST(Constants.FAQ_API_BASE_PATH, this::createFaq)
                .PUT(Constants.FAQ_API_BASE_PATH + "/{name}", this::updateFaq)
                .DELETE(Constants.FAQ_API_BASE_PATH + "/{name}", this::deleteFaq)
                .build();
    }

    private Mono<ServerResponse> listFaqs(ServerRequest request) {
        int page = parseInt(request.queryParam("page"), 1);
        int size = parseInt(request.queryParam("size"), 20);
        Boolean visible = request.queryParam("visible")
                .map(v -> Boolean.parseBoolean(v.trim()))
                .orElse(null);
        String keyword = request.queryParam("keyword").orElse("").trim();
        String category = request.queryParam("category").orElse("").trim();
        return faqService.list(keyword, category, visible, page, size)
                .flatMap(result -> ServerResponse.ok().bodyValue(result));
    }

    private Mono<ServerResponse> createFaq(ServerRequest request) {
        return request.bodyToMono(Faq.class)
                .flatMap(faqService::create)
                .flatMap(created -> ServerResponse.ok().bodyValue(created))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    private Mono<ServerResponse> updateFaq(ServerRequest request) {
        String name = request.pathVariable("name");
        return request.bodyToMono(Faq.class)
                .doOnNext(faq -> {
                    if (faq.getMetadata() == null) {
                        faq.setMetadata(new run.halo.app.extension.Metadata());
                    }
                    faq.getMetadata().setName(name);
                })
                .flatMap(faqService::update)
                .flatMap(updated -> ServerResponse.ok().bodyValue(updated))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }

    private Mono<ServerResponse> deleteFaq(ServerRequest request) {
        return faqService.delete(request.pathVariable("name"))
                .then(ServerResponse.ok().bodyValue(Map.of("success", true)))
                .onErrorResume(IllegalArgumentException.class,
                        e -> ServerResponse.badRequest().bodyValue(Map.of("message", e.getMessage())));
    }
}
