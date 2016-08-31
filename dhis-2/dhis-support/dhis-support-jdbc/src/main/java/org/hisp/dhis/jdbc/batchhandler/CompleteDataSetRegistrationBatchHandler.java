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
import org.hisp.dhis.dataset.CompleteDataSetRegistration;

import static org.hisp.dhis.system.util.DateUtils.getLongDateString;

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
        super( config, true );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected void setTableName()
    {
        statementBuilder.setTableName( "completedatasetregistration" );
    }

    @Override
    protected void setIdentifierColumns()
    {
        statementBuilder.setIdentifierColumn( "datasetid" );
        statementBuilder.setIdentifierColumn( "periodid" );
        statementBuilder.setIdentifierColumn( "sourceid" );
        statementBuilder.setIdentifierColumn( "attributeoptioncomboid" );
    }
    
    @Override
    protected void setIdentifierValues( CompleteDataSetRegistration registration )
    {        
        statementBuilder.setIdentifierValue( registration.getDataSet().getId() );
        statementBuilder.setIdentifierValue( registration.getPeriod().getId() );
        statementBuilder.setIdentifierValue( registration.getSource().getId() );
        statementBuilder.setIdentifierValue( registration.getAttributeOptionCombo().getId() );
    }

    @Override
    protected void setUniqueColumns()
    {
        statementBuilder.setUniqueColumn( "datasetid" );
        statementBuilder.setUniqueColumn( "periodid" );
        statementBuilder.setUniqueColumn( "sourceid" );
        statementBuilder.setIdentifierColumn( "attributeoptioncomboid" );
    }
    
    @Override
    protected void setUniqueValues( CompleteDataSetRegistration registration )
    {        
        statementBuilder.setUniqueValue( registration.getDataSet().getId() );
        statementBuilder.setUniqueValue( registration.getPeriod().getId() );
        statementBuilder.setUniqueValue( registration.getSource().getId() );
        statementBuilder.setUniqueValue( registration.getAttributeOptionCombo().getId() );
    }

    @Override
    protected void setColumns()
    {
        statementBuilder.setColumn( "datasetid" );
        statementBuilder.setColumn( "periodid" );
        statementBuilder.setColumn( "sourceid" );
        statementBuilder.setColumn( "attributeoptioncomboid" );
        statementBuilder.setColumn( "date" );
        statementBuilder.setColumn( "storedby" );
    }
    
    @Override
    public void setValues( CompleteDataSetRegistration registration )
    {
        statementBuilder.setValue( registration.getDataSet().getId() );
        statementBuilder.setValue( registration.getPeriod().getId() );
        statementBuilder.setValue( registration.getSource().getId() );
        statementBuilder.setValue( registration.getAttributeOptionCombo().getId() );
        statementBuilder.setValue( getLongDateString( registration.getDate() ) );
        statementBuilder.setValue( registration.getStoredBy() );
    }
}
