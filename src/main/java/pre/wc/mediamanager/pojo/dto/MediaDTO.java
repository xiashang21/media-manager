package pre.wc.mediamanager.pojo.dto;

/**
 * 媒体数据传输对象（pojo/dto 层）。
 * 用于接口层与外部交互，隔离实体与前端展示。
 */
public class MediaDTO {

    private Long id;
    private String title;
    private String type;
    private String url;

    public MediaDTO() {
    }

    public MediaDTO(Long id, String title, String type, String url) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.url = url;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
