package org.hisp.dhis.jdbc.batchhandler;

import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

/**
 * @author Lars Helge Overland
 */
public class DataValueAuditBatchHandler
    extends AbstractBatchHandler<DataValueAudit>
{
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
 
    public DataValueAuditBatchHandler( JdbcConfiguration config )
    {
        super( config );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getTableName()
    {
        return "datavalueaudit";
    }

    @Override
    public String getAutoIncrementColumn()
    {
        return "datavalueauditid";
    }

    @Override
    public boolean isInclusiveUniqueColumns()
    {
        return true;
    }
    
    @Override
    public List<String> getIdentifierColumns()
    {
        return getStringList( "datavalueauditid" );
    }

    @Override
    public List<Object> getIdentifierValues( DataValueAudit dataValueAudit )
    {        
        return getObjectList( dataValueAudit.getId() );
    }

    @Override
    public List<String> getUniqueColumns()
    {
        return getStringList();
    }
    
    @Override
    public List<Object> getUniqueValues( DataValueAudit dataValueAudit )
    {
        return getObjectList();
    }
    
    @Override
    public List<String> getColumns()
    {
        return getStringList( 
            "dataelementid", 
            "periodid", 
            "organisationunitid", 
            "categoryoptioncomboid", 
            "attributeoptioncomboid", 
            "value", 
            "modifiedby", 
            "created", 
            "audittype" );
    }

    @Override
    public List<Object> getValues( DataValueAudit dataValueAudit )
    {
        return getObjectList( 
            dataValueAudit.getDataElement().getId(),
            dataValueAudit.getPeriod().getId(),
            dataValueAudit.getOrganisationUnit().getId(),
            dataValueAudit.getCategoryOptionCombo().getId(),
            dataValueAudit.getAttributeOptionCombo().getId(),
            dataValueAudit.getValue(),
            dataValueAudit.getModifiedBy(),
            getLongDateString( dataValueAudit.getCreated() ),
            dataValueAudit.getAuditType().toString() );
    }

    @Override
    public DataValueAudit mapRow( ResultSet resultSet )
        throws SQLException
    {
        DataValueAudit dva = new DataValueAudit();
        
        dva.setValue( resultSet.getString( "value" ) );
        dva.setModifiedBy( resultSet.getString( "modifiedby" ) );
        dva.setCreated( resultSet.getDate( "created" ) );
        dva.setAuditType( AuditType.valueOf( resultSet.getString( "audittype" ) ) );
        
        return dva;
    }
}
