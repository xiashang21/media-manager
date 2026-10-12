package pre.wc.mediamanager.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    private List<T> records;   // 当前页数据
    private Long total;        // 总记录数
    private Integer page;      // 当前页码
    private Integer size;      // 每页条数
    private Integer totalPages;// 总页数

    public static <T> PageResult<T> of(List<T> records, Long total, Integer page, Integer size) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, totalPages);
    }
}

