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
import org.hisp.dhis.minmax.MinMaxDataElement;

/**
 * @author Lars Helge Overland
 */
public class MinMaxDataElementBatchHandler
    extends AbstractBatchHandler<MinMaxDataElement>
{
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------
 
    public MinMaxDataElementBatchHandler( JdbcConfiguration config )
    {
        super( config, true );
    }

    @Override
    protected void setTableName()
    {
        statementBuilder.setTableName( "minmaxdataelement" );
    }

    @Override
    protected void setAutoIncrementColumn()
    {
        statementBuilder.setAutoIncrementColumn( "minmaxdataelementid" );
    }

    @Override
    protected void setIdentifierColumns()
    {
        statementBuilder.setIdentifierColumn( "minmaxdataelementid" );
    }

    @Override
    protected void setIdentifierValues( MinMaxDataElement dataElement )
    {        
        statementBuilder.setIdentifierValue( dataElement.getId() );
    }

    @Override
    protected void setUniqueColumns()
    {
        statementBuilder.setUniqueColumn( "sourceid" );
        statementBuilder.setUniqueColumn( "dataelementid" );
        statementBuilder.setUniqueColumn( "categoryoptioncomboid" );
    }

    @Override
    protected void setUniqueValues( MinMaxDataElement dataElement )
    {
        statementBuilder.setUniqueValue( dataElement.getSource().getId() );
        statementBuilder.setUniqueValue( dataElement.getDataElement().getId() );
        statementBuilder.setUniqueValue( dataElement.getOptionCombo().getId() );        
    }

    @Override
    protected void setColumns()
    {
        statementBuilder.setColumn( "sourceid" );
        statementBuilder.setColumn( "dataelementid" );
        statementBuilder.setColumn( "categoryoptioncomboid" );
        statementBuilder.setColumn( "minimumvalue" );
        statementBuilder.setColumn( "maximumvalue" );
        statementBuilder.setColumn( "generatedvalue" );
    }

    @Override
    protected void setValues( MinMaxDataElement dataElement )
    {
        statementBuilder.setValue( dataElement.getSource().getId() );
        statementBuilder.setValue( dataElement.getDataElement().getId() );
        statementBuilder.setValue( dataElement.getOptionCombo().getId() );
        statementBuilder.setValue( dataElement.getMin() );
        statementBuilder.setValue( dataElement.getMax() );
        statementBuilder.setValue( dataElement.isGenerated() );
    }
}
