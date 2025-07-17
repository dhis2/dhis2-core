/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller.tracker.export;

import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.mapstruct.Context;
import org.mapstruct.Mapper;

/**
 * MetadataMapper maps {@link MetadataObject}s to a string identifier using the {@code idScheme}
 * specified in the {@code idSchemeParams}. Metadata that does not have an identifier for the
 * idScheme is collected in {@link MappingErrors}.
 */
@Mapper
public interface MetadataMapper {

  default <T extends IdentifiableObject & MetadataObject> String map(
      @Context TrackerIdSchemeParams idSchemeParams, @Context MappingErrors errors, T metadata) {
    String identifier = idSchemeParams.getIdentifier(metadata);

    if (StringUtils.isEmpty(identifier)) {
      errors.add(metadata);
    }

    return identifier;
  }

  default String map(
      @Context TrackerIdSchemeParams idSchemeParams,
      @Context MappingErrors errors,
      Set<CategoryOption> categoryOptions) {
    if (categoryOptions == null || categoryOptions.isEmpty()) {
      return null;
    }

    return categoryOptions.stream()
        .map(co -> map(idSchemeParams, errors, co))
        .collect(Collectors.joining(TextUtils.COMMA));
  }
}
