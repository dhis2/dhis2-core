package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.jsontree.JsonObject;

public interface JsonDataValue extends JsonObject
{
    default String getDataElement()
    {
        return getString( "dataElement" ).string();
    }

    default String getPeriod()
    {
        return getString( "period" ).string();
    }

    default String getOrgUnit()
    {
        return getString( "orgUnit" ).string();
    }

    default String getCategoryOptionCombo()
    {
        return getString( "categoryOptionCombo" ).string();
    }

    default String getAttributeOptionCombo()
    {
        return getString( "attributeOptionCombo" ).string();
    }

    default String getValue()
    {
        return getString( "value" ).string();
    }

    default String getStoredBy()
    {
        return getString( "storedBy" ).string();
    }

    default String getComment()
    {
        return getString( "comment" ).string();
    }

    default String getFollowUp()
    {
        return getString( "followUp" ).string();
    }
}
