package org.hisp.dhis.resourcetable.table;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;

import java.util.List;
import java.util.Optional;

import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;

@RequiredArgsConstructor
public class DataElementOptionResourceTable implements ResourceTable {
    public static final String TABLE_NAME = "analytics_rs_dataelementoption";

    private final Logged logged;
    @Override
    public Table getTable() {
        return new Table(toStaging(TABLE_NAME), getColumns(), getPrimaryKey(), logged);
    }

    @Override
    public Table getMainTable() {
        return new Table(TABLE_NAME, getColumns(), getPrimaryKey(), logged);
    }

    private List<Column> getColumns() {
        return List.of(
                new Column("dataelementid", DataType.BIGINT, Nullable.NOT_NULL),
                new Column("optionsetid", DataType.BIGINT, Nullable.NOT_NULL),
                new Column("optionvalueid", DataType.BIGINT, Nullable.NOT_NULL),
                new Column("optionsetuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
                new Column("optionvalueuid", DataType.CHARACTER_11, Nullable.NOT_NULL));
    }

    private List<String> getPrimaryKey() {
        return List.of("dataelementid", "optionsetid", "optionvalueid");
    }

    @Override
    public List<Index> getIndexes() {
        return List.of(
                Index.builder()
                        .name(appendRandom("in_optionsetoptionvalue"))
                        .tableName(toStaging(TABLE_NAME))
                        .columns(List.of("optionsetuid", "optionvalueuid"))
                        .build());
    }

    @Override
    public ResourceTableType getTableType() {
        return ResourceTableType.DATA_ELEMENT_CATEGORY_OPTION_COMBO;
    }

    @Override
    public Optional<String> getPopulateTempTableStatement() {
        String sql =
                replace(
                        """
                        insert into ${tableName} \
                        (dataelementid, optionsetid, optionvalueid, optionsetuid, optionvalueuid) \
                        select de.dataelementid, os.optionsetid as optionsetid, ov.optionvalueid as optionvalueid, \
                        os.uid as optionsetuid, ov.uid as optionvalueuid from optionvalue ov \
                        inner join optionset os on ov.optionsetid = os.optionsetid \
                        inner join dataelement de on os.optionsetid = de.optionsetid;""",
                        "tableName",
                        toStaging(TABLE_NAME));

        return Optional.of(sql);
    }

    @Override
    public Optional<List<Object[]>> getPopulateTempTableContent() {
        return Optional.empty();
    }
}
