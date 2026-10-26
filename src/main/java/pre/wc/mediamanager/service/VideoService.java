package pre.wc.mediamanager.service;

import org.springframework.web.multipart.MultipartFile;
import pre.wc.mediamanager.common.PageResult;
import pre.wc.mediamanager.entity.dto.LikeResult;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;
import pre.wc.mediamanager.entity.dto.VideoVO;

import java.util.List;

public interface VideoService {

    VideoVO upload(MultipartFile file, MultipartFile cover, String title, String description,
                   String category, String tags, Integer clientDuration);

    PageResult<VideoVO> page(VideoQueryDTO query);

    VideoVO getDetail(Long id);

    VideoVO updateCover(Long id, MultipartFile cover);

    void delete(Long id);

    void incrementPlay(Long id);

    LikeResult like(Long id);

    List<String> listCategories();

    List<VideoVO> hotVideos(int limit);
}
