package org.hisp.dhis.analytics;

import org.hisp.dhis.common.ValueType;

public enum AnalyticsDataType
{
    NUMERIC, 
    BOOLEAN,
    TEXT;
    
    public static AnalyticsDataType fromValueType( ValueType valueType )
    {
        if ( ValueType.NUMERIC_TYPES.contains( valueType ) )
        {
            return AnalyticsDataType.NUMERIC;
        }
        else if ( ValueType.BOOLEAN_TYPES.contains( valueType ) )
        {
            return AnalyticsDataType.BOOLEAN;
        }
        else
        {        
            return AnalyticsDataType.TEXT;
        }
    }
}
