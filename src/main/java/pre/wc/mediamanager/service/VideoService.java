package pre.wc.mediamanager.service;

import org.springframework.web.multipart.MultipartFile;
import pre.wc.mediamanager.common.PageResult;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;
import pre.wc.mediamanager.entity.dto.VideoVO;

import java.util.List;

public interface VideoService {
    VideoVO upload(MultipartFile file, String title, String description,
                   String category, String tags, String uploader, Integer clientDuration);

    PageResult<VideoVO> page(VideoQueryDTO query);

    VideoVO getDetail(Long id);

    void delete(Long id);

    void incrementPlay(Long id);

    void like(Long id);

    List<String> listCategories();

    List<VideoVO> hotVideos(int limit);
}


