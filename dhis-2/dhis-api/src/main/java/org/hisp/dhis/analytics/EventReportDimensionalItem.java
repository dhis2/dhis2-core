package org.hisp.dhis.analytics;

import org.hisp.dhis.common.*;
import org.hisp.dhis.option.Option;

/**
 * @author Henning Håkonsen
 */
public class EventReportDimensionalItem
{
    private String parentUid;

    private Option option;

    private DimensionalItemObject dimensionalItemObject;

    public EventReportDimensionalItem( Option option, String parentUid )
    {
        this.option = option;
        this.parentUid = parentUid;
    }

    EventReportDimensionalItem( DimensionalItemObject dimensionalItemObject, String parentUid )
    {
        this.dimensionalItemObject = dimensionalItemObject;
        this.parentUid = parentUid;
    }

    public Option getOption()
    {
        return option;
    }

    public String getParentUid()
    {
       return parentUid;
    }

    public String getDisplayProperty( DisplayProperty displayProperty )
    {
        if ( option != null )
        {
            return option.getDisplayName();
        }
        else
        {
            if ( displayProperty == DisplayProperty.NAME )
            {
                return dimensionalItemObject.getName();
            }
            else
            {
                return dimensionalItemObject.getShortName();
            }
        }

    }

    @Override
    public String toString()
    {
        if ( option == null )
        {
            return dimensionalItemObject.getDimensionItem();
        }
        else
        {
            return option.getCode();
        }
    }
}
