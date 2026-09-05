package com.volt.module.modules.client;

import com.volt.Volt;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.setting.BooleanSetting;

public class Panic extends Module {
    public final BooleanSetting disableModules = new BooleanSetting("Disable Modules", true);

    public Panic() {
        super("Panic", "Disables every module", -1, Category.CLIENT);
        this.addSettings(disableModules);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        for (Module m : Volt.INSTANCE.moduleManager.getModules()) {
            if (m.isEnabled() && disableModules.getValue()) m.setEnabled(false);
        }
    }
}
