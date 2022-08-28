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
package org.hisp.dhis.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LockExceptionStoreTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private LockExceptionStore store;

    @Autowired
    private SessionFactory sessionFactory;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    @Override
    public void setUpTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
    }

    @Test
    void testDeleteByOrganisationUnit()
    {
        PeriodType periodType = new MonthlyPeriodType();
        Period period = new MonthlyPeriodType().createPeriod();
        DataSet ds = createDataSet( 'A', periodType );
        dataSetService.addDataSet( ds );
        LockException leA = new LockException( period, ouA, ds );
        LockException leB = new LockException( period, ouB, ds );
        store.save( leA );
        store.save( leB );
        assertEquals( 1, getLockExceptionCount( ouA ) );
        assertEquals( 1, getLockExceptionCount( ouB ) );
        store.delete( ouA );
        assertEquals( 0, getLockExceptionCount( ouA ) );
        assertEquals( 1, getLockExceptionCount( ouB ) );
        store.delete( ouB );
        assertEquals( 0, getLockExceptionCount( ouA ) );
        assertEquals( 0, getLockExceptionCount( ouB ) );
    }

    /**
     * Test HQL delete statement with an HQL select statement to ensure the
     * deleted rows are visible by the current transaction.
     *
     * @param target the {@link OrganisationUnit}
     * @return the count of interpretations.
     */
    private long getLockExceptionCount( OrganisationUnit target )
    {
        return (Long) sessionFactory.getCurrentSession()
            .createQuery( "select count(*) from LockException le where le.organisationUnit = :target" )
            .setParameter( "target", target ).uniqueResult();
    }
}
