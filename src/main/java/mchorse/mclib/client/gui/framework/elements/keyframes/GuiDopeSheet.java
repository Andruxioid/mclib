package mchorse.mclib.client.gui.framework.elements.keyframes;

import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.framework.elements.utils.GuiDraw;
import mchorse.mclib.client.gui.utils.Scale;
import mchorse.mclib.client.gui.utils.keys.IKey;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiDopeSheet extends GuiKeyframeElement
{
    public List<GuiSheet> sheets = new ArrayList<GuiSheet>();
    public Scale scale = new Scale(false);
    public GuiSheet current;
    public int duration;

    public boolean sliding = false;
    public boolean dragging = false;
    private boolean moving = false;
    private boolean scrolling = false;
    public int which = 0;
    private int lastX;
    private double lastT;

    public GuiDopeSheet(Minecraft mc, Consumer<Keyframe> callback)
    {
        super(mc, callback);
    }

    /* Graphing code */

    public int toGraph(double tick)
    {
        return (int) (this.scale.to(tick)) + this.area.mx();
    }

    public double fromGraph(int mouseX)
    {
        return this.scale.from(mouseX - this.area.mx());
    }

    public void resetView()
    {
        int c = 0;

        this.scale.set(0, 2);

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        /* Find minimum and maximum */
        for (GuiSheet sheet : this.sheets)
        {
            for (Keyframe frame : sheet.channel.getKeyframes())
            {
                min = Integer.min((int) frame.tick, min);
                max = Integer.max((int) frame.tick, max);
            }

            c = Math.max(c, sheet.channel.getKeyframes().size());
        }

        if (c <= 1)
        {
            if (c == 0)
            {
                min = 0;
            }

            max = this.duration;
        }

        if (Math.abs(max - min) > 0.01F)
        {
            this.scale.view(min, max, this.area.w, 30);
        }

        this.recalcMultipliers();
    }

    /**
     * Recalculate grid's multipliers 
     */
    private void recalcMultipliers()
    {
        this.scale.mult = this.recalcMultiplier(this.scale.zoom);
    }

    @Override
    public Keyframe getCurrent()
    {
        if (this.current != null)
        {
            return this.current.getKeyframe();
        }

        return null;
    }

    @Override
    public void setDuration(long duration)
    {
        this.duration = (int) duration;
    }

    @Override
    public void setSliding()
    {}

    @Override
    public void selectByDuration(long duration)
    {}

    @Override
    public void doubleClick(int mouseX, int mouseY)
    {
        if (this.which == -1)
        {
            int count = this.sheets.size();
            int h = (this.area.h - 15) / count;
            int i = (mouseY - (this.area.ey() - h * count)) / h;

            if (i < 0 || i >= count)
            {
                return;
            }

            this.current = this.sheets.get(i);

            KeyframeEasing easing = KeyframeEasing.IN;
            KeyframeInterpolation interp = KeyframeInterpolation.LINEAR;
            Keyframe frame = this.getCurrent();
            long tick = (long) this.fromGraph(mouseX);
            long oldTick = tick;

            if (frame != null)
            {
                easing = frame.easing;
                interp = frame.interp;
                oldTick = frame.tick;
            }

            this.current.selected = this.current.channel.insert(tick, this.current.channel.interpolate(tick));
            frame = this.getCurrent();

            if (oldTick != tick)
            {
                frame.setEasing(easing);
                frame.setInterpolation(interp);
            }

            this.addedDoubleClick(frame, tick, mouseX, mouseY);
        }
        else if (this.which == 0)
        {
            Keyframe frame = this.getCurrent();

            if (frame == null)
            {
                return;
            }

            this.current.channel.remove(this.current.selected);
            this.current.selected -= 1;
            this.which = -1;
        }
    }

    protected void addedDoubleClick(Keyframe frame, long tick, int mouseX, int mouseY)
    {}

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
                if (GuiScreen.isAltKeyDown() && this.current != null && this.which == 0)
                {
                    Keyframe frame = this.getCurrent();

                    if (frame != null)
                    {
                        long offset = (long) this.fromGraph(mouseX);
                        Keyframe created = this.current.channel.get(this.current.channel.insert(offset, frame.value));

                        this.current.selected = this.current.channel.getKeyframes().indexOf(created);
                        created.copy(frame);
                        created.tick = offset;
                    }

                    return false;
                }

                this.which = -1;
                this.current = null;

                int count = this.sheets.size();
                int h = (this.area.h - 15) / count;
                int y = this.area.ey() - h * count;
                boolean reset = true;

                for (GuiSheet sheet : this.sheets)
                {
                    int i = 0;
                    sheet.selected = -1;

                    for (Keyframe frame : sheet.channel.getKeyframes())
                    {
                        boolean point = this.isInside(this.toGraph(frame.tick), y + h / 2, mouseX, mouseY);

                        if (point)
                        {
                            this.which = 0;
                            this.current = sheet;
                            this.setKeyframe(frame);

                            this.lastT = frame.tick;

                            this.lastX = mouseX;
                            this.dragging = true;
                            sheet.selected = i;
                            reset = false;

                            break;
                        }

                        i++;
                    }

                    y += h;
                }

                if (this.parent != null && reset)
                {
                    this.dragging = true;
                }
            }
            else if (context.mouseButton == 2)
            {
                this.scrolling = true;
                this.lastX = mouseX;
                this.lastT = this.scale.shift;
            }
        }

        return false;
    }

    private boolean isInside(double x, double y, int mouseX, int mouseY)
    {
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

        if (this.area.isInside(context.mouseX, context.mouseY) && !this.scrolling)
        {
            int scroll = context.mouseWheel;

            if (!Minecraft.IS_RUNNING_ON_MAC)
            {
                scroll = -scroll;
            }

            this.scale.zoom(Math.copySign(this.getZoomFactor(this.scale.zoom), scroll), 0.01F, 50F);
            this.recalcMultipliers();

            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(GuiContext context)
    {
        super.mouseReleased(context);

        if (this.current != null)
        {
            if (this.sliding)
            {
                /* Resort after dragging the tick thing */
                this.current.sort();
                this.sliding = false;

                this.finishSorting();
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

    protected void finishSorting()
    {}

    @Override
    public void draw(GuiContext context)
    {
        int mouseX = context.mouseX;

        if (this.dragging && !this.moving && (Math.abs(this.lastX - mouseX) > 3))
        {
            this.moving = true;
            this.sliding = true;
        }

        if (this.scrolling)
        {
            this.scale.shift = -(mouseX - this.lastX) / this.scale.zoom + this.lastT;
        }
        /* Move the current keyframe */
        else if (this.moving)
        {
            Keyframe frame = this.getCurrent();
            double x = this.fromGraph(mouseX);

            if (this.which == 0)
            {
                frame.setTick((long) x);
            }
            else if (this.which == -1 && this.parent != null)
            {
                this.moveNoKeyframe(context, frame, x, 0);
            }

            this.setKeyframe(this.getCurrent());
        }

        /* Draw shit */
        this.area.draw(0x88000000);

        int w = this.area.w;
        int leftBorder = (int) this.toGraph(0);
        int rightBorder = (int) this.toGraph(this.duration);

        if (leftBorder > 0) Gui.drawRect(0, this.area.y, leftBorder, this.area.y + this.area.h, 0x88000000);
        if (rightBorder < w) Gui.drawRect(rightBorder, this.area.y, w, this.area.y + this.area.h, 0x88000000);

        /* Draw scaling grid */
        int hx = this.duration / this.scale.mult;

        for (int j = 0; j <= hx; j++)
        {
            int x = (int) this.toGraph(j * this.scale.mult);

            Gui.drawRect(this.area.x + x, this.area.y, this.area.x + x + 1, this.area.ey(), 0x44ffffff);
            this.font.drawString(String.valueOf(j * this.scale.mult), this.area.x + x + 4, this.area.y + 4, 0xffffff);
        }

        /* Draw current point at the timeline */
        this.drawCursor(context);

        /* Draw dope sheet */
        int count = this.sheets.size();
        int h = (this.area.h - 15) / count;
        int y = this.area.ey() - h * count;

        for (GuiSheet sheet : this.sheets)
        {
            float r = (sheet.color >> 16 & 255) / 255.0F;
            float g = (sheet.color >> 8 & 255) / 255.0F;
            float b = (sheet.color & 255) / 255.0F;

            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(Minecraft.getMinecraft().gameSettings.guiScale * 1.5F);

            BufferBuilder vb = Tessellator.getInstance().getBuffer();

            vb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            vb.pos(this.area.x, y + h / 2, 0).color(r, g, b, 0.65F).endVertex();
            vb.pos(this.area.ex(), y + h / 2, 0).color(r, g, b, 0.65F).endVertex();

            Tessellator.getInstance().draw();

            /* Draw points */
            vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            int i = 0;

            for (Keyframe frame : sheet.channel.getKeyframes())
            {
                this.drawRect(vb, this.toGraph(frame.tick), y + h / 2, 3, this.current == sheet && i == sheet.selected ? 0xffffff : sheet.color);

                i++;
            }

            i = 0;

            for (Keyframe frame : sheet.channel.getKeyframes())
            {
                this.drawRect(vb, this.toGraph(frame.tick), y + h / 2, 2, this.current == sheet && i == sheet.selected ? 0x0080ff : 0);

                i++;
            }

            Tessellator.getInstance().draw();

            int lw = this.font.getStringWidth(sheet.title.get()) + 10;
            GuiDraw.drawHorizontalGradientRect(this.area.ex() - lw - 10, y, this.area.ex(), y + h, sheet.color, 0xaa000000 + sheet.color, 0);
            this.font.drawStringWithShadow(sheet.title.get(), this.area.ex() - lw + 5, y + (h - this.font.FONT_HEIGHT) / 2 + 1, 0xffffff);

            y += h;
        }

        super.draw(context);
    }

    public static class GuiSheet
    {
        public IKey title;
        public int color;
        public KeyframeChannel channel;
        public int selected = -1;

        public GuiSheet(IKey title, int color, KeyframeChannel channel)
        {
            this.title = title;
            this.color = color;
            this.channel = channel;
        }

        public void sort()
        {
            Keyframe frame = this.getKeyframe();

            this.channel.sort();
            this.selected = this.channel.getKeyframes().indexOf(frame);
        }

        public Keyframe getKeyframe()
        {
            return this.channel.get(this.selected);
        }
    }
}