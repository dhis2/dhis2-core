package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.user.User;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface Importer<T>
{
    /**
     * Import a single object, return null or a ImportConflict is there is a conflict.
     * <p>
     * This is meant to be a one-off import of a single object, if you want to import multiple
     * objects, then use importObjects..
     *
     * @param user    User User who is importing
     * @param objects Objects to import
     * @param options Import options
     * @return ImportConflict instance if a conflict occurred, if not null
     */
    ImportTypeSummary importObjects( User user, List<T> objects, ObjectBridge objectBridge, ImportOptions options );

    /**
     * Import a collection of objects.
     *
     * @param user    User User who is importing
     * @param object  The object to import
     * @param options Import options
     * @return List of all the ImportConflicts encountered
     */
    ImportTypeSummary importObject( User user, T object, ObjectBridge objectBridge, ImportOptions options );

    /**
     * Can this importer handle a certain Class type?
     *
     * @param clazz Class to check for
     * @return true or false depending on if class is supported
     */
    boolean canHandle( Class<?> clazz );
}
