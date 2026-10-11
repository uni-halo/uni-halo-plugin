package cn.ialley.unihalo.vo;

import lombok.Data;

/**
 * 猜画作答结果视图：仅猜中时回传谜底（给猜中者本人看），猜错不泄露任何谜底信息。
 *
 * @author 小莫唐尼
 */
@Data
public class GuessResultVo {

    public static final String RESULT_CORRECT = "CORRECT";
    public static final String RESULT_CLOSE = "CLOSE";
    public static final String RESULT_WRONG = "WRONG";

    /**
     * 作答结果：CORRECT / CLOSE（很接近）/ WRONG
     */
    private String result;

    /**
     * 谜底（仅 CORRECT 时非空，供猜中者本人查看）
     */
    private String answer;

    public static GuessResultVo of(String result, String answer) {
        var vo = new GuessResultVo();
        vo.setResult(result);
        vo.setAnswer(answer);
        return vo;
    }

    public static GuessResultVo correct(String answer) {
        return of(RESULT_CORRECT, answer);
    }

    public static GuessResultVo close() {
        return of(RESULT_CLOSE, null);
    }

    public static GuessResultVo wrong() {
        return of(RESULT_WRONG, null);
    }
}
