package org.hisp.dhis.schema.validation;

import org.hisp.dhis.schema.SchemaService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * This is a unit test for the {@link DefaultSchemaValidator}.
 *
 * @author Jan Bernitt
 */
public class DefaultSchemaValidatorTest
{

    private final SchemaService schemaService = Mockito.mock( SchemaService.class );

    private final DefaultSchemaValidator validator = new DefaultSchemaValidator( schemaService );

    @Test
    public void testRequiredPropertyIsNull()
    {

    }

    @Test
    public void testNonRequiredPropertyIsNull()
    {

    }

    @Test
    public void testStringPropertyTooLong()
    {

    }

    @Test
    public void testStringPropertyShorterThanMinLength()
    {

    }

    @Test
    public void testStringPropertyLongerThanMaxLength()
    {

    }

    @Test
    public void testEmailPropertyValid()
    {

    }

    @Test
    public void testEmailPropertyInvalid()
    {

    }

    @Test
    public void testUrlPropertyValid()
    {

    }

    @Test
    public void testUrlPropertyInvalid()
    {

    }

    @Test
    public void testPasswordPropertyValid()
    {

    }

    @Test
    public void testPasswordPropertyInvalid()
    {

    }

    @Test
    public void testColorPropertyValid()
    {

    }

    @Test
    public void testColorPropertyInvalid()
    {

    }

    @Test
    public void testIntegerPropertySmallerThanMinValue()
    {

    }

    @Test
    public void testIntegerPropertyLargerThanMaxValue()
    {

    }

    @Test
    public void testFloatPropertySmallerThanMinValue()
    {

    }

    @Test
    public void testFloatPropertyLargerThanMaxValue()
    {

    }

    @Test
    public void testDoublePropertySmallerThanMinValue()
    {

    }

    @Test
    public void testDoublePropertyLargerThanMaxValue()
    {

    }

    @Test
    public void testCollectionPropertySizeSmallerThanMinSize()
    {

    }

    @Test
    public void testCollectionPropertySizeLargerThanMaxSize()
    {

    }
}
