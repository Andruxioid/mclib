package mchorse.mclib.math.functions.trig;

import mchorse.mclib.math.IValue;
import mchorse.mclib.math.functions.Function;

public class Cos extends Function
{
    public Cos(IValue[] values, String name) throws Exception
    {
        super(values, name);
    }

    @Override
    public int getRequiredArguments()
    {
        return 1;
    }

    @Override
    public double get()
    {
        return Math.cos(this.getArg(0));
    }
}