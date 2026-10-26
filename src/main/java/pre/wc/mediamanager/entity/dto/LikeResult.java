package pre.wc.mediamanager.entity.dto;

/**
 * 点赞切换结果：liked 表示切换后当前用户是否点赞，likeCount 表示切换后的总点赞数。
 */
public record LikeResult(boolean liked, long likeCount) {
}
