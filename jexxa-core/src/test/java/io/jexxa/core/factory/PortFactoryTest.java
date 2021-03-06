package io.jexxa.core.factory;


import static io.jexxa.TestConstants.JEXXA_APPLICATION_SERVICE;
import static io.jexxa.TestConstants.JEXXA_DOMAIN_SERVICE;
import static io.jexxa.TestConstants.JEXXA_DRIVEN_ADAPTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import io.jexxa.TestConstants;
import io.jexxa.application.applicationservice.ApplicationServiceWithDrivenAdapters;
import io.jexxa.application.applicationservice.ApplicationServiceWithInvalidDrivenAdapters;
import io.jexxa.application.infrastructure.drivingadapter.InvalidPortAdapter;
import io.jexxa.application.infrastructure.drivingadapter.messaging.SimpleApplicationServiceAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
@Tag(TestConstants.UNIT_TEST)
class PortFactoryTest
{
    @Test
    void createPortWithAvailableDrivenAdapter()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER);
        var objectUnderTest = new PortFactory(drivenAdapterFactory)
                .acceptPackage(JEXXA_APPLICATION_SERVICE);

        //Act
        boolean result = objectUnderTest.isAvailable(ApplicationServiceWithDrivenAdapters.class);

        //Assert
        assertTrue(result);
    }

    @Test
    void createPortWithMissingAdapter()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage("invalid.package");
        var objectUnderTest = new PortFactory(drivenAdapterFactory).
                acceptPackage(JEXXA_APPLICATION_SERVICE);
        var properties = new Properties();

        //Act
        boolean result = objectUnderTest.isAvailable(ApplicationServiceWithDrivenAdapters.class);

        //Assert
        assertFalse(result);
        assertThrows(MissingAdapterException.class, () -> objectUnderTest.getInstanceOf(ApplicationServiceWithDrivenAdapters.class, properties));
    }

    @Test
    void createPortWithInvalidDrivenAdapter()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER).acceptPackage(JEXXA_DOMAIN_SERVICE);
        var objectUnderTest = new PortFactory(drivenAdapterFactory)
                .acceptPackage(JEXXA_APPLICATION_SERVICE)
                .acceptPackage(JEXXA_DOMAIN_SERVICE);

        var properties = new Properties();

        //Act - Assert
        assertThrows(InvalidAdapterException.class,
                () -> objectUnderTest.getInstanceOf(ApplicationServiceWithInvalidDrivenAdapters.class, properties));
    }

    @Test
    void newInstanceOfPort()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER);
        var objectUnderTest = new PortFactory(drivenAdapterFactory).
                acceptPackage(JEXXA_APPLICATION_SERVICE);

        //Act
        var first = objectUnderTest.newInstanceOf(ApplicationServiceWithDrivenAdapters.class, new Properties());
        var second = objectUnderTest.newInstanceOf(ApplicationServiceWithDrivenAdapters.class, new Properties());

        //Assert
        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    @Test
    void getInstanceOfPort()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER);
        var objectUnderTest = new PortFactory(drivenAdapterFactory).
                acceptPackage(JEXXA_APPLICATION_SERVICE);

        //Act
        var first = objectUnderTest.getInstanceOf(ApplicationServiceWithDrivenAdapters.class, new Properties());
        var second = objectUnderTest.getInstanceOf(ApplicationServiceWithDrivenAdapters.class, new Properties());

        //Assert that first and second adapter are equal
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }


    @Test
    void getInstanceOfPortAdapter()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER);
        var objectUnderTest = new PortFactory(drivenAdapterFactory).
                acceptPackage(JEXXA_APPLICATION_SERVICE);

        //Act
        var first = objectUnderTest.getPortAdapterOf(SimpleApplicationServiceAdapter.class, new Properties());
        var second = objectUnderTest.getPortAdapterOf(SimpleApplicationServiceAdapter.class, new Properties());

        //Assert that first and second adapter are equal
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first.getPort(), second.getPort());
    }

    @Test
    void getInstanceOfInvalidPortAdapter()
    {
        //Arrange
        var drivenAdapterFactory = new AdapterFactory().
                acceptPackage(JEXXA_DRIVEN_ADAPTER);
        var objectUnderTest = new PortFactory(drivenAdapterFactory).
                acceptPackage(JEXXA_APPLICATION_SERVICE);

        //Act / Assert
        var exception = assertThrows(PortFactory.InvalidPortConfigurationException.class,
                () -> objectUnderTest.getPortAdapterOf(InvalidPortAdapter.class, new Properties()) );

        assertNotNull(exception.getMessage());
    }
}