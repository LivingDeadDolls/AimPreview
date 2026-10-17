package aimpreview;

import aimpreview.config.AimPreviewConfig;

/** Loader-independent startup, called from each loader's client entrypoint. */
public final class AimPreviewCommon {
    private AimPreviewCommon() {
    }

    public static void init() {
        AimPreviewConfig.HANDLER.load();
        Constants.LOG.info("AimPreview loaded on {}", aimpreview.platform.Services.PLATFORM.getPlatformName());
    }
}
