package pl.cukrzycowy.ollee.glycemia.synthetic

class SgvAnchorNormalizer {

    fun normalize(
        anchors: List<SgvAnchor>,
        config: SgvGenerationConfig
    ): List<NormalizedAnchor> {
        require(anchors.size >= 2) { "At least two anchors are required" }
        require(config.sampleIntervalMinutes > 0) { "Sample interval must be positive" }

        val sorted = anchors
            .sortedBy { it.timestampMs }
            .fold(mutableListOf<SgvAnchor>()) { acc, anchor ->
                if (acc.isNotEmpty() && acc.last().timestampMs == anchor.timestampMs) {
                    acc[acc.lastIndex] = anchor
                } else {
                    acc += anchor
                }
                acc
            }

        require(sorted.size >= 2) { "At least two distinct anchor timestamps are required" }

        val stepMillis = config.sampleIntervalMinutes * 60_000L
        val firstTimestamp = sorted.first().timestampMs

        return sorted.mapIndexed { index, anchor ->
            if (index > 0) {
                val previous = sorted[index - 1]
                require(anchor.timestampMs > previous.timestampMs) {
                    "Anchor timestamps must be strictly increasing"
                }
                require(anchor.timestampMs - previous.timestampMs >= stepMillis) {
                    "Anchor gap must be at least one sample interval"
                }
            }

            val normalizedValue = anchor.valueMgDl.coerceIn(config.minSgvMgDl, config.maxSgvMgDl)
            val rawStepIndex = (anchor.timestampMs - firstTimestamp) / stepMillis

            NormalizedAnchor(
                timestampMs = anchor.timestampMs,
                stepIndex = rawStepIndex.toInt(),
                valueMgDl = normalizedValue,
                deltaMgDlPer5Min = anchor.deltaMgDlPer5Min
            )
        }
    }
}