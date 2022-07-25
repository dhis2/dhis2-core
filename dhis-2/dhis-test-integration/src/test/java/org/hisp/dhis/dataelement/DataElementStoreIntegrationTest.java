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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataElementStoreIntegrationTest extends TransactionalIntegrationTest
{

    @Autowired
    private DataElementStore dataElementStore;

    @Test
    void testDeleteAndGetDataElement()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        dataElementStore.save( dataElementA );
        long idA = dataElementA.getId();
        dataElementStore.save( dataElementB );
        long idB = dataElementB.getId();
        dataElementStore.save( dataElementC );
        long idC = dataElementC.getId();
        dataElementStore.save( dataElementD );
        long idD = dataElementD.getId();
        assertNotNull( dataElementStore.get( idA ) );
        assertNotNull( dataElementStore.get( idB ) );
        assertNotNull( dataElementStore.get( idC ) );
        assertNotNull( dataElementStore.get( idD ) );
        dataElementA = dataElementStore.get( idA );
        dataElementB = dataElementStore.get( idB );
        dataElementC = dataElementStore.get( idC );
        dataElementD = dataElementStore.get( idD );
        dataElementStore.delete( dataElementA );
        assertNull( dataElementStore.get( idA ) );
        assertNotNull( dataElementStore.get( idB ) );
        assertNotNull( dataElementStore.get( idC ) );
        assertNotNull( dataElementStore.get( idD ) );
        dataElementStore.delete( dataElementB );
        assertNull( dataElementStore.get( idA ) );
        assertNull( dataElementStore.get( idB ) );
        assertNotNull( dataElementStore.get( idC ) );
        assertNotNull( dataElementStore.get( idD ) );
        dataElementStore.delete( dataElementC );
        assertNull( dataElementStore.get( idA ) );
        assertNull( dataElementStore.get( idB ) );
        assertNull( dataElementStore.get( idC ) );
        assertNotNull( dataElementStore.get( idD ) );
        dataElementStore.delete( dataElementD );
        assertNull( dataElementStore.get( idA ) );
        assertNull( dataElementStore.get( idB ) );
        assertNull( dataElementStore.get( idC ) );
        assertNull( dataElementStore.get( idD ) );
    }
}
