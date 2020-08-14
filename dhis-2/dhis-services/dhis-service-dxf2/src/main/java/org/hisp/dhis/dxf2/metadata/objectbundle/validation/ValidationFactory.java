package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleHook;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.schema.validation.SchemaValidator;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Component
@Slf4j
public class ValidationFactory
{
    private final SchemaValidator schemaValidator;

    private final SchemaService schemaService;

    private final AclService aclService;

    private final UserService userService;

    private final Map<ImportStrategy, List<Class<? extends ValidationCheck>>> validatorMap;

    private List<ObjectBundleHook> objectBundleHooks;

    public ValidationFactory( SchemaValidator schemaValidator, SchemaService schemaService, AclService aclService,
        UserService userService, List<ObjectBundleHook> objectBundleHooks,
        Map<ImportStrategy, List<Class<? extends ValidationCheck>>> validatorMap )
    {
        this.schemaValidator = schemaValidator;
        this.schemaService = schemaService;
        this.aclService = aclService;
        this.userService = userService;
        this.validatorMap = validatorMap;
        this.objectBundleHooks = objectBundleHooks == null ? Collections.emptyList() : objectBundleHooks;
    }

    /**
     * Run the validation checks against the bundle
     *
     * @param bundle an {@see ObjectBundle}
     * @param klass the Class type that is getting validated
     * @param persistedObjects a List of IdentifiableObject
     * @param nonPersistedObjects a List of IdentifiableObject
     *
     * @return a {@see TypeReport} containing the outcome of the validation
     */
    public TypeReport validateBundle( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects )
    {
        ValidationContext ctx = getContext();
        TypeReport typeReport = new ValidationRunner( validatorMap.get( bundle.getImportMode() ) )
            .executeValidationChain( bundle, klass, persistedObjects, nonPersistedObjects, ctx );

        // Remove invalid objects from the bundle
        removeFromBundle( klass, ctx, bundle );

        return addStatistics( typeReport, bundle, persistedObjects, nonPersistedObjects );
    }

    private TypeReport addStatistics( TypeReport typeReport, ObjectBundle bundle,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects )
    {
        if ( bundle.getImportMode().isCreateAndUpdate() )
        {
            typeReport.getStats().incCreated( nonPersistedObjects.size() );
            typeReport.getStats().incUpdated( persistedObjects.size() );
        }
        else if ( bundle.getImportMode().isCreate() )
        {
            typeReport.getStats().incCreated( nonPersistedObjects.size() );

        }
        else if ( bundle.getImportMode().isUpdate() )
        {
            typeReport.getStats().incUpdated( persistedObjects.size() );

        }
        else if ( bundle.getImportMode().isDelete() )
        {
            typeReport.getStats().incDeleted( persistedObjects.size() );
        }

        return typeReport;
    }

    /**
     *
     * @param klass the class of the objects to remove from bundle
     * @param ctx the {@see ValidationContext} containing the list of objects to
     *        remove
     * @param bundle the {@see ObjectBundle}
     */
    private void removeFromBundle( Class<? extends IdentifiableObject> klass, ValidationContext ctx,
        ObjectBundle bundle )
    {
        List<IdentifiableObject> persisted = bundle.getObjects( klass, true );
        persisted.removeAll( ctx.getMarkedForRemoval() );

        List<IdentifiableObject> nonPersisted = bundle.getObjects( klass, false );
        nonPersisted.removeAll( ctx.getMarkedForRemoval() );
    }

    private ValidationContext getContext()
    {
        return new ValidationContext( this.objectBundleHooks, this.schemaValidator, this.aclService, this.userService,
            this.schemaService );
    }

    static class ValidationRunner
    {
        private List<Class<? extends ValidationCheck>> validators;

        public ValidationRunner( List<Class<? extends ValidationCheck>> validators )
        {
            this.validators = validators;
        }

        public TypeReport executeValidationChain( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
            List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects,
            ValidationContext ctx )
        {
            TypeReport typeReport = new TypeReport( klass );
            for ( Class<? extends ValidationCheck> validator : validators )
            {
                try
                {
                    ValidationCheck validationCheck = validator.newInstance();
                    typeReport.merge( validationCheck.check( bundle, klass, persistedObjects, nonPersistedObjects,
                        bundle.getImportMode(), ctx ) );
                }
                catch ( InstantiationException | IllegalAccessException e )
                {
                    log.error( "An error occurred during metadata import validation", e );
                }
            }
            return typeReport;
        }
    }
}
