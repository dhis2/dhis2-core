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

import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;

import static org.hisp.dhis.system.util.DateUtils.getLongDateString;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
            "storedby" );
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
            registration.getStoredBy() );
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
