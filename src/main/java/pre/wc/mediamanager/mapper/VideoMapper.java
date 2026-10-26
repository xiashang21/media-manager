package pre.wc.mediamanager.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import pre.wc.mediamanager.entity.Video;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;

import java.util.List;

@Mapper
public interface VideoMapper {

    /** 插入视频，主键回填 */
    int insert(Video video);

    /** 根据 ID 查询（未删除） */
    Video selectById(@Param("id") Long id);

    /** 条件查询列表 */
    List<Video> selectList(@Param("q") VideoQueryDTO query);

    /** 条件统计总数 */
    long countList(@Param("q") VideoQueryDTO query);

    /** 逻辑删除 */
    int deleteById(@Param("id") Long id);

    /** 更新视频封面 */
    int updateCover(@Param("id") Long id,
                    @Param("coverFileName") String coverFileName,
                    @Param("coverUrl") String coverUrl);

    /** 播放量 +1 */
    int incrementPlayCount(@Param("id") Long id);

    /** 点赞数 +1 */
    int incrementLikeCount(@Param("id") Long id);

    /** 点赞数 -1（不低于 0） */
    int decrementLikeCount(@Param("id") Long id);

    /** 查询当前点赞数 */
    Long selectLikeCount(@Param("id") Long id);

    /** 当前用户是否已点赞 */
    int countLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 写入点赞记录 */
    int insertLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 删除点赞记录 */
    int deleteLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    /** 所有分类去重 */
    List<String> selectCategories();

    /** 热门视频（按播放量倒序） */
    List<Video> selectHot(@Param("limit") int limit);
}
