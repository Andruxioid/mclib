package mchorse.mclib.client.gui.framework.elements.keyframes;

import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.framework.elements.utils.GuiDraw;
import mchorse.mclib.client.gui.utils.Scale;
import mchorse.mclib.utils.keyframes.Keyframe;
import mchorse.mclib.utils.keyframes.KeyframeChannel;
import mchorse.mclib.utils.keyframes.KeyframeEasing;
import mchorse.mclib.utils.keyframes.KeyframeInterpolation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

public class GuiGraphView extends GuiKeyframeElement
{
    public KeyframeChannel channel;
    public int duration;
    public int color;
    public int selected = -1;

    public boolean sliding = false;
    public boolean dragging = false;
    private boolean moving = false;
    private boolean scrolling = false;
    public int which = 0;
    private int lastX;
    private int lastY;
    private double lastT;
    private double lastV;

    private Scale scaleX = new Scale(false);
    private Scale scaleY = new Scale(true);

    public GuiGraphView(Minecraft mc, Consumer<Keyframe> callback)
    {
        super(mc, callback);
    }

    /* Implementation of abstract methods */

    public Keyframe getCurrent()
    {
        return this.channel.get(this.selected);
    }

    public void setChannel(KeyframeChannel channel)
    {
        this.channel = channel;
        this.resetView();
    }

    public void setColor(int color)
    {
        this.color = color;
    }

    @Override
    public void setDuration(long duration)
    {
        this.duration = (int) duration;
    }

    @Override
    public void setSliding()
    {
        this.sliding = true;
    }

    @Override
    public void doubleClick(int mouseX, int mouseY)
    {
        if (this.which == -1)
        {
            this.addCurrent((long) this.fromGraphX(mouseX), this.fromGraphY(mouseY));
        }
        else if (this.which == 0)
        {
            this.removeCurrent();
        }
    }

    public void addCurrent(long tick, double value)
    {
        KeyframeEasing easing = KeyframeEasing.IN;
        KeyframeInterpolation interp = KeyframeInterpolation.LINEAR;
        Keyframe frame = this.getCurrent();
        long oldTick = tick;

        if (frame != null)
        {
            easing = frame.easing;
            interp = frame.interp;
            oldTick = frame.tick;
        }

        this.selected = this.channel.insert(tick, value);

        if (oldTick != tick)
        {
            frame = this.getCurrent();
            frame.setEasing(easing);
            frame.setInterpolation(interp);
        }
    }

    public void removeCurrent()
    {
        Keyframe frame = this.getCurrent();

        if (frame == null)
        {
            return;
        }

        this.channel.remove(this.selected);
        this.selected -= 1;
        this.which = -1;
    }

    /* Graphing code */

    public int toGraphX(double tick)
    {
        return (int) (this.scaleX.to(tick)) + this.area.mx();
    }

    public int toGraphY(double value)
    {
        return (int) (this.scaleY.to(value)) + this.area.my();
    }

    public double fromGraphX(int mouseX)
    {
        return this.scaleX.from(mouseX - this.area.mx());
    }

    public double fromGraphY(int mouseY)
    {
        return this.scaleY.from(mouseY - this.area.my());
    }

    /**
     * Resets the view  
     */
    public void resetView()
    {
        this.scaleX.set(0, 2);
        this.scaleY.set(0, 2);

        int c = this.channel.getKeyframes().size();

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        if (c > 1)
        {
            for (Keyframe frame : this.channel.getKeyframes())
            {
                minX = Math.min(minX, frame.tick);
                minY = Math.min(minY, frame.value);
                maxX = Math.max(maxX, frame.tick);
                maxY = Math.max(maxY, frame.value);
            }
        }
        else
        {
            minX = 0;
            maxX = this.duration;
            minY = -10;
            maxY = 10;

            if (c == 1)
            {
                Keyframe first = this.channel.get(0);

                minX = Math.min(0, first.tick);
                maxX = Math.max(this.duration, first.tick);
                minY = maxY = first.value;
            }
        }

        if (Math.abs(maxY - minY) < 0.01F)
        {
            /* Centerize */
            this.scaleY.shift = minY;
        }
        else
        {
            /* Spread apart vertically */
            this.scaleY.view(minY, maxY, this.area.h, 20);
        }

        /* Spread apart horizontally */
        this.scaleX.view(minX, maxX, this.area.w, 20);
        this.recalcMultipliers();
    }

