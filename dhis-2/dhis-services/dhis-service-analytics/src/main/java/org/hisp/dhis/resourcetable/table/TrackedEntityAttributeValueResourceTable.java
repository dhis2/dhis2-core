package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.db.model.Table.toStaging;
import static org.hisp.dhis.system.util.SqlUtils.appendRandom;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;
import org.hisp.dhis.db.model.constraint.Unique;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;
import lombok.RequiredArgsConstructor;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class TrackedEntityAttributeValueResourceTable implements ResourceTable {
  public static final String TABLE_NAME = "trackedentityattributevalue";

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
        new Column("dataelementuid", DataType.CHARACTER_11, Nullable.NOT_NULL),
        new Column("dataelementname", DataType.VARCHAR_255, Nullable.NOT_NULL));
  }

  private List<String> getPrimaryKey() {
    return List.of("");
  }

  @Override
  public List<Index> getIndexes() {
    return List.of(
        Index.builder()
            .name(appendRandom("in_dataelementstructure_dataelementuid"))
            .tableName(toStaging(TABLE_NAME))
            .unique(Unique.UNIQUE)
            .columns(List.of("dataelementuid"))
            .build());
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.TRACKED_ENTITTY_ATTRIBUTE_VALUE;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    return Optional.empty();
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
