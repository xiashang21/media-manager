package pre.wc.mediamanager.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pre.wc.mediamanager.entity.Video;
import pre.wc.mediamanager.entity.dto.VideoVO;
import pre.wc.mediamanager.mapper.VideoMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoServiceImplTest {

    @TempDir
    Path tempDir;

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
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "0123456789".getBytes(StandardCharsets.UTF_8));

        VideoVO result = service.upload(
                file, "title", "description", "教程", "vue,mybatis", "tester", 42);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDuration()).isEqualTo(42);
        assertThat(result.getDurationText()).isEqualTo("00:42");
        assertThat(result.getFormat()).isEqualTo("mp4");
        assertThat(result.getTags()).containsExactly("vue", "mybatis");
        verify(mapper).insert(any(Video.class));
        try (var files = Files.list(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(1);
        }
    }
}


