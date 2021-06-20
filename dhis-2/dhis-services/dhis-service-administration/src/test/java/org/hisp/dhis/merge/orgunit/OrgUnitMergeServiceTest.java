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
package org.hisp.dhis.merge.orgunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class OrgUnitMergeServiceTest
    extends DhisSpringTest
{
    @Autowired
    private OrgUnitMergeService service;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    @Override
    public void setUpTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
    }

    @Test
    public void testGetFromQuery()
    {
        OrgUnitMergeQuery query = new OrgUnitMergeQuery();
        query.setSources( Sets.newHashSet( BASE_OU_UID + 'A', BASE_OU_UID + 'B' ) );
        query.setTarget( BASE_OU_UID + 'C' );

        OrgUnitMergeRequest request = service.getFromQuery( query );

        assertEquals( 2, request.getSources().size() );
        assertTrue( request.getSources().contains( ouA ) );
        assertTrue( request.getSources().contains( ouB ) );
        assertEquals( DataMergeStrategy.LAST_UPDATED, request.getDataValueMergeStrategy() );
        assertEquals( DataMergeStrategy.LAST_UPDATED, request.getDataApprovalMergeStrategy() );
        assertTrue( request.isDeleteSources() );
        assertEquals( ouC, request.getTarget() );
    }
}
