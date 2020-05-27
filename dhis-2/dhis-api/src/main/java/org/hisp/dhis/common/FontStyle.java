package org.hisp.dhis.common;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "fontStyle", namespace = DXF_2_0 )
public class FontStyle
{
    private String font;

    private Integer fontSize;

    private Boolean bold;

    private Boolean italic;

    private Boolean underline;

    private String textColor;

    public FontStyle()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getFont()
    {
        return font;
    }

    public void setFont( String font )
    {
        this.font = font;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Integer getFontSize()
    {
        return fontSize;
    }

    public void setFontSize( Integer fontSize )
    {
        this.fontSize = fontSize;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Boolean getBold()
    {
        return bold;
    }

    public void setBold( Boolean bold )
    {
        this.bold = bold;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Boolean getItalic()
    {
        return italic;
    }

    public void setItalic( Boolean italic )
    {
        this.italic = italic;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public Boolean getUnderline()
    {
        return underline;
    }

    public void setUnderline( Boolean underline )
    {
        this.underline = underline;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public String getTextColor()
    {
        return textColor;
    }

    public void setTextColor( String textColor )
    {
        this.textColor = textColor;
    }
}
