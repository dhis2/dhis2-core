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
package org.hisp.dhis.dxf2.deprecated.tracker.importer;

import static org.hisp.dhis.dxf2.importsummary.ImportSummary.error;
import static org.hisp.dhis.dxf2.importsummary.ImportSummary.success;

import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

/**
 * Interface for classes that act as Tracker Import validation components.
 *
 * A class implementing this interface is responsible for a validation unit.
 *
 * The smaller the validation unit, the better.
 *
 * This interface only accepts {@see ImmutableEvent}, because a validation
 * component is not supposed to modify the object being validated.
 *
 * @author Luciano Fiandesio
 *
 * @deprecated this is a class related to "old" (deprecated) tracker which will
 *             be removed with "old" tracker. Make sure to plan migrating to new
 *             tracker.
 */
@Deprecated( since = "2.41" )
public interface Checker
{
    /**
     * Verify that the event satisfies the validation logic
     *
     * @param event an {@see ImmutableEvent}
     * @param workContext the work context containing the data required for
     *        validation
     * @return an {@see ImportSummary} class. If the validation is successful,
     *         the ImportSummary does not contain any error
     */
    ImportSummary check( ImmutableEvent event, WorkContext workContext );

    /**
     * Returns an {@see ImportSummary} object with the specified error
     * description, if the object is null.
     */
    default ImportSummary checkNull( Object object, String description, ImmutableEvent event )
    {
        if ( object == null )
        {
            return error( description, event.getEvent() );
        }

        return success();
    }
}
