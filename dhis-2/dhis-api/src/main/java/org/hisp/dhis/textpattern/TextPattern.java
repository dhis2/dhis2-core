package org.hisp.dhis.textpattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.collect.ImmutableList;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a TextPattern - A String that is used to generate and validate a user-defined patterns.
 * Example pattern:
 * "Current date: " + CURRENT_DATE("DD-MM-yyyy")
 * <p>
 * Read more about patterns in TextPatternMethod.
 *
 * @author Stian Sandvold
 */
public class TextPattern
{
    private ImmutableList<TextPatternSegment> segments;

    private String clazz;

    private String ownerUID;

    TextPattern()
    {
        this.segments = ImmutableList.of();
    }

    TextPattern( List<TextPatternSegment> segments )
    {
        this.segments = ImmutableList.copyOf( segments );
    }

    public void setOwnerUID( String ownerUID )
    {
        this.ownerUID = ownerUID;
    }

    public String getOwnerUID()
    {
        return ownerUID;
    }

    public void setSegments( ArrayList<TextPatternSegment> segments )
    {
        this.segments = ImmutableList.copyOf( segments );
    }

    public String getClazz()
    {
        return clazz;
    }

    public void setClazz( String clazz )
    {
        this.clazz = clazz;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<TextPatternSegment> getSegments()
    {
        return this.segments;
    }

}
