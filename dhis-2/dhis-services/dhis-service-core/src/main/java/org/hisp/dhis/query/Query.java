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
import org.hisp.dhis.user.UserDetails;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Query<T extends IdentifiableObject> {

  private final List<Filter> filters = new ArrayList<>();

  @Getter private final Class<T> objectType;

  @ToString.Exclude private UserDetails currentUserDetails;

  private String locale;

  private final List<Order> orders = new ArrayList<>();

  /** Order by id and name if not other orders are defined and those properties exist */
  private boolean defaultOrders;

  /** true, if the schema of the {@link #objectType} has a persisted short name property */
  private boolean shortNamePersisted;

  private boolean skipPaging;

  private boolean skipSharing;

  private boolean dataSharing;

  private Integer firstResult = 0;

  private Integer maxResults = Integer.MAX_VALUE;

  private final Junction.Type rootJunctionType;

  private Defaults defaults = Defaults.EXCLUDE;

  private boolean cacheable = true;

  @ToString.Exclude private List<T> objects;

  public static <T extends IdentifiableObject> Query<T> of(Class<T> objectType) {
    return new Query<>(objectType);
  }

  public static <T extends IdentifiableObject> Query<T> of(
      Class<T> objectType, Junction.Type rootJunction) {
    return new Query<>(objectType, rootJunction);
  }

  public static <T extends IdentifiableObject> Query<T> emptyOf(Query<T> query) {
    Query<T> copy = Query.of(query.getObjectType(), query.getRootJunctionType());
    // context attributes
    copy.setShortNamePersisted(query.isShortNamePersisted());
    copy.setDefaultOrders(query.isDefaultOrders());
    copy.setSkipSharing(query.isSkipSharing());
    copy.setCurrentUserDetails(query.getCurrentUserDetails());
    copy.setLocale(query.getLocale());
    return copy;
  }

  public static <T extends IdentifiableObject> Query<T> copyOf(Query<T> query) {
    Query<T> copy = Query.of(query.getObjectType(), query.getRootJunctionType());
    // context attributes
    copy.setShortNamePersisted(query.isShortNamePersisted());
    copy.setDefaultOrders(query.isDefaultOrders());
    copy.setSkipSharing(query.isSkipSharing());
    copy.setCurrentUserDetails(query.getCurrentUserDetails());
    copy.setLocale(query.getLocale());
    // specific attributes
    copy.addOrders(query.getOrders());
    copy.setFirstResult(query.getFirstResult());
    copy.setMaxResults(query.getMaxResults());
    copy.add(query.getFilters());
    copy.setObjects(query.getObjects());

    return copy;
  }

  private Query(Class<T> objectType) {
    this(objectType, Junction.Type.AND);
  }

  private Query(Class<T> objectType, Junction.Type rootJunctionType) {
    this.objectType = objectType;
    this.rootJunctionType = rootJunctionType;
  }

  public boolean isEmpty() {
    return filters.isEmpty() && orders.isEmpty();
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

  public Query<T> addOrder(Order... orders) {
    Stream.of(orders).filter(Objects::nonNull).forEach(this.orders::add);
    return this;
  }

  public Query<T> addOrders(Collection<Order> orders) {
    this.orders.addAll(orders);
    return this;
  }

  public Query<T> add(Filter filter) {
    this.filters.add(filter);
    return this;
  }

  public Query<T> add(Filter... filters) {
    this.filters.addAll(asList(filters));
    return this;
  }

  public Query<T> add(Collection<Filter> filters) {
    this.filters.addAll(filters);
    return this;
  }

  public Query<T> setDefaultOrder() {
    if (orders.isEmpty()) {
      defaultOrders = true;
    }
    return this;
  }
}
