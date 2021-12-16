/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.events.importer.context;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.util.SystemUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractSupplier
{
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    protected final Environment environment;

    public AbstractSupplier( NamedParameterJdbcTemplate jdbcTemplate, Environment environment )
    {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    public <T> T jsonToEntity( String json, Class<T> clazz )
    {
        if ( StringUtils.isEmpty( json ) )
            return null;

        try
        {
            return JacksonObjectMapperConfig.staticJsonMapper().readValue( json, clazz );
        }
        catch ( JsonProcessingException e )
        {
            log.error( e.getMessage() );
        }

        return null;
    }

    public Geometry getGeometryFrom( String field, ResultSet rs, String entity )
        throws SQLException
    {
        if ( null != rs.getObject( field ) )
        {
            try
            {
                return new WKTReader().read( rs.getString( field ) );
            }
            catch ( ParseException e )
            {
                log.error( "Unable to read geometry for '" + entity + "': ", e );
            }
        }

        return null;
    }

    public String getGeometryField( String field )
    {
        return SystemUtils.isH2( environment.getActiveProfiles() ) ? field : "ST_AsText( " + field + " )";
    }
}
