package org.hisp.dhis.jdbc.batchhandler;

/*
 * Copyright (c) 2004-2017, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.hisp.dhis.system.util.DateUtils.getLongDateString;

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
