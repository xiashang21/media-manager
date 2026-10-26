package pre.wc.mediamanager.entity.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoQueryDTOTest {

    @Test
    void cacheKeyKeepsWhitespaceSensitiveKeyword() {
        VideoQueryDTO plain = new VideoQueryDTO();
        plain.setKeyword("vue");

        VideoQueryDTO withSpace = new VideoQueryDTO();
        withSpace.setKeyword("vue ");

        assertThat(plain.cacheKey()).isNotEqualTo(withSpace.cacheKey());
    }

    @Test
    void cacheKeyEscapesDelimiterCharacters() {
        VideoQueryDTO keywordWithDelimiter = new VideoQueryDTO();
        keywordWithDelimiter.setKeyword("a&category=动画");

        VideoQueryDTO splitAcrossFields = new VideoQueryDTO();
        splitAcrossFields.setKeyword("a");
        splitAcrossFields.setCategory("动画");

        assertThat(keywordWithDelimiter.cacheKey()).isNotEqualTo(splitAcrossFields.cacheKey());
    }

    @Test
    void cacheKeyNormalizesBlankDefaults() {
        VideoQueryDTO query = new VideoQueryDTO();
        query.setPage(null);
        query.setSize(null);
        query.setKeyword(null);
        query.setCategory(null);
        query.setSortBy(" ");
        query.setOrder("ASC");

        assertThat(query.cacheKey())
                .isEqualTo("page=1&size=8&keyword=&category=全部&sortBy=create_time&order=asc");
    }
}
