package io.jexxa.infrastructure.drivenadapterstrategy.messaging;

import java.util.Optional;
import java.util.Properties;

import io.jexxa.utils.factory.ClassFactory;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.jms.JMSSender;
import io.jexxa.utils.annotations.CheckReturnValue;
import org.apache.commons.lang3.Validate;

public final class MessageSenderManager
{
    private static final MessageSenderManager MESSAGE_SENDER_MANAGER = new MessageSenderManager();

    private static Class<? extends MessageSender> defaultStrategy = JMSSender.class;

    private MessageSenderManager()
    {
        //Private constructor
    }

    @CheckReturnValue
    public MessageSender getStrategy(Properties properties)
    {
        try {
            Optional<MessageSender> strategy = ClassFactory.newInstanceOf(defaultStrategy, new Object[]{properties});

            if (strategy.isPresent())
            {
                return strategy.get();
            }

            return ClassFactory.newInstanceOf(defaultStrategy).orElseThrow();
        }
        catch (ReflectiveOperationException e)
        {
            if ( e.getCause() != null)
            {
                throw new IllegalArgumentException(e.getCause().getMessage(), e);
            }

            throw new IllegalArgumentException("No suitable default IRepository available", e);
        }
    }

    public static void setDefaultStrategy(Class<? extends MessageSender>  defaultStrategy)
    {
        Validate.notNull(defaultStrategy);

        MessageSenderManager.defaultStrategy = defaultStrategy;
    }

    public static Class<? extends MessageSender> getDefaultStrategy()
    {
        return defaultStrategy;
    }


    /**
     * @deprecated getInstance will be removed in future releases. Instead a public static API is offered to configure the the strategies
     * @return Returns the managing component for message sender strategies
     */
    @Deprecated(forRemoval = true)
    public static MessageSenderManager getInstance()
    {
        return MESSAGE_SENDER_MANAGER;
    }

    public static MessageSender getMessageSender(Properties properties) { return MESSAGE_SENDER_MANAGER.getStrategy(properties); }
}
