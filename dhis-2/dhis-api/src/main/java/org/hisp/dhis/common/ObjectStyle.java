package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.io.Serializable;

public class ObjectStyle implements Serializable
{

    private String color;

    private String icon;

    private String cake;

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getColor()
    {
        return color;
    }

    public void setColor( String color )
    {
        this.color = color;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getIcon()
    {
        return icon;
    }

    public void setIcon( String icon )
    {
        this.icon = icon;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCake()
    {
        return cake;
    }

    public void setCake( String cake )
    {
        this.cake = cake;
    }
}
