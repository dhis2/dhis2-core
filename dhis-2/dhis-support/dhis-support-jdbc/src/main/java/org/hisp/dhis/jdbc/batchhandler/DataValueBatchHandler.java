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
import org.hisp.dhis.datavalue.DataValue;

import static org.hisp.dhis.system.util.DateUtils.getLongDateString;

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
        super( config, true );
    }

    // -------------------------------------------------------------------------
    // AbstractBatchHandler implementation
    // -------------------------------------------------------------------------

    @Override
    protected void setTableName()
    {
        statementBuilder.setTableName( "datavalue" );
    }

    @Override
    protected void setIdentifierColumns()
    {
        statementBuilder.setIdentifierColumn( "dataelementid" );
        statementBuilder.setIdentifierColumn( "periodid" );
        statementBuilder.setIdentifierColumn( "sourceid" );
        statementBuilder.setIdentifierColumn( "categoryoptioncomboid" );
        statementBuilder.setIdentifierColumn( "attributeoptioncomboid" );
    }

    @Override
    protected void setIdentifierValues( DataValue value )
    {        
        statementBuilder.setIdentifierValue( value.getDataElement().getId() );
        statementBuilder.setIdentifierValue( value.getPeriod().getId() );
        statementBuilder.setIdentifierValue( value.getSource().getId() );
        statementBuilder.setIdentifierValue( value.getCategoryOptionCombo().getId() );
        statementBuilder.setIdentifierValue( value.getAttributeOptionCombo().getId() );
    }
    
    @Override
    protected void setUniqueColumns()
    {
        statementBuilder.setUniqueColumn( "dataelementid" );
        statementBuilder.setUniqueColumn( "periodid" );
        statementBuilder.setUniqueColumn( "sourceid" );
        statementBuilder.setUniqueColumn( "categoryoptioncomboid" );
        statementBuilder.setUniqueColumn( "attributeoptioncomboid" );
    }
    
    @Override
    protected void setUniqueValues( DataValue value )
    {        
        statementBuilder.setUniqueValue( value.getDataElement().getId() );
        statementBuilder.setUniqueValue( value.getPeriod().getId() );
        statementBuilder.setUniqueValue( value.getSource().getId() );
        statementBuilder.setUniqueValue( value.getCategoryOptionCombo().getId() );
        statementBuilder.setUniqueValue( value.getAttributeOptionCombo().getId() );
    }
    
    @Override
    protected void setColumns()
    {
        statementBuilder.setColumn( "dataelementid" );
        statementBuilder.setColumn( "periodid" );
        statementBuilder.setColumn( "sourceid" );
        statementBuilder.setColumn( "categoryoptioncomboid" );
        statementBuilder.setColumn( "attributeoptioncomboid" );
        statementBuilder.setColumn( "value" );
        statementBuilder.setColumn( "storedby" );
        statementBuilder.setColumn( "created ");
        statementBuilder.setColumn( "lastupdated" );
        statementBuilder.setColumn( "comment" );
        statementBuilder.setColumn( "followup" );
    }
    
    @Override
    protected void setValues( DataValue value )
    {        
        statementBuilder.setValue( value.getDataElement().getId() );
        statementBuilder.setValue( value.getPeriod().getId() );
        statementBuilder.setValue( value.getSource().getId() );
        statementBuilder.setValue( value.getCategoryOptionCombo().getId() );
        statementBuilder.setValue( value.getAttributeOptionCombo().getId() );
        statementBuilder.setValue( value.getValue() );
        statementBuilder.setValue( value.getStoredBy() );
        statementBuilder.setValue( getLongDateString( value.getCreated() ) );
        statementBuilder.setValue( getLongDateString( value.getLastUpdated() ) );
        statementBuilder.setValue( value.getComment() );
        statementBuilder.setValue( value.isFollowup() );
    }
}
