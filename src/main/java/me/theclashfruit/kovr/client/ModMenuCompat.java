package me.theclashfruit.kovr.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.theclashfruit.kovr.client.menu.SettingsMenu;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Consumer;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsMenu::new;
    }
}
