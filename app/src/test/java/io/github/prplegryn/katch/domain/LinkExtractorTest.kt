package io.github.prplegryn.katch.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkExtractorTest {
    @Test
    fun extractsXhsShortLinkFromShareText() {
        val input = "牙牙大屏小舞蹈 http://xhslink.com/o/AOASQXmnp3X 复制这段，去【小红书】发现更多好内容~"

        assertEquals("http://xhslink.com/o/AOASQXmnp3X", LinkExtractor.extract(input))
    }

    @Test
    fun trimsChinesePunctuation() {
        val input = "看这个：https://www.xiaohongshu.com/explore/abc123。"

        assertEquals("https://www.xiaohongshu.com/explore/abc123", LinkExtractor.extract(input))
    }

    @Test
    fun ignoresUnsupportedHost() {
        assertNull(LinkExtractor.extract("https://example.com/xhslink.com/o/nope"))
    }
}
