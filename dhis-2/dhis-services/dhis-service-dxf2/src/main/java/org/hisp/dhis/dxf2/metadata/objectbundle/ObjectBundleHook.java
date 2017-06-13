package org.hisp.dhis.dxf2.metadata.objectbundle;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorReport;

import java.util.List;

/**
 * Contains hooks for object bundle commit phase.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface ObjectBundleHook
{
    /**
     * Hook to run custom validation code. Run before any other validation.
     *
     * @param object Object to validate
     * @param bundle Current validation phase bundle
     * @return Empty list if not errors, if errors then populated with one or more ErrorReports
     */
    <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle );

    /**
     * Run before commit phase has started.
     *
     * @param bundle Current commit phase bundle
     */
    void preCommit( ObjectBundle bundle );

    /**
     * Run after commit phase has finished.
     *
     * @param bundle Current commit phase bundle
     */
    void postCommit( ObjectBundle bundle );

    /**
     * Run before a type import has started. I.e. run before importing orgUnits, dataElements, etc.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void preTypeImport( Class<? extends IdentifiableObject> klass, List<T> objects, ObjectBundle bundle );

    /**
     * Run after a type import has finished. I.e. run before importing orgUnits, dataElements, etc.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void postTypeImport( Class<? extends IdentifiableObject> klass, List<T> objects, ObjectBundle bundle );

    /**
     * Run before object has been created.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void preCreate( T object, ObjectBundle bundle );

    /**
     * Run after object has been created.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void postCreate( T persistedObject, ObjectBundle bundle );

    /**
     * Run before object has been updated.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void preUpdate( T object, T persistedObject, ObjectBundle bundle );

    /**
     * Run after object has been updated.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void postUpdate( T persistedObject, ObjectBundle bundle );

    /**
     * Run before object has been deleted.
     *
     * @param bundle Current commit phase bundle
     */
    <T extends IdentifiableObject> void preDelete( T persistedObject, ObjectBundle bundle );
}