    /**
     * Recalculate grid's multipliers 
     */
    private void recalcMultipliers()
    {
        this.scaleX.mult = this.recalcMultiplier(this.scaleX.zoom);
        this.scaleY.mult = this.recalcMultiplier(this.scaleY.zoom);
    }

    /**
     * Make current keyframe by given duration 
     */
    public void selectByDuration(long duration)
    {
        if (this.channel == null)
        {
            return;
        }

        int i = 0;
        this.selected = -1;

        for (Keyframe frame : this.channel.getKeyframes())
        {
            if (frame.tick >= duration)
            {
                this.selected = i;

                break;
            }

            i++;
        }

        this.setKeyframe(this.getCurrent());
    }

    /* Input handling */

    @Override
    public boolean mouseClicked(GuiContext context)
    {
        if (super.mouseClicked(context))
        {
            return true;
        }

        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        /* Select current point with a mouse click */
        if (this.area.isInside(mouseX, mouseY))
        {
            if (context.mouseButton == 0)
            {
                /* Duplicate the keyframe */
                if (GuiScreen.isAltKeyDown() && this.which == 0)
                {
                    Keyframe frame = this.getCurrent();

                    if (frame != null)
                    {
                        long offset = (long) this.fromGraphX(mouseX);
                        Keyframe created = this.channel.get(this.channel.insert(offset, frame.value));

                        this.selected = this.channel.getKeyframes().indexOf(created);
                        created.copy(frame);
                        created.tick = offset;
                    }

                    return false;
                }

                this.which = -1;
                this.selected = -1;

                Keyframe prev = null;
                int index = 0;

                for (Keyframe frame : this.channel.getKeyframes())
                {
                    boolean left = prev != null && prev.interp == KeyframeInterpolation.BEZIER && this.isInside(frame.tick - frame.lx, frame.value + frame.ly, mouseX, mouseY);
                    boolean right = frame.interp == KeyframeInterpolation.BEZIER && this.isInside(frame.tick + frame.rx, frame.value + frame.ry, mouseX, mouseY);
                    boolean point = this.isInside(frame.tick, frame.value, mouseX, mouseY);

                    if (left || right || point)
                    {
                        this.which = left ? 1 : (right ? 2 : 0);
                        this.selected = index;
                        this.setKeyframe(frame);

                        this.lastT = left ? frame.tick - frame.lx : (right ? frame.tick + frame.rx : frame.tick);
                        this.lastV = left ? frame.value + frame.ly : (right ? frame.value + frame.ry : frame.value);

                        this.lastX = mouseX;
                        this.lastY = mouseY;
                        this.dragging = true;

                        return false;
                    }

                    prev = frame;
                    index++;
                }

                if (this.parent != null)
                {
                    this.dragging = true;
                }
            }
            else if (context.mouseButton == 2)
            {
                this.scrolling = true;
                this.lastX = mouseX;
                this.lastY = mouseY;
                this.lastT = this.scaleX.shift;
                this.lastV = this.scaleY.shift;
            }
        }

        return false;
    }

    private boolean isInside(double tick, double value, int mouseX, int mouseY)
    {
        int x = this.toGraphX(tick);
        int y = this.toGraphY(value);
        double d = Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2);

