package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface CategoryOptionComboMapper extends PreheatMapper<CategoryOptionCombo>
{
    CategoryOptionComboMapper INSTANCE = Mappers.getMapper( CategoryOptionComboMapper.class );

    CategoryOptionCombo map( CategoryOptionCombo categoryOptionCombo );
}
