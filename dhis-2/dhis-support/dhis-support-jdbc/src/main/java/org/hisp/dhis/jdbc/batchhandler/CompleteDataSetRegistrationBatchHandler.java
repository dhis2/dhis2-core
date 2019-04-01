package org.hisp.dhis.jdbc.batchhandler;

import static org.hisp.dhis.util.DateUtils.getLongDateString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

/**
 * @author Lars Helge Overland
 */
public class CompleteDataSetRegistrationBatchHandler
    extends AbstractBatchHandler<CompleteDataSetRegistration>
{
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public CompleteDataSetRegistrationBatchHandler( JdbcConfiguration config )
    {
        super( config );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getTableName()
    {
        return "completedatasetregistration";
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
            "datasetid",
            "periodid",
            "sourceid",
            "attributeoptioncomboid" );
    }

    @Override
    public List<Object> getIdentifierValues( CompleteDataSetRegistration registration )
    {
        return getObjectList(
            registration.getDataSet().getId(),
            registration.getPeriod().getId(),
            registration.getSource().getId(),
            registration.getAttributeOptionCombo().getId() );
    }

    @Override
    public List<String> getUniqueColumns()
    {
        return getStringList(
            "datasetid",
            "periodid",
            "sourceid",
            "attributeoptioncomboid" );
    }

    @Override
    public List<Object> getUniqueValues( CompleteDataSetRegistration registration )
    {
        return getObjectList(
            registration.getDataSet().getId(),
            registration.getPeriod().getId(),
            registration.getSource().getId(),
            registration.getAttributeOptionCombo().getId() );
    }

    @Override
    public List<String> getColumns()
    {
        return getStringList(
            "datasetid",
            "periodid",
            "sourceid",
            "attributeoptioncomboid",
            "date",
            "storedby",
            "lastupdatedby",
            "lastupdated",
            "completed" );
    }

    @Override
    public List<Object> getValues( CompleteDataSetRegistration registration )
    {
        return getObjectList(
            registration.getDataSet().getId(),
            registration.getPeriod().getId(),
            registration.getSource().getId(),
            registration.getAttributeOptionCombo().getId(),
            getLongDateString( registration.getDate() ),
            registration.getStoredBy(),
            registration.getLastUpdatedBy(),
            getLongDateString( registration.getLastUpdated() ),
            registration.getCompleted() );
    }

    @Override
    public CompleteDataSetRegistration mapRow( ResultSet resultSet )
        throws SQLException
    {
        CompleteDataSetRegistration cdr = new CompleteDataSetRegistration();

        cdr.setStoredBy( resultSet.getString( "storedby" ) );

        return cdr;
    }
}
