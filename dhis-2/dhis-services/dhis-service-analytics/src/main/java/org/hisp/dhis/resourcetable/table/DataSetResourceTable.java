package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.Table.toStaging;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.resourcetable.ResourceTable;
import org.hisp.dhis.resourcetable.ResourceTableType;
import lombok.RequiredArgsConstructor;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
public class DataSetResourceTable implements ResourceTable {

  public static final String TABLE_NAME = "analytics_rs_dataset";
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
    return List.of();
  }

  private List<String> getPrimaryKey() {
    return List.of("datasetid");
  }

  @Override
  public ResourceTableType getTableType() {
    return ResourceTableType.DATA_SET;
  }

  @Override
  public Optional<String> getPopulateTempTableStatement() {
    String sql =
        replace(
            """
                """,
            "tableName",
            toStaging(TABLE_NAME));

    return Optional.of(sql);
  }

  @Override
  public Optional<List<Object[]>> getPopulateTempTableContent() {
    return Optional.empty();
  }
}
