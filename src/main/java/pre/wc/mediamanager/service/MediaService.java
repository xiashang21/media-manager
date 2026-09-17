package pre.wc.mediamanager.service;

import org.springframework.stereotype.Service;
import pre.wc.mediamanager.pojo.entity.Media;

import java.util.List;

/**
 * 媒体管理业务层
 */
@Service
public class MediaService {

    public List<Media> list() {
        return List.of(
                new Media(1L, "实训视频", "video", "https://example.com/video.mp4"),
                new Media(2L, "实训截图", "image", "https://example.com/image.png")
        );
    }
}
