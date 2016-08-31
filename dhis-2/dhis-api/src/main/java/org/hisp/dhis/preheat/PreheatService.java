package org.hisp.dhis.preheat;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.TypeReport;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface PreheatService
{
    /**
     * Preheat a set of pre-defined classes. If size == 0, then preheat all metadata classes automatically.
     *
     * @param params Params for preheating
     */
    Preheat preheat( PreheatParams params );

    /**
     * Validate PreheatParams.
     *
     * @param params PreheatParams
     */
    void validate( PreheatParams params );

    /**
     * Scan object and collect all references (both id object and collections with id objects).
     *
     * @param object Object to scan
     * @return Maps classes to collections of identifiers
     */
    Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( IdentifiableObject object );

    /**
     * Scan object and collect all references (both id object and collections with id objects).
     *
     * @param objects Object to scan
     * @return Maps classes to collections of identifiers
     */
    Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( Collection<IdentifiableObject> objects );

    /**
     * Scan objects and collect all references (both id object and collections with id objects).
     *
     * @param objects Objects to scan
     * @return Maps classes to collections of identifiers
     */
    Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Set<String>>> collectReferences( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects );

    /**
     * Scan objects and collect unique values (used to verify object properties with unique=true)
     *
     * @param objects Objects to scan
     * @return Klass -> Property.name -> Value -> UID
     */
    Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> collectUniqueness( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects );

    Map<Class<?>, Map<String, Map<String, Object>>> collectObjectReferences( Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> objects );

    /**
     * Checks but does not connect any references, returns check report
     *
     * @param objects    Object to check
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type to check references
     */
    TypeReport checkReferences( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Checks but does not connect any references, returns check report
     *
     * @param object     Object to check
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type to check references
     */
    List<PreheatErrorReport> checkReferences( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check for properties that are unique.
     *
     * @param objects    Object to check
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type report issues
     */
    TypeReport checkUniqueness( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check for properties that are unique.
     *
     * @param object  Object to check
     * @param preheat Preheat Cache to use
     */
    List<ErrorReport> checkUniqueness( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check that all required mandatory attributes are present.
     *
     * @param objects    Object to check
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type report issues
     */
    TypeReport checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check that all required mandatory attributes are present.
     *
     * @param object  Object to check
     * @param preheat Preheat Cache to use
     */
    List<ErrorReport> checkMandatoryAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check that all attribute values where attribute.unique=true is unique.
     *
     * @param objects    Object to check
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type report issues
     */
    TypeReport checkUniqueAttributes( Class<? extends IdentifiableObject> klass, List<IdentifiableObject> objects, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Check that all attribute values where attribute.unique=true is unique.
     *
     * @param object  Object to check
     * @param preheat Preheat Cache to use
     */
    List<ErrorReport> checkUniqueAttributes( Class<? extends IdentifiableObject> klass, IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier );

    /**
     * Connects id object references on a given object using a given identifier + a preheated Preheat cache.
     *
     * @param object     Object to connect to
     * @param preheat    Preheat Cache to use
     * @param identifier Use this identifier type to attach references
     */
    void connectReferences( Object object, Preheat preheat, PreheatIdentifier identifier );
}
