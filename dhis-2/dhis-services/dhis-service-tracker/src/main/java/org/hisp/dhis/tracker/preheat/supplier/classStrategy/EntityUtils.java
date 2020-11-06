package org.hisp.dhis.tracker.preheat.supplier.classStrategy;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;

/**
 * @author Luciano Fiandesio
 */
public class EntityUtils
{

    public static void addNewEntitiesToPreheat( List<String> persisted, List<String> persistable, TrackerPreheat preheat,
                                               Class<? extends IdentifiableObject> clazz )
    {
        if ( persisted.size() != persistable.size() ) // DELTA
        {
            List<String> list = new ArrayList<>( CollectionUtils.disjunction( persistable, persisted ) );
            for ( String uid : list )
            {
                preheat.addUnpersisted( clazz, uid );
            }
        }
    }
}
