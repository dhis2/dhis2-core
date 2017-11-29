package org.hisp.dhis.idgenerator;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestIDExpression
{
    @Test
    public void testAddSimpleSegment()
    {
        IDExpression idExpression = new IDExpression();

        idExpression.addSimpleSegment( "Hello world!", IDExpressionType.TEXT );

        assertEquals( "Hello world!", idExpression.getSegments().get( 0 ).getValue() );
        assertEquals( IDExpressionType.TEXT, idExpression.getSegments().get( 0 ).getType() );
    }

    @Test
    public void testAddComplexSegment()
    {
        IDExpression idExpression = new IDExpression();
        List<Segment> segments = Lists.newArrayList(
            new DummySegment(),
            new DummySegment(),
            new DummySegment()
        );

        idExpression.addComplexSegment( segments );

        assertEquals( 1, idExpression.getSegments().size() );
        assertEquals( "ValueValueValue", idExpression.getSegments().get( 0 ).getValue() );
        assertEquals( IDExpressionType.COMPLEX, idExpression.getSegments().get( 0 ).getType() );

        ComplexSegment complexSegment = (ComplexSegment) idExpression.getSegments().get( 0 );

        assertEquals( 3, complexSegment.getChildSegments().size() );
        assertEquals( DummySegment.class, complexSegment.getChildSegments().get( 0 ).getClass() );

    }

    private class DummySegment
        implements Segment
    {

        @Override
        public String getValue()
        {
            return "Value";
        }

        @Override
        public IDExpressionType getType()
        {
            return IDExpressionType.TEXT;
        }
    }
}
