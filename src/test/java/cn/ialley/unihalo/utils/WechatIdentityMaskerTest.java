package cn.ialley.unihalo.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 微信标识脱敏规则测试。
 */
@DisplayName("微信标识脱敏")
class WechatIdentityMaskerTest {

    @Test
    @DisplayName("null / 空串 / 纯空白 → 返回 null（调用方按未绑定处理）")
    void nullAndBlankReturnNull() {
        assertThat(WechatIdentityMasker.mask(null)).isNull();
        assertThat(WechatIdentityMasker.mask("")).isNull();
        assertThat(WechatIdentityMasker.mask("   ")).isNull();
    }

    @Test
    @DisplayName("长度不足 11（前6+后4不够遮）→ 全部打星号，星号数 = 原文长度")
    void shortValueFullyMasked() {
        assertThat(WechatIdentityMasker.mask("0123456789")).isEqualTo("**********");
        assertThat(WechatIdentityMasker.mask("oABC1")).isEqualTo("*****");
    }

    @Test
    @DisplayName("正常长度 → 保留前 6 位与后 4 位，中间固定 6 个星号")
    void normalValueKeepsHead6AndTail4() {
        // 长度 11：前 6 + ****** + 后 4
        assertThat(WechatIdentityMasker.mask("0123456789a"))
                .isEqualTo("012345******789a");
    }

    @Test
    @DisplayName("中间星号段固定 6 个：openid 与 unionid 输出等长，不泄露真实长度")
    void middleStarsAreFixedLength() {
        // 长度 27 与 40 的标识输出均为 16 字符
        String openid = "oX7kQ9mN2pR4sT6uV8wY0zA1bC3";
        String unionid = "union_" + openid + "_long_suffix_0123456789";
        assertThat(WechatIdentityMasker.mask(openid)).hasSize(16);
        assertThat(WechatIdentityMasker.mask(unionid)).hasSize(16);
        assertThat(WechatIdentityMasker.mask(openid))
                .isEqualTo(openid.substring(0, 6) + "******"
                        + openid.substring(openid.length() - 4));
    }
}
