package org.hisp.dhis.tracker.preheat.mappers;

import org.hisp.dhis.dataelement.DataElement;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper( uses = DebugMapper.class )
public interface DataElementMapper extends PreheatMapper<DataElement>
{
    DataElementMapper INSTANCE = Mappers.getMapper( DataElementMapper.class );

    @BeanMapping( ignoreByDefault = true )
    @Mapping( target = "id" )
    @Mapping( target = "uid" )
    @Mapping( target = "code" )
    @Mapping( target = "valueType" )
    DataElement map( DataElement dataElement );
}
