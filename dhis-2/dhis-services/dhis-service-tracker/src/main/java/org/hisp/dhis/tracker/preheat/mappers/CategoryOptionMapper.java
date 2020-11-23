package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.category.CategoryOption;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface CategoryOptionMapper extends PreheatMapper<CategoryOption>
{
    CategoryOptionMapper INSTANCE = Mappers.getMapper( CategoryOptionMapper.class );

    CategoryOption map( CategoryOption categoryOption );
}
