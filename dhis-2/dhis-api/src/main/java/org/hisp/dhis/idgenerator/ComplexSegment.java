package org.hisp.dhis.idgenerator;

import java.util.List;

class ComplexSegment
    implements Segment
{
    private List<Segment> segments;

    ComplexSegment( List<Segment> segments )
    {
        this.segments = segments;
    }

    @Override
    public String getValue()
    {
        return segments.stream()
            .map( Segment::getValue )
            .reduce( ( id, acc ) -> acc + id )
            .get();
    }

    @Override
    public IDExpressionType getType()
    {
        return IDExpressionType.COMPLEX;
    }

    public List<Segment> getChildSegments()
    {
        return this.segments;
    }
}