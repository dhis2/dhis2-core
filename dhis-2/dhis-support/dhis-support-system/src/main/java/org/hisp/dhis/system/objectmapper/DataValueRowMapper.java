package org.hisp.dhis.system.objectmapper;

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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.amplecode.quick.mapper.RowMapper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataValueRowMapper
    implements RowMapper<DataValue>, org.springframework.jdbc.core.RowMapper<DataValue>
{
    @Override
    public DataValue mapRow( ResultSet resultSet )
        throws SQLException
    {
        final DataValue dataValue = new DataValue();
        
        dataValue.setDataElement( new DataElement() );
        dataValue.setCategoryOptionCombo( new DataElementCategoryOptionCombo() );
        dataValue.setAttributeOptionCombo( new DataElementCategoryOptionCombo() );
        dataValue.setSource( new OrganisationUnit() );
        dataValue.setPeriod( new Period() );

        dataValue.getDataElement().setId( resultSet.getInt( "dataelementid" ) );
        dataValue.getPeriod().setId( resultSet.getInt( "periodid" ) );
        dataValue.getSource().setId( resultSet.getInt( "sourceid" ) );
        dataValue.getCategoryOptionCombo().setId( resultSet.getInt( "categoryoptioncomboid" ) );
        dataValue.setValue( resultSet.getString( "value" ) );
        dataValue.setStoredBy( resultSet.getString( "storedby" ) );
        dataValue.setCreated( resultSet.getDate( "created" ) );
        dataValue.setLastUpdated( resultSet.getDate( "lastupdated" ) );
        dataValue.setComment( resultSet.getString( "comment" ) );

        return dataValue;
    }

    @Override
    public DataValue mapRow( ResultSet resultSet, int rowNum )
        throws SQLException
    {
        return mapRow( resultSet );
    }
}
