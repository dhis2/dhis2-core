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
package org.hisp.dhis.datavalue;

import static org.hisp.dhis.test.setup.MetadataSetup.withDefaultSetup;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.test.setup.MetadataSetup;
import org.hisp.dhis.test.setup.MetadataSetupServiceExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DataValueService} API methods that are related to data
 * integrity checks.
 */
class DataValueServiceIntegrityTest extends SingleSetupIntegrationTestBase
{
    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private MetadataSetupServiceExecutor setupExecutor;

    private final MetadataSetup setup = new MetadataSetup();

    @Override
    protected void setUpTest()
    {
        setup.addOrganisationUnit( "A", withDefaultSetup );
        setup.addPeriod( "2022-07", withDefaultSetup );
        setup.addCategory( "Vertical", vert -> vert.addOption( "Up" ).addOption( "Down" ) );
        setup.addCategory( "Horizontal", hor -> hor.addOption( "Left" ).addOption( "Right" ) );
        setup.addCategory( "Age", age -> age.addOption( "0-39" ).addOption( "40+" ) );
        setup.addDataElement( "A", de -> de.addCombo( "L-R-U-D",
            combo -> combo.useCategory( "Vertical" ).useCategory( "Horizontal" ) ) );
        setup.addCategoryCombo( "VAge", combo -> combo.useCategory( "Vertical" ).useCategory( "Age" ) );
        setup.addCategoryOptionCombo( "UpLeft", a -> a.useCombo( "L-R-U-D" ).useOption( "Up" ).useOption( "Left" ) );
        setup.addCategoryOptionCombo( "DownRight",
            a -> a.useCombo( "L-R-U-D" ).useOption( "Down" ).useOption( "Right" ) );

        setupExecutor.create( setup );

        dataValueService.addDataValue( setup.newDateValue( "A", "2022-07", "A", "UpLeft", "1" ) );
    }

    @Test
    void testExistsAnyValue()
    {
        assertTrue( dataValueService.dataValueExists( setup.getCategoryCombo( "L-R-U-D" ) ) );
        assertFalse( dataValueService.dataValueExists( setup.getCategoryCombo( "VAge" ) ) );
    }
}
