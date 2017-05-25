package org.hisp.dhis.dxf2.metadata;

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
import org.hisp.dhis.node.types.RootNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface MetadataExportService
{
    /**
     * Exports metadata using provided params.
     *
     * @param params Export parameters
     * @return Map of all exported objects
     */
    Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> getMetadata( MetadataExportParams params );

    /**
     * Returns same result as getMetadata, but metadata is returned as Node objects instead.
     *
     * @param params Export parameters
     * @return RootNode instance with children containing all exported objects
     */
    RootNode getMetadataAsNode( MetadataExportParams params );

    /**
     * Validates the import params. Not currently implemented.
     *
     * @param params Export parameters to validate
     */
    void validate( MetadataExportParams params );

    /**
     * Parses, and creates a MetadataExportParams instance based on given map of parameters.
     *
     * @param parameters Key-Value map of wanted parameters
     * @return MetadataExportParams instance created based on input parameters
     */
    MetadataExportParams getParamsFromMap( Map<String, List<String>> parameters );

    /**
     * Exports an object including a set of selected dependencies.
     *
     * @param object Object to export including dependencies
     * @return Original object + selected set of dependencies
     */
    Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> getMetadataWithDependencies( IdentifiableObject object );

    /**
     * Exports an object including a set of selected dependencies as RootNode.
     *
     * @param object Object to export including dependencies
     * @return Original object + selected set of dependencies, exported as RootNode
     */
    RootNode getMetadataWithDependenciesAsNode( IdentifiableObject object );


}
