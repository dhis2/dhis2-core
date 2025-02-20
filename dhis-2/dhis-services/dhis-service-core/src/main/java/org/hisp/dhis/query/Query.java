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
package org.hisp.dhis.query;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString(onlyExplicitlyIncluded = true)
public class Query {

  @ToString.Include private final List<Restriction> criterions = new ArrayList<>();

  @Getter private final Schema schema;

  private UserDetails currentUserDetails;

  @ToString.Include private String locale;

  @ToString.Include private final List<Order> orders = new ArrayList<>();

  @ToString.Include private boolean skipPaging;

  @ToString.Include private boolean skipSharing;

  @ToString.Include private boolean dataSharing;

  @ToString.Include private Integer firstResult = 0;

  @ToString.Include private Integer maxResults = Integer.MAX_VALUE;

  @ToString.Include private final Junction.Type rootJunctionType;

  @ToString.Include private Defaults defaults = Defaults.EXCLUDE;

  @ToString.Include private boolean cacheable = true;

  private List<? extends IdentifiableObject> objects;

  public static Query from(Schema schema) {
    return new Query(schema);
  }

  public static Query from(Schema schema, Junction.Type rootJunction) {
    return new Query(schema, rootJunction);
  }

  public static Query copy(Query query) {
    Query clone = Query.from(query.getSchema(), query.getRootJunctionType());
    clone.setSkipSharing(query.isSkipSharing());
    clone.setCurrentUserDetails(query.getCurrentUserDetails());
    clone.setLocale(query.getLocale());
    clone.addOrders(query.getOrders());
    clone.setFirstResult(query.getFirstResult());
    clone.setMaxResults(query.getMaxResults());
    clone.add(query.getCriterions());
    clone.setObjects(query.getObjects());

    return clone;
  }

  private Query(Schema schema) {
    this(schema, Junction.Type.AND);
  }

  private Query(Schema schema, Junction.Type rootJunctionType) {
    this.schema = schema;
    this.rootJunctionType = rootJunctionType;
  }

  public boolean isEmpty() {
    return criterions.isEmpty() && orders.isEmpty();
  }

  public boolean ordersPersisted() {
    return orders.stream().noneMatch(Order::isNonPersisted);
  }

  public void clearOrders() {
    orders.clear();
  }

  public Integer getFirstResult() {
    return skipPaging ? 0 : firstResult;
  }

  public Integer getMaxResults() {
    return skipPaging ? Integer.MAX_VALUE : maxResults;
  }

  public Query addOrder(Order... orders) {
    Stream.of(orders).filter(Objects::nonNull).forEach(this.orders::add);
    return this;
  }

  public Query addOrders(Collection<Order> orders) {
    this.orders.addAll(orders);
    return this;
  }

  public Query add(Restriction criterion) {
    this.criterions.add(criterion);
    return this;
  }

  public Query add(Restriction... criterions) {
    this.criterions.addAll(asList(criterions));
    return this;
  }

  public Query add(Collection<Restriction> criterions) {
    this.criterions.addAll(criterions);
    return this;
  }

  public Query setDefaultOrder() {
    if (!orders.isEmpty()) {
      return this;
    }

    if (schema.hasPersistedProperty("name")) {
      addOrder(Order.iasc(schema.getPersistedProperty("name")));
    }

    if (schema.hasPersistedProperty("id")) {
      addOrder(Order.asc(schema.getPersistedProperty("id")));
    }

    return this;
  }
}
