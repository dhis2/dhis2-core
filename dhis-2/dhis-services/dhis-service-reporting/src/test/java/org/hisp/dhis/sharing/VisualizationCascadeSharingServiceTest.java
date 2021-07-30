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
package org.hisp.dhis.sharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class VisualizationCascadeSharingServiceTest
    extends CascadeSharingTest
{
    @Autowired
    private IdentifiableObjectManager objectManager;

    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CascadeSharingService<Visualization> cascadeSharingService;

    private User userA;

    private User userB;

    private Sharing sharingReadForUserA;

    @Override
    public void setUpTest()
    {
        userService = _userService;

        userA = createUser( 'A' );
        userB = createUser( 'B' );

        sharingReadForUserA = new Sharing();
        sharingReadForUserA.setPublicAccess( AccessStringHelper.DEFAULT );
        sharingReadForUserA.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );
        createAndInjectAdminUser();
    }

    @Test
    public void testCascadeIndicatorAndDataElement()
    {
        IndicatorType indicatorType = createIndicatorType( 'A' );
        objectManager.save( indicatorType );
        Indicator indicator = createIndicator( 'A', indicatorType );
        indicator.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( indicator, false );

        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA, false );

        Visualization visualizationA = createVisualization( 'A' );

        addDimensionItemToVisualizationRow( visualizationA, dataElementA.getUid(), DimensionItemType.DATA_ELEMENT );
        addDimensionItemToVisualizationColumn( visualizationA, indicator.getUid(), DimensionItemType.INDICATOR );
        visualizationA.setSharing( sharingReadForUserA );
        objectManager.save( visualizationA, false );

        List<ErrorReport> errors = cascadeSharingService.cascadeSharing( visualizationA,
            new CascadeSharingParameters() );
        assertEquals( 0, errors.size() );

        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        Indicator indicator1 = objectManager.get( Indicator.class, indicator.getUid() );

        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userA, indicator1 ) );
        assertFalse( aclService.canRead( userB, visualizationA ) );
        assertFalse( aclService.canRead( userB, updatedDataElementA ) );
        assertFalse( aclService.canRead( userB, indicator1 ) );
    }
}
