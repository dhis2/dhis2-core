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

import static org.hisp.dhis.commons.collection.CollectionUtils.addAllUnique;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.config.InclusionStrategy;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MetadataExportParams {
  /** User to use for sharing filtering. */
  private User user;

  /** If doing full export, this contains the list of classes you want exported. */
  private Set<Class<? extends IdentifiableObject>> classes = new HashSet<>();

  /** Contains a set of queries that allows for filtered export. */
  private Map<Class<? extends IdentifiableObject>, Query> queries = new HashMap<>();

  /**
   * Contains a set of field filters that allows the default field filter (:owner) to be overridden.
   */
  private Map<Class<? extends IdentifiableObject>, List<String>> fields = new HashMap<>();

  /** Default field filter applied to exports. */
  private List<String> defaultFields = Lists.newArrayList(":owner");

  /** Default object filter to apply to exports (default is no filter) */
  private List<String> defaultFilter = new ArrayList<>();

  /** Default order to apply to all exports. */
  private List<String> defaultOrder = new ArrayList<>();

  /** Should exported payload include defaults (coc, co etc) objects/references. */
  private Defaults defaults = Defaults.INCLUDE;

  /**
   * Inclusion strategy to use. There are a few already defined inclusions in the Inclusions enum.
   */
  private InclusionStrategy inclusionStrategy = InclusionStrategy.Include.NON_NULL;

  /** Indicates whether sharing properties should be included in the export. */
  private boolean skipSharing;

  public MetadataExportParams() {}

  public String getUsername() {
    return user != null ? user.getUsername() : "system-process";
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Set<Class<? extends IdentifiableObject>> getClasses() {
    return classes;
  }

  public void setClasses(Set<Class<? extends IdentifiableObject>> classes) {
    this.classes = classes;
  }

  public MetadataExportParams addClass(Class<? extends IdentifiableObject> klass) {
    classes.add(klass);
    return this;
  }

  @SuppressWarnings("unchecked")
  public MetadataExportParams addQuery(Query query) {
    if (!query.getSchema().isIdentifiableObject()) return this;

    Class<? extends IdentifiableObject> klass =
        (Class<? extends IdentifiableObject>) query.getSchema().getKlass();
    classes.add(klass);
    queries.put(klass, query);

    return this;
  }

  public Query getQuery(Class<? extends IdentifiableObject> klass) {
    return queries.get(klass);
  }

  public MetadataExportParams addFields(
      Class<? extends IdentifiableObject> klass, List<String> classFields) {
    if (!fields.containsKey(klass)) {
      fields.put(klass, classFields);
    } else {
      fields.get(klass).addAll(classFields);
    }

    return this;
  }

  public List<String> getFields(Class<? extends IdentifiableObject> klass) {
    List<String> strings = fields.get(klass);
    return strings != null ? strings : defaultFields;
  }

  public List<String> getDefaultFields() {
    return defaultFields;
  }

  public void setDefaultFields(List<String> defaultFields) {
    this.defaultFields = defaultFields;
  }

  public List<String> getDefaultFilter() {
    return defaultFilter;
  }

  public void setDefaultFilter(List<String> filter) {
    this.defaultFilter = filter;
  }

  public List<String> getDefaultOrder() {
    return defaultOrder;
  }

  public void setDefaultOrder(List<String> defaultOrder) {
    this.defaultOrder = defaultOrder;
  }

  public Defaults getDefaults() {
    return defaults;
  }

  public void setDefaults(Defaults defaults) {
    this.defaults = defaults;
  }

  public InclusionStrategy getInclusionStrategy() {
    return inclusionStrategy;
  }

  public void setInclusionStrategy(InclusionStrategy inclusionStrategy) {
    this.inclusionStrategy = inclusionStrategy;
  }

  public void setSkipSharing(boolean skipSharing) {
    if (skipSharing) {
      addAllUnique(defaultFields, FieldFilterService.SHARING_FIELDS);
    } else {
      defaultFields.removeAll(FieldFilterService.SHARING_FIELDS);
    }

    this.skipSharing = skipSharing;
  }

  public boolean getSkipSharing() {
    return this.skipSharing;
  }
}
