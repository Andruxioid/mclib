package mchorse.mclib.client.gui.framework.elements;

import java.util.function.Consumer;

import mchorse.mclib.client.gui.framework.GuiTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPageButtonList.GuiResponder;
import net.minecraft.client.gui.GuiTextField;

/**
 * GUI text element
 * 
 * This element is a wrapper for the text field class
 */
public class GuiTextElement extends GuiElement implements GuiResponder
{
    public GuiTextField field;
    public Consumer<String> callback;

    public GuiTextElement(Minecraft mc, int maxLength, Consumer<String> callback)
    {
        this(mc, callback);
        this.field.setMaxStringLength(maxLength);
    }

    public GuiTextElement(Minecraft mc, Consumer<String> callback)
    {
        super(mc);

        this.field = new GuiTextField(0, this.font, 0, 0, 0, 0);
        this.field.setGuiResponder(this);
        this.callback = callback;
    }

    public void setText(String text)
    {
        if (text == null)
        {
            text = "";
        }

        this.field.setText(text);
        this.field.setCursorPositionZero();
    }

    @Override
    public void setEntryValue(int id, boolean value)
    {}

    @Override
    public void setEntryValue(int id, float value)
    {}

    @Override
    public void setEntryValue(int id, String value)
    {
        if (this.callback != null)
        {
            this.callback.accept(value);
        }
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        this.field.setEnabled(enabled);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        this.field.setVisible(visible);
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);

        this.field.x = this.area.x + 1;
        this.field.y = this.area.y + 1;
        this.field.width = this.area.w - 2;
        this.field.height = this.area.h - 2;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (super.mouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        this.field.mouseClicked(mouseX, mouseY, mouseButton);

        return false;
    }

    @Override
    public boolean hasActiveTextfields()
    {
        return this.field.isFocused();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode)
    {
        super.keyTyped(typedChar, keyCode);

        this.field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    public void draw(GuiTooltip tooltip, int mouseX, int mouseY, float partialTicks)
    {
        this.field.drawTextBox();

        super.draw(tooltip, mouseX, mouseY, partialTicks);
    }
}