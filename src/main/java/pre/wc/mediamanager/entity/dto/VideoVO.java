package pre.wc.mediamanager.entity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoVO {
    private Long id;
    private String title;
    private String description;
    private String url;
    private String coverUrl;
    private String sizeText;
    private Integer duration;
    private String durationText;
    private String format;
    private String category;
    private String[] tags;
    private Long playCount;
    private Long likeCount;
    private Long ownerId;
    private String uploader;
    private LocalDateTime createTime;
}
