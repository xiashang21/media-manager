package pre.wc.mediamanager.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VideoMetadataUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void readsDurationFromMp4MovieHeader() throws Exception {
        byte[] mvhd = ByteBuffer.allocate(20)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(0)
                .putLong(0)
                .putInt(1000)
                .putInt(12_345)
                .array();
        byte[] moov = box("moov", box("mvhd", mvhd));
        Path file = tempDir.resolve("sample.mp4");
        Files.write(file, moov);

        assertThat(VideoMetadataUtils.readMp4Duration(file)).isEqualTo(13);
    }

    private static byte[] box(String type, byte[] payload) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(ByteBuffer.allocate(4).putInt(8 + payload.length).array());
        output.write(type.getBytes(StandardCharsets.US_ASCII));
        output.write(payload);
        return output.toByteArray();
    }
}


