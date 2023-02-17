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
package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.importexport.ImportStrategy.CREATE_AND_UPDATE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleHooks;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class ValidatingEventCheckerTest
{
    @Mock
    private SchemaValidator schemaValidator;

    @Mock
    private SchemaService schemaService;

    @Mock
    private AclService aclService;

    @Mock
    private UserService userService;

    private ValidationFactory validationFactory;

    @BeforeEach
    public void setUp()
    {
        // Create a validation factory with a dummy check
        validationFactory = new ValidationFactory( schemaValidator, schemaService, aclService, userService,
            new ObjectBundleHooks( Collections.emptyList() ),
            new ValidationRunner( Map.of( CREATE_AND_UPDATE, ListUtils.newList( new DummyCheck() ) ) ) );
    }

    @Test
    void verifyValidationFactoryProcessValidationCheck()
    {
        ObjectBundle bundle = createObjectBundle();

        TypeReport typeReport = validationFactory.validateBundle( bundle, Attribute.class,
            bundle.getObjects( Attribute.class, true ), bundle.getObjects( Attribute.class, false ) );

        // verify that object has been removed from bundle
        assertThat( bundle.getObjects( Attribute.class, false ), hasSize( 0 ) );
        assertThat( bundle.getObjects( Attribute.class, true ), hasSize( 0 ) );
        assertThat( typeReport.getStats().getCreated(), is( 0 ) );
        assertThat( typeReport.getStats().getUpdated(), is( 0 ) );
        assertThat( typeReport.getStats().getDeleted(), is( 0 ) );
        assertThat( typeReport.getStats().getIgnored(), is( 1 ) );
        assertThat( typeReport.getObjectReportsCount(), is( 1 ) );
    }

    private ObjectBundle createObjectBundle()
    {

        Attribute attribute1 = new Attribute();
        attribute1.setUid( "u1" );

        ObjectBundleParams objectBundleParams = new ObjectBundleParams();
        Preheat preheat = new Preheat();

        final Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objectMap = new HashMap<>();
        objectMap.put( Attribute.class, new ArrayList<>() );

        objectMap.get( Attribute.class ).add( attribute1 );

        preheat.put( PreheatIdentifier.UID, attribute1 );

        return new ObjectBundle( objectBundleParams, preheat, objectMap );
    }

}
