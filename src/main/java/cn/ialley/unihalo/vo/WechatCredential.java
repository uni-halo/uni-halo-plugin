package cn.ialley.unihalo.vo;

/**
 * 微信小程序凭据（来自 Halo Secret 扩展，仅服务端持有）。
 *
 * <p>严禁把本对象序列化进任何响应、日志或异常消息。
 * 注意 Halo 的 Secret 不做静态加密，保护来自「与 ConfigMap 分离 + RBAC」，
 * 因此站点备份文件中同样包含明文凭据，备份外发等同于泄露。</p>
 *
 * @param appId     小程序 AppID
 * @param appSecret 小程序 AppSecret
 * @author 小莫唐尼
 */
public record WechatCredential(String appId, String appSecret) {

    @Override
    public String toString() {
        return "WechatCredential[appId=" + appId + ", appSecret=***]";
    }
}
