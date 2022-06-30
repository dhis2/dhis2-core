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
package org.hisp.dhis.merge.orgunit.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.merge.orgunit.OrgUnitMergeRequest;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnalyticalObjectOrgUnitMergeHandlerTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private AnalyticalObjectOrgUnitMergeHandler handler;

    private DataElement deA;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    @Override
    public void setUpTest()
    {
        deA = createDataElement( 'A' );
        idObjectManager.save( deA );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
    }

    @Test
    void testMergeVisualizations()
    {
        Visualization vA = createVisualization( 'A' );
        vA.addDataDimensionItem( deA );
        vA.getOrganisationUnits().add( ouA );
        vA.getOrganisationUnits().add( ouB );
        Visualization vB = createVisualization( 'B' );
        vB.addDataDimensionItem( deA );
        vB.getOrganisationUnits().add( ouA );
        vB.getOrganisationUnits().add( ouB );
        idObjectManager.save( vA );
        idObjectManager.save( vB );
        assertEquals( 2, getVisualizationCount( ouA ) );
        assertEquals( 2, getVisualizationCount( ouB ) );
        assertEquals( 0, getVisualizationCount( ouC ) );
        OrgUnitMergeRequest request = new OrgUnitMergeRequest.Builder().addSource( ouA ).addSource( ouB )
            .withTarget( ouC ).build();
        handler.mergeAnalyticalObjects( request );
        idObjectManager.update( ouC );
        assertEquals( 0, getVisualizationCount( ouA ) );
        assertEquals( 0, getVisualizationCount( ouB ) );
        assertEquals( 2, getVisualizationCount( ouC ) );
    }

    /**
     * Test migrate HQL update statement with an HQL select statement to ensure
     * the updated rows are visible by the current transaction.
     *
     * @param target the {@link OrganisationUnit}
     * @return the count of interpretations.
     */
    private long getVisualizationCount( OrganisationUnit target )
    {
        return (Long) sessionFactory.getCurrentSession()
            .createQuery(
                "select count(distinct v) from Visualization v where :target in elements(v.organisationUnits)" )
            .setParameter( "target", target ).uniqueResult();
    }
}
