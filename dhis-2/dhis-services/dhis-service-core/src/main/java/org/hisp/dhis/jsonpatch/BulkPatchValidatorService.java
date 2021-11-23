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
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Service;

/**
 * Contains validation methods to be used in {@link BulkPatchManager}
 */
@Service
@AllArgsConstructor
public class BulkPatchValidatorService
{
    private final IdentifiableObjectManager manager;

    private final SchemaService schemaService;

    /**
     * Validate {@link BulkJsonPatch} with by using validators from given
     * {@link BulkPatchParameters}
     *
     * @param bulkJsonPatch {@link BulkJsonPatch} for validating
     * @param patchParameters {@link BulkPatchParameters} contains validators
     * @return {@link PatchBundle} contains validated {@link IdentifiableObject}
     *         and {@link JsonPatch}
     */
    public PatchBundle validate( BulkJsonPatch bulkJsonPatch, BulkPatchParameters patchParameters )
    {
        PatchBundle bundle = new PatchBundle();
        Schema schema = schemaService.getSchemaByPluralName( bulkJsonPatch.getClassName() );

        if ( schema == null )
        {
            patchParameters.addTypeReport( createTypeReport( Collections.singletonList(
                new ErrorReport( JsonPatchException.class, ErrorCode.E6002 ) ) ) );
            return bundle;
        }

        if ( !patchParameters.getValidators().validateSchema( schema, errorReports -> patchParameters
            .addTypeReport( createTypeReport( errorReports ) ) ) )
        {
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
     * Validate {@link BulkJsonPatches} with by using validators from given
     * {@link BulkPatchParameters}
     *
     * @param patches {@link BulkJsonPatches} for validating
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

            if ( !patchParameters.getValidators().validateSchema( schema,
                errors -> patchParameters.addTypeReport( createTypeReport( errors ) ) ) )
            {
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
     * {@link BulkPatchValidators} from given {@link BulkPatchParameters}.
     * <p>
     * The valid {@link IdentifiableObject} and {@link JsonPatch} will be added
     * to given {@link PatchBundle}
     *
     * @return {@link ObjectReport} contains {@link ErrorReport} if any.
     */
    private ObjectReport checkPatchObject( Schema schema, String id, JsonPatch jsonPatch,
        BulkPatchParameters patchParams, PatchBundle bundle )
    {
        ObjectReport objectReport = new ObjectReport( schema.getKlass(), 0, id );

        if ( !patchParams.getValidators()
            .validateJsonPatch( jsonPatch, errors -> objectReport.addErrorReports( errors ) ) )
        {
            return objectReport;
        }

        IdentifiableObject entity = manager.get( (Class<? extends IdentifiableObject>) schema.getKlass(), id );

        if ( !patchParams.getValidators()
            .validatePatchEntity( schema, jsonPatch, id, entity, err -> objectReport.addErrorReports( err ) ) )
        {
            return objectReport;
        }

        bundle.addEntity( id, entity, jsonPatch );
        return objectReport;
    }

    /**
     * Util method to create TypeReport
     */
    private static TypeReport createTypeReport( List<ErrorReport> errorReports )
    {
        TypeReport typeReport = new TypeReport( JsonPatchException.class );
        ObjectReport objectReport = new ObjectReport( JsonPatchException.class, 0 );
        objectReport.addErrorReports( errorReports );
        typeReport.addObjectReport( objectReport );
        return typeReport;
    }
}
