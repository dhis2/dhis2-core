package org.hisp.dhis.dto.schemas;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@JsonIgnoreProperties( ignoreUnknown = true )
public class SchemaProperty
{
    private String name;

    private boolean required;

    private List<String> constants;

    private String relativeApiEndpoint;

    public Double min;

    public Double max;

    private long length;

    private PropertyType propertyType;

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public boolean isRequired()
    {
        return required;
    }

    public void setRequired( boolean required )
    {
        this.required = required;
    }

    public List<String> getConstants()
    {
        return constants;
    }

    public void setConstants( List<String> constants )
    {
        this.constants = constants;
    }

    public String getRelativeApiEndpoint()
    {
        return relativeApiEndpoint;
    }

    public void setRelativeApiEndpoint( String relativeApiEndpoint )
    {
        this.relativeApiEndpoint = relativeApiEndpoint;
    }

    public Double getMin()
    {
        return min;
    }

    public void setMin( Double o )
    {

        this.min = o;
    }

    public PropertyType getPropertyType()
    {
        return propertyType;
    }

    public void setPropertyType( PropertyType propertyType )
    {
        this.propertyType = propertyType;
    }

    public Double getMax()
    {
        return max;
    }

    public void setMax( Double max )
    {
        this.max = max;
    }

    public long getLength()
    {
        return length;
    }

    public void setLength( long length )
    {
        this.length = length;
    }
}
