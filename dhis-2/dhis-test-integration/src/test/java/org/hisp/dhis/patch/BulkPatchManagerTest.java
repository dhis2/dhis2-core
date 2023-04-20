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
package org.hisp.dhis.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsonpatch.BulkJsonPatch;
import org.hisp.dhis.jsonpatch.BulkJsonPatches;
import org.hisp.dhis.jsonpatch.BulkPatchManager;
import org.hisp.dhis.jsonpatch.BulkPatchParameters;
import org.hisp.dhis.jsonpatch.validator.BulkPatchValidatorFactory;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author viet@dhis2.org
 */
class BulkPatchManagerTest extends TransactionalIntegrationTest
{

    @Autowired
    private UserService _userService;

    @Autowired
    private BulkPatchManager patchManager;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private AclService aclService;

    @Autowired
    private PeriodService periodService;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataSet dataSetA;

    private User userA;

    private User userB;

    private User userC;

    private User userD;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        userA = createUserWithId( "A", "NOOF56dveaZ" );
        userB = createUserWithId( "B", "Kh68cDMwZsg" );
        userC = createUserWithAuth( "C" );
        userD = createUserWithAuth( "D" );
        dataElementA = createDataElement( 'A' );
        dataElementA
            .setSharing( Sharing.builder().owner( userD.getUid() ).publicAccess( AccessStringHelper.DEFAULT ).build() );
        dataElementA.setUid( "fbfJHSPpUQD" );
        manager.save( dataElementA, false );
        dataElementB = createDataElement( 'B' );
        dataElementB.setUid( "cYeuwXTCPkU" );
        dataElementB
            .setSharing( Sharing.builder().owner( userD.getUid() ).publicAccess( AccessStringHelper.DEFAULT ).build() );
        manager.save( dataElementB, false );
        PeriodType periodType = periodService.getPeriodTypeByClass( MonthlyPeriodType.class );
        dataSetA = createDataSet( 'A', periodType );
        dataSetA.setUid( "em8Bg4LCr5k" );
        dataSetA
            .setSharing( Sharing.builder().owner( userD.getUid() ).publicAccess( AccessStringHelper.DEFAULT ).build() );
        manager.save( dataSetA, false );
    }

    @Test
    void testApplyPatchOk()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = loadPatch( "bulk_sharing_patch.json", BulkJsonPatch.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 2, patchedObjects.size() );
        assertTrue( aclService.canRead( userA, patchedObjects.get( 0 ) ) );
        assertTrue( aclService.canRead( userA, patchedObjects.get( 1 ) ) );
        assertFalse( aclService.canRead( userC, patchedObjects.get( 0 ) ) );
    }

    @Test
    void testApplyPatchInvalidClassName()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = loadPatch( "bulk_sharing_patch_invalid_class_name.json",
            BulkJsonPatch.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 0, patchedObjects.size() );
        assertEquals( 1, patchParameters.getErrorReportsCount() );
        assertEquals( 1, patchParameters.getErrorReportsCount( ErrorCode.E6002 ) );
    }

    @Test
    void testApplyPatchInvalidUid()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = loadPatch( "bulk_sharing_patch_invalid_uid.json", BulkJsonPatch.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 1, patchedObjects.size() );
        assertEquals( 1, patchParameters.getErrorReportsCount() );
        assertEquals( 1, patchParameters.getErrorReportsCount( ErrorCode.E4014 ) );
        assertTrue( aclService.canRead( userA, patchedObjects.get( 0 ) ) );
        assertFalse( aclService.canRead( userC, patchedObjects.get( 0 ) ) );
    }

    @Test
    void testApplyPatchInvalidPath()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = loadPatch( "bulk_sharing_patch_invalid_path.json", BulkJsonPatch.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 0, patchedObjects.size() );
        assertEquals( 1, patchParameters.getErrorReportsCount() );
        assertEquals( 1, patchParameters.getErrorReportsCount( ErrorCode.E4032 ) );
    }

    @Test
    void testApplyPatchNotShareableSchema()
        throws IOException
    {
        final BulkJsonPatch bulkJsonPatch = loadPatch( "bulk_sharing_patch_not_shareable.json", BulkJsonPatch.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatch( bulkJsonPatch, patchParameters );
        assertEquals( 0, patchedObjects.size() );
        assertEquals( 2, patchParameters.getErrorReportsCount() );
        assertEquals( 1, patchParameters.getErrorReportsCount( ErrorCode.E3019 ) );
        assertEquals( 1, patchParameters.getErrorReportsCount( ErrorCode.E4014 ) );
    }

    @Test
    void testApplyPatchesOk()
        throws IOException
    {
        final BulkJsonPatches bulkJsonPatch = loadPatch( "bulk_sharing_patches.json", BulkJsonPatches.class );
        BulkPatchParameters patchParameters = BulkPatchParameters.builder()
            .validators( BulkPatchValidatorFactory.SHARING ).build();
        List<IdentifiableObject> patchedObjects = patchManager.applyPatches( bulkJsonPatch, patchParameters );
        assertEquals( 3, patchedObjects.size() );
        assertFalse( aclService.canRead( userA, dataSetA ) );
        assertFalse( aclService.canRead( userB, dataSetA ) );
        IdentifiableObject patchedDataElementA = patchedObjects.stream()
            .filter( de -> de.getUid().equals( dataElementA.getUid() ) ).findFirst().get();
        IdentifiableObject patchedDataElementB = patchedObjects.stream()
            .filter( de -> de.getUid().equals( dataElementB.getUid() ) ).findFirst().get();
        IdentifiableObject patchedDataSetA = patchedObjects.stream()
            .filter( de -> de.getUid().equals( dataSetA.getUid() ) ).findFirst().get();
        assertTrue( aclService.canRead( userA, patchedDataElementA ) );
        assertTrue( aclService.canRead( userB, patchedDataElementB ) );
        assertTrue( aclService.canRead( userA, patchedDataSetA ) );
        assertTrue( aclService.canRead( userB, patchedDataSetA ) );
        assertFalse( aclService.canRead( userC, patchedDataElementA ) );
        assertFalse( aclService.canRead( userC, patchedDataSetA ) );
    }

    private <T> T loadPatch( String fileName, Class<T> klass )
        throws IOException
    {
        return jsonMapper.readValue( new ClassPathResource( "patch/" + fileName ).getInputStream(), klass );
    }
}
