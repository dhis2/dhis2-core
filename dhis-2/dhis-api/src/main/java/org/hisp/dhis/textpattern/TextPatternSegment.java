package org.hisp.dhis.textpattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.sun.javafx.binding.StringFormatter;
import org.hisp.dhis.common.DxfNamespaces;

public class TextPatternSegment
{
    private TextPatternMethod method;

    private String parameter;

    public TextPatternSegment()
    {
        this.parameter = "";
    }

    public TextPatternSegment( TextPatternMethod method, String segment )
    {
        this.method = method;
        this.parameter = method.getType().getParam( segment );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public TextPatternMethod getMethod()
    {
        return method;
    }

    public void setMethod( TextPatternMethod method )
    {
        this.method = method;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getParameter()
    {
        return parameter;
    }

    public void setParameter( String parameter )
    {
        this.parameter = parameter;
    }


    /* Helper methods */

    /**
     * Recreates the original segment text from the method name and segment parameter.
     *
     * @return The original segment based on method and parameter
     */
    public String getRawSegment()
    {
        if ( method.equals( TextPatternMethod.TEXT ) )
        {
            return "\"" + parameter + "\"";
        }
        else
        {
            return StringFormatter.format( "%s(%s)", method.name(), parameter ).getValue();
        }
    }
}
