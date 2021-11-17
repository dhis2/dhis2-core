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
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contains functions to apply {@link JsonPatch} to one or multiple object
 * types.
 */
@Service
@AllArgsConstructor
public class BulkPatchManager
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    private final JsonPatchManager jsonPatchManager;

    /**
     * Apply one {@link JsonPatch} to multiple objects of same class.
     *
     * @param bulkJsonPatch {@link BulkJsonPatch} instance contains the data
     *        parsed from request payload.
     * @param patchParameters {@link BulkPatchParameters} contains all
     *        parameters used for patch function.
     * @return List of patched objects
     */
    @Transactional( readOnly = true )
    public List<IdentifiableObject> applyPatch( BulkJsonPatch bulkJsonPatch,
        BulkPatchParameters patchParameters )
    {
        Optional<Schema> schema = validateClassName( bulkJsonPatch.getClassName(), patchParameters );

        if ( !schema.isPresent() )
        {
            return Collections.emptyList();
        }

        return bulkJsonPatch.getIds().stream()
            .map( id -> patchObject( id, schema.get(), bulkJsonPatch.getPatch(), patchParameters ) )
            .filter( patched -> patched.isPresent() )
            .map( Optional::get )
            .collect( Collectors.toList() );
    }

    /**
     * Apply multiple {@link JsonPatch} to multiple objects of different classes
     * from given {@link BulkJsonPatches}.
     * <p>
     * Each object has its own {@link JsonPatch}.
     */
    @Transactional( readOnly = true )
    public List<IdentifiableObject> applyPatches( BulkJsonPatches patches,
        BulkPatchParameters patchParameters )
    {
        List<IdentifiableObject> patchedObjects = new ArrayList<>();

        for ( String className : patches.getClassNames() )
        {
            Optional<Schema> schema = validateClassName( className, patchParameters );

            if ( !schema.isPresent() )
            {
                continue;
            }

            patchedObjects.addAll( patches.get( className ).entrySet().stream()
                .map( entry -> patchObject( entry.getKey(), schema.get(), entry.getValue(), patchParameters ) )
                .filter( Optional::isPresent )
                .map( Optional::get )
                .collect( Collectors.toList() ) );
        }

        return patchedObjects;
    }

    /**
     * Main method for validate and patch an object with given id and given
     * {@link JsonPatch}
     */
    private Optional<IdentifiableObject> patchObject( String id, Schema schema, JsonPatch patch,
        BulkPatchParameters patchParams )
    {
        TypeReport typeReport = new TypeReport( schema.getKlass() );
        ObjectReport objectReport = new ObjectReport( schema.getKlass(), 0 );

        if ( !validateJsonPatch( patch, patchParams, objectReport ) )
        {
            typeReport.addObjectReport( objectReport );
            patchParams.addTypeReport( typeReport );
            return Optional.empty();
        }

        Optional<IdentifiableObject> entity = validateId( schema.getKlass(), id, objectReport );

        Optional<IdentifiableObject> patched = entity.isPresent() ? applySafely( schema, patch, entity.get(),
            objectReport ) : Optional.empty();

        patched.ifPresent( patchedObject -> postApply( id, patchedObject ) );

        typeReport.addObjectReport( objectReport );
        patchParams.addTypeReport( typeReport );

        return patched;
    }

    /**
     * Try to apply given {@link JsonPatch} to given entity by calling
     * {@link JsonPatchManager#apply}.
     * <p>
     * If there is an error, add it to the given errors list and return
     * {@link Optional#empty()}.
     */
    private Optional<IdentifiableObject> applySafely( Schema schema, JsonPatch patch, IdentifiableObject entity,
        ObjectReport objectReport )
    {
        try
        {
            return Optional.ofNullable( jsonPatchManager.apply( patch, entity ) );
        }
        catch ( JsonPatchException e )
        {
            objectReport.addErrorReport( new ErrorReport( schema.getKlass(), ErrorCode.E6003, e.getMessage() ) );
            return Optional.empty();
        }
    }

    /**
     * Validate if there is a {@link Schema} exists with the given className.
     * <p>
     * Also apply schema validator from
     * {@link org.hisp.dhis.jsonpatch.SchemaValidator}
     *
     * @return {@link Schema}
     */
    private Optional<Schema> validateClassName( String className, BulkPatchParameters patchParameters )
    {
        Schema schema = schemaService.getSchemaByPluralName( className );

        if ( schema == null )
        {
            patchParameters.addTypeReport(
                createTypeReport( JsonPatchException.class,
                    new ErrorReport( JsonPatchException.class, ErrorCode.E6002, className ) ) );
            return Optional.empty();
        }

        List<ErrorReport> errors = patchParameters.getValidators().getSchemaValidator().apply( schema );

        if ( !errors.isEmpty() )
        {
            patchParameters.addTypeReport( createTypeReport( schema.getKlass(), errors ) );
            return Optional.empty();
        }

        return Optional.of( schema );
    }

    /**
     * Validate if an object exists with given id.
     *
     * @param klass Class of the object that need to be patched
     * @param id UID of the object that need to be patched
     * @return {@link IdentifiableObject}
     */
    private Optional<IdentifiableObject> validateId( Class<?> klass, String id, ObjectReport objectReport )
    {
        IdentifiableObject entity = manager.get( (Class<? extends IdentifiableObject>) klass, id );

        if ( entity == null )
        {
            objectReport.addErrorReport( new ErrorReport( klass, ErrorCode.E4014, id, klass.getSimpleName() ) );
            return Optional.empty();
        }

        return Optional.of( entity );
    }

    /**
     * Apply {@link JsonPatchValidator} to given {@link JsonPatch}.
     *
     * @param patch {@link JsonPatch} to be validated
     * @param patchParams {@link BulkPatchParameters} contains validation
     *        errors.
     */
    private boolean validateJsonPatch( JsonPatch patch, BulkPatchParameters patchParams, ObjectReport objectReport )
    {
        List<ErrorReport> errors = patchParams.getValidators().getJsonPatchValidator().apply( patch );

        if ( !errors.isEmpty() )
        {
            objectReport.addErrorReports( errors );
            return false;
        }

        return true;
    }

    /**
     * Apply some additional logics for patched object.
     */
    private void postApply( String id, IdentifiableObject patchedObject )
    {
        // we don't allow changing UIDs
        ((BaseIdentifiableObject) patchedObject).setUid( id );

        // Only supports new Sharing format
        ((BaseIdentifiableObject) patchedObject).clearLegacySharingCollections();
    }

    private TypeReport createTypeReport( Class<?> klass, ErrorReport errorReport )
    {
        TypeReport typeReport = new TypeReport( JsonPatchException.class );
        ObjectReport objectReport = new ObjectReport( JsonPatchException.class, 0 );
        objectReport.addErrorReport( errorReport );
        typeReport.addObjectReport( objectReport );
        return typeReport;
    }

    private TypeReport createTypeReport( Class<?> klass, List<ErrorReport> errorReports )
    {
        TypeReport typeReport = new TypeReport( klass );
        ObjectReport objectReport = new ObjectReport( klass, 0 );
        objectReport.addErrorReports( errorReports );
        typeReport.addObjectReport( objectReport );
        return typeReport;
    }
}
