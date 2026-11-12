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
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.schema.Property;
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
   * Returns the item type of the collection a filter path such as {@code userGroups.id} or {@code
   * dataViewOrganisationUnits.path} goes through, when the filter can be matched with a correlated
   * EXISTS.
   *
   * <p>Only the shape {@code collection.property} is eligible: a collection directly on the root
   * type followed by a single-valued simple property. Deeper paths (for example {@code
   * userGroups.managedGroups.id} or {@code createdBy.userGroups.id}) stay in memory.
   *
   * <p>Collection-valued terminals (for example {@code userRoles.authorities}, a {@code
   * Set<String>} that the schema marks as simple) are rejected: the EXISTS predicate cannot match
   * their elements, so they stay in memory. A like filter additionally requires a text terminal.
   *
   * @param rootType root type of the query
   * @param filter the filter to evaluate
   * @param terminal the terminal property of the filter path
   * @return the item type of the collection, or {@code null} when the path is not eligible
   */
  @CheckForNull
  public Class<?> itemType(Class<?> rootType, Filter filter, Property terminal) {
    if (!isEligibleTerminal(filter, terminal)) return null;

    PropertyPath path = PropertyPath.of(filter.getPath());
    if (path.length() != 2) return null;

    Property collection = schemaService.getSchema(rootType).getProperty(path.head().toString());
    if (collection == null || !collection.isCollection() || collection.isEmbeddedObject()) {
      return null;
    }
    return collection.getItemKlass();
  }

  /** A single-valued simple terminal; a like filter additionally requires text. */
  private static boolean isEligibleTerminal(Filter filter, Property terminal) {
    if (terminal == null || !terminal.isSimple() || terminal.isCollection()) return false;
    return !filter.isLike() || terminal.getKlass() == String.class;
  }
}
