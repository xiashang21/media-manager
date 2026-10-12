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
}


