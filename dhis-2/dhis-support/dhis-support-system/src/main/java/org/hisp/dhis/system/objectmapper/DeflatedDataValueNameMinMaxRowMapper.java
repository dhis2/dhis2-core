/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.system.objectmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.quick.mapper.RowMapper;

/**
 * RowMapper which expects a result set with the following columns:
 *
 * <ul>
 * <li>1: dataelementid - int</li>
 * <li>2: periodid - int</li>
 * <li>3: sourceid - int</li>
 * <li>4: categoryoptioncomboid - int</li>
 * <li>5: categoryoptioncomboid - int</li>
 * <li>6: value - String</li>
 * <li>7: storedby - String</li>
 * <li>8: lastupdated - date</li>
 * <li>9<: comment - String</li>
 * <li>10: followup - Boolean</li>
 * <li>11: minimumvalue - int</li>
 * <li>12: maximumvalue - int</li>
 * <li>13: dataelementname - String</li>
 * <li>14: periodtypename - String</li>
 * <li>15: startdate - String</li>
 * <li>16: enddate - String</li>
 * <li>17: sourcename - String</li>
 * <li>18: categoryoptioncomboname - String</li>
 * </ul>
 *
 * @author Lars Helge Overland
 */
public class DeflatedDataValueNameMinMaxRowMapper
    implements RowMapper<DeflatedDataValue>, org.springframework.jdbc.core.RowMapper<DeflatedDataValue>
{
    private Map<Long, Integer> minMap;

    private Map<Long, Integer> maxMap;

    public DeflatedDataValueNameMinMaxRowMapper()
    {
    }

    public DeflatedDataValueNameMinMaxRowMapper( Map<Long, Integer> minMap, Map<Long, Integer> maxMap )
    {
        this.minMap = minMap;
        this.maxMap = maxMap;
    }

    @Override
    public DeflatedDataValue mapRow( ResultSet resultSet )
        throws SQLException
    {
        final DeflatedDataValue value = new DeflatedDataValue();

        value.setDataElementId( resultSet.getLong( "dataelementid" ) );
        value.setPeriodId( resultSet.getLong( "periodid" ) );
        value.setSourceId( resultSet.getLong( "sourceid" ) );
        value.setCategoryOptionComboId( resultSet.getLong( "categoryoptioncomboid" ) );
        value.setAttributeOptionComboId( resultSet.getLong( "attributeoptioncomboid" ) );
        value.setValue( resultSet.getString( "value" ) );
        value.setStoredBy( resultSet.getString( "storedby" ) );
        value.setCreated( resultSet.getDate( "created" ) );
        value.setLastUpdated( resultSet.getDate( "lastupdated" ) );
        value.setComment( resultSet.getString( "comment" ) );
        value.setFollowup( resultSet.getBoolean( "followup" ) );
        value.setMin( minMap != null ? minMap.get( value.getSourceId() ) : resultSet.getInt( "minimumvalue" ) );
        value.setMax( maxMap != null ? maxMap.get( value.getSourceId() ) : resultSet.getInt( "maximumvalue" ) );
        value.setDataElementName( resultSet.getString( "dataelementname" ) );
        value.setPeriod(
            resultSet.getString( "periodtypename" ),
            resultSet.getDate( "startdate" ),
            resultSet.getDate( "enddate" ) );
        value.setSourceName( resultSet.getString( "sourcename" ) );
        value.setCategoryOptionComboName( resultSet.getString( "categoryoptioncomboname" ) );

        return value;
    }

    @Override
    public DeflatedDataValue mapRow( ResultSet resultSet, int rowNum )
        throws SQLException
    {
        return mapRow( resultSet );
    }
}
