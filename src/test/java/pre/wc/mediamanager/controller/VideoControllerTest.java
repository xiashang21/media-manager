package pre.wc.mediamanager.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import pre.wc.mediamanager.service.VideoService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VideoControllerTest {

    @TempDir
    Path tempDir;

    private VideoController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new VideoController(mock(VideoService.class));
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.toString());
        Files.writeString(tempDir.resolve("sample.mp4"), "0123456789", StandardCharsets.UTF_8);
    }

    @Test
    void returnsFirstChunkWithoutRange() throws Exception {
        ResponseEntity<ResourceRegion> response = controller.stream("sample.mp4", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPosition()).isZero();
        assertThat(response.getBody().getCount()).isEqualTo(10);
        assertThat(response.getHeaders().getFirst("Accept-Ranges")).isEqualTo("bytes");
    }

    @Test
    void returnsRequestedRangeAsPartialContent() throws Exception {
        ResponseEntity<ResourceRegion> response = controller.stream("sample.mp4", "bytes=2-5");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPosition()).isEqualTo(2);
        assertThat(response.getBody().getCount()).isEqualTo(4);
    }

    @Test
    void fillsEndWhenRangeEndIsMissing() throws Exception {
        ResponseEntity<ResourceRegion> response = controller.stream("sample.mp4", "bytes=7-");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPosition()).isEqualTo(7);
        assertThat(response.getBody().getCount()).isEqualTo(3);
    }

    @Test
    void rejectsSuffixRange() throws Exception {
        ResponseEntity<ResourceRegion> response = controller.stream("sample.mp4", "bytes=-3");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes */10");
    }

    @Test
    void rejectsRangeBeyondFileLength() throws Exception {
        ResponseEntity<ResourceRegion> response = controller.stream("sample.mp4", "bytes=20-30");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(response.getHeaders().getFirst("Content-Range")).isEqualTo("bytes */10");
    }
}
