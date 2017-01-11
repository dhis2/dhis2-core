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

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;

/**
 * @author Chau Thu Tran
 * @version $Id: ProgramStore.java Dec 14, 2011 9:22:17 AM $
 */
public interface ProgramStore
    extends GenericIdentifiableObjectStore<Program>
{
    String ID = ProgramStore.class.getName();

    /**
     * Get {@link Program} by a type
     *
     * @param type The type of program. There are three types, include Multi
     *             events with registration, Single event with registration and
     *             Single event without registration
     * @return Program list by a type specified
     */
    List<Program> getByType( ProgramType type );

    /**
     * Get {@link Program} assigned to an {@link OrganisationUnit} by a type
     *
     * @param organisationUnit Where programs assigned
     * @return Program list by a type specified
     */
    List<Program> get( OrganisationUnit organisationUnit );

    /**
     * Get {@link Program} assigned to an {@link OrganisationUnit} by a type
     *
     * @param type    The type of program. There are three types, include Multi
     *                events with registration, Single event with registration and
     *                Single event without registration
     * @param orgunit Where programs assigned
     * @return Program list by a type specified
     */
    List<Program> get( ProgramType type, OrganisationUnit orgunit );

    /**
     * Get {@link Program} by TrackedEntity
     *
     * @param trackedEntity {@link TrackedEntity}
     */
    List<Program> getByTrackedEntity( TrackedEntity trackedEntity );

    /**
     * Get all Programs associated with the given DataEntryForm.
     * @param dataEntryForm the DataEntryForm.
     * @return a list of {@link Program}
     */
    List<Program> getByDataEntryForm( DataEntryForm dataEntryForm );
}
