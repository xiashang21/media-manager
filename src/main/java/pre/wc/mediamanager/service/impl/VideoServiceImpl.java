package pre.wc.mediamanager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.ForbiddenException;
import pre.wc.mediamanager.common.PageResult;
import pre.wc.mediamanager.config.CacheConfig;
import pre.wc.mediamanager.entity.Video;
import pre.wc.mediamanager.entity.dto.LikeResult;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;
import pre.wc.mediamanager.entity.dto.VideoVO;
import pre.wc.mediamanager.mapper.VideoMapper;
import pre.wc.mediamanager.security.UserContext;
import pre.wc.mediamanager.security.UserPrincipal;
import pre.wc.mediamanager.service.VideoService;
import pre.wc.mediamanager.util.VideoMetadataUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;

    @Value("${video.upload-path}")
    private String uploadPath;

    @Value("${video.cover-path}")
    private String coverPath;

    private static final Marker BUSINESS = MarkerFactory.getMarker("BUSINESS");

    private static final List<String> ALLOWED_FORMATS =
            Arrays.asList("mp4", "webm", "mkv", "mov", "avi");

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("create_time", "play_count", "like_count", "size");

    private static final long MAX_COVER_SIZE = 5L * 1024 * 1024;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 50;
    private static final int MAX_TAGS_LENGTH = 255;
    private static final int MAX_UPLOADER_LENGTH = 50;
    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_HOT_LIMIT = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.VIDEO_PAGE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_HOT, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_CATEGORIES, allEntries = true)
    })
    public VideoVO upload(MultipartFile file, MultipartFile cover, String title,
                          String description, String category, String tags,
                          Integer clientDuration) {
        UserPrincipal owner = UserContext.requireUser();
        if (file == null || file.isEmpty()) {
            throw new BusinessException("视频文件为空");
        }

        String originalName = normalizeOriginalName(file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(originalName);
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_FORMATS.contains(ext)) {
            throw new BusinessException("不支持的视频格式：" + (ext.isEmpty() ? "未知" : ext));
        }

        String normalizedTitle = trimToNull(title);
        if (normalizedTitle == null) {
            normalizedTitle = originalName;
        }
        normalizedTitle = truncate(normalizedTitle, MAX_TITLE_LENGTH);
        String normalizedDescription = validateLength(trimToNull(description), "简介", MAX_DESCRIPTION_LENGTH);
        String normalizedCategory = defaultIfBlank(
                validateLength(trimToNull(category), "分类", MAX_CATEGORY_LENGTH), "默认");
        String normalizedTags = normalizeTags(tags);

        Path root = getUploadRoot();
        Path coverRoot = getCoverRoot();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = resolveStoredFile(root, fileName);
        Path coverTarget = null;
        String coverFileName = null;

        try {
            Files.createDirectories(root);
            file.transferTo(target.toFile());

            if (cover != null && !cover.isEmpty()) {
                coverTarget = storeCover(cover, coverRoot);
                coverFileName = coverTarget.getFileName().toString();
            }

            int duration = VideoMetadataUtils.readDurationSeconds(target, ext);
            if (duration <= 0 && clientDuration != null && clientDuration > 0) {
                duration = clientDuration;
            }

            Video video = new Video();
            video.setTitle(normalizedTitle);
            video.setDescription(normalizedDescription);
            video.setFileName(fileName);
            video.setOriginalName(originalName);
            video.setUrl("/api/video/stream/" + fileName);
            video.setCoverFileName(coverFileName);
            video.setCoverUrl(coverFileName == null ? null : "/api/video/cover/" + coverFileName);
            video.setSize(file.getSize());
            video.setDuration(duration);
            video.setFormat(ext);
            video.setCategory(normalizedCategory);
            video.setTags(normalizedTags);
            video.setOwnerId(owner.id());
            video.setUploader(truncate(owner.displayName(), MAX_UPLOADER_LENGTH));
            video.setPlayCount(0L);
            video.setLikeCount(0L);
            video.setStatus(1);
            video.setCreateTime(LocalDateTime.now());
            video.setUpdateTime(LocalDateTime.now());

            if (videoMapper.insert(video) != 1) {
                throw new BusinessException("视频信息保存失败");
            }
            log.info(BUSINESS, "视频上传成功 videoId={} ownerId={} file={} cover={} size={}",
                    video.getId(), owner.id(), fileName, coverFileName, file.getSize());
            return toVO(video);
        } catch (IOException e) {
            deleteQuietly(target);
            deleteQuietly(coverTarget);
            throw new BusinessException("文件保存失败：" + e.getMessage());
        } catch (RuntimeException e) {
            deleteQuietly(target);
            deleteQuietly(coverTarget);
            throw e;
        }
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.VIDEO_PAGE, key = "#query.cacheKey()")
    public PageResult<VideoVO> page(VideoQueryDTO query) {
        if (query == null) {
            query = new VideoQueryDTO();
        }
        query.setPage(query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage());
        query.setSize(normalizePageSize(query.getSize()));
        if (!ALLOWED_SORT_FIELDS.contains(query.getSortBy())) {
            query.setSortBy("create_time");
        }
        if (!"asc".equalsIgnoreCase(query.getOrder())) {
            query.setOrder("desc");
        }

        long total = videoMapper.countList(query);
        List<Video> list = total > 0 ? videoMapper.selectList(query) : Collections.emptyList();
        List<VideoVO> vos = list.stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(vos, total, query.getPage(), query.getSize());
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.VIDEO_DETAIL, key = "#id")
    public VideoVO getDetail(Long id) {
        requirePositiveId(id);
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw notFound();
        }
        return toVO(video);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.VIDEO_PAGE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_HOT, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_DETAIL, key = "#id")
    })
    public VideoVO updateCover(Long id, MultipartFile cover) {
        requirePositiveId(id);
        UserPrincipal principal = UserContext.requireUser();
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw notFound();
        }
        assertCanManage(video, principal);
        if (cover == null || cover.isEmpty()) {
            throw new BusinessException("请选择要上传的封面图片");
        }

        Path coverRoot = getCoverRoot();
        Path newCover = null;
        try {
            newCover = storeCover(cover, coverRoot);
            String coverFileName = newCover.getFileName().toString();
            String coverUrl = "/api/video/cover/" + coverFileName;
            if (videoMapper.updateCover(id, coverFileName, coverUrl) != 1) {
                throw new BusinessException("封面更新失败");
            }
            if (StringUtils.hasText(video.getCoverFileName())) {
                deleteQuietly(resolveStoredFile(coverRoot, video.getCoverFileName()));
            }
            video.setCoverFileName(coverFileName);
            video.setCoverUrl(coverUrl);
            log.info(BUSINESS, "视频封面更新成功 videoId={} ownerId={} cover={}",
                    id, principal.id(), coverFileName);
            return toVO(video);
        } catch (IOException e) {
            deleteQuietly(newCover);
            throw new BusinessException("封面保存失败：" + e.getMessage());
        } catch (RuntimeException e) {
            deleteQuietly(newCover);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.VIDEO_PAGE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_HOT, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_DETAIL, key = "#id")
    })
    public void delete(Long id) {
        requirePositiveId(id);
        UserPrincipal principal = UserContext.requireUser();
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw notFound();
        }
        assertCanManage(video, principal);

        if (videoMapper.deleteById(id) != 1) {
            throw new BusinessException("视频删除失败");
        }

        Path file = resolveStoredFile(getUploadRoot(), video.getFileName());
        Path coverFile = StringUtils.hasText(video.getCoverFileName())
                ? resolveStoredFile(getCoverRoot(), video.getCoverFileName()) : null;
        runAfterCommit(() -> {
            deleteQuietly(file);
            deleteQuietly(coverFile);
        });
        log.info(BUSINESS, "视频删除成功 videoId={} ownerId={} fileCleanup=afterCommit", id, principal.id());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 播放是最高频的写操作，如果每次播放都清空分页缓存会导致缓存几乎失效，
    // 因此这里只失效热门榜和详情，分页里的播放量允许在 TTL 内短暂滞后。
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.VIDEO_HOT, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_DETAIL, key = "#id")
    })
    public void incrementPlay(Long id) {
        requirePositiveId(id);
        if (videoMapper.incrementPlayCount(id) != 1) {
            throw notFound();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.VIDEO_PAGE, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_HOT, allEntries = true),
            @CacheEvict(cacheNames = CacheConfig.VIDEO_DETAIL, key = "#id")
    })
    public LikeResult like(Long id) {
        requirePositiveId(id);
        UserPrincipal principal = UserContext.requireUser();
        if (videoMapper.selectById(id) == null) {
            throw notFound();
        }

        boolean liked;
        if (videoMapper.countLike(principal.id(), id) > 0) {
            videoMapper.deleteLike(principal.id(), id);
            videoMapper.decrementLikeCount(id);
            liked = false;
        } else {
            try {
                videoMapper.insertLike(principal.id(), id);
            } catch (DuplicateKeyException e) {
                // 并发重复点赞时唯一索引兜底，视为已经点赞，不再重复加一。
                liked = true;
                Long count = videoMapper.selectLikeCount(id);
                return new LikeResult(true, count == null ? 0L : count);
            }
            videoMapper.incrementLikeCount(id);
            liked = true;
        }
        Long likeCount = videoMapper.selectLikeCount(id);
        return new LikeResult(liked, likeCount == null ? 0L : likeCount);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.VIDEO_CATEGORIES, key = "'all'")
    public List<String> listCategories() {
        List<String> list = new ArrayList<>(videoMapper.selectCategories());
        if (!list.contains("全部")) {
            list.add(0, "全部");
        }
        return list;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.VIDEO_HOT, key = "#limit")
    public List<VideoVO> hotVideos(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_HOT_LIMIT));
        return videoMapper.selectHot(safeLimit).stream().map(this::toVO).collect(Collectors.toList());
    }

    private Path storeCover(MultipartFile cover, Path coverRoot) throws IOException {
        if (cover.getSize() > MAX_COVER_SIZE) {
            throw new BusinessException("封面大小不能超过 5MB");
        }
        String extension = detectCoverExtension(cover);
        Files.createDirectories(coverRoot);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = resolveStoredFile(coverRoot, fileName);
        try {
            cover.transferTo(target.toFile());
            return target;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    private String detectCoverExtension(MultipartFile cover) {
        byte[] header;
        try (InputStream input = cover.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException e) {
            throw new BusinessException("封面读取失败");
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return "png";
        }
        if (header.length >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return "webp";
        }
        throw new BusinessException("封面仅支持 JPG、PNG 或 WebP 图片");
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String normalizeOriginalName(String originalName) {
        String cleaned = StringUtils.cleanPath(originalName == null ? "" : originalName).trim();
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
            throw new BusinessException("文件名无效");
        }
        if (cleaned.length() > 255) {
            throw new BusinessException("原始文件名不能超过 255 个字符");
        }
        return cleaned;
    }

    private String normalizeTags(String tags) {
        String trimmed = validateLength(trimToNull(tags), "标签", MAX_TAGS_LENGTH);
        if (trimmed == null) {
            return null;
        }
        String normalized = Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }

    private String validateLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private BusinessException notFound() {
        return new BusinessException(404, "视频不存在或不可用");
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("视频 ID 无效");
        }
    }

    private void assertCanManage(Video video, UserPrincipal principal) {
        if (!principal.isAdmin() && !principal.id().equals(video.getOwnerId())) {
            throw new ForbiddenException("只能管理自己上传的视频");
        }
    }

    private Path getUploadRoot() {
        if (!StringUtils.hasText(uploadPath)) {
            throw new BusinessException("未配置视频上传目录");
        }
        return Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    private Path getCoverRoot() {
        if (!StringUtils.hasText(coverPath)) {
            throw new BusinessException("未配置封面上传目录");
        }
        return Paths.get(coverPath).toAbsolutePath().normalize();
    }

    private Path resolveStoredFile(Path root, String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("存储文件名无效");
        }
        Path file = root.resolve(fileName).normalize();
        if (file.equals(root) || !file.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        return file;
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("临时文件清理失败 path={}", path, e);
        }
    }

    private VideoVO toVO(Video video) {
        VideoVO vo = new VideoVO();
        BeanUtils.copyProperties(video, vo);
        vo.setSizeText(formatSize(video.getSize()));
        vo.setDurationText(formatDuration(video.getDuration()));
        vo.setTags(StringUtils.hasText(video.getTags()) ? video.getTags().split(",") : new String[0]);
        return vo;
    }

    private String formatSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = size;
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format("%.2f %s", value, units[unit]);
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "--:--";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;
        return hours > 0
                ? String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
                : String.format("%02d:%02d", minutes, remainingSeconds);
    }
}
