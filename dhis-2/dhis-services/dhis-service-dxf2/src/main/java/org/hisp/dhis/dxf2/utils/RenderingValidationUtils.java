package org.hisp.dhis.dxf2.utils;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.configuration.ObjectValueTypeRenderingOption;
import org.hisp.dhis.render.type.ValueTypeRenderingType;

import static org.hisp.dhis.dxf2.configuration.StaticRenderingConfiguration.RENDERING_OPTIONS_MAPPING;

public class RenderingValidationUtils
{
    public static boolean validate( Class clazz, ValueType valueType, boolean optionSet, ValueTypeRenderingType type )
    {
        if ( valueType != null && type.equals( ValueTypeRenderingType.DEFAULT ))
        {
            return true;
        }

        for ( ObjectValueTypeRenderingOption option : RENDERING_OPTIONS_MAPPING )
        {
            if ( option.equals( new ObjectValueTypeRenderingOption( clazz, valueType, optionSet, null ) ) )
            {
                return option.getRenderingTypes().contains( type );
            }
        }

        return false;
    }
}
