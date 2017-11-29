package org.hisp.dhis.idgenerator;

public class SimpleSegment
    implements Segment
{
    private String value;

    private IDExpressionType type;

    SimpleSegment( String value, IDExpressionType type )
    {
        this.value = value;
        this.type = type;
    }

    @Override
    public String getValue()
    {
        return value;
    }

    @Override
    public IDExpressionType getType()
    {
        return type;
    }
}
