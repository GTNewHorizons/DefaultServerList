package glowredman.defaultserverlist.mixins;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import glowredman.defaultserverlist.Config;

@Mixin(GuiMultiplayer.class)
public abstract class GuiMultiplayerMixin extends GuiScreen {

    @Shadow
    private GuiScreen field_146798_g;

    @Inject(
            method = "func_146794_g",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiMultiplayer;func_146790_a(I)V"))
    private void defaultServerList$initDisableButton(CallbackInfo ci) {
        // noinspection unchecked
        this.buttonList.add(
                new GuiButton(
                        10,
                        (this.width / 2 + 4 + 50) + 100 + 4,
                        this.height - 52,
                        20,
                        20,
                        I18n.format("defaultServerList.button.disable")) {

                    @Override
                    public void func_146111_b(int mouseX, int mouseY) {
                        drawHoveringText(
                                Collections.singletonList(
                                        I18n.format(
                                                Config.isDisabled()
                                                        ? "defaultServerList.button.disable.tooltip.disabled"
                                                        : "defaultServerList.button.disable.tooltip")),
                                mouseX,
                                mouseY,
                                fontRendererObj);
                    }
                });
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"))
    private void defaultServerList$disableButtonClicked(GuiButton button, CallbackInfo ci) {
        if (button.id == 10) {
            Config.setDisabled(!Config.isDisabled());
            // reopen the screen to apply the change
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuiMultiplayer) {
                GuiScreen parentScreen = ((GuiMultiplayerMixin) mc.currentScreen).field_146798_g;
                mc.displayGuiScreen(new GuiMultiplayer(parentScreen));
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void defaultServerList$renderButtonTooltips(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        // noinspection unchecked
        for (GuiButton button : (List<GuiButton>) this.buttonList) {
            if (button.func_146115_a()) { // if in the boundary of button
                button.func_146111_b(mouseX, mouseY); // call the tooltip rendering function
            }
        }
    }

}
