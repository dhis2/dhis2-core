/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.patch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsonpatch.BulkJsonPatch;
import org.hisp.dhis.jsonpatch.BulkJsonPatchValidator;
import org.hisp.dhis.jsonpatch.BulkPatchManager;
import org.hisp.dhis.jsonpatch.BulkPatchParameters;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author viet@dhis2.org
 */
public class BulkPatchManagerTest extends DhisSpringTest
{
    @Autowired
    private UserService _userService;

    @Autowired
    private BulkPatchManager patchManager;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private IdentifiableObjectManager manager;

    private DataElement dataElementA;

    private DataElement dataElementB;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        User userA = createUserWithId( "A", "NOOF56dveaZ" );
        User userB = createUserWithId( "B", "Kh68cDMwZsg" );
        dataElementA = createDataElement( 'A' );
        dataElementA.setUid( "fbfJHSPpUQD" );
        manager.save( dataElementA );

        dataElementB = createDataElement( 'B' );
        dataElementB.setUid( "cYeuwXTCPkU" );
        manager.save( dataElementB );

    }

    @Test
    public void testApplyPatchOk()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = jsonMapper.readValue(
            new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            BulkJsonPatch.class );

        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .patchValidator( BulkJsonPatchValidator::validateSharingPath )
            .schemaValidator( BulkJsonPatchValidator::validateShareableSchema )
            .build();

        List<IdentifiableObject> patchedObjects = patchManager
            .applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 2, patchedObjects.size() );
    }

    @Test
    public void testApplyPatchInvalidUid()
        throws IOException
    {
        manager.delete( dataElementA );
        final BulkJsonPatch bulkJsonPatch = jsonMapper.readValue(
            new ClassPathResource( "patch/bulk_sharing_patch.json" ).getInputStream(),
            BulkJsonPatch.class );

        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .patchValidator( BulkJsonPatchValidator::validateSharingPath )
            .schemaValidator( BulkJsonPatchValidator::validateShareableSchema )
            .build();

        List<IdentifiableObject> patchedObjects = patchManager
            .applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 1, patchedObjects.size() );
        assertEquals( 1, patchParameters.getErrorReports().size() );
        assertEquals( ErrorCode.E4014, patchParameters.getErrorReports().get( 0 ).getErrorCode() );
    }
}
