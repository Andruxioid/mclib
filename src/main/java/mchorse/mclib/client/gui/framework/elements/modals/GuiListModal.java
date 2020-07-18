package mchorse.mclib.client.gui.framework.elements.modals;

import mchorse.mclib.client.gui.framework.elements.buttons.GuiButtonElement;
import mchorse.mclib.client.gui.framework.elements.list.GuiStringListElement;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.utils.keys.IKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.util.Collection;
import java.util.function.Consumer;

public class GuiListModal extends GuiModal
{
    public Consumer<String> callback;
    public String label;

    public GuiButtonElement pick;
    public GuiButtonElement cancel;
    public GuiStringListElement limbs;

    public GuiListModal(Minecraft mc, IKey label, Consumer<String> callback)
    {
        super(mc, label);

        this.callback = callback;

        this.pick = new GuiButtonElement(mc, IKey.lang("mclib.gui.ok"), (b) -> this.send());
        this.cancel = new GuiButtonElement(mc, IKey.lang("mclib.gui.cancel"), (b) -> this.removeFromParent());
        this.limbs = new GuiStringListElement(mc, null);

        this.pick.flex().relative(this.area).set(10, 0, 0, 20).y(1, -30).w(0.5F, -15);
        this.cancel.flex().relative(this.area).set(10, 0, 0, 20).y(1, -30).x(0.5F, 5).w(0.5F, -15);

        this.limbs.flex().set(10, 0, 0, 0).relative(this.area).y(0.4F, 0).w(1, -20).h(0.6F, -35);
        this.limbs.add(I18n.format("mclib.gui.none"));
        this.limbs.setIndex(0);

        this.add(this.pick, this.cancel, this.limbs);
    }

    public GuiListModal setValue(String value)
    {
        if (value.isEmpty())
        {
            this.limbs.setIndex(0);
        }
        else
        {
            this.limbs.setCurrent(value);
        }

        return this;
    }

    public GuiListModal addValues(Collection<String> values)
    {
        this.limbs.add(values);

        return this;
    }

    public void send()
    {
        if (this.limbs.isDeselected())
        {
            return;
        }

        this.removeFromParent();

        if (this.callback != null)
        {
            this.callback.accept(this.limbs.getIndex() == 0 ? "" : this.limbs.getCurrentFirst());
        }
    }

    @Override
    public boolean keyTyped(GuiContext context)
    {
        if (super.keyTyped(context))
        {
            return true;
        }

        if (context.keyCode == Keyboard.KEY_RETURN)
        {
            this.send();

            return true;
        }
        else if (context.keyCode == Keyboard.KEY_ESCAPE)
        {
            this.removeFromParent();

            return true;
        }

        return false;
    }
}