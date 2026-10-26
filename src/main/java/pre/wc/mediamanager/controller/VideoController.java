package pre.wc.mediamanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pre.wc.mediamanager.common.PageResult;
import pre.wc.mediamanager.common.Result;
import pre.wc.mediamanager.entity.dto.LikeResult;
import pre.wc.mediamanager.entity.dto.VideoQueryDTO;
import pre.wc.mediamanager.entity.dto.VideoVO;
import pre.wc.mediamanager.service.VideoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private static final long CHUNK_SIZE = 1024 * 1024;

    private final VideoService videoService;

    @Value("${video.upload-path}")
    private String uploadPath;

    @Value("${video.cover-path}")
    private String coverPath;

    /** 上传视频，可选同时上传封面。 */
    @PostMapping("/upload")
    public Result<VideoVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "duration", required = false) Integer clientDuration) {
        return Result.success(videoService.upload(
                file, cover, title, description, category, tags, clientDuration));
    }

    /** 为已有视频单独更换封面，仅视频所有者或管理员可操作。 */
    @PostMapping("/{id}/cover")
    public Result<VideoVO> updateCover(@PathVariable Long id,
                                       @RequestParam("cover") MultipartFile cover) {
        return Result.success(videoService.updateCover(id, cover));
    }

    /** 分页 */
    @GetMapping("/page")
    public Result<PageResult<VideoVO>> page(VideoQueryDTO query) {
        return Result.success(videoService.page(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<VideoVO> detail(@PathVariable Long id) {
        return Result.success(videoService.getDetail(id));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return Result.success();
    }

    /** 播放量 +1 */
    @PostMapping("/{id}/play")
    public Result<Void> play(@PathVariable Long id) {
        videoService.incrementPlay(id);
        return Result.success();
    }

    /** 点赞或取消点赞（切换） */
    @PostMapping("/{id}/like")
    public Result<LikeResult> like(@PathVariable Long id) {
        return Result.success(videoService.like(id));
    }

    /** 分类列表 */
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.success(videoService.listCategories());
    }

    /** 热门 */
    @GetMapping("/hot")
    public Result<List<VideoVO>> hot(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(videoService.hotVideos(limit));
    }

    /** 读取上传的封面图片，公开访问并允许浏览器缓存。 */
    @GetMapping("/cover/{fileName:.+}")
    public ResponseEntity<Resource> cover(@PathVariable String fileName) throws IOException {
        Path path = resolveStoredPath(coverPath, fileName);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                .body(resource);
    }

    /**
     * 视频流式播放：使用 ResponseEntity<ResourceRegion> 返回分片。
     * 无 Range 时返回前 1MB；有 Range 时返回 206 Partial Content。
     */
    @GetMapping("/stream/{fileName:.+}")
    public ResponseEntity<ResourceRegion> stream(
            @PathVariable String fileName,
            @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        Path path = resolveStoredPath(uploadPath, fileName);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(path);
        long contentLength = resource.contentLength();
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (contentLength <= 0) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(mediaType)
                    .build();
        }

        long start = 0;
        long end;
        if (!StringUtils.hasText(rangeHeader)) {
            end = Math.min(CHUNK_SIZE, contentLength) - 1;
        } else {
            if (!rangeHeader.startsWith("bytes=")) {
                return rangeNotSatisfiable(contentLength);
            }
            String[] ranges = rangeHeader.substring("bytes=".length()).split("-", -1);
            if (ranges.length != 2 || ranges[0].isBlank()) {
                return rangeNotSatisfiable(contentLength);
            }
            try {
                start = Long.parseLong(ranges[0].trim());
            } catch (NumberFormatException e) {
                return rangeNotSatisfiable(contentLength);
            }
            if (start < 0 || start >= contentLength) {
                return rangeNotSatisfiable(contentLength);
            }

            if (ranges[1].isBlank()) {
                end = Math.min(start + CHUNK_SIZE - 1, contentLength - 1);
            } else {
                try {
                    end = Long.parseLong(ranges[1].trim());
                } catch (NumberFormatException e) {
                    return rangeNotSatisfiable(contentLength);
                }
                if (end < start) {
                    return rangeNotSatisfiable(contentLength);
                }
                end = Math.min(end, contentLength - 1);
            }
        }

        long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(mediaType)
                .body(region);
    }

    private ResponseEntity<ResourceRegion> rangeNotSatisfiable(long contentLength) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                .build();
    }

    private Path resolveStoredPath(String configuredRoot, String fileName) {
        if (!StringUtils.hasText(configuredRoot) || !StringUtils.hasText(fileName)) {
            return null;
        }
        Path root = Paths.get(configuredRoot).toAbsolutePath().normalize();
        Path path = root.resolve(fileName).normalize();
        if (path.equals(root) || !path.startsWith(root) || !Files.isRegularFile(path)) {
            return null;
        }
        return path;
    }
}
