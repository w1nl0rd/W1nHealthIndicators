
package ru.w1nlord.w1nhealthindicators;

import net.fabricmc.api.ModInitializer;
import ru.w1nlord.w1nhealthindicators.config.Config;

public final class W1nHealthIndicators implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load();
    }
}
