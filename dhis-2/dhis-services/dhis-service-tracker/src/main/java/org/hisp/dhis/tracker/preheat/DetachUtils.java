package org.hisp.dhis.tracker.preheat;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.preheat.mappers.PreheatMapper;
import org.mapstruct.factory.Mappers;

/**
 * @author Luciano Fiandesio
 */
public class DetachUtils
{

    public static <T> List<T> detach( PreheatMapper<T> mapper, List<T> objects )
    {
        return objects.stream().map( mapper::map ).collect( Collectors.toList() );
    }

    public static <T> List<T> detach( Class<? extends PreheatMapper> mapperKlass, List<T> objects )
    {
        final PreheatMapper<T> mapper = Mappers.getMapper( mapperKlass );
        return objects.stream().map( mapper::map ).collect( Collectors.toList() );
    }

}
