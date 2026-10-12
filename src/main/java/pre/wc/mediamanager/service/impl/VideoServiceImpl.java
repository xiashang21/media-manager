package pre.wc.mediamanager.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.PageResult;
import pre.wc.mediamanager.entity.Video;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;
import pre.wc.mediamanager.entity.dto.VideoVO;
import pre.wc.mediamanager.mapper.VideoMapper;
import pre.wc.mediamanager.service.VideoService;
import pre.wc.mediamanager.util.VideoMetadataUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;

    @Value("${video.upload-path}")
    private String uploadPath;

    private static final List<String> ALLOWED_FORMATS =
            Arrays.asList("mp4", "webm", "mkv", "mov", "avi");

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("create_time", "play_count", "like_count", "size");

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
    public VideoVO upload(MultipartFile file, String title, String description,
                          String category, String tags, String uploader,
                          Integer clientDuration) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
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
        String normalizedUploader = defaultIfBlank(
                validateLength(trimToNull(uploader), "上传者", MAX_UPLOADER_LENGTH), "anonymous");

        Path root = getUploadRoot();
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = resolveStoredFile(root, fileName);

        try {
            Files.createDirectories(root);
            file.transferTo(target.toFile());

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
            video.setSize(file.getSize());
            video.setDuration(duration);
            video.setFormat(ext);
            video.setCategory(normalizedCategory);
            video.setTags(normalizedTags);
            video.setUploader(normalizedUploader);
            video.setPlayCount(0L);
            video.setLikeCount(0L);
            video.setStatus(1);

            if (videoMapper.insert(video) != 1) {
                throw new BusinessException("视频信息保存失败");
            }
            return toVO(video);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new BusinessException("文件保存失败：" + e.getMessage());
        } catch (RuntimeException e) {
            deleteQuietly(target);
            throw e;
        }
    }

    @Override
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
    public VideoVO getDetail(Long id) {
        requirePositiveId(id);
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在或不可用");
        }
        return toVO(video);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requirePositiveId(id);
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BusinessException("视频不存在或不可用");
        }

        if (videoMapper.deleteById(id) != 1) {
            throw new BusinessException("视频删除失败");
        }

        Path root = getUploadRoot();
        Path file = resolveStoredFile(root, video.getFileName());
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BusinessException("磁盘文件删除失败，数据库操作已回滚");
        }
    }

    @Override
    public void incrementPlay(Long id) {
        requirePositiveId(id);
        if (videoMapper.incrementPlayCount(id) != 1) {
            throw new BusinessException("视频不存在或不可用");
        }
    }

    @Override
    public void like(Long id) {
        requirePositiveId(id);
        if (videoMapper.incrementLikeCount(id) != 1) {
            throw new BusinessException("视频不存在或不可用");
        }
    }

    @Override
    public List<String> listCategories() {
        List<String> list = new ArrayList<>(videoMapper.selectCategories());
        if (!list.contains("全部")) {
            list.add(0, "全部");
        }
        return list;
    }

    @Override
    public List<VideoVO> hotVideos(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_HOT_LIMIT));
        return videoMapper.selectHot(safeLimit).stream().map(this::toVO).collect(Collectors.toList());
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

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("视频 ID 无效");
        }
    }

    private Path getUploadRoot() {
        if (!StringUtils.hasText(uploadPath)) {
            throw new BusinessException("未配置视频上传目录");
        }
        return Paths.get(uploadPath).toAbsolutePath().normalize();
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

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The original exception is more useful to the caller.
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


