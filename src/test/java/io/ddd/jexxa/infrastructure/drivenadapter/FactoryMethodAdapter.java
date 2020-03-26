package io.ddd.jexxa.infrastructure.drivenadapter;

import java.util.Properties;

import io.ddd.jexxa.applicationcore.domainservice.IFactroyMethodService;

public class FactoryMethodAdapter implements IFactroyMethodService
{
    private FactoryMethodAdapter()
    {
        
    }


    private FactoryMethodAdapter(Properties properties)
    {

    }

    public static IFactroyMethodService create()
    {
        return new FactoryMethodAdapter();
    }

    public static IFactroyMethodService create(Properties properties)
    {
        return new FactoryMethodAdapter(properties);
    }

}