package org.hisp.dhis.idgenerator;

import java.util.ArrayList;
import java.util.List;

public class IDExpression
{
    private ArrayList<Segment> segments;

    IDExpression()
    {
        this.segments = new ArrayList<>();
    }

    public void addSimpleSegment( String value, IDExpressionType type )
    {
        this.segments.add( new SimpleSegment( value, type ) );
    }

    public void addComplexSegment( List<Segment> segments )
    {
        this.segments.add( new ComplexSegment( segments ) );
    }

    public List<Segment> getSegments()
    {
        return segments;
    }

}
