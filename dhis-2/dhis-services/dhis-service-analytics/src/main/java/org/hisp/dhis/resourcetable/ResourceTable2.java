package org.hisp.dhis.resourcetable;

import static org.hisp.dhis.db.model.Table.toStaging;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.db.model.Table;

public interface ResourceTable2
{
    Table getTable();

    ResourceTableType getTableType();

    Optional<String> getPopulateTempTableStatement();

    Optional<List<Object[]>> getPopulateTempTableContent();

    default String getStagingTableName()
    {
        return toStaging( getTableType().getTableName() );
    }

}
