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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link SectionStore} additional methods.
 *
 * @author Jan Bernitt
 */
class SectionStoreTest extends NonTransactionalIntegrationTest
{
    @Autowired
    private SectionStore sectionStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataSetService dataSetService;

    private DataElement de;

    private DataSet ds;

    @BeforeEach
    void setUp()
    {
        de = createDataElement( 'A' );
        dataElementService.addDataElement( de );

        ds = createDataSet( 'A' );
        dataSetService.addDataSet( ds );
    }

    @Test
    void testGetSectionsByDataElement_SectionOfDataElement()
    {
        Section s = new Section( "test", ds, List.of( de ), Set.of() );
        assertDoesNotThrow( () -> sectionStore.save( s ) );

        assertEquals( 1, sectionStore.getSectionsByDataElement( de.getUid() ).size() );
    }

    @Test
    void testGetSectionsByDataElement_SectionOfDataElementOperand()
    {
        DataElementOperand deo = new DataElementOperand( de );
        Section s = new Section( "test", ds, List.of(), Set.of( deo ) );
        assertDoesNotThrow( () -> sectionStore.save( s ) );

        assertEquals( 1, sectionStore.getSectionsByDataElement( de.getUid() ).size() );
    }
}
