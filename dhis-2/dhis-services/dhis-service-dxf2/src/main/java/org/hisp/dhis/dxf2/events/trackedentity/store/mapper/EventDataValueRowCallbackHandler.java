/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.dxf2.events.trackedentity.store.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.util.DateUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.google.gson.Gson;

/**
 * @author Luciano Fiandesio
 */
public class EventDataValueRowCallbackHandler implements
        RowCallbackHandler

{
    private Map<String, List<DataValue>> dataValues;

    public EventDataValueRowCallbackHandler()
    {
        this.dataValues = new HashMap<>();
    }

    @Override
    public void processRow( ResultSet rs )
        throws SQLException
    {
        dataValues.put( rs.getString( "key" ), getDataValue( rs ) );
    }

    private List<DataValue> getDataValue( ResultSet rs )
        throws SQLException
    {
        // TODO not sure this is the most efficient way to handle JSONB -> java
        List<DataValue> dataValues = new ArrayList<>();
        // PGobject
        PGobject values = (PGobject) rs.getObject( "eventdatavalues" );
        Gson gson = new Gson();
        Map json = gson.fromJson( values.getValue(), Map.class  );

        for ( Object o : json.keySet() )
        {
            Map m = (Map) json.get( o );
            DataValue value = new DataValue();
            System.out.println( "created: " + m.get( "created" ) );
            System.out.println( "lastUpdated: " + m.get( "lastUpdated" ) ); // TODO
            // value.setCreated( DateUtils.getIso8601NoTz( (Date) m.get( "created" ) ) );
            // value.setLastUpdated( DateUtils.getIso8601NoTz( (Date) m.get( "lastUpdated" )
            // ) );
            value.setValue( (String) m.get( "value" ) );
            value.setStoredBy( (String) m.get( "storedBy" ) );
            value.setDataElement( (String) m.get( "dataElement" ) );
            value.setProvidedElsewhere( (Boolean) m.get( "providedElsewhere" ) );
            dataValues.add( value );
        }

        return dataValues;
    }

    public Map<String, List<DataValue>> getItems()
    {
        return this.dataValues;
    }
}
