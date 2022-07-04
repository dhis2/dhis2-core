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
package org.hisp.dhis.jsonpatch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.jsonpatch.validator.BulkPatchValidateParams;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

/**
 * Contains validation methods to be used in
 * {@link org.hisp.dhis.jsonpatch.BulkPatchManager}
 */
@Service
@AllArgsConstructor
public class BulkPatchValidatorService
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    /**
     * Validate {@link org.hisp.dhis.jsonpatch.BulkJsonPatch} with by using
     * validators from given {@link org.hisp.dhis.jsonpatch.BulkPatchParameters}
     *
     * @param bulkJsonPatch {@link org.hisp.dhis.jsonpatch.BulkJsonPatch} for
     *        validating
     * @param patchParameters
     *        {@link org.hisp.dhis.jsonpatch.BulkPatchParameters} contains
     *        validators
     * @return {@link org.hisp.dhis.jsonpatch.PatchBundle} contains validated
     *         {@link IdentifiableObject} and {@link JsonPatch}
     */
    public PatchBundle validate( BulkJsonPatch bulkJsonPatch, BulkPatchParameters patchParameters )
    {
        PatchBundle bundle = new PatchBundle();

        Schema schema = schemaService.getSchemaByPluralName( bulkJsonPatch.getClassName() );

        if ( schema == null )
        {
            patchParameters.addTypeReport( createTypeReport(
                new ErrorReport( JsonPatchException.class, ErrorCode.E6002, bulkJsonPatch.getClassName() ) ) );
            return bundle;
        }

        List<ObjectReport> objectReports = new ArrayList<>();

        objectReports.addAll( bulkJsonPatch.getIds()
            .stream().map( id -> checkPatchObject( schema, id, bulkJsonPatch.getPatch(), patchParameters, bundle ) )
            .filter( objectReport -> objectReport.hasErrorReports() )
            .collect( Collectors.toList() ) );

        if ( !objectReports.isEmpty() )
        {
            TypeReport typeReport = new TypeReport( schema.getKlass() );
            typeReport.setObjectReports( objectReports );
            patchParameters.addTypeReport( typeReport );
        }

        return bundle;
    }

    /**
     * Validate {@link org.hisp.dhis.jsonpatch.BulkJsonPatches} with by using
     * validators from given {@link BulkPatchParameters}
     *
     * @param patches {@link org.hisp.dhis.jsonpatch.BulkJsonPatches} for
     *        validating
     * @param patchParameters {@link BulkPatchParameters} contains validators
     * @return {@link PatchBundle} contains validated {@link IdentifiableObject}
     *         and {@link JsonPatch}
     */
    public PatchBundle validate( BulkJsonPatches patches, BulkPatchParameters patchParameters )
    {
        PatchBundle bundle = new PatchBundle();

        for ( String className : patches.getClassNames() )
        {
            Schema schema = schemaService.getSchemaByPluralName( className );

            if ( schema == null )
            {
                patchParameters.addTypeReport(
                    createTypeReport( new ErrorReport( JsonPatchException.class, ErrorCode.E6002, className ) ) );
                continue;
            }

            List<ObjectReport> objectReports = new ArrayList<>();

            objectReports.addAll( patches.get( className ).entrySet().stream()
                .map( entry -> checkPatchObject( schema, entry.getKey(), entry.getValue(), patchParameters, bundle ) )
                .filter( objectReport -> objectReport.hasErrorReports() )
                .collect( Collectors.toList() ) );

            if ( !objectReports.isEmpty() )
            {
                TypeReport typeReport = new TypeReport( schema.getKlass() );
                typeReport.setObjectReports( objectReports );
                patchParameters.addTypeReport( typeReport );
            }
        }
        return bundle;
    }

    /**
     * Validate given ID and {@link JsonPatch} by using
     * {@link org.hisp.dhis.jsonpatch.validator.BulkPatchValidator} from given
     * {@link BulkPatchParameters}.
     * <p>
     * The valid {@link IdentifiableObject} and {@link JsonPatch} will be added
     * to given {@link PatchBundle}
     *
     * @return {@link ObjectReport} contains {@link ErrorReport} if any.
     */
    @SuppressWarnings( "unchecked" )
    private ObjectReport checkPatchObject( Schema schema, String id, JsonPatch jsonPatch,
        BulkPatchParameters patchParams, PatchBundle bundle )
    {
        ObjectReport objectReport = new ObjectReport( schema.getKlass(), 0, id );

        IdentifiableObject entity = manager.get( (Class<? extends IdentifiableObject>) schema.getKlass(), id );

        BulkPatchValidateParams bulkPatchValidateParams = BulkPatchValidateParams.builder()
            .schema( schema )
            .jsonPatch( jsonPatch )
            .id( id )
            .entity( entity )
            .build();

        patchParams.getValidators().forEach(
            validator -> validator.validate( bulkPatchValidateParams, error -> objectReport.addErrorReport( error ) ) );

        if ( objectReport.hasErrorReports() )
        {
            return objectReport;
        }

        bundle.addEntity( id, entity, jsonPatch );
        return objectReport;
    }

    private TypeReport createTypeReport( ErrorReport errorReport )
    {
        ObjectReport objectReport = new ObjectReport( JsonPatchException.class, 0 );
        objectReport.addErrorReport( errorReport );
        TypeReport typeReport = new TypeReport( JsonPatchException.class );
        typeReport.addObjectReport( objectReport );
        return typeReport;
    }
}
