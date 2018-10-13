package mchorse.mclib.client.gui.widgets.buttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * Textured button
 *
 * This button is like regular {@link GuiButton}, but it gets drawn as a
 * texture icon.
 */
public class GuiTextureButton extends GuiButton
{
    public ResourceLocation texture;

    public int tx;
    public int ty;

    public int atx;
    public int aty;

    public GuiTextureButton(int id, int x, int y, ResourceLocation texture)
    {
        super(id, x, y, 16, 16, "");
        this.texture = texture;
    }

    public GuiTextureButton setTexPos(int x, int y)
    {
        this.tx = x;
        this.ty = y;

        return this;
    }

    public GuiTextureButton setActiveTexPos(int x, int y)
    {
        this.atx = x;
        this.aty = y;

        return this;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks)
    {
        if (this.visible)
        {
            GlStateManager.enableAlpha();
            mc.renderEngine.bindTexture(this.texture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            this.drawTexturedModalRect(this.x, this.y, this.hovered ? this.atx : this.tx, this.hovered ? this.aty : this.ty, this.width, this.height);
            GlStateManager.disableAlpha();
        }
    }
}