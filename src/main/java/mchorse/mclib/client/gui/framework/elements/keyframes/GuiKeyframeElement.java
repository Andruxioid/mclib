package mchorse.mclib.client.gui.framework.elements.keyframes;

import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.utils.Color;
import mchorse.mclib.utils.keyframes.Keyframe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;

import java.util.function.Consumer;

public abstract class GuiKeyframeElement extends GuiElement
{
    public static final Color color = new Color();

    public Consumer<Keyframe> callback;

    public GuiKeyframeElement(Minecraft mc, Consumer<Keyframe> callback)
    {
        super(mc);

        this.callback = callback;
    }

    protected void setKeyframe(Keyframe current)
    {
        if (this.callback != null)
        {
            this.callback.accept(current);
        }
    }

    public abstract Keyframe getCurrent();

    public abstract void setDuration(long duration);

    public abstract void setSliding();

    public abstract void selectByDuration(long duration);

    public abstract void doubleClick(int mouseX, int mouseY);

    protected void updateMoved()
    {}

    /* Offsets/multipliers */

    protected int recalcMultiplier(double zoom)
    {
        int factor = (int) (60F / zoom);

        /* Hardcoded caps */
        if (factor > 10000) factor = 10000;
        else if (factor > 5000) factor = 5000;
        else if (factor > 2500) factor = 2500;
        else if (factor > 1000) factor = 1000;
        else if (factor > 500) factor = 500;
        else if (factor > 250) factor = 250;
        else if (factor > 100) factor = 100;
        else if (factor > 50) factor = 50;
        else if (factor > 25) factor = 25;
        else if (factor > 10) factor = 10;
        else if (factor > 5) factor = 5;

        return factor <= 0 ? 1 : factor;
    }

    /**
     * Get zoom factor based by current zoom value 
     */
    protected double getZoomFactor(double zoom)
    {
        double factor = 0;

        if (zoom < 0.2F) factor = 0.005F;
        else if (zoom < 1.0F) factor = 0.025F;
        else if (zoom < 2.0F) factor = 0.1F;
        else if (zoom < 15.0F) factor = 0.5F;
        else if (zoom <= 50.0F) factor = 1F;

        return factor;
    }

    protected void drawRect(BufferBuilder builder, int x, int y, int offset, int c)
    {
        color.set(c, false);

        builder.pos(x - offset, y + offset, 0.0D).color(color.r, color.g, color.b, 1F).endVertex();
        builder.pos(x + offset, y + offset, 0.0D).color(color.r, color.g, color.b, 1F).endVertex();
        builder.pos(x + offset, y - offset, 0.0D).color(color.r, color.g, color.b, 1F).endVertex();
        builder.pos(x - offset, y - offset, 0.0D).color(color.r, color.g, color.b, 1F).endVertex();
    }

    protected void moveNoKeyframe(GuiContext context, Keyframe frame, double x, double y)
    {}

    protected void drawCursor(GuiContext context)
    {}
}