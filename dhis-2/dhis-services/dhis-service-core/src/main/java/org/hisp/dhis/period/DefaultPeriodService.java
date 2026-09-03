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
package org.hisp.dhis.period;

import static java.lang.System.currentTimeMillis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IndirectTransactional;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.period.PeriodTypeStore.Labels;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.period.PeriodService")
public class DefaultPeriodService implements PeriodService {

  private final PeriodStore periodStore;
  private final PeriodTypeStore periodTypeStore;
  private final RelativePeriodStore relativePeriodStore;
  private final I18nManager i18nManager;

  // -------------------------------------------------------------------------
  // Period
  // -------------------------------------------------------------------------

  @Override
  @IndirectTransactional
  public void addPeriod(Period period) {
    periodStore.addPeriod(period);
  }

  @Override
  @Transactional(readOnly = true)
  public Period getPeriod(long id) {
    return periodStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Period getPeriod(String isoPeriod) {
    Period period = Period.ofNullable(isoPeriod);
    return period == null ? null : periodStore.reloadPeriod(period);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getAllPeriods() {
    return periodStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getPeriodsBetweenDates(Date startDate, Date endDate) {
    return periodStore.getPeriodsBetweenDates(startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getPeriodsBetweenDates(PeriodType periodType, Date startDate, Date endDate) {
    return periodStore.getPeriodsBetweenDates(periodType, startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getIntersectingPeriods(Date startDate, Date endDate) {
    return periodStore.getIntersectingPeriods(startDate, endDate);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getIntersectionPeriods(Collection<Period> periods) {
    Set<Period> intersecting = new HashSet<>();

    for (Period period : periods) {
      intersecting.addAll(getIntersectingPeriods(period.getStartDate(), period.getEndDate()));
    }

    return new ArrayList<>(intersecting);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Period> getInclusivePeriods(Period period, Collection<Period> periods) {
    List<Period> immutablePeriods = new ArrayList<>(periods);

    Iterator<Period> iterator = immutablePeriods.iterator();

    while (iterator.hasNext()) {
      Period iterated = iterator.next();

      if (!DateUtils.between(iterated.getStartDate(), period.getStartDate(), period.getEndDate())
          || !DateUtils.between(
              iterated.getEndDate(), period.getStartDate(), period.getEndDate())) {
        iterator.remove();
      }
    }

    return immutablePeriods;
  }

  @Override
  @IndirectTransactional
  public List<Period> reloadPeriods(Collection<Period> periods) {
    return periods.stream().map(periodStore::reloadForceAddPeriod).toList();
  }

  @Override
  @Transactional
  public List<Period> getPeriods(Period lastPeriod, int previousPeriods) {
    List<Period> periods = new ArrayList<>(previousPeriods);

    lastPeriod = periodStore.reloadForceAddPeriod(lastPeriod);

    PeriodType periodType = lastPeriod.getPeriodType();

    for (int i = 0; i < previousPeriods; ++i) {
      Period pe = periodStore.getPeriodFromDates(lastPeriod.getStartDate(), periodType);
      periods.add(pe != null ? pe : lastPeriod);
      lastPeriod = periodType.getPreviousPeriod(lastPeriod);
    }

    Collections.reverse(periods);

    return periods;
  }

  @Override
  @IndirectTransactional
  public Period reloadPeriod(Period period) {
    return periodStore.reloadForceAddPeriod(period);
  }

  /**
   * Fix issue DHIS2-7539 If period doesn't exist in cache and database. Need to add and sync with
   * database right away in a separate session/transaction. Otherwise will get foreign key
   * constraint error in subsequence calls of batch.flush()
   */
  @Override
  @IndirectTransactional
  public Period reloadIsoPeriodInStatelessSession(String isoPeriod) {
    return reloadPeriod(Period.of(isoPeriod));
  }

  @Override
  @IndirectTransactional
  public Period reloadIsoPeriod(String isoPeriod) {
    return reloadPeriod(Period.of(isoPeriod));
  }

  @Override
  @IndirectTransactional
  public List<Period> reloadIsoPeriods(List<String> isoPeriods) {
    return isoPeriods.stream().map(this::reloadIsoPeriod).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public int getDayInPeriod(Period period, Date date) {
    int days =
        (int)
            TimeUnit.DAYS.convert(
                date.getTime() - period.getStartDate().getTime(), TimeUnit.MILLISECONDS);

    return Math.min(Math.max(0, days), period.getDaysInPeriod());
  }

  // -------------------------------------------------------------------------
  // PeriodType
  // -------------------------------------------------------------------------

  private static final Map<Locale, PeriodTypeCacheEntry> PERIOD_TYPES_CACHE =
      new ConcurrentHashMap<>();

  private record PeriodTypeCacheEntry(PeriodTypes types, long validUntil) {}

  @Override
  @Transactional(readOnly = true)
  public PeriodType getPeriodTypeByName(String name) {
    return PeriodType.getPeriodTypeByName(name);
  }

  @Override
  @IndirectTransactional
  public PeriodType getPeriodTypeByClass(Class<? extends PeriodType> periodType) {
    PeriodType type = PeriodType.getPeriodTypeByClass(periodType);
    if (type == null) throw new IllegalArgumentException("Unknown period type: " + periodType);
    periodTypeStore.addPeriodType(type);
    return type;
  }

  @Override
  @IndirectTransactional
  public PeriodType reloadPeriodType(PeriodType periodType) {
    periodTypeStore.addPeriodType(periodType);
    return periodType;
  }

  @Override
  @IndirectTransactional
  public boolean updatePeriodTypeLabel(
      @Nonnull PeriodTypeEnum name, @Nonnull Collection<Translation> translations) {
    translations.forEach(t -> PERIOD_TYPES_CACHE.remove(t.getLocale()));
    return periodTypeStore.updateLabel(name, translations);
  }

  @Override
  @IndirectTransactional
  public boolean updatePeriodTypeLabel(
      @Nonnull PeriodTypeEnum name, @CheckForNull String label, @CheckForNull Locale locale) {
    if (locale != null) {
      PERIOD_TYPES_CACHE.remove(locale);
    } else {
      PERIOD_TYPES_CACHE.clear();
    }
    return periodTypeStore.updateLabel(name, label, locale);
  }

  @Override
  @IndirectTransactional
  public PeriodTypes getAllPeriodTypes(@Nonnull Locale locale) {
    return PERIOD_TYPES_CACHE.compute(
            locale,
            (k, v) -> {
              if (v != null && v.validUntil > currentTimeMillis()) return v;
              PeriodTypes types = reloadAllPeriodTypes(k);
              return new PeriodTypeCacheEntry(
                  types, currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
            })
        .types;
  }

  @Nonnull
  private PeriodTypes reloadAllPeriodTypes(Locale locale) {
    Map<PeriodTypeEnum, Labels> labelsByType = new HashMap<>();
    periodTypeStore.getAllLabels().forEach(e -> labelsByType.put(e.name(), e));

    I18n i18n = i18nManager.getI18n(locale);
    List<PeriodType> types = PeriodType.getAvailablePeriodTypes();
    List<PeriodTypes.PeriodTypeEntry> entries = new ArrayList<>(types.size());
    for (PeriodType t : types) {
      PeriodTypeEnum name = t.getPeriodTypeEnum();
      PeriodTypeStore.Labels l = labelsByType.get(name);
      PeriodTypes.Labels labels =
          PeriodTypes.Labels.of(
              locale,
              i18n.getString(name.getName(), name.getName()),
              l == null ? null : l.label(),
              l == null ? null : l.translations());
      Map<RelativePeriodEnum, PeriodTypes.Labels> relativePeriods = Map.of();

      PeriodTypes.PeriodTypeEntry e =
          new PeriodTypes.PeriodTypeEntry(
              name,
              t.getIso8601Duration(),
              t.getIsoFormat(),
              t.getFrequencyOrder(),
              labels,
              relativePeriods);
      entries.add(e);
    }
    return new PeriodTypes(locale, entries);
  }

  // -------------------------------------------------------------------------
  // PeriodType
  // -------------------------------------------------------------------------

}
