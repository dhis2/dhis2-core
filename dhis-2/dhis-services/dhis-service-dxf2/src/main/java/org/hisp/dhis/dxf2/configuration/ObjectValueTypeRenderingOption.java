package org.hisp.dhis.dxf2.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.render.type.ValueTypeRenderingType;

import java.util.Objects;
import java.util.Set;

public class ObjectValueTypeRenderingOption
{

    private Class clazz;

    private ValueType valueType;

    private boolean hasOptionSet;

    private Set<ValueTypeRenderingType> renderingTypes;

    public ObjectValueTypeRenderingOption( Class clazz, ValueType valueType, boolean hasOptionSet,
        Set<ValueTypeRenderingType> renderingTypes )
    {
        this.clazz = clazz;
        this.valueType = valueType;
        this.hasOptionSet = hasOptionSet;
        this.renderingTypes = renderingTypes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<ValueTypeRenderingType> getRenderingTypes()
    {
        return renderingTypes;
    }

    public void setRenderingTypes( Set<ValueTypeRenderingType> renderingTypes )
    {
        this.renderingTypes = renderingTypes;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public boolean isHasOptionSet()
    {
        return hasOptionSet;
    }

    public void setHasOptionSet( boolean hasOptionSet )
    {
        this.hasOptionSet = hasOptionSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Class getClazz()
    {
        return clazz;
    }

    public void setClazz( Class clazz )
    {
        this.clazz = clazz;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        ObjectValueTypeRenderingOption that = (ObjectValueTypeRenderingOption) o;
        return hasOptionSet == that.hasOptionSet &&
            Objects.equals( clazz, that.clazz ) &&
            valueType == that.valueType;
    }

    @Override
    public int hashCode()
    {

        return Objects.hash( clazz, valueType, hasOptionSet );
    }
}
