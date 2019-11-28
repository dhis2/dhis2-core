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

import com.google.common.collect.Sets;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AuditRepositoryTest extends IntegrationTestBase
{
    @Autowired
    private AuditRepository auditRepository;

    @Test
    public void testSaveAudit()
    {
        String uid = CodeGenerator.generateUid();
        String code = CodeGenerator.generateUid();

        Audit audit = Audit.builder()
            .auditType( AuditType.CREATE )
            .auditScope( AuditScope.AGGREGATE )
            .createdAt( LocalDateTime.of( 2019, 1, 1, 0, 0 ) )
            .createdBy( "test-user" )
            .klass( DataElement.class.getName() )
            .uid( uid )
            .code( code )
            .data( "{}" )
            .build();

        auditRepository.save( audit );
        assertEquals( 1, auditRepository.query( AuditQuery.builder().build() ).size() );
    }

    @Test
    public void testDeleteAudit()
    {
        String uid = CodeGenerator.generateUid();
        String code = CodeGenerator.generateUid();

        Audit audit = Audit.builder()
            .auditType( AuditType.CREATE )
            .auditScope( AuditScope.AGGREGATE )
            .createdAt( LocalDateTime.of( 2019, 1, 1, 0, 0 ) )
            .createdBy( "test-user" )
            .klass( DataElement.class.getName() )
            .uid( uid )
            .code( code )
            .data( "{}" )
            .build();

        long id = auditRepository.save( audit );
        assertEquals( 1, id );

        audit.setId( id );
        auditRepository.delete( audit );

        List<Audit> audits = auditRepository.query( AuditQuery.builder().build() );
        assertTrue( audits.isEmpty() );
    }

    @Test
    public void testAuditQueryAll()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder().build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryKlasses()
    {
        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .klass( Sets.newHashSet( DataElement.class.getName(), OrganisationUnit.class.getName() ) )
            .build() );

        assertEquals( 100, audits.size() );
    }

    @Test
    public void testAuditQueryUids()
    {
        List<String> uids = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            uids.add( uid );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .uid( new HashSet<>( uids.subList( 0, 50 ) ) )
            .build() );

        assertEquals( 50, audits.size() );
    }

    @Test
    public void testAuditQueryCodes()
    {
        List<String> codes = new ArrayList<>();

        IntStream.rangeClosed( 1, 100 ).forEach( n -> {
            String uid = CodeGenerator.generateUid();
            String code = CodeGenerator.generateUid();

            codes.add( code );

            Audit audit = Audit.builder()
                .auditType( AuditType.CREATE )
                .auditScope( AuditScope.AGGREGATE )
                .createdAt( LocalDateTime.of( 1999 + n, 1, 1, 0, 0 ) )
                .createdBy( "test-user" )
                .klass( DataElement.class.getName() )
                .uid( uid )
                .code( code )
                .data( "{}" )
                .build();

            auditRepository.save( audit );
        } );

        List<Audit> audits = auditRepository.query( AuditQuery.builder()
            .code( new HashSet<>( codes.subList( 50, 100 ) ) )
            .build() );

        assertEquals( 50, audits.size() );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}
