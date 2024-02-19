package org.hisp.dhis.db.sql;

import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.system.util.SqlUtils.quote;

import org.apache.commons.lang3.Validate;
import org.hisp.dhis.db.model.Column;
import org.hisp.dhis.db.model.Index;
import org.hisp.dhis.db.model.Table;
import org.hisp.dhis.db.model.constraint.Nullable;

public class DorisSqlBuilder
    extends AbstractSqlBuilder
{

    @Override
    public String dataTypeSmallInt()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeInteger()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeBigInt()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeNumeric()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeReal()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeDouble()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeBoolean()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeCharacter( int length )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeVarchar( int length )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeText()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeDate()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeTimestamp()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeTimestampTz()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeTime()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeTimeTz()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeGeometry()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeGeometryPoint()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dataTypeJsonb()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String indexTypeBtree()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String indexTypeGist()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String indexTypeGin()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String indexFunctionUpper()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String indexFunctionLower()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean supportsAnalyze()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsVacuum()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String createTable( Table table )
    {
    	Validate.isTrue(table.hasPrimaryKey()); // ?

        StringBuilder sql =
            new StringBuilder("create table ")
                .append(quote(table.getName()))
                .append(" ");

        // Columns

        if (table.hasColumns()) {
        	sql.append("(");
	        for (Column column : table.getColumns()) {
	          String dataType = getDataTypeName(column.getDataType());
	          String nullable = column.getNullable() == Nullable.NOT_NULL ? " not null" : " null";
	
	          sql.append(quote(column.getName()) + " ")
	              .append(dataType)
	              .append(nullable)
	              .append(", ");
	        }
	        sql.append(") engine=olap");
        }

        // Primary key

        if (table.hasPrimaryKey()) {
          sql.append("duplicate key (");

          for (String columnName : table.getPrimaryKey()) {
            sql.append(quote(columnName) + ", ");
          }

          removeLastComma(sql).append("), ");
        }
        
        // Partitions

        // Checks

        if (table.hasChecks()) {
          for (String check : table.getChecks()) {
            sql.append("check(" + check + "), ");
          }
        }

        removeLastComma(sql).append(")");

        if (table.hasParent()) {
          sql.append(" inherits (").append(quote(table.getParent().getName())).append(")");
        }

        return sql.append(";").toString();
    }

    @Override
    public String analyzeTable( Table table )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String analyzeTable( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String vacuumTable( Table table )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String vacuumTable( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String renameTable( Table table, String newName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dropTableIfExists( Table table )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dropTableIfExists( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dropTableIfExistsCascade( Table table )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String dropTableIfExistsCascade( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String swapTable( Table table, String newName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String setParentTable( Table table, String parentName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String removeParentTable( Table table, String parentName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String swapParentTable( Table table, String parentName, String newParentName )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String tableExists( String name )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createIndex( Index index )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
