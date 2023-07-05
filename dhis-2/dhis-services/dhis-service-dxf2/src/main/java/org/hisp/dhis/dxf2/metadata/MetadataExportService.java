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
package org.hisp.dhis.dxf2.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.IdentifiableObject;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface MetadataExportService {
  /**
   * Exports metadata using provided params.
   *
   * @param params Export parameters
   * @return Map of all exported objects
   */
  Map<Class<? extends IdentifiableObject>, List<? extends IdentifiableObject>> getMetadata(
      MetadataExportParams params);

  /**
   * Returns same result as getMetadata, but metadata is returned as Node objects instead.
   *
   * @param params Export parameters
   * @return RootNode instance with children containing all exported objects
   */
  ObjectNode getMetadataAsObjectNode(MetadataExportParams params);

  /**
   * Returns same result as getMetadata, but metadata is written to outputStream instead.
   *
   * @param params Export parameters
   * @param outputStream Streaming target
   */
  void getMetadataAsObjectNodeStream(MetadataExportParams params, OutputStream outputStream)
      throws IOException;

  /**
   * Validates the import params. Not currently implemented.
   *
   * @param params Export parameters to validate
   */
  void validate(MetadataExportParams params);

  /**
   * Parses, and creates a MetadataExportParams instance based on given map of parameters.
   *
   * @param parameters Key-Value map of wanted parameters
   * @return MetadataExportParams instance created based on input parameters
   */
  MetadataExportParams getParamsFromMap(Map<String, List<String>> parameters);

  /**
   * Exports an object including a set of selected dependencies. Only a subset of the specified
   * export parameters are used for the metadata with dependencies export.
   *
   * @param object Object to export including dependencies
   * @return Original object + selected set of dependencies
   */
  Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> getMetadataWithDependencies(
      IdentifiableObject object);

  /**
   * Exports an object including a set of selected dependencies. Only a subset of the specified
   * export parameters are used for the metadata with dependencies export. All objects are written
   * to given outputStream
   *
   * @param object The {@link IdentifiableObject} to be exported with dependencies.
   * @param params {@link MetadataExportParams}
   * @param outputStream Streaming target.
   * @throws IOException
   */
  void getMetadataWithDependenciesAsNodeStream(
      IdentifiableObject object, @Nonnull MetadataExportParams params, OutputStream outputStream)
      throws IOException;

  /**
   * Exports an object including a set of selected dependencies as RootNode. Only a subset of the
   * specified export parameters are used for the metadata with dependencies export.
   *
   * @param object Object to export including dependencies
   * @param params Parameters that affect the export.
   * @return Original object + selected set of dependencies, exported as RootNode
   */
  ObjectNode getMetadataWithDependenciesAsNode(
      IdentifiableObject object, @Nonnull MetadataExportParams params);
}
