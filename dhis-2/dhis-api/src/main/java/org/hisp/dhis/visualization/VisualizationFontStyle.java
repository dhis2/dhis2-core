package org.hisp.dhis.visualization;

import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import org.hisp.dhis.common.FontStyle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "visualizationFontStyle", namespace = DXF_2_0 )
public class VisualizationFontStyle
{
    private FontStyle visualizationTitle;

    private FontStyle visualizationSubtitle;

    private FontStyle horizontalAxisTitle;

    private FontStyle verticalAxisTitle;

    private FontStyle legend;

    public VisualizationFontStyle()
    {
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontStyle getVisualizationTitle()
    {
        return visualizationTitle;
    }

    public void setVisualizationTitle( FontStyle visualizationTitle )
    {
        this.visualizationTitle = visualizationTitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontStyle getVisualizationSubtitle()
    {
        return visualizationSubtitle;
    }

    public void setVisualizationSubtitle( FontStyle visualizationSubtitle )
    {
        this.visualizationSubtitle = visualizationSubtitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontStyle getHorizontalAxisTitle()
    {
        return horizontalAxisTitle;
    }

    public void setHorizontalAxisTitle( FontStyle horizontalAxisTitle )
    {
        this.horizontalAxisTitle = horizontalAxisTitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontStyle getVerticalAxisTitle()
    {
        return verticalAxisTitle;
    }

    public void setVerticalAxisTitle( FontStyle verticalAxisTitle )
    {
        this.verticalAxisTitle = verticalAxisTitle;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    public FontStyle getLegend()
    {
        return legend;
    }

    public void setLegend( FontStyle legend )
    {
        this.legend = legend;
    }
}
