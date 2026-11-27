package com.skymaxx.tiki.light;

/** Light: added to FishingHookRenderState by a mixin, holds the Light Crew line / bobber look (0 = Standard). */
public interface LightHookState {
    int light$line();

    int light$bobber();

    void light$style(int line, int bobber);
}
