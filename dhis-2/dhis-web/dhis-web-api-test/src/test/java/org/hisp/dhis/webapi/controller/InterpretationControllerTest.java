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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link InterpretationController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
public class InterpretationControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private IdentifiableObjectManager manager;

    private String uid;

    @Before
    public void setUp()
    {
        OrganisationUnit ouA = createOrganisationUnit( 'A' );
        manager.save( ouA );
        Visualization vzA = createVisualization( 'A' );
        visualizationService.save( vzA );
        Interpretation ipA = new Interpretation( vzA, ouA, "Interpration of visualization A" );
        interpretationService.saveInterpretation( ipA );
        uid = ipA.getUid();
    }

    @Test
    public void testDeleteObject()
    {
        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/interpretations/" + uid ) );
    }

    @Test
    public void testDeleteObject_NotFound()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Interpretation does not exist: xyz",
            DELETE( "/interpretations/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }

}
