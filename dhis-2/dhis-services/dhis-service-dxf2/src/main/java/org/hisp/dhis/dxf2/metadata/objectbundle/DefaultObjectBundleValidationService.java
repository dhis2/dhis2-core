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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.validation.ValidationFactory;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@RequiredArgsConstructor
@Service( "org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService" )
public class DefaultObjectBundleValidationService
    implements ObjectBundleValidationService
{
    private final ValidationFactory validationFactory;

    private final SchemaService schemaService;

    @Override
    @Transactional( readOnly = true )
    public ObjectBundleValidationReport validate( ObjectBundle bundle )
    {
        Timer timer = new SystemTimer().start();

        ObjectBundleValidationReport validation = new ObjectBundleValidationReport();

        if ( (bundle.getUser() == null || bundle.getUser().isSuper()) && bundle.isSkipValidation() )
        {
            log.warn( "Skipping validation for metadata import by user '" +
                bundle.getUsername() + "'. Not recommended." );
            return validation;
        }

        List<Class<? extends IdentifiableObject>> klasses = getSortedClasses( bundle );

        for ( Class<? extends IdentifiableObject> klass : klasses )
        {
            validateObjectType( bundle, validation, klass );
        }

        validateAtomicity( bundle, validation );
        bundle.setObjectBundleStatus( ObjectBundleStatus.VALIDATED );

        log.info( "(" + bundle.getUsername() + ") Import:Validation took " + timer.toString() );

        return validation;
    }

    private <T extends IdentifiableObject> void validateObjectType( ObjectBundle bundle,
        ObjectBundleValidationReport validation, Class<T> klass )
    {
        List<T> nonPersistedObjects = bundle.getObjects( klass, false );
        List<T> persistedObjects = bundle.getObjects( klass, true );

        cleanDefaults( bundle.getPreheat(), nonPersistedObjects );
        cleanDefaults( bundle.getPreheat(), persistedObjects );

        // Validate the bundle by running the validation checks chain
        validation
            .addTypeReport( validationFactory.validateBundle( bundle, klass, persistedObjects, nonPersistedObjects ) );
    }

    private void cleanDefaults( Preheat preheat, List<? extends IdentifiableObject> objects )
    {
        objects.removeIf( preheat::isDefault );
    }

    // ----------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------

    private void validateAtomicity( ObjectBundle bundle, ObjectBundleValidationReport validation )
    {
        if ( AtomicMode.NONE == bundle.getAtomicMode() )
        {
            return;
        }

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> nonPersistedObjects = bundle
            .getObjects( false );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> persistedObjects = bundle.getObjects( true );

        if ( AtomicMode.ALL == bundle.getAtomicMode() )
        {
            if ( validation.hasErrorReports() )
            {
                nonPersistedObjects.clear();
                persistedObjects.clear();
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private List<Class<? extends IdentifiableObject>> getSortedClasses( ObjectBundle bundle )
    {
        return schemaService.getMetadataSchemas().stream()
            .map( schema -> (Class<? extends IdentifiableObject>) schema.getKlass() )
            .filter( bundle::hasObjects )
            .collect( toList() );
    }
}
