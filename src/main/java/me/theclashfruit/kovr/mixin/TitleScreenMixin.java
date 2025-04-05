package me.theclashfruit.kovr.mixin;

import me.theclashfruit.kovr.client.menu.SettingsMenu;
import net.minecraft.client.gui.screen.AccessibilityOnboardingButtons;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public  abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("RETURN"))
    public void init(CallbackInfo ci) {
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("K"), button -> {
                    assert this.client != null;
                    this.client.setScreen(new SettingsMenu(this));
                })
                .dimensions(this.width / 2 + 128, (this.height / 4 + 48) + 72 + 12, 20, 20)
                .build()
        );
    }
}
