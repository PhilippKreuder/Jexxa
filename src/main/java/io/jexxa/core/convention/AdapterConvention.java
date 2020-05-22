package io.jexxa.core.convention;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

public class AdapterConvention
{
    public static <T> void validate(Class<T> clazz)
    {
        try
        {
            clazz.getConstructor();
            return; //Default constructor available
        }
        catch (NoSuchMethodException | SecurityException ignored) {}


        try
        {
            clazz.getConstructor(Properties.class);
            return; //Constructor with Properties argument available
        }
        catch (NoSuchMethodException | SecurityException ignored)
        {
        }

        var factoryMethods = Arrays
                .stream(clazz.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getReturnType().isAssignableFrom(clazz))
                .collect(Collectors.toList());

        if ( factoryMethods.stream().anyMatch(method -> method.getParameterCount() == 0) )
        {
            return; //Factory method with no arguments available
        }

        if ( factoryMethods.stream().anyMatch(method -> (
                    method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0].isAssignableFrom(Properties.class)))
        )
        {
            return; //Factory method with Properties argument available
        }

        throw new AdapterConventionViolation("No suitable constructor available for adapter : " + clazz.getName());
    }


    public static <P> boolean isPortAdapter(Class<P> port)
    {
        return Arrays.stream(port.getConstructors())
                .filter(constructor -> constructor.getParameterTypes().length == 1)
                .anyMatch(constructor -> !constructor.getParameterTypes()[0].isInterface());
    }

    private AdapterConvention()
    {

    }
}