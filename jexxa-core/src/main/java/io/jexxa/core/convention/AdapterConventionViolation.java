package io.jexxa.core.convention;

public class AdapterConventionViolation extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    AdapterConventionViolation(String message)
    {
        super(message);
    }
}