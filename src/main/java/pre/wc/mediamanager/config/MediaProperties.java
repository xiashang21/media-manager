package pre.wc.mediamanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 媒体管理器自定义配置。
 * 通过 @ConfigurationProperties 将 application.yml 中 media 前缀的参数绑定到本类字段，
 * 再注入到 Controller 中，实现配置参数的读取。
 */
@Component
@ConfigurationProperties(prefix = "media")
public class MediaProperties {

    private String name;
    private String version;
    private String uploadDir;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
