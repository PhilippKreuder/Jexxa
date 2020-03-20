package io.ddd.jexxa.core;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import io.ddd.stereotype.applicationcore.ApplicationService;
import org.junit.Test;

public class ClassFactoryTest
{
    @Test
    public void createApplicationService()
    {
        //Arrange
        var annotationScanner = new AnnotationScanner();
        var factoryResults = new ArrayList<Object>();

        var objectUnderTest = new ClassFactory();

        //Act
        var result = annotationScanner.findClassAnnotation(ApplicationService.class);
        result.forEach( element -> factoryResults.add( objectUnderTest.createByConstructor(element)) );

        //Assert
        assertFalse(factoryResults.isEmpty());

        result.forEach( element -> System.out.println(element.getSimpleName()));
        
    }
}