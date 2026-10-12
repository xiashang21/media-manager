package pre.wc.mediamanager.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Lightweight video metadata reader.
 *
 * <p>The project deliberately keeps the parser dependency-free. It handles the
 * common MP4/MOV, WebM/MKV and AVI containers used by the assignment. If a
 * container or an unusual variant cannot be parsed, the upload still succeeds
 * and the browser-supplied duration is used as a fallback.</p>
 */
public final class VideoMetadataUtils {

    private static final long SEGMENT_ID = 0x18538067L;
    private static final long INFO_ID = 0x1549A966L;
    private static final long TIMECODE_SCALE_ID = 0x2AD7B1L;
    private static final long DURATION_ID = 0x4489L;
    private static final long DEFAULT_TIMECODE_SCALE = 1_000_000L;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private VideoMetadataUtils() {
    }

    public static int readDurationSeconds(Path path, String format) {
        if (path == null || format == null || !Files.isRegularFile(path)) {
            return 0;
        }
        try {
            int seconds = switch (format.toLowerCase(Locale.ROOT)) {
                case "mp4", "mov" -> readMp4Duration(path);
                case "webm", "mkv" -> readMatroskaDuration(path);
                case "avi" -> readAviDuration(path);
                default -> 0;
            };
            return seconds > 0 ? seconds : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    static int readMp4Duration(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = file.length();
            long position = 0;
            long moovStart = -1;
            long moovEnd = -1;

            while (position + 8 <= fileLength) {
                file.seek(position);
                long size = readUnsignedInt(file);
                String type = readType(file);
                int headerSize = 8;

                if (size == 1) {
                    size = readUnsignedLong(file);
                    headerSize = 16;
                } else if (size == 0) {
                    size = fileLength - position;
                }

                if (size < headerSize || position + size > fileLength) {
                    break;
                }
                if ("moov".equals(type)) {
                    moovStart = position + headerSize;
                    moovEnd = position + size;
                    break;
                }
                position += size;
            }

            return moovStart < 0 ? 0 : findMvhd(file, moovStart, moovEnd);
        }
    }

    private static int findMvhd(RandomAccessFile file, long start, long end) throws IOException {
        long position = start;
        while (position + 8 <= end) {
            file.seek(position);
            long size = readUnsignedInt(file);
            String type = readType(file);
            int headerSize = 8;

            if (size == 1) {
                size = readUnsignedLong(file);
                headerSize = 16;
            } else if (size == 0) {
                size = end - position;
            }

            if (size < headerSize || position + size > end) {
                return 0;
            }
            if ("mvhd".equals(type)) {
                return readMvhd(file, position + headerSize);
            }
            position += size;
        }
        return 0;
    }

    private static int readMvhd(RandomAccessFile file, long payloadStart) throws IOException {
        file.seek(payloadStart);
        int version = file.readUnsignedByte();
        skipFully(file, 3);

        long timescale;
        long duration;
        if (version == 1) {
            skipFully(file, 16);
            timescale = readUnsignedInt(file);
            duration = readUnsignedLong(file);
        } else {
            skipFully(file, 8);
            timescale = readUnsignedInt(file);
            duration = readUnsignedInt(file);
        }

        if (timescale <= 0 || duration <= 0) {
            return 0;
        }
        return toSeconds(Math.ceil(duration / (double) timescale));
    }

    static int readMatroskaDuration(Path path) throws IOException {
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            while (true) {
                EbmlElement element = readEbmlElement(input);
                if (element == null) {
                    return 0;
                }
                if (element.id() == SEGMENT_ID) {
                    return readSegmentDuration(input, element);
                }
                if (!skipPayload(input, element.size())) {
                    return 0;
                }
            }
        }
    }

    private static int readSegmentDuration(DataInputStream input, EbmlElement segment)
            throws IOException {
        long remaining = segment.size() >= 0 ? segment.size() : Long.MAX_VALUE;
        while (remaining > 0) {
            EbmlElement child = readEbmlElement(input);
            if (child == null) {
                return 0;
            }
            remaining -= child.headerSize();

            if (child.id() == INFO_ID) {
                return child.size() < 0 || child.size() > remaining
                        ? 0
                        : readInfoDuration(input, child.size());
            }
            if (child.size() < 0 || child.size() > remaining
                    || !skipPayload(input, child.size())) {
                return 0;
            }
            remaining -= child.size();
        }
        return 0;
    }

    private static int readInfoDuration(DataInputStream input, long infoSize) throws IOException {
        long remaining = infoSize;
        long timecodeScale = DEFAULT_TIMECODE_SCALE;
        double duration = -1;

        while (remaining >= 2) {
            EbmlElement child = readEbmlElement(input);
            if (child == null) {
                return 0;
            }
            remaining -= child.headerSize();
            if (child.size() < 0 || child.size() > remaining) {
                return 0;
            }

            if (child.id() == TIMECODE_SCALE_ID && child.size() > 0) {
                timecodeScale = readUnsignedInteger(input, (int) child.size());
            } else if (child.id() == DURATION_ID
                    && (child.size() == 4 || child.size() == 8)) {
                duration = child.size() == 4
                        ? Float.intBitsToFloat((int) readUnsignedInteger(input, 4))
                        : Double.longBitsToDouble(readUnsignedInteger(input, 8));
            } else if (!skipPayload(input, child.size())) {
                return 0;
            }

            remaining -= child.size();
            if (duration >= 0 && timecodeScale > 0) {
                return toSeconds(Math.ceil(duration * timecodeScale / (double) NANOS_PER_SECOND));
            }
        }
        return 0;
    }

