package pre.wc.mediamanager.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pre.wc.mediamanager.config.MediaProperties;
import pre.wc.mediamanager.pojo.entity.Media;
import pre.wc.mediamanager.service.MediaService;

import java.util.List;

/**
 * 媒体管理接口层。
 * 通过构造器注入配置参数（MediaProperties）与业务服务（MediaService）。
 */
@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaProperties mediaProperties;
    private final MediaService mediaService;

    public MediaController(MediaProperties mediaProperties, MediaService mediaService) {
        this.mediaProperties = mediaProperties;
        this.mediaService = mediaService;
    }

    /**
     * 通过接口验证配置读取成功：GET /media/config
     */
    @GetMapping("/config")
    public MediaProperties config() {
        return mediaProperties;
    }

    /**
     * 媒体列表：GET /media/list
     */
    @GetMapping("/list")
    public List<Media> list() {
        return mediaService.list();
    }
}
