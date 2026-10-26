package pre.wc.mediamanager.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Video {
    private Long id;
    private String title;
    private String description;
    private String fileName;
    private String originalName;
    private String url;
    private String coverFileName;
    private String coverUrl;
    private Long size;
    private Integer duration;
    private String format;
    private String category;
    private String tags;
    private Long playCount;
    private Long likeCount;
    private Long ownerId;
    private String uploader;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
