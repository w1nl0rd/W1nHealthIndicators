
package ru.w1nlord.w1nhealthindicators.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import ru.w1nlord.w1nhealthindicators.config.Config;

public final class ConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::create;
    }

    private static Screen create(Screen parent) {
        Config.load();
        return new SettingsScreen(parent);
    }

    private static final class SettingsScreen extends Screen {
        private final Screen parent;
        private ButtonWidget heartsButton;
        private ButtonWidget armorButton;
        private ButtonWidget healthSourceButton;
        private SliderWidget scaleSlider;

        private SettingsScreen(Screen parent) {
            super(Text.literal("W1nHealthIndicators"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int startY = this.height / 2 - 65;

            this.heartsButton = ButtonWidget.builder(getHeartsText(), button -> {
                Config.setRenderHearts(!Config.shouldRenderHearts());
                button.setMessage(getHeartsText());
                Config.save();
            }).dimensions(centerX - 100, startY, 200, 20).build();

            this.armorButton = ButtonWidget.builder(getArmorText(), button -> {
                Config.setRenderArmor(!Config.shouldRenderArmor());
                button.setMessage(getArmorText());
                Config.save();
            }).dimensions(centerX - 100, startY + 25, 200, 20).build();

            this.healthSourceButton = ButtonWidget.builder(getHealthSourceText(), button -> {
                Config.HealthSource next = switch (Config.getHealthSource()) {
                    case SCOREBOARD -> Config.HealthSource.FORMATTED_TEXT;
                    case FORMATTED_TEXT -> Config.HealthSource.NONE;
                    case NONE -> Config.HealthSource.SCOREBOARD;
                };
                Config.setHealthSource(next);
                button.setMessage(getHealthSourceText());
                Config.save();
            }).dimensions(centerX - 100, startY + 50, 200, 20).build();

            this.scaleSlider = new SliderWidget(centerX - 100, startY + 75, 200, 20, getScaleText(), (Config.getScale() - 0.5F) / 1.5F) {
                @Override
                protected void updateMessage() {
                    this.setMessage(getScaleText());
                }

                @Override
                protected void applyValue() {
                    Config.setScale(0.5F + (float) this.value * 1.5F);
                    Config.save();
                }
            };

            this.addDrawableChild(this.heartsButton);
            this.addDrawableChild(this.armorButton);
            this.addDrawableChild(this.healthSourceButton);
            this.addDrawableChild(this.scaleSlider);
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close()).dimensions(centerX - 100, startY + 105, 200, 20).build());
        }

        private Text getHeartsText() {
            return Text.literal("Рендер сердец: " + (Config.shouldRenderHearts() ? "Вкл." : "Выкл."));
        }

        private Text getArmorText() {
            return Text.literal("Рендер брони: " + (Config.shouldRenderArmor() ? "Вкл." : "Выкл."));
        }

        private Text getHealthSourceText() {
            String source = switch (Config.getHealthSource()) {
                case SCOREBOARD -> "Скорборд";
                case FORMATTED_TEXT -> "Текст под ником";
                case NONE -> "Нету";
            };
            return Text.literal("Обход: " + source);
        }

        private Text getScaleText() {
            return Text.literal("Масштаб шкал: " + Math.round(Config.getScale() * 100) + "%");
        }

        @Override
        public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
            // this.renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 95, 0xFFFFFF);
        }

        @Override
        public void close() {
            this.client.setScreen(this.parent);
        }
    }
}
