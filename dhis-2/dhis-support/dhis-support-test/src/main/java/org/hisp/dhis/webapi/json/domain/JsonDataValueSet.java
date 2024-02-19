package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;

public interface JsonDataValueSet extends JsonObject
{
    default String getDataSet()
    {
        return getString( "dataSet" ).string();
    }

    default String getCompleteDate()
    {
        return getString( "completeDate" ).string();
    }

    default String getPeriod()
    {
        return getString( "period" ).string();
    }

    default String getOrgUnit()
    {
        return getString( "orgUnit" ).string();
    }
    default String getAttributeOptionCombo()
    {
        return getString( "attributeOptionCombo" ).string();
    }

    default JsonList<JsonDataValue> getDataValues()
    {
        return get( "dataValues" ).asList( JsonDataValue.class );
    }
}
