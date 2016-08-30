package org.hisp.dhis.jdbc.batchhandler;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.amplecode.quick.JdbcConfiguration;
import org.amplecode.quick.batchhandler.AbstractBatchHandler;
import org.hisp.dhis.datavalue.DataValueAudit;

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
        super( config, false );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected void setTableName()
    {
        statementBuilder.setTableName( "datavalueaudit" );
    }

    @Override
    protected void setAutoIncrementColumn()
    {
        statementBuilder.setAutoIncrementColumn( "datavalueauditid" );
    }

    @Override
    protected void setIdentifierColumns()
    {
        statementBuilder.setIdentifierColumn( "datavalueauditid" );
    }

    @Override
    protected void setIdentifierValues( DataValueAudit dataValueAudit )
    {        
        statementBuilder.setIdentifierValue( dataValueAudit.getId() );
    }

    @Override
    protected void setUniqueColumns()
    {
    }
    
    @Override
    protected void setUniqueValues( DataValueAudit dataValueAudit )
    {
    }
    
    @Override
    protected void setColumns()
    {
        statementBuilder.setColumn( "dataelementid" );
        statementBuilder.setColumn( "periodid" );
        statementBuilder.setColumn( "organisationunitid" );
        statementBuilder.setColumn( "categoryoptioncomboid" );
        statementBuilder.setColumn( "attributeoptioncomboid" );
        statementBuilder.setColumn( "value" );
        statementBuilder.setColumn( "modifiedby" );
        statementBuilder.setColumn( "created" );
        statementBuilder.setColumn( "audittype" );
    }

    @Override
    protected void setValues( DataValueAudit dataValueAudit )
    {
        statementBuilder.setValue( dataValueAudit.getDataElement().getId() );
        statementBuilder.setValue( dataValueAudit.getPeriod().getId() );
        statementBuilder.setValue( dataValueAudit.getOrganisationUnit().getId() );
        statementBuilder.setValue( dataValueAudit.getCategoryOptionCombo().getId() );
        statementBuilder.setValue( dataValueAudit.getAttributeOptionCombo().getId() );
        statementBuilder.setValue( dataValueAudit.getValue() );
        statementBuilder.setValue( dataValueAudit.getModifiedBy() );
        statementBuilder.setValue( dataValueAudit.getCreated() );
        statementBuilder.setValue( dataValueAudit.getAuditType() );
    }
}
