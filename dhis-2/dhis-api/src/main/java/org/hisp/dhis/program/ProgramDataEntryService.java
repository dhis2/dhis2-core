package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author Chau Thu Tran
 * @version $ ProgramDataEntryService.java May 26, 2011 3:56:03 PM $
 */
public interface ProgramDataEntryService
{
    Pattern INPUT_PATTERN = Pattern.compile( "(<input.*?)[/]?>", Pattern.DOTALL );

    Pattern IDENTIFIER_PATTERN_FIELD = Pattern.compile( "id=\"(\\w+)-(\\w+)-val\"" );

    // --------------------------------------------------------------------------
    // ProgramDataEntryService
    // --------------------------------------------------------------------------

    /**
     * Prepares the data entry form for data entry by injecting required
     * javascripts and drop down lists.
     *
     * @param htmlCode             the HTML code of the data entry form.
     * @param dataValues           the {@link TrackedEntityDataValue} which are registered for
     *                             this form.
     * @param programStage         {@link ProgramStage}
     * @param programStageInstance The {@link ProgramStageInstance} associated
     *                             with entry form
     * @param organisationUnit     The {@link OrganisationUnit} associated with this
     *                             program stage instance.
     * @return HTML code for the form.
     */
    String prepareDataEntryFormForEntry( String htmlCode, Collection<TrackedEntityDataValue> dataValues, I18n i18n,
        ProgramStage programStage, ProgramStageInstance programStageInstance, OrganisationUnit organisationUnit );

    /**
     * Prepare DataEntryForm code for save by reversing the effects of
     * prepareDataEntryFormForEdit().
     *
     * @param htmlCode     the HTML code of the data entry form.
     * @param i18n         I18n object
     * @param programStage {@link ProgramStage}
     * @return htmlCode the HTML code of the data entry form.
     */
    String prepareDataEntryFormForAdd( String htmlCode, I18n i18n, ProgramStage programStage );

    /**
     * Prepares the data entry form code by injecting the data element operand
     * name as value and title for each entry field.
     *
     * @param htmlCode the HTML code of the data entry form.
     * @return HTML code for the data entry form.
     */
    String prepareDataEntryFormForEdit( String htmlCode );
}
