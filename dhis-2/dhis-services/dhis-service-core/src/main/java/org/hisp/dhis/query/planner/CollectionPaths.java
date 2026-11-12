/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.query.planner;

import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * Decides whether a filter path such as {@code userGroups.id} or {@code
 * dataViewOrganisationUnits.path} can be matched in SQL with a correlated EXISTS. Shared by the
 * query planner and the JPA query engine so both apply the same rules.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class CollectionPaths {

  private final SchemaService schemaService;

  /**
   * Walks the filter path and returns the type that owns the terminal property, provided the path
   * traverses at least one collection and ends in a single-valued simple property.
   *
   * <p>Collection-valued terminals (for example {@code userRoles.authorities}, a {@code
   * Set<String>} that the schema marks as simple) are rejected: the EXISTS predicate cannot match
   * their elements, so they stay in memory. A like filter additionally requires a text terminal.
   *
   * @param rootType root type of the query
   * @param filter the filter to evaluate
   * @param terminal the terminal property of the filter path
   * @return the type owning the terminal property, or {@code null} when the path is not eligible
   */
  @CheckForNull
  public Class<?> itemType(Class<?> rootType, Filter filter, Property terminal) {
    if (!isEligibleTerminal(filter, terminal)) return null;

    String[] components = filter.getPath().split("\\.");
    if (components.length < 2) return null;

    Schema schema = schemaService.getSchema(rootType);
    Class<?> owner = null;
    boolean hasCollection = false;

    for (int i = 0; i < components.length - 1; i++) {
      Property property = schema.getProperty(components[i]);
      owner = stepType(property);
      if (owner == null) return null;
      hasCollection |= property.isCollection();
      schema = schemaService.getSchema(owner);
    }

    return hasCollection ? owner : null;
  }

  /** A single-valued simple terminal; a like filter additionally requires text. */
  private static boolean isEligibleTerminal(Filter filter, Property terminal) {
    if (terminal == null || !terminal.isSimple() || terminal.isCollection()) return false;
    return !filter.isLike() || terminal.getKlass() == String.class;
  }

  /**
   * @return the type the path continues into after {@code property}: the item type of a collection
   *     or the type of a reference, or {@code null} when the path cannot continue through it
   */
  @CheckForNull
  private static Class<?> stepType(@CheckForNull Property property) {
    if (property == null || property.isEmbeddedObject()) return null;
    if (property.isCollection()) return property.getItemKlass();
    return property.isSimple() ? null : property.getKlass();
  }
}
