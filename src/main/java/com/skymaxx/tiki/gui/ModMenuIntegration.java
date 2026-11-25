package com.skymaxx.tiki.gui;

import com.skymaxx.tiki.gui.SkyMaxxScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration
implements ModMenuApi {
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SkyMaxxScreen::create;
    }
}

