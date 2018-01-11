package org.hisp.dhis.render.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;

public class ValueTypeRenderingObject
{
    private ValueTypeRenderingType type;

    private Integer min;

    private Integer max;

    private Integer step;

    private Integer decimalPoints;

    public ValueTypeRenderingObject()
    {
        this.type = ValueTypeRenderingType.DEFAULT;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getDecimalPoints()
    {
        return decimalPoints;
    }

    public void setDecimalPoints( Integer decimalPoints )
    {
        this.decimalPoints = decimalPoints;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getStep()
    {
        return step;
    }

    public void setStep( Integer step )
    {
        this.step = step;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getMax()
    {
        return max;
    }

    public void setMax( Integer max )
    {
        this.max = max;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getMin()
    {
        return min;
    }

    public void setMin( Integer min )
    {
        this.min = min;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueTypeRenderingType getType()
    {
        return type;
    }

    public void setType( ValueTypeRenderingType renderingType )
    {
        this.type = renderingType;
    }
}
