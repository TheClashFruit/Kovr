package me.theclashfruit.kovr.client.menu;

import me.theclashfruit.kovr.client.menu.xml.MenuParser;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SettingsMenu extends Screen {
    private final Screen parent;

    public final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this);

    public SettingsMenu(Screen parent) {
        super(Text.literal("Kovr Settings"));

        this.parent = parent;
    }

    @Override
    public void init() {
        this.layout.addHeader(this.title, this.textRenderer);
        //this.layout.addBody(new EditBoxWidget(this.textRenderer, 0, 0, 0, 200, Text.literal("hehehaw"), Text.literal("ok")));

        Widget wg = new MenuParser(this.textRenderer).parseForFile(Identifier.of("kovr", "menu/settings.xml"));
        this.layout.addBody(wg);

        DirectionalLayoutWidget directionalLayoutWidget = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));

        directionalLayoutWidget.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).build());

        this.layout.forEachChild(this::addDrawableChild);
        this.initTabNavigation();
    }

    @Override
    protected void initTabNavigation() {
        this.layout.refreshPositions();
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }
}
