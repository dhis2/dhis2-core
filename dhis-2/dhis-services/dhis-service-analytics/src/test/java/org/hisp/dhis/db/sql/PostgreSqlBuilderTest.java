package org.hisp.dhis.db.sql;

import java.util.List;

import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.DataType;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.model.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgreSqlBuilderTest
{
    private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

    private Table tableA;

    @BeforeEach
    public void beforeEach()
    {
        List<Column> columnsA = List.of(
            new Column( "id", DataType.BIGINT ),
            new Column( "data", DataType.CHARACTER_11 ),
            new Column( "period", DataType.VARCHAR_50 ),
            new Column( "created", DataType.TIMESTAMP ),
            new Column( "value", DataType.DOUBLE ) );

        List<String> primaryKeyA = List.of( "id" );

        List<Index> indexesA = List.of(
            new Index( "in_immunization_data", List.of( "data" ) ),
            new Index( "in_immunization_peroid", List.of( "period" ) ) );

        tableA = new Table( "immunization", columnsA, primaryKeyA, indexesA, Logged.LOGGED );
    }

    @Test
    void testCreateTable()
    {
        String expected = """

            """;
    }

}