    static int readAviDuration(Path path) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = file.length();
            if (fileLength < 12) {
                return 0;
            }
            file.seek(0);
            if (!"RIFF".equals(readType(file))) {
                return 0;
            }
            readUnsignedIntLittleEndian(file);
            if (!"AVI ".equals(readType(file))) {
                return 0;
            }

            long riffEnd = Math.min(fileLength, file.getFilePointer() + Math.max(0, fileLength - 8));
            long position = 12;
            while (position + 8 <= riffEnd) {
                file.seek(position);
                String type = readType(file);
                long size = readUnsignedIntLittleEndian(file);
                long payloadStart = position + 8;
                if (size < 0 || payloadStart + size > riffEnd) {
                    return 0;
                }

                if ("LIST".equals(type) && size >= 4) {
                    file.seek(payloadStart);
                    String listType = readType(file);
                    if ("hdrl".equals(listType)) {
                        return findAviHeader(file, payloadStart + 4, payloadStart + size);
                    }
                }
                position = payloadStart + size + (size & 1);
            }
            return 0;
        }
    }

    private static int findAviHeader(RandomAccessFile file, long start, long end) throws IOException {
        long position = start;
        while (position + 8 <= end) {
            file.seek(position);
            String type = readType(file);
            long size = readUnsignedIntLittleEndian(file);
            long payloadStart = position + 8;
            if (size < 0 || payloadStart + size > end) {
                return 0;
            }
            if ("avih".equals(type) && size >= 24) {
                file.seek(payloadStart);
                long microsPerFrame = readUnsignedIntLittleEndian(file);
                skipFully(file, 12);
                long totalFrames = readUnsignedIntLittleEndian(file);
                if (microsPerFrame > 0 && totalFrames > 0) {
                    return toSeconds(Math.ceil(microsPerFrame * totalFrames / 1_000_000.0));
                }
                return 0;
            }
            position = payloadStart + size + (size & 1);
        }
        return 0;
    }

    private static EbmlElement readEbmlElement(DataInputStream input) throws IOException {
        int first = input.read();
        if (first < 0) {
            return null;
        }

        int idLength = vintLength(first);
        if (idLength <= 0 || idLength > 4) {
            return null;
        }
        long id = first;
        for (int i = 1; i < idLength; i++) {
            int next = input.read();
            if (next < 0) {
                return null;
            }
            id = (id << 8) | next;
        }

        int sizeFirst = input.read();
        if (sizeFirst < 0) {
            return null;
        }
        int sizeLength = vintLength(sizeFirst);
        if (sizeLength <= 0 || sizeLength > 8) {
            return null;
        }

        long dataMask = (1L << (8 - sizeLength)) - 1;
        long size = sizeFirst & dataMask;
        boolean unknownSize = (sizeFirst & dataMask) == dataMask;
        for (int i = 1; i < sizeLength; i++) {
            int next = input.read();
            if (next < 0) {
                return null;
            }
            size = (size << 8) | next;
            if (next != 0xFF) {
                unknownSize = false;
            }
        }

        return new EbmlElement(id, unknownSize ? -1 : size, idLength + sizeLength);
    }

    private static int vintLength(int firstByte) {
        int mask = 0x80;
        for (int length = 1; length <= 8; length++) {
            if ((firstByte & mask) != 0) {
                return length;
            }
            mask >>= 1;
        }
        return -1;
    }

    private static long readUnsignedInteger(DataInputStream input, int length) throws IOException {
        if (length < 1 || length > 8) {
            return 0;
        }
        long value = 0;
        for (int i = 0; i < length; i++) {
            value = (value << 8) | input.readUnsignedByte();
        }
        return value;
    }

    private static boolean skipPayload(DataInputStream input, long size) throws IOException {
        if (size < 0) {
            return false;
        }
        return skipFully(input, size);
    }

    private static boolean skipFully(DataInputStream input, long size) throws IOException {
        while (size > 0) {
            long skipped = input.skip(size);
            if (skipped <= 0) {
                if (input.read() < 0) {
                    return false;
                }
                skipped = 1;
            }
            size -= skipped;
        }
        return true;
    }

    private static long readUnsignedInt(RandomAccessFile file) throws IOException {
        return Integer.toUnsignedLong(file.readInt());
    }

    private static long readUnsignedLong(RandomAccessFile file) throws IOException {
        long value = file.readLong();
        if (value < 0) {
            throw new IOException("Box size is too large");
        }
        return value;
    }

    private static long readUnsignedIntLittleEndian(RandomAccessFile file) throws IOException {
        return Integer.toUnsignedLong(Integer.reverseBytes(file.readInt()));
    }

    private static String readType(RandomAccessFile file) throws IOException {
        byte[] bytes = new byte[4];
        file.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static void skipFully(RandomAccessFile file, long size) throws IOException {
        long target = file.getFilePointer() + size;
        file.seek(target);
    }

    private static int toSeconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.ceil(seconds)));
    }

    private record EbmlElement(long id, long size, int headerSize) {
    }
}


