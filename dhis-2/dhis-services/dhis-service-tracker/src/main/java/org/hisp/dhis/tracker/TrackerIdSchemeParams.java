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
package org.hisp.dhis.tracker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper object to handle identifier-related parameters for tracker
 * import/export
 *
 * @author Stian Sandvold
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerIdSchemeParams
{
    /**
     * Specific identifier to match data elements on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam dataElementIdScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match organisation units on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam orgUnitIdScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match program on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam programIdScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match program stage on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam programStageIdScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match all metadata on. Will be overridden by
     * metadata-specific idSchemes.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam idScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match category option combo on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam categoryOptionComboIdScheme = TrackerIdSchemeParam.UID;

    /**
     * Specific identifier to match category option on.
     */
    @JsonProperty
    @Builder.Default
    private TrackerIdSchemeParam categoryOptionIdScheme = TrackerIdSchemeParam.UID;

    public TrackerIdSchemeParam getByClass( Class<?> klazz )
    {
        switch ( klazz.getSimpleName() )
        {
        case "CategoryOptionCombo":
            return categoryOptionComboIdScheme;
        case "OrganisationUnit":
            return orgUnitIdScheme;
        case "CategoryOption":
            return categoryOptionIdScheme;
        case "DataElement":
            return dataElementIdScheme;
        case "Program":
            return programIdScheme;
        case "ProgramStage":
            return programStageIdScheme;
        default:
            return idScheme;
        }

    }

    /**
     * Creates metadata identifier for given {@code categoryOptionCombo} using
     * {@link #categoryOptionComboIdScheme}. For more details refer to
     * {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
     *
     * @param categoryOptionCombo to create metadata identifier for
     * @return metadata identifier representing metadata using the
     *         categoryOptionComboIdScheme
     */
    public MetadataIdentifier toMetadataIdentifier( CategoryOptionCombo categoryOptionCombo )
    {
        return categoryOptionComboIdScheme.toMetadataIdentifier( categoryOptionCombo );
    }

    /**
     * Creates metadata identifier for given {@code categoryOption} using
     * {@link #categoryOptionIdScheme}. For more details refer to
     * {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
     *
     * @param categoryOption to create metadata identifier for
     * @return metadata identifier representing metadata using the
     *         categoryOptionIdScheme
     */
    public MetadataIdentifier toMetadataIdentifier( CategoryOption categoryOption )
    {
        return categoryOptionIdScheme.toMetadataIdentifier( categoryOption );
    }

    /**
     * Creates metadata identifier for given {@code orgUnit} using
     * {@link #orgUnitIdScheme}. For more details refer to
     * {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
     *
     * @param orgUnit to create metadata identifier for
     * @return metadata identifier representing metadata using the
     *         orgUnitIdScheme
     */
    public MetadataIdentifier toMetadataIdentifier( OrganisationUnit orgUnit )
    {
        return orgUnitIdScheme.toMetadataIdentifier( orgUnit );
    }

    /**
     * Creates metadata identifier for given {@code program} using
     * {@link #programIdScheme}. For more details refer to
     * {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
     *
     * @param program to create metadata identifier for
     * @return metadata identifier representing metadata using the
     *         programIdScheme
     */
    public MetadataIdentifier toMetadataIdentifier( Program program )
    {
        return programIdScheme.toMetadataIdentifier( program );
    }

    /**
     * Creates metadata identifier for given {@code programStage} using
     * {@link #programStageIdScheme}. For more details refer to
     * {@link TrackerIdSchemeParam#toMetadataIdentifier(IdentifiableObject)}
     *
     * @param programStage to create metadata identifier for
     * @return metadata identifier representing metadata using the
     *         programIdScheme
     */
    public MetadataIdentifier toMetadataIdentifier( ProgramStage programStage )
    {
        return programStageIdScheme.toMetadataIdentifier( programStage );
    }
}
