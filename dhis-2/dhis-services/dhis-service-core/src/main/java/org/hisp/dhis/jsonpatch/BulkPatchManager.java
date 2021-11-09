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
package org.hisp.dhis.jsonpatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

@Service
public class BulkPatchManager
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    private final JsonPatchManager jsonPatchManager;

    public BulkPatchManager( SchemaService schemaService, IdentifiableObjectManager manager,
        JsonPatchManager jsonPatchManager )
    {
        this.manager = manager;
        this.schemaService = schemaService;
        this.jsonPatchManager = jsonPatchManager;
    }

    /**
     * Apply a {@link JsonPatch} to multiple objects.
     *
     * @param schema {@link Schema} instance of the patching entity.
     * @param bulkJsonPatch {@link BulkJsonPatch} instance contains the data
     *        parsed from request payload.
     * @param patchParameters {@link BulkPatchParameters} contains all
     *        parameters used for patch function.
     * @return List of patched objects
     */
    public List<IdentifiableObject> applyPatch( Schema schema, BulkJsonPatch bulkJsonPatch,
        BulkPatchParameters patchParameters )
    {
        String className = bulkJsonPatch.getTargetIds().keySet().iterator().next();

        if ( !schema.getPlural().equals( className ) )
        {
            patchParameters.addErrorReport( new ErrorReport( JsonPatchException.class, ErrorCode.E6002, className ) );
            return Collections.emptyList();
        }

        List<ErrorReport> errors = patchParameters.getSchemaValidator().apply( schema );
        if ( !errors.isEmpty() )
        {
            patchParameters.addErrorReports( errors );
            return Collections.emptyList();
        }

        List<IdentifiableObject> patchedObjects = new ArrayList<>();

        bulkJsonPatch.getIdsByClassName( className )
            .forEach( id -> patchObject( id, schema, bulkJsonPatch.getPatch(), patchParameters )
                .ifPresent( patched -> patchedObjects.add( patched ) ) );

        return patchedObjects;
    }

    /**
     * Apply {@link JsonPatch} to list of objects from given Map.
     * <p>
     * Each object has its own {@link JsonPatch}.
     *
     * @param patches
     * @param patchParameters
     * @return
     */
    public List<IdentifiableObject> applyPatches( Map<String, Map<String, JsonPatch>> patches,
        BulkPatchParameters patchParameters )
    {
        List<IdentifiableObject> patchedObjects = new ArrayList<>();

        for ( String className : patches.keySet() )
        {
            Schema schema = schemaService.getSchemaByPluralName( className );

            if ( !validateSchema( className, schema, patchParameters ) )
            {
                continue;
            }

            Map<String, JsonPatch> mapPatches = patches.get( className );

            mapPatches.keySet()
                .forEach( id -> patchObject( id, schema, mapPatches.get( id ), patchParameters )
                    .ifPresent( patchedObjects::add ) );
        }
        return patchedObjects;
    }

    private Optional<IdentifiableObject> patchObject( String id, Schema schema, JsonPatch patch,
        BulkPatchParameters patchParams )
    {
        Optional<IdentifiableObject> entity = validate( schema.getKlass(), patch, id, patchParams );

        Optional<IdentifiableObject> patched = entity.isPresent() ? applyWithTryCatch( schema, patch, entity.get(),
            patchParams.getErrorReports() ) : Optional.empty();

        patched.ifPresent( patchedObject -> postApply( id, patchedObject ) );

        return patched;
    }

    private Optional<IdentifiableObject> applyWithTryCatch( Schema schema, JsonPatch patch, IdentifiableObject entity,
        List<ErrorReport> errors )
    {
        try
        {
            return Optional.ofNullable( jsonPatchManager.apply( patch, entity ) );
        }
        catch ( JsonPatchException e )
        {
            errors.add( new ErrorReport( schema.getKlass(), ErrorCode.E6003, e.getMessage() ) );
            return Optional.empty();
        }
    }

    private boolean validateSchema( String className, Schema schema, BulkPatchParameters patchParameters )
    {
        if ( schema == null )
        {
            patchParameters.addErrorReport( new ErrorReport( JsonPatchException.class, ErrorCode.E6002, className ) );
            return false;
        }

        if ( patchParameters.hasSchemaValidator() )
        {
            List<ErrorReport> errors = patchParameters.getSchemaValidator().apply( schema );
            if ( !errors.isEmpty() )
            {
                patchParameters.addErrorReports( errors );
                return false;
            }
        }

        return true;
    }

    private Optional<IdentifiableObject> validate( Class<?> klass, JsonPatch patch, String id,
        BulkPatchParameters patchParams )
    {
        IdentifiableObject entity = manager.get( (Class<? extends IdentifiableObject>) klass, id );

        if ( entity == null )
        {
            patchParams.addErrorReport( new ErrorReport( klass, ErrorCode.E4014, id, klass.getSimpleName() ) );
            return Optional.empty();
        }

        if ( patchParams.hasPatchValidator() )
        {
            List<ErrorReport> errors = patchParams.getPatchValidator().apply( patch );
            if ( !errors.isEmpty() )
            {
                patchParams.addErrorReports( errors );
                return Optional.empty();
            }
        }

        return Optional.of( entity );
    }

    private void postApply( String id, IdentifiableObject patchedObject )
    {
        // we don't allow changing UIDs
        ((BaseIdentifiableObject) patchedObject).setUid( id );

        // Only supports new Sharing format
        ((BaseIdentifiableObject) patchedObject).clearLegacySharingCollections();
    }
}
