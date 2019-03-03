package mchorse.mclib.client.gui.framework.elements;

import java.io.IOException;

import mchorse.mclib.client.gui.framework.GuiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Delegated {@link IGuiElement}
 */
@SideOnly(Side.CLIENT)
public class GuiDelegateElement<T extends IGuiElement> extends GuiElement implements IGuiLegacy
{
    public T delegate;

    public GuiDelegateElement(Minecraft mc, T element)
    {
        super(mc);
        this.delegate = element;
    }

    public void setDelegate(T element)
    {
        GuiScreen screen = this.mc.currentScreen;

        this.delegate = element;

        if (screen != null)
        {
            this.resize(screen.width, screen.height);
        }
    }

    @Override
    public boolean isEnabled()
    {
        return this.delegate == null ? false : this.delegate.isEnabled();
    }

    @Override
    public boolean isVisible()
    {
        return this.delegate == null ? true : this.delegate.isVisible();
    }

    @Override
    public void resize(int width, int height)
    {
        if (this.delegate instanceof GuiElement)
        {
            ((GuiElement) this.delegate).resizer = this.resizer;
        }

        if (this.delegate != null)
        {
            this.delegate.resize(width, height);
        }
    }

    @Override
    public boolean handleMouseInput(int mouseX, int mouseY) throws IOException
    {
        if (this.delegate instanceof IGuiLegacy)
        {
            return ((IGuiLegacy) this.delegate).handleMouseInput(mouseX, mouseY);
        }

        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.delegate != null)
        {
            return this.delegate.mouseClicked(mouseX, mouseY, mouseButton);
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, int scroll)
    {
        if (this.delegate != null)
        {
            return this.delegate.mouseScrolled(mouseX, mouseY, scroll);
        }

        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state)
    {
        if (this.delegate != null)
        {
            this.delegate.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public boolean hasActiveTextfields()
    {
        return this.delegate != null ? this.delegate.hasActiveTextfields() : false;
    }

    @Override
    public void unfocus()
    {
        if (this.delegate != null)
        {
            this.delegate.unfocus();
        }
    }

    @Override
    public boolean handleKeyboardInput() throws IOException
    {
        if (this.delegate instanceof IGuiLegacy)
        {
            return ((IGuiLegacy) this.delegate).handleKeyboardInput();
        }

        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode)
    {
        if (this.delegate != null)
        {
            this.delegate.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void draw(GuiTooltip tooltip, int mouseX, int mouseY, float partialTicks)
    {
        if (this.delegate != null)
        {
            this.delegate.draw(tooltip, mouseX, mouseY, partialTicks);
        }
    }
}