package org.hisp.dhis.jdbc.batchhandler;

import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hisp.dhis.datavalue.DataValue;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

/**
 * @author Lars Helge Overland
 */
public class DataValueBatchHandler
    extends AbstractBatchHandler<DataValue>
{
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
 
    public DataValueBatchHandler( JdbcConfiguration config )
    {
        super( config );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getTableName()
    {
        return "datavalue";
    }

    @Override
    public String getAutoIncrementColumn()
    {
        return null;
    }

    @Override
    public boolean isInclusiveUniqueColumns()
    {
        return true;
    }
    
    @Override
    public List<String> getIdentifierColumns()
    {
        return getStringList(
            "dataelementid",
            "periodid",
            "sourceid",
            "categoryoptioncomboid",
            "attributeoptioncomboid" );
    }

    @Override
    public List<Object> getIdentifierValues( DataValue value )
    {
        return getObjectList(
            value.getDataElement().getId(),
            value.getPeriod().getId(),
            value.getSource().getId(),
            value.getCategoryOptionCombo().getId(),
            value.getAttributeOptionCombo().getId() );
    }
    
    @Override
    public List<String> getUniqueColumns()
    {
        return getStringList(
            "dataelementid",
            "periodid",
            "sourceid",
            "categoryoptioncomboid",
            "attributeoptioncomboid" );
    }
    
    @Override
    public List<Object> getUniqueValues( DataValue value )
    {        
        return getObjectList(
            value.getDataElement().getId(),
            value.getPeriod().getId(),
            value.getSource().getId(),
            value.getCategoryOptionCombo().getId(),
            value.getAttributeOptionCombo().getId() );
    }
    
    @Override
    public List<String> getColumns()
    {
        return getStringList(
            "dataelementid",
            "periodid",
            "sourceid",
            "categoryoptioncomboid",
            "attributeoptioncomboid",
            "value",
            "storedby",
            "created",
            "lastupdated",
            "comment",
            "followup",
            "deleted" );
    }
    
    @Override
    public List<Object> getValues( DataValue value )
    {        
        return getObjectList(
            value.getDataElement().getId(),
            value.getPeriod().getId(),
            value.getSource().getId(),
            value.getCategoryOptionCombo().getId(),
            value.getAttributeOptionCombo().getId(),
            value.getValue(),
            value.getStoredBy(),
            getLongDateString( value.getCreated() ),
            getLongDateString( value.getLastUpdated() ),
            value.getComment(),
            value.isFollowup(),
            value.isDeleted() );
    }

    @Override
    public DataValue mapRow( ResultSet resultSet )
        throws SQLException
    {
        DataValue dv = new DataValue();
        
        dv.setValue( resultSet.getString( "value" ) );
        dv.setStoredBy( resultSet.getString( "storedBy" ) );
        dv.setComment( resultSet.getString( "comment" ) );
        dv.setFollowup( resultSet.getBoolean( "followup" ) );
        dv.setDeleted( resultSet.getBoolean( "deleted" ) );
        
        return dv;
    }
}
