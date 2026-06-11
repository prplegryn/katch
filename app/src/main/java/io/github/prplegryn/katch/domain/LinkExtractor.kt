package io.github.prplegryn.katch.domain

object LinkExtractor {
    private val urlPattern = Regex(
        pattern = """https?://(?:www\.)?(?:xiaohongshu\.com|xhslink\.com)[^\s<>"'，。、“”‘’【】（）()]+""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    fun extract(text: String): String? {
        return urlPattern.find(text)
            ?.value
            ?.trim()
            ?.trimEnd('.', ',', ';', ':', '!', '?', '。', '，', '；', '：', '！', '？')
    }
}
