package pre.wc.mediamanager.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pre.wc.mediamanager.common.BusinessException;
import pre.wc.mediamanager.common.ForbiddenException;
import pre.wc.mediamanager.entity.Video;
import pre.wc.mediamanager.entity.dto.VideoVO;
import pre.wc.mediamanager.mapper.VideoMapper;
import pre.wc.mediamanager.security.UserContext;
import pre.wc.mediamanager.security.UserPrincipal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoServiceImplTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void uploadUsesBrowserDurationAsFallback() throws Exception {
        VideoMapper mapper = mock(VideoMapper.class);
        when(mapper.insert(any(Video.class))).thenAnswer(invocation -> {
            Video video = invocation.getArgument(0);
            video.setId(1L);
            return 1;
        });

        VideoServiceImpl service = new VideoServiceImpl(mapper);
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "coverPath", tempDir.resolve("covers").toString());
        UserContext.set(new UserPrincipal(7L, "tester", "测试用户", "USER"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "0123456789".getBytes(StandardCharsets.UTF_8));

        VideoVO result = service.upload(
                file, null, "title", "description", "教程", "vue,mybatis", 42);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDuration()).isEqualTo(42);
        assertThat(result.getDurationText()).isEqualTo("00:42");
        assertThat(result.getFormat()).isEqualTo("mp4");
        assertThat(result.getTags()).containsExactly("vue", "mybatis");
        assertThat(result.getOwnerId()).isEqualTo(7L);
        assertThat(result.getUploader()).isEqualTo("测试用户");
        verify(mapper).insert(any(Video.class));
        try (var files = Files.list(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(1);
        }
    }

    @Test
    void updateCoverRejectsNonOwner() {
        VideoMapper mapper = mock(VideoMapper.class);
        Video video = new Video();
        video.setId(10L);
        video.setOwnerId(1L);
        video.setFileName("movie.mp4");
        when(mapper.selectById(10L)).thenReturn(video);

        VideoServiceImpl service = new VideoServiceImpl(mapper);
        ReflectionTestUtils.setField(service, "coverPath", tempDir.resolve("covers").toString());
        UserContext.set(new UserPrincipal(2L, "other", "Other", "USER"));
        MockMultipartFile cover = new MockMultipartFile(
                "cover", "cover.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0});

        assertThatThrownBy(() -> service.updateCover(10L, cover))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("只能管理自己上传的视频");
    }

    @Test
    void getDetailReturns404WhenVideoMissing() {
        VideoMapper mapper = mock(VideoMapper.class);
        when(mapper.selectById(99L)).thenReturn(null);
        VideoServiceImpl service = new VideoServiceImpl(mapper);

        assertThatThrownBy(() -> service.getDetail(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(404));
    }
}
