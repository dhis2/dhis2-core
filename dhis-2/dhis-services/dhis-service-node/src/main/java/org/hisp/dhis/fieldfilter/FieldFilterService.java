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
package org.hisp.dhis.fieldfilter;

import java.util.Arrays;
import java.util.List;

import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.ComplexNode;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface FieldFilterService
{
    List<String> SHARING_FIELDS = Arrays.asList(
        "!user", "!publicAccess", "!userGroupAccesses", "!userAccesses", "!externalAccess", "!sharing" );

    /**
     * Perform inclusion/exclusion on a list of objects.
     */
    ComplexNode toComplexNode( FieldFilterParams params );

    /**
     * Perform inclusion/exclusion on a list of objects.
     */
    CollectionNode toCollectionNode( Class<?> wrapper, FieldFilterParams params );

    /**
     * This method will build and return a CollectionNode based on the given
     * parameters. This method works with POJO/DTO without nested objects. It's
     * main goal is to handle simple view objects and DTOs that do not have a
     * real schema and/or that are not persisted. This method doesn't evaluates
     * any complex logic based on Schema, sharing or access details. Its goal is
     * simply to return back a CollectionNode based on the concrete "klass" and
     * its direct attributes.
     *
     * @param klass the concrete class
     * @param fieldFilterParams the fields to be added to the response
     * @param collectionName the name of the collection for the node
     * @param namespace a namespace for the node
     * @return a CollectionNode populated/based on the input arguments
     */
    CollectionNode toConcreteClassCollectionNode( Class<?> klass, FieldFilterParams fieldFilterParams,
        String collectionName, String namespace );
}
