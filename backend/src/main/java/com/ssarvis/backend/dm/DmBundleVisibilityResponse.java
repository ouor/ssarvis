package com.ssarvis.backend.dm;

public record DmBundleVisibilityResponse(
        Long bundleRootMessageId,
        boolean hidden
) {
}
