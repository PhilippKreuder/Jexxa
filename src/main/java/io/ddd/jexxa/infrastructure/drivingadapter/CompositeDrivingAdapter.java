package io.ddd.jexxa.infrastructure.drivingadapter;

import java.util.ArrayList;
import java.util.List;

public class CompositeDrivingAdapter implements IDrivingAdapter
{
    private final List<IDrivingAdapter> drivingAdapterlist = new ArrayList<>();

    @Override
    public void start()
    {
        drivingAdapterlist.forEach(IDrivingAdapter::start);
    }

    @Override
    public void stop()
    {
        drivingAdapterlist.forEach(IDrivingAdapter::stop);
    }

    @Override
    public void register(Object object)
    {
        drivingAdapterlist.forEach(element -> element.register(object));
    }

    public void add(IDrivingAdapter drivingAdapter)
    {
        drivingAdapterlist.add(drivingAdapter);
    }

}