        return Math.sqrt(d) < 4;
    }

    @Override
    public boolean mouseScrolled(GuiContext context)
    {
        if (super.mouseScrolled(context))
        {
            return true;
        }

        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        if (this.area.isInside(mouseX, mouseY) && !this.scrolling)
        {
            int scroll = context.mouseWheel;

            if (!Minecraft.IS_RUNNING_ON_MAC)
            {
                scroll = -scroll;
            }

            boolean x = GuiScreen.isShiftKeyDown();
            boolean y = GuiScreen.isCtrlKeyDown();
            boolean none = !x && !y;

            /* Scaling X */
            if (x && !y || none)
            {
                this.scaleX.zoom(Math.copySign(this.getZoomFactor(this.scaleX.zoom), scroll), 0.01F, 50F);
            }

            /* Scaling Y */
            if (y && !x || none)
            {
                this.scaleY.zoom(Math.copySign(this.getZoomFactor(this.scaleY.zoom), scroll), 0.01F, 50F);
            }

            this.recalcMultipliers();

            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(GuiContext context)
    {
        super.mouseReleased(context);

        if (this.selected != -1)
        {
            if (this.sliding)
            {
                /* Resort after dragging the tick thing */
                Keyframe frame = this.getCurrent();

                this.channel.sort();
                this.sliding = false;
                this.selected = this.channel.getKeyframes().indexOf(frame);
            }

            if (this.moving)
            {
                this.updateMoved();
            }
        }

        this.dragging = false;
        this.moving = false;
        this.scrolling = false;
    }

    /* Rendering */

    @Override
    public void draw(GuiContext context)
    {
        GuiScreen screen = this.mc.currentScreen;
        int w = screen.width;
        int h = screen.height;
        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        if (this.dragging && !this.moving && (Math.abs(this.lastX - mouseX) > 3 || Math.abs(this.lastY - mouseY) > 3))
        {
            this.moving = true;
            this.sliding = true;
        }

        this.area.draw(0x88000000);
        GuiDraw.scissor(this.area.x, this.area.y, this.area.w, this.area.h, w, h);

        this.drawGraph(context, mouseX, mouseY, w, h);

        GuiDraw.unscissor(context);

        super.draw(context);
    }

    /**
     * Render the graph 
     */
    private void drawGraph(GuiContext context, int mouseX, int mouseY, int w, int h)
    {
        if (this.channel == null)
        {
            return;
        }

        if (this.scrolling)
        {
            this.scaleX.shift = -(mouseX - this.lastX) / this.scaleX.zoom + this.lastT;
            this.scaleY.shift = (mouseY - this.lastY) / this.scaleY.zoom + this.lastV;
        }
        /* Move the current keyframe */
        else if (this.moving)
        {
            Keyframe frame = this.channel.get(this.selected);
            double x = this.fromGraphX(mouseX);
            double y = this.fromGraphY(mouseY);

            if (GuiScreen.isShiftKeyDown()) x = this.lastT;
            if (GuiScreen.isCtrlKeyDown()) y = this.lastV;

            if (this.which == 0)
            {
                frame.setTick((long) x);
                frame.setValue(y);
            }
            else if (this.which == 1)
            {
                frame.lx = (float) -(x - frame.tick);
                frame.ly = (float) (y - frame.value);

                if (!GuiScreen.isAltKeyDown())
                {
                    frame.rx = frame.lx;
                    frame.ry = -frame.ly;
                }
            }
            else if (this.which == 2)
            {
                frame.rx = (float) x - frame.tick;
                frame.ry = (float) (y - frame.value);

                if (!GuiScreen.isAltKeyDown())
                {
                    frame.lx = frame.rx;
                    frame.ly = -frame.ry;
                }
            }
            else if (this.which == -1 && this.parent != null)
            {
                this.moveNoKeyframe(context, frame, x, y);
            }

            this.setKeyframe(this.getCurrent());
        }

        int leftBorder = this.toGraphX(0);
        int rightBorder = this.toGraphX(this.duration);

        if (leftBorder > this.area.x) Gui.drawRect(this.area.x, this.area.y, leftBorder, this.area.y + this.area.h, 0x88000000);
        if (rightBorder < this.area.ex()) Gui.drawRect(rightBorder, this.area.y, this.area.ex() , this.area.y + this.area.h, 0x88000000);

        /* Draw scaling grid */
        int hx = this.duration / this.scaleX.mult;
        int ht = (int) this.fromGraphX(this.area.x);

        for (int j = Math.max(ht / this.scaleX.mult, 0); j <= hx; j++)
        {
            int x = this.toGraphX(j * this.scaleX.mult);

            if (x >= this.area.ex())
            {
                break;
            }

            Gui.drawRect(x, this.area.y, x + 1, this.area.ey(), 0x44ffffff);
            this.font.drawString(String.valueOf(j * this.scaleX.mult), x + 4, this.area.y + 4, 0xffffff);
        }

        int ty = (int) this.fromGraphY(this.area.ey());
        int by = (int) this.fromGraphY(this.area.y - 12);

        int min = Math.min(ty, by) - 1;
        int max = Math.max(ty, by) + 1;
        int mult = this.scaleY.mult;

        min -= min % mult + mult;
        max -= max % mult - mult;

        for (int j = 0, c = (max - min) / mult; j < c; j++)
        {
            int y = this.toGraphY(min + j * mult);

            if (y > this.area.ey())
            {
                continue;
            }

            Gui.drawRect(this.area.x, y, this.area.ex(), y + 1, 0x44ffffff);
            this.font.drawString(String.valueOf(min + j * mult), this.area.x + 4, y + 4, 0xffffff);
        }

        /* Draw current point at the timeline */
        this.drawCursor(context);

        if (this.channel.isEmpty())
        {
            return;
        }

        /* Draw graph of the keyframe channel */
        GL11.glLineWidth(Minecraft.getMinecraft().gameSettings.guiScale * 1.5F);
        GlStateManager.disableTexture2D();

        BufferBuilder vb = Tessellator.getInstance().getBuffer();

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        /* Colorize the graph for given channel */
        float r = (this.color >> 16 & 255) / 255.0F;
        float g = (this.color >> 8 & 255) / 255.0F;
        float b = (this.color & 255) / 255.0F;

        GlStateManager.color(1, 1, 1, 1);

        /* Draw lines */
        vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        Keyframe prev = null;

        for (Keyframe frame : this.channel.getKeyframes())
        {
            if (prev != null)
            {
                int px = this.toGraphX(prev.tick);
                int fx = this.toGraphX(frame.tick);

                if (prev.interp == KeyframeInterpolation.LINEAR)
                {
                    vb.pos(px, this.toGraphY(prev.value), 0).color(r, g, b, 1).endVertex();
                    vb.pos(fx, this.toGraphY(frame.value), 0).color(r, g, b, 1).endVertex();
                }
                else
                {
                    for (int i = 0; i < 10; i++)
                    {
                        vb.pos(px + (fx - px) * (i / 10F), this.toGraphY(prev.interpolate(frame, i / 10F)), 0).color(r, g, b, 1).endVertex();
                        vb.pos(px + (fx - px) * ((i + 1) / 10F), this.toGraphY(prev.interpolate(frame, (i + 1) / 10F)), 0).color(r, g, b, 1).endVertex();
                    }
                }

                if (prev.interp == KeyframeInterpolation.BEZIER)
                {
                    vb.pos(this.toGraphX(frame.tick - frame.lx), this.toGraphY(frame.value + frame.ly), 0).color(r, g, b, 0.6F).endVertex();
                    vb.pos(this.toGraphX(frame.tick), this.toGraphY(frame.value), 0).color(r, g, b, 0.6F).endVertex();
                }
            }

            if (prev == null)
            {
                vb.pos(0, this.toGraphY(frame.value), 0).color(r, g, b, 1).endVertex();
                vb.pos(this.toGraphX(frame.tick), this.toGraphY(frame.value), 0).color(r, g, b, 1).endVertex();
            }

            if (frame.interp == KeyframeInterpolation.BEZIER)
            {
                vb.pos(this.toGraphX(frame.tick), this.toGraphY(frame.value), 0).color(r, g, b, 0.6F).endVertex();
                vb.pos(this.toGraphX(frame.tick + frame.rx), this.toGraphY(frame.value + frame.ry), 0).color(r, g, b, 0.6F).endVertex();
            }

            prev = frame;
        }

        vb.pos(this.toGraphX(prev.tick), this.toGraphY(prev.value), 0).color(r, g, b, 1).endVertex();
        vb.pos(w, this.toGraphY(prev.value), 0).color(r, g, b, 1).endVertex();

        Tessellator.getInstance().draw();

        /* Draw points */
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for (Keyframe frame : this.channel.getKeyframes())
        {
            this.drawRect(vb, this.toGraphX(frame.tick), this.toGraphY(frame.value), 3, 0xffffff);
        }

        int i = 0;
        prev = null;

        for (Keyframe frame : this.channel.getKeyframes())
        {
            this.drawRect(vb, this.toGraphX(frame.tick), this.toGraphY(frame.value), 2, this.selected == i ? 0x0080ff : 0);

            if (frame.interp == KeyframeInterpolation.BEZIER)
            {
                this.drawRect(vb, this.toGraphX(frame.tick + frame.rx), this.toGraphY(frame.value + frame.ry), 2, this.selected != i ? 0xffffff : 0x0080ff);
            }

            if (prev != null && prev.interp == KeyframeInterpolation.BEZIER)
            {
                this.drawRect(vb, this.toGraphX(frame.tick - frame.lx), this.toGraphY(frame.value + frame.ly), 2, this.selected != i ? 0xffffff : 0x0080ff);
            }

            prev = frame;
            i++;
        }

        Tessellator.getInstance().draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }
}