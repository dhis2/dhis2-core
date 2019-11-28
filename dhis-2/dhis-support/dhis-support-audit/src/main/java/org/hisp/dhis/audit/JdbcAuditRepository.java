package org.hisp.dhis.audit;

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

import org.hisp.dhis.commons.util.SqlHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Repository
public class JdbcAuditRepository implements AuditRepository
{
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert auditInsert;

    public JdbcAuditRepository( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
        this.auditInsert = new SimpleJdbcInsert( jdbcTemplate )
            .withTableName( "audit" )
            .usingGeneratedKeyColumns( "auditid" );
    }

    @Override
    public long save( Audit audit )
    {
        Map<String, Object> values = new HashMap<>();
        values.put( "auditType", audit.getAuditType() );
        values.put( "auditScope", audit.getAuditScope() );
        values.put( "createdAt", audit.getCreatedAt() );
        values.put( "createdBy", audit.getCreatedBy() );
        values.put( "klass", audit.getKlass() );
        values.put( "uid", audit.getUid() );
        values.put( "code", audit.getCode() );
        values.put( "data", audit.getData() );

        return auditInsert.executeAndReturnKey( values ).longValue();
    }

    @Override
    public void save( List<Audit> audits )
    {

    }

    @Override
    public void delete( Audit audit )
    {
        jdbcTemplate.update( "DELETE FROM audit WHERE auditId=?", audit.getId() );
    }

    @Override
    public void delete( AuditQuery query )
    {

    }

    @Override
    public int count( AuditQuery query )
    {
        return 0;
    }

    @Override
    public List<Audit> query( AuditQuery query )
    {
        String sql = buildQuery( query );
        return jdbcTemplate.query( "SELECT * FROM audit" + sql, auditRowMapper );
    }

    private String buildQuery( AuditQuery query )
    {
        StringBuilder sqlBuilder = new StringBuilder();
        SqlHelper sqlHelper = new SqlHelper( true );

        if ( !query.getKlass().isEmpty() )
        {
            sqlBuilder.append( sqlHelper.whereAnd() )
                .append( "klass in (" ).append( buildQuotedSet( query.getKlass() ) ).append( ")" );
        }

        if ( !query.getUid().isEmpty() || !query.getCode().isEmpty() )
        {
            sqlBuilder.append( sqlHelper.whereAnd() ).append( "(" );
            SqlHelper idHelper = new SqlHelper( true );

            if ( !query.getUid().isEmpty() )
            {
                sqlBuilder.append( idHelper.or() )
                    .append( "uid in (" ).append( buildQuotedSet( query.getUid() ) ).append( ")" );
            }

            if ( !query.getCode().isEmpty() )
            {
                sqlBuilder.append( idHelper.or() )
                    .append( "code in (" ).append( buildQuotedSet( query.getCode() ) ).append( ")" );
            }

            sqlBuilder.append( ")" );
        }

        return sqlBuilder.toString();
    }

    private String buildQuotedSet( Set<String> items )
    {
        return items.stream()
            .map( s -> "'" + s + "'" )
            .collect( Collectors.joining( ", " ) );
    }

    private RowMapper<Audit> auditRowMapper = ( rs, rowNum ) -> {
        Date createdAt = rs.getDate( "createdAt" );

        return Audit.builder()
            .id( rs.getLong( "auditId" ) )
            .auditType( AuditType.valueOf( rs.getString( "auditType" ) ) )
            .auditScope( AuditScope.valueOf( rs.getString( "auditScope" ) ) )
            .createdAt( new Timestamp( createdAt.getTime() ).toLocalDateTime() )
            .createdBy( rs.getString( "createdBy" ) )
            .klass( rs.getString( "klass" ) )
            .uid( rs.getString( "uid" ) )
            .code( rs.getString( "code" ) )
            .data( rs.getString( "data" ) )
            .build();
    };
}
