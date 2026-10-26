package pre.wc.mediamanager.entity.dto;

import lombok.Data;

@Data
public class VideoQueryDTO {
    private Integer page = 1;
    private Integer size = 8;
    private String keyword;
    private String category;
    private String sortBy = "create_time";
    private String order = "desc";

    /** 计算 offset，供 SQL LIMIT 使用，使用 long 避免大页码溢出。 */
    public long getOffset() {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentSize = size == null || size < 1 ? 8 : size;
        long offset = (long) (currentPage - 1) * currentSize;
        return Math.min(offset, Integer.MAX_VALUE);
    }

    /**
     * 缓存 key 使用稳定的字符串，日志与缓存监控中可以直接识别查询条件。
     * 这里刻意保留 keyword/category 的原始内容，避免 “abc” 与 “abc ” 命中同一个 key
     * 但 SQL 实际查询条件不同而返回错误结果。空值与默认值仍保持稳定映射。
     */
    public String cacheKey() {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentSize = size == null || size < 1 ? 8 : size;
        String currentKeyword = keyword == null ? "" : keyword;
        String currentCategory = category == null ? "全部" : category;
        String currentSort = sortBy == null || sortBy.isBlank() ? "create_time" : sortBy;
        String currentOrder = "asc".equalsIgnoreCase(order) ? "asc" : "desc";
        return "page=" + currentPage + "&size=" + currentSize
                + "&keyword=" + escapeKeyPart(currentKeyword)
                + "&category=" + escapeKeyPart(currentCategory)
                + "&sortBy=" + escapeKeyPart(currentSort)
                + "&order=" + currentOrder;
    }

    /** 转义 cache key 分隔符，避免查询值中的 & 或 = 造成 key 碰撞。 */
    private String escapeKeyPart(String value) {
        return value.replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D");
    }
}
