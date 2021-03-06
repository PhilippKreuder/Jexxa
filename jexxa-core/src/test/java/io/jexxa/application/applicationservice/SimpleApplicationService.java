package io.jexxa.application.applicationservice;


import java.util.ArrayList;
import java.util.List;

import io.jexxa.application.annotation.ApplicationService;
import io.jexxa.application.domain.valueobject.JexxaValueObject;
import io.jexxa.application.domain.valueobject.SpecialCasesValueObject;

@SuppressWarnings("unused")
@ApplicationService
public class SimpleApplicationService
{
    private int firstValue;
    private List<String> messages = new ArrayList<>();
    private List<JexxaValueObject> valueObjects = new ArrayList<>();

    public static class SimpleApplicationException extends Exception
    {
        private static final long serialVersionUID = 1L;

        public SimpleApplicationException(String information)
        {
            super(information);
        }
    }

    public SimpleApplicationService()
    {
        firstValue = 42;
    }

    public int getSimpleValue()
    {
      return firstValue;
    }

    public int setGetSimpleValue(int newValue )
    {
        int oldValue = firstValue;
        this.firstValue = newValue;
        return oldValue;
    }

    public void throwExceptionTest() throws SimpleApplicationException
    {
        throw new SimpleApplicationException("TestException");
    }

    public int throwNullPointerException()   // Test runtime exception
    {
        JexxaValueObject jexxaValueObject = null;
        //noinspection ConstantConditions
        return jexxaValueObject.getValue();
    }


    public void setSimpleValue(int simpleValue)
    {
        this.firstValue = simpleValue;
    }

    public void setSimpleValueObject(JexxaValueObject simpleValueObject)
    {
        setSimpleValue(simpleValueObject.getValue());
    }

    public void setSimpleValueObjectTwice(JexxaValueObject first, JexxaValueObject second)
    {
        setSimpleValue(first.getValue());
        setSimpleValue(second.getValue());
    }

    public void addMessage(String message)
    {
        messages.add(message);
    }

    public void setMessages(List<String> messages)
    {
        this.messages = messages;
    }

    public void setValueObjectsAndMessages(List<JexxaValueObject> valueObjects, List<String> messages)
    {
        this.messages = messages;
        this.valueObjects = valueObjects;
    }

    public List<String> getMessages()
    {
        return messages;
    }

    public List<JexxaValueObject> getValueObjects()
    {
        return valueObjects;
    }

    public JexxaValueObject getSimpleValueObject()
    {
        return  new JexxaValueObject(firstValue);
    }

    public SpecialCasesValueObject getSpecialCasesValueObject()
    {
        return  SpecialCasesValueObject.SPECIAL_CASES_VALUE_OBJECT;
    }

    public static SpecialCasesValueObject testStaticGetMethod()
    {
        return SpecialCasesValueObject.SPECIAL_CASES_VALUE_OBJECT;
    }

    public static void testStaticSetMethod(JexxaValueObject jexxaValueObject)
    {
        //This method is just for testing static behavior => So no code is required
    }


}
