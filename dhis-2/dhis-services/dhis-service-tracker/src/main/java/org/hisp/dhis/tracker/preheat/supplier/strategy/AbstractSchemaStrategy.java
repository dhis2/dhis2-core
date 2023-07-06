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
package org.hisp.dhis.tracker.preheat.supplier.strategy;

import static org.hisp.dhis.tracker.TrackerIdentifierCollector.ID_WILDCARD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.hisp.dhis.tracker.preheat.mappers.CopyMapper;
import org.hisp.dhis.tracker.preheat.mappers.PreheatMapper;
import org.hisp.dhis.user.User;
import org.mapstruct.factory.Mappers;

/**
 * Abstract Tracker Preheat strategy that applies to strategies that employ the generic {@link
 * QueryService} to fetch data (mostly for Metadata classes)
 *
 * @author Luciano Fiandesio
 */
public abstract class AbstractSchemaStrategy implements ClassBasedSupplierStrategy {
  protected final SchemaService schemaService;

  private final QueryService queryService;

  private final IdentifiableObjectManager manager;

  private final PreheatCacheService cache;

  public AbstractSchemaStrategy(
      SchemaService schemaService,
      QueryService queryService,
      IdentifiableObjectManager manager,
      PreheatCacheService preheatCacheService) {
    this.schemaService = schemaService;
    this.queryService = queryService;
    this.manager = manager;
    this.cache = preheatCacheService;
  }

  @Override
  public void add(
      TrackerImportParams params, List<List<String>> splitList, TrackerPreheat preheat) {
    TrackerIdSchemeParam idSchemeParam = params.getIdSchemes().getByClass(getSchemaClass());
    Schema schema = schemaService.getDynamicSchema(getSchemaClass());

    queryForIdentifiableObjects(preheat, schema, idSchemeParam, splitList, mapper());
  }

  protected Class<?> getSchemaClass() {
    return getClass().getAnnotation(StrategyFor.class).value();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void queryForIdentifiableObjects(
      TrackerPreheat preheat,
      Schema schema,
      TrackerIdSchemeParam idSchemeParam,
      List<List<String>> splitList,
      Class<? extends PreheatMapper> mapper) {

    TrackerIdScheme idScheme = idSchemeParam.getIdScheme();
    for (List<String> ids : splitList) {
      List<? extends IdentifiableObject> objects;

      if (TrackerIdScheme.ATTRIBUTE.equals(idScheme)) {
        Attribute attribute = new Attribute();
        attribute.setUid(idSchemeParam.getValue());
        objects =
            manager.getAllByAttributeAndValues(
                (Class<? extends IdentifiableObject>) schema.getKlass(), attribute, ids);
      } else {
        objects = cacheAwareFetch(preheat.getUser(), schema, idSchemeParam, ids, mapper);
      }

      preheat.put(idSchemeParam, objects);
    }
  }

  private String buildCacheKey(Schema schema) {
    return schema.getKlass().getSimpleName();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<IdentifiableObject> cacheAwareFetch(
      User user,
      Schema schema,
      TrackerIdSchemeParam idSchemeParam,
      List<String> ids,
      Class<? extends PreheatMapper> mapper) {
    TrackerIdScheme idScheme = idSchemeParam.getIdScheme();

    List<IdentifiableObject> objects;
    final String cacheKey = buildCacheKey(schema);

    if (isCacheable()) // check if this strategy requires caching
    {
      if (isLoadAllEntities(ids)) {
        return map(cacheAndReturnLookupData(schema), mapper);
      } else {
        Map<String, IdentifiableObject> foundInCache = new HashMap<>();
        for (String id : ids) {
          // is the object reference by the given id in cache?
          cache
              .get(cacheKey, id)
              .ifPresent(identifiableObject -> foundInCache.put(id, identifiableObject));
        }

        // is there any object which was not found in cache?
        if (ids.size() > foundInCache.size()) {
          // remove from the list of ids the ids found in cache
          ids.removeAll(foundInCache.keySet());

          // execute the query, fetching only the ids which are not in
          // cache
          objects =
              map(
                  (List<IdentifiableObject>)
                      queryService.query(buildQuery(schema, user, idScheme, ids)),
                  mapper);

          // put objects in query based on given scheme. If the key
          // can't get resolved, send null to the
          // cacheService, which will ignore the entry
          objects.forEach(
              o ->
                  cache.put(
                      cacheKey, idSchemeParam.getIdentifier(o), o, getCacheTTL(), getCapacity()));

          // add back the cached objects to the final list
          objects.addAll(foundInCache.values());
        } else {
          objects = new ArrayList<>(foundInCache.values());
        }
      }
    } else {
      objects =
          isLoadAllEntities(ids)
              ? manager.getAll((Class<IdentifiableObject>) schema.getKlass())
              : map(
                  (List<IdentifiableObject>)
                      queryService.query(buildQuery(schema, user, idScheme, ids)),
                  mapper);
    }

    return objects;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private List<IdentifiableObject> map(
      List<IdentifiableObject> objects, Class<? extends PreheatMapper> mapper) {

    if (mapper.isAssignableFrom(CopyMapper.class)) {
      return objects;
    } else {
      return objects.stream()
          .map(o -> Mappers.getMapper(mapper).map(o))
          .map(IdentifiableObject.class::cast)
          .collect(Collectors.toList());
    }
  }

  private Query buildQuery(Schema schema, User user, TrackerIdScheme idScheme, List<String> ids) {
    Query query = Query.from(schema);
    query.setUser(user);
    query.add(generateRestrictionFromIdentifiers(idScheme, ids));
    query.setDefaults(Defaults.INCLUDE);

    return query;
  }

  private Restriction generateRestrictionFromIdentifiers(
      TrackerIdScheme idScheme, List<String> ids) {
    if (TrackerIdScheme.CODE.equals(idScheme)) {
      return Restrictions.in("code", ids);
    } else if (TrackerIdScheme.NAME.equals(idScheme)) {
      return Restrictions.in("name", ids);
    } else {
      return Restrictions.in("id", ids);
    }
  }

  @SuppressWarnings("unchecked")
  private List<IdentifiableObject> cacheAndReturnLookupData(Schema schema) {
    List<IdentifiableObject> objects;
    if (cache.hasKey(buildCacheKey(schema)) && !cache.getAll(buildCacheKey(schema)).isEmpty()) {
      objects = new ArrayList<>(cache.getAll(buildCacheKey(schema)));

    } else {
      objects = manager.getAll((Class<IdentifiableObject>) schema.getKlass());

      objects.forEach(
          rt ->
              cache.put(
                  HibernateProxyUtils.getRealClass(rt).getSimpleName(),
                  rt.getUid(),
                  rt,
                  getCacheTTL(),
                  getCapacity()));
    }

    return objects;
  }

  private boolean isLoadAllEntities(List<String> ids) {
    return ids.size() == 1 && ids.contains(ID_WILDCARD);
  }

  @SuppressWarnings("rawtypes")
  private Class<? extends PreheatMapper> mapper() {
    return getClass().getAnnotation(StrategyFor.class).mapper();
  }

  private boolean isCacheable() {
    return getClass().getAnnotation(StrategyFor.class).cache();
  }

  private int getCacheTTL() {
    return getClass().getAnnotation(StrategyFor.class).ttl();
  }

  private long getCapacity() {
    return getClass().getAnnotation(StrategyFor.class).capacity();
  }
}
