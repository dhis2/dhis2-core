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
package org.hisp.dhis.tracker.validation.validator;

public class TrackerImporterAssertErrors
{
    private TrackerImporterAssertErrors()
    {
        throw new IllegalArgumentException( "Don't make an instance of me!" );
    }

    public static final String TRACKED_ENTITY_CANT_BE_NULL = "TrackedEntity can't be null";

    public static final String TRACKED_ENTITY_TYPE_CANT_BE_NULL = "TrackedEntityType can't be null";

    public static final String USER_CANT_BE_NULL = "User can't be null";

    public static final String EVENT_CANT_BE_NULL = "Event can't be null";

    public static final String PROGRAM_CANT_BE_NULL = "Program can't be null";

    public static final String TRACKED_ENTITY_INSTANCE_CANT_BE_NULL = "TrackedEntityInstance can't be null";

    public static final String ATTRIBUTE_VALUE_MAP_CANT_BE_NULL = "AttributeValueMap can't be null";

    public static final String ATTRIBUTE_CANT_BE_NULL = "Attribute can't be null";

    public static final String ENROLLMENT_CANT_BE_NULL = "Enrollment can't be null";

    public static final String PROGRAM_INSTANCE_CANT_BE_NULL = "ProgramInstance can't be null";

    public static final String ORGANISATION_UNIT_CANT_BE_NULL = "OrganisationUnit can't be null";

    public static final String OWNER_ORGANISATION_UNIT_CANT_BE_NULL = "Owner OrganisationUnit can't be null";

    public static final String TRACKED_ENTITY_ATTRIBUTE_VALUE_CANT_BE_NULL = "TrackedEntityAttributeValue can't be null";

    public static final String TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL = "TrackedEntityAttribute can't be null";

    public static final String PROGRAM_STAGE_CANT_BE_NULL = "ProgramStage can't be null";

    public static final String PROGRAM_STAGE_INSTANCE_CANT_BE_NULL = "ProgramStageInstance can't be null";

    public static final String CATEGORY_OPTION_COMBO_CANT_BE_NULL = "CategoryOptionCombo can't be null";

    public static final String DATE_STRING_CANT_BE_NULL = "Date string can not be null";

    public static final String GEOMETRY_CANT_BE_NULL = "Geometry can not be null";
}