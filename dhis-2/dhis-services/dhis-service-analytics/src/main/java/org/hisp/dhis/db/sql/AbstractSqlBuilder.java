package org.hisp.dhis.db.sql;

import org.hisp.dhis.db.model.DataType;

public abstract class AbstractSqlBuilder
    implements SqlBuilder
{
    protected String getDataTypeName( DataType dataType )
    {
        switch ( dataType )
        {
        case SMALLINT:
            return typeSmallInt();
        case INTEGER:
            return typeInteger();
        case BIGINT:
            return typeBigInt();
        case NUMERIC:
            return typeNumeric();
        case REAL:
            return typeReal();
        case DOUBLE:
            return typeDouble();
        case BOOLEAN:
            return typeBoolean();
        case CHARACTER_11:
            return typeCharacter( 11 );
        case CHARACTER_32:
            return typeCharacter( 32 );
        case VARCHAR_50:
            return typeVarchar( 50 );
        case VARCHAR_255:
            return typeVarchar( 255 );
        case VARCHAR_1200:
            return typeVarchar( 1200 );
        case TEXT:
            return typeText();
        case DATE:
            return typeDate();
        case TIMESTAMP:
            return typeTimestamp();
        case TIMESTAMPTZ:
            return typeTimestampTz();
        case TIME:
            return typeTime();
        case TIMETZ:
            return typeTimeTz();
        case GEOMETRY:
            return typeGeometry();
        case GEOMETRY_POINT:
            return typeGeometryPoint();
        case JSONB:
            return typeJsonb();
        default:
            throw new UnsupportedOperationException(
                String.format( "Unsuported data type: %s", dataType ) );
        }
    }
}
