/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataintegrity;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableCollection;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.StreamSupport.stream;
import static org.hisp.dhis.commons.collection.ListUtils.getDuplicates;
import static org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue.toIssue;
import static org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue.toRefsList;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.ResourceLocation.CLASS_PATH;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.ResourceLocation.FILE_SYSTEM;
import static org.hisp.dhis.dataintegrity.DataIntegrityYamlReader.readDataIntegrityYaml;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.dataintegrity.DataIntegrityDetails.DataIntegrityIssue;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DefaultDataIntegrityService implements DataIntegrityService {
  private static final String FORMULA_SEPARATOR = "#";

  private final I18nManager i18nManager;

  private final LocationManager locationManager;

  private final ProgramRuleService programRuleService;

  private final ProgramRuleActionService programRuleActionService;

  private final ProgramRuleVariableService programRuleVariableService;

  private final DataElementService dataElementService;

  private final IndicatorService indicatorService;

  private final DataSetService dataSetService;

  private final OrganisationUnitService organisationUnitService;

  private final ValidationRuleService validationRuleService;

  private final ExpressionService expressionService;

  private final DataEntryFormService dataEntryFormService;

  private final ProgramIndicatorService programIndicatorService;

  private final CacheProvider cacheProvider;

  private final DataIntegrityStore dataIntegrityStore;

  private final SchemaService schemaService;

  private Cache<DataIntegritySummary> summaryCache;

  private Cache<DataIntegrityDetails> detailsCache;

  private final Set<String> runningSummaryChecks = ConcurrentHashMap.newKeySet();

  private final Set<String> runningDetailsChecks = ConcurrentHashMap.newKeySet();

  @PostConstruct
  public void init() {
    summaryCache = cacheProvider.createDataIntegritySummaryCache();
    detailsCache = cacheProvider.createDataIntegrityDetailsCache();
  }

  private static int alphabeticalOrder(DataIntegrityIssue a, DataIntegrityIssue b) {
    return a.getName().compareTo(b.getName());
  }

  private static List<DataIntegrityIssue> toSimpleIssueList(
      Stream<? extends IdentifiableObject> items) {
    return items
        .map(DataIntegrityIssue::toIssue)
        .sorted(DefaultDataIntegrityService::alphabeticalOrder)
        .toList();
  }

  @Nonnull
  @Override
  public Set<String> getRunningSummaryChecks() {
    return Set.copyOf(runningSummaryChecks);
  }

  @Nonnull
  @Override
  public Set<String> getRunningDetailsChecks() {
    return Set.copyOf(runningDetailsChecks);
  }

  @Nonnull
  @Override
  public Set<String> getCompletedSummaryChecks() {
    return stream(summaryCache.keys().spliterator(), false).collect(toUnmodifiableSet());
  }

  @Nonnull
  @Override
  public Set<String> getCompletedDetailsChecks() {
    return stream(detailsCache.keys().spliterator(), false).collect(toUnmodifiableSet());
  }

  // -------------------------------------------------------------------------
  // DataElement
  // -------------------------------------------------------------------------

  /** Gets all data elements which are not assigned to any data set. */
  List<DataIntegrityIssue> getDataElementsWithoutDataSet() {
    return toSimpleIssueList(dataElementService.getDataElementsWithoutDataSets().stream());
  }

  /** Gets all data elements which are not members of any groups. */
  List<DataIntegrityIssue> getDataElementsWithoutGroups() {
    return toSimpleIssueList(dataElementService.getDataElementsWithoutGroups().stream());
  }

  /**
   * Returns all data elements which are member of a data set but not part of either the custom form
   * or sections of the data set.
   */
  List<DataIntegrityIssue> getDataElementsInDataSetNotInForm() {
    List<DataIntegrityIssue> issues = new ArrayList<>();

    Collection<DataSet> dataSets = dataSetService.getAllDataSets();

    for (DataSet dataSet : dataSets) {
      if (!dataSet.getFormType().isDefault()) {
        Set<DataElement> formElements = new HashSet<>();

        if (dataSet.hasDataEntryForm()) {
          formElements.addAll(dataEntryFormService.getDataElementsInDataEntryForm(dataSet));
        } else if (dataSet.hasSections()) {
          formElements.addAll(dataSet.getDataElementsInSections());
        }

        Set<DataElement> dataSetElements = new HashSet<>(dataSet.getDataElements());

        dataSetElements.removeAll(formElements);

        if (!dataSetElements.isEmpty()) {
          issues.add(DataIntegrityIssue.toIssue(dataSet, dataSetElements));
        }
      }
    }

    return issues;
  }

  // -------------------------------------------------------------------------
  // DataSet
  // -------------------------------------------------------------------------

  List<DataIntegrityIssue> getDataSetsNotAssignedToOrganisationUnits() {
    return toSimpleIssueList(dataSetService.getDataSetsNotAssignedToOrganisationUnits().stream());
  }

  // -------------------------------------------------------------------------
  // Indicator
  // -------------------------------------------------------------------------

  /** Gets all indicators with identical numerator and denominator. */
  List<DataIntegrityIssue> getIndicatorsWithIdenticalFormulas() {
    List<DataIntegrityIssue> issues = new ArrayList<>();

    Map<String, List<Indicator>> byFormula =
        indicatorService.getAllIndicators().stream()
            .collect(
                groupingBy(
                    indicator ->
                        indicator.getNumerator() + FORMULA_SEPARATOR + indicator.getDenominator()));

    for (Entry<String, List<Indicator>> e : byFormula.entrySet()) {
      if (e.getValue().size() > 1) {
        issues.add(
            new DataIntegrityIssue(null, e.getKey(), null, toRefsList(e.getValue().stream())));
      }
    }
    return issues;
  }

  /** Gets all indicators which are not assigned to any groups. */
  List<DataIntegrityIssue> getIndicatorsWithoutGroups() {
    return toSimpleIssueList(indicatorService.getIndicatorsWithoutGroups().stream());
  }

  /** Gets all indicators with invalid indicator numerators. */
  List<DataIntegrityIssue> getInvalidIndicatorNumerators() {
    return getInvalidIndicators(Indicator::getNumerator);
  }

  /** Gets all indicators with invalid indicator denominators. */
  List<DataIntegrityIssue> getInvalidIndicatorDenominators() {
    return getInvalidIndicators(Indicator::getDenominator);
  }

  private List<DataIntegrityIssue> getInvalidIndicators(Function<Indicator, String> getter) {
    List<DataIntegrityIssue> issues = new ArrayList<>();
    I18n i18n = i18nManager.getI18n();

    for (Indicator indicator : indicatorService.getAllIndicators()) {
      ExpressionValidationOutcome result =
          expressionService.expressionIsValid(getter.apply(indicator), INDICATOR_EXPRESSION);

      if (!result.isValid()) {
        issues.add(toIssue(indicator, i18n.getString(result.getKey())));
      }
    }

    return issues;
  }

  /**
   * Gets all indicators units which are members of more than one group which enter into an
   * exclusive group set.
   */
  List<DataIntegrityIssue> getIndicatorsViolatingExclusiveGroupSets() {
    Collection<IndicatorGroupSet> groupSets = indicatorService.getAllIndicatorGroupSets();

    List<DataIntegrityIssue> issues = new ArrayList<>();

    for (IndicatorGroupSet groupSet : groupSets) {
      Collection<Indicator> duplicates = getDuplicates(new ArrayList<>(groupSet.getIndicators()));

      for (Indicator duplicate : duplicates) {
        issues.add(DataIntegrityIssue.toIssue(duplicate, duplicate.getGroups()));
      }
    }

    return issues;
  }

  // -------------------------------------------------------------------------
  // OrganisationUnit
  // -------------------------------------------------------------------------

  List<DataIntegrityIssue> getOrganisationUnitsWithCyclicReferences() {
    return toSimpleIssueList(
        organisationUnitService.getOrganisationUnitsWithCyclicReferences().stream());
  }

  // -------------------------------------------------------------------------
  // ValidationRule
  // -------------------------------------------------------------------------

  List<DataIntegrityIssue> getValidationRulesWithoutGroups() {
    return toSimpleIssueList(validationRuleService.getValidationRulesWithoutGroups().stream());
  }

  /** Gets all ValidationRules with invalid left side expressions. */
  List<DataIntegrityIssue> getInvalidValidationRuleLeftSideExpressions() {
    return getInvalidValidationRuleExpressions(ValidationRule::getLeftSide);
  }

  /** Gets all ValidationRules with invalid right side expressions. */
  List<DataIntegrityIssue> getInvalidValidationRuleRightSideExpressions() {
    return getInvalidValidationRuleExpressions(ValidationRule::getRightSide);
  }

  private List<DataIntegrityIssue> getInvalidValidationRuleExpressions(
      Function<ValidationRule, Expression> getter) {
    List<DataIntegrityIssue> issues = new ArrayList<>();
    I18n i18n = i18nManager.getI18n();

    for (ValidationRule rule : validationRuleService.getAllValidationRules()) {
      ExpressionValidationOutcome result =
          expressionService.expressionIsValid(
              getter.apply(rule).getExpression(), VALIDATION_RULE_EXPRESSION);

      if (!result.isValid()) {
        issues.add(toIssue(rule, i18n.getString(result.getKey())));
      }
    }
    return issues;
  }

  private void registerNonDatabaseIntegrityCheck(
      DataIntegrityCheckType type,
      Class<? extends IdentifiableObject> issueIdType,
      Supplier<List<DataIntegrityIssue>> check) {
    String name = type.getName();
    I18n i18n = i18nManager.getI18n(DataIntegrityService.class);
    BinaryOperator<String> info =
        (property, defaultValue) ->
            i18n.getString(format("data_integrity.%s.%s", name, property), defaultValue);

    try {
      Schema issueSchema = issueIdType == null ? null : schemaService.getDynamicSchema(issueIdType);
      String issueIdTypeName = issueSchema == null ? null : issueSchema.getPlural();
      checksByName.putIfAbsent(
          name,
          DataIntegrityCheck.builder()
              .name(name)
              .displayName(info.apply("name", name.replace('_', ' ')))
              .isSlow(true)
              .isProgrammatic(true)
              .severity(
                  DataIntegritySeverity.valueOf(
                      info.apply("severity", DataIntegritySeverity.WARNING.name()).toUpperCase()))
              .section(info.apply("section", "Other"))
              .description(info.apply("description", null))
              .introduction(info.apply("introduction", null))
              .recommendation(info.apply("recommendation", null))
              .issuesIdType(issueIdTypeName)
              .runDetailsCheck(
                  c -> {
                    Date startTime = new Date();
                    List<DataIntegrityIssue> issues = check.get();
                    return new DataIntegrityDetails(c, startTime, new Date(), null, issues);
                  })
              .runSummaryCheck(
                  c -> {
                    Date startTime = new Date();
                    List<DataIntegrityIssue> issues = check.get();
                    return new DataIntegritySummary(
                        c, startTime, new Date(), null, issues.size(), null);
                  })
              .build());
    } catch (Exception ex) {
      log.error("Failed to register data integrity check " + type, ex);
    }
  }

  /**
   * Maps all "hard coded" checks to their implementation method and registers a {@link
   * DataIntegrityCheck} to perform the method as {@link DataIntegrityDetails}.
   */
  public void initIntegrityChecks() {

    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.DATA_ELEMENTS_IN_DATA_SET_NOT_IN_FORM,
        DataSet.class,
        this::getDataElementsInDataSetNotInForm);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.INDICATORS_WITH_IDENTICAL_FORMULAS,
        null,
        this::getIndicatorsWithIdenticalFormulas);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.INDICATORS_WITH_INVALID_NUMERATOR,
        Indicator.class,
        this::getInvalidIndicatorNumerators);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.INDICATORS_WITH_INVALID_DENOMINATOR,
        Indicator.class,
        this::getInvalidIndicatorDenominators);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.ORG_UNITS_WITH_CYCLIC_REFERENCES,
        OrganisationUnit.class,
        this::getOrganisationUnitsWithCyclicReferences);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_LEFT_SIDE_EXPRESSION,
        ValidationRule.class,
        this::getInvalidValidationRuleLeftSideExpressions);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.VALIDATION_RULES_WITH_INVALID_RIGHT_SIDE_EXPRESSION,
        ValidationRule.class,
        this::getInvalidValidationRuleRightSideExpressions);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_EXPRESSIONS,
        ProgramIndicator.class,
        this::getInvalidProgramIndicatorExpressions);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_INDICATORS_WITH_INVALID_FILTERS,
        ProgramIndicator.class,
        this::getInvalidProgramIndicatorFilters);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_DATA_ELEMENT,
        Program.class,
        this::getProgramRuleVariablesWithNoDataElement);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_VARIABLES_WITHOUT_ATTRIBUTE,
        Program.class,
        this::getProgramRuleVariablesWithNoAttribute);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_DATA_OBJECT,
        ProgramRule.class,
        this::getProgramRuleActionsWithNoDataObject);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_NOTIFICATION,
        ProgramRule.class,
        this::getProgramRuleActionsWithNoNotificationTemplate);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_SECTION,
        ProgramRule.class,
        this::getProgramRuleActionsWithNoSectionId);
    registerNonDatabaseIntegrityCheck(
        DataIntegrityCheckType.PROGRAM_RULE_ACTIONS_WITHOUT_STAGE_ID,
        ProgramRule.class,
        this::getProgramRuleActionsWithNoProgramStageId);
  }

  Set<String> addSQLChecksToFlattedReport() {
    Set<String> checks = new LinkedHashSet<>();
    checks.add("organisation_units_without_groups");
    checks.add("data_elements_aggregate_no_groups");
    checks.add("data_elements_aggregate_with_different_period_types");
    checks.add("data_elements_without_datasets");
    checks.add("datasets_not_assigned_to_org_units");
    checks.add("data_elements_violating_exclusive_group_sets");
    checks.add("invalid_category_combos");
    checks.add("indicators_not_grouped");
    checks.add("periods_same_start_date_period_type");
    checks.add("orgunits_orphaned");
    checks.add("validation_rules_without_groups");
    checks.add("program_rules_without_condition");
    checks.add("program_rules_no_action");
    checks.add("program_rules_no_priority");
    checks.add("program_indicators_without_expression");
    checks.add("organisation_units_violating_exclusive_group_sets");
    checks.add("orgunits_compulsory_group_count");
    checks.add("indicators_violating_exclusive_group_sets");
    return checks;
  }

  /** Get all ProgramIndicators with invalid expressions. */
  List<DataIntegrityIssue> getInvalidProgramIndicatorExpressions() {
    return getInvalidProgramIndicators(
        ProgramIndicator::getExpression,
        pi -> !programIndicatorService.expressionIsValid(pi.getExpression()));
  }

  /** Get all ProgramIndicators with invalid filters. */
  List<DataIntegrityIssue> getInvalidProgramIndicatorFilters() {
    return getInvalidProgramIndicators(
        ProgramIndicator::getFilter, pi -> !programIndicatorService.filterIsValid(pi.getFilter()));
  }

  private List<DataIntegrityIssue> getInvalidProgramIndicators(
      Function<ProgramIndicator, String> property, Predicate<ProgramIndicator> filter) {
    List<ProgramIndicator> programIndicators =
        programIndicatorService.getAllProgramIndicators().stream().filter(filter).toList();

    List<DataIntegrityIssue> issues = new ArrayList<>();
    for (ProgramIndicator programIndicator : programIndicators) {
      String description = getInvalidExpressionDescription(property.apply(programIndicator));
      if (description != null) {
        issues.add(toIssue(programIndicator, description));
      }
    }
    return issues;
  }

  /**
   * @return all {@link ProgramRuleAction} which are not linked to any
   *     DataElement/TrackedEntityAttribute
   */
  List<DataIntegrityIssue> getProgramRuleActionsWithNoDataObject() {
    return groupActionsByProgramRule(
        programRuleActionService.getProgramActionsWithNoLinkToDataObject());
  }

  /**
   * @return all {@link ProgramRuleAction} which are not linked to any {@link
   *     org.hisp.dhis.notification.NotificationTemplate}
   */
  List<DataIntegrityIssue> getProgramRuleActionsWithNoNotificationTemplate() {
    return groupActionsByProgramRule(
        programRuleActionService.getProgramActionsWithNoLinkToNotification());
  }

  /**
   * @return all {@link ProgramRuleAction} which are not linked to any {@link
   *     org.hisp.dhis.program.ProgramStageSection}
   */
  List<DataIntegrityIssue> getProgramRuleActionsWithNoSectionId() {
    return groupActionsByProgramRule(
        programRuleActionService.getProgramRuleActionsWithNoSectionId());
  }

  /**
   * @return all {@link ProgramRuleAction} which are not linked to any {@link
   *     org.hisp.dhis.program.ProgramStage}
   */
  List<DataIntegrityIssue> getProgramRuleActionsWithNoProgramStageId() {
    return groupActionsByProgramRule(programRuleActionService.getProgramRuleActionsWithNoStageId());
  }

  /**
   * @return all {@link ProgramRuleVariable} which are not linked to any DataElement and grouped
   *     them by {@link Program}
   */
  List<DataIntegrityIssue> getProgramRuleVariablesWithNoDataElement() {
    return groupVariablesByProgram(programRuleVariableService.getVariablesWithNoDataElement());
  }

  /**
   * @return all {@link ProgramRuleVariable} which are not linked to any TrackedEntityAttribute and
   *     grouped them by {@link Program}
   */
  List<DataIntegrityIssue> getProgramRuleVariablesWithNoAttribute() {
    return groupVariablesByProgram(programRuleVariableService.getVariablesWithNoAttribute());
  }

  private String getInvalidExpressionDescription(String expression) {
    try {
      expressionService.getExpressionDescription(expression, INDICATOR_EXPRESSION);
    } catch (ParserException e) {
      return e.getMessage();
    }

    return null;
  }

  private static List<DataIntegrityIssue> groupVariablesByProgram(
      List<ProgramRuleVariable> variables) {
    return groupBy(ProgramRuleVariable::getProgram, variables);
  }

  private static List<DataIntegrityIssue> groupActionsByProgramRule(
      List<ProgramRuleAction> actions) {
    return groupBy(ProgramRuleAction::getProgramRule, actions);
  }

  private static <K extends IdentifiableObject, V extends IdentifiableObject>
      List<DataIntegrityIssue> groupBy(Function<V, K> property, Collection<V> values) {
    return values.stream().collect(groupingBy(property)).entrySet().stream()
        .map(e -> DataIntegrityIssue.toIssue(e.getKey(), e.getValue()))
        .toList();
  }

  /*
   * Configuration based data integrity checks
   */

  private final Map<String, DataIntegrityCheck> checksByName = new ConcurrentHashMap<>();

  private final AtomicBoolean configurationsAreLoaded = new AtomicBoolean(false);

  @Nonnull
  @Override
  public Collection<DataIntegrityCheck> getDataIntegrityChecks(Set<String> checks) {
    ensureConfigurationsAreLoaded();
    return checks.isEmpty()
        ? unmodifiableCollection(checksByName.values())
        : expandChecks(checks, false).stream().map(checksByName::get).toList();
  }

  @Nonnull
  @Override
  public Map<String, DataIntegritySummary> getSummaries(@Nonnull Set<String> checks, long timeout) {
    return getCached(checks, timeout, summaryCache);
  }

  // OBS! We intentionally do not open the transaction here to have each check
  // be independent
  @Override
  public void runSummaryChecks(@Nonnull Set<String> checks, JobProgress progress) {
    runDataIntegrityChecks(
        "Data Integrity summary checks",
        expandChecks(checks, true),
        progress,
        summaryCache,
        runningSummaryChecks,
        check -> check.getRunSummaryCheck().apply(check),
        (check, startTime, ex) ->
            new DataIntegritySummary(
                check, startTime, new Date(), errorMessage(check, ex), -1, null));
  }

  @Nonnull
  @Override
  public Map<String, DataIntegrityDetails> getDetails(@Nonnull Set<String> checks, long timeout) {
    return getCached(checks, timeout, detailsCache);
  }

  // OBS! We intentionally do not open the transaction here to have each check
  // be independent
  @Override
  public void runDetailsChecks(@Nonnull Set<String> checks, JobProgress progress) {
    runDataIntegrityChecks(
        "Data Integrity details checks",
        expandChecks(checks, true),
        progress,
        detailsCache,
        runningDetailsChecks,
        check -> check.getRunDetailsCheck().apply(check),
        (check, startTime, ex) ->
            new DataIntegrityDetails(
                check, startTime, new Date(), errorMessage(check, ex), List.of()));
  }

  private static String errorMessage(DataIntegrityCheck check, RuntimeException ex) {
    String message = "Check failed because an exception was thrown: " + ex.getMessage();
    log.error("Check " + check.getName() + " failed because an exception was thrown", ex);
    return message;
  }

  private <T> Map<String, T> getCached(Set<String> checks, long timeout, Cache<T> cache) {
    Set<String> names = expandChecks(checks, false);
    long giveUpTime = currentTimeMillis() + timeout;
    Map<String, T> resByName = new LinkedHashMap<>();
    boolean retry = false;
    do {
      if (retry) {
        try {
          Thread.sleep(Math.max(10, Math.min(50, (giveUpTime - currentTimeMillis()) / 2)));
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return resByName;
        }
      }
      for (String name : names) {
        if (!resByName.containsKey(name)) {
          cache.get(name).ifPresent(res -> resByName.put(name, res));
        }
      }
      retry = resByName.size() < names.size() && (timeout < 0 || currentTimeMillis() < giveUpTime);
    } while (retry);
    return resByName;
  }

  @FunctionalInterface
  private interface DataIntegrityCheckErrorHandler<T> {
    T createErrorReport(DataIntegrityCheck check, Date startTime, RuntimeException ex);
  }

  private <T> void runDataIntegrityChecks(
      String stageDesc,
      Set<String> checks,
      JobProgress progress,
      Cache<T> cache,
      Set<String> running,
      Function<DataIntegrityCheck, T> runCheck,
      DataIntegrityCheckErrorHandler<T> createErrorReport) {
    try {
      running.addAll(checks);
      progress.startingProcess("Data integrity check");
      progress.startingStage(stageDesc, checks.size(), SKIP_ITEM);
      progress.runStage(
          checks.stream()
              .map(checksByName::get)
              .filter(Objects::nonNull)
              .sorted(DataIntegrityCheck.FAST_TO_SLOW),
          DataIntegrityCheck::getDescription,
          check -> {
            Date startTime = new Date();
            T res;
            try {
              res = runCheck.apply(check);
            } catch (RuntimeException ex) {
              cache.put(check.getName(), createErrorReport.createErrorReport(check, startTime, ex));
              throw ex;
            } finally {
              running.remove(check.getName());
            }
            if (res != null) {
              check.addExecution(currentTimeMillis() - startTime.getTime());
              cache.put(check.getName(), res);
            }
          });
      progress.completedProcess(null);
    } finally {
      running.removeAll(checks);
    }
  }

  private Set<String> expandChecks(Set<String> names, boolean restricted) {
    ensureConfigurationsAreLoaded();

    if (CollectionUtils.isEmpty(names)) {
      return getDefaultChecks(restricted);
    }
    Set<String> expanded = new LinkedHashSet<>();

    for (String name : names) {
      if (name.toUpperCase().equals(name) && name.indexOf('_') < 0) {
        // assume it is a code
        checksByName.values().stream()
            .filter(check -> check.getCode().equals(name))
            .map(DataIntegrityCheck::getName)
            .forEach(expanded::add);
      } else if (name.contains("*")) {
        String pattern =
            name.toLowerCase()
                .replace('-', '_') // make uniform
                .replaceAll("[^*_a-z0-9]+", "") // sanitise against regex attacks
                .replace("*", ".*"); // expand regex wildcard match
        for (DataIntegrityCheck check : checksByName.values()) {
          if (check.getName().matches(pattern)) {
            expanded.add(check.getName());
          }
        }
      } else {
        expanded.add(name.toLowerCase().replace('-', '_'));
      }
    }
    // Filter out any checks which actually do not exist but have been requested
    expanded.retainAll(
        getDataIntegrityChecks(Set.of()).stream()
            .map(check -> check.getName().toLowerCase().replace('-', '_'))
            .collect(toUnmodifiableSet()));

    return expanded;
  }

  private Set<String> getDefaultChecks(boolean restricted) {
    ensureConfigurationsAreLoaded();

    Predicate<DataIntegrityCheck> filter =
        restricted ? not(DataIntegrityCheck::isSlow) : check -> true;
    return checksByName.values().stream()
        .filter(filter)
        .map(DataIntegrityCheck::getName)
        .collect(toUnmodifiableSet());
  }

  private void ensureConfigurationsAreLoaded() {
    if (configurationsAreLoaded.compareAndSet(false, true)) {
      // load system-packaged data integrity checks
      loadChecks(CLASS_PATH, "data-integrity-checks.yaml", "data-integrity-checks");

      // programmatic checks
      initIntegrityChecks();

      // load user-packaged custom data integrity checks
      try {
        String dhis2Home = locationManager.getExternalDirectoryPath();
        loadChecks(
            FILE_SYSTEM,
            dhis2Home + "/custom-data-integrity-checks.yaml",
            dhis2Home + "/custom-data-integrity-checks");
      } catch (LocationManagerException ex) {
        log.warn(
            "Could not get DHIS2_HOME external directory. No custom data integrity checks loaded.");
      }
    }
  }

  private void loadChecks(
      DataIntegrityYamlReader.ResourceLocation resourceLocation,
      String yamlFileChecks,
      String checksDir) {
    I18n i18n = i18nManager.getI18n(DataIntegrityService.class);
    readDataIntegrityYaml(
        new DataIntegrityRecord(
            resourceLocation,
            yamlFileChecks,
            checksDir,
            addToChecks,
            (property, defaultValue) ->
                i18n.getString(format("data_integrity.%s", property), defaultValue),
            sql -> check -> dataIntegrityStore.querySummary(check, sql),
            sql -> check -> dataIntegrityStore.queryDetails(check, sql)));
  }

  /**
   * Consumer that adds a {@link DataIntegrityCheck} to a map. It only adds the {@link
   * DataIntegrityCheck} to the map if:
   *
   * <ol>
   *   <li>the {@link DataIntegrityCheck} code is unique
   *   <li>the map does not already have a key with the same {@link DataIntegrityCheck} name
   * </ol>
   */
  private final Consumer<DataIntegrityCheck> addToChecks =
      check -> {
        String checkCode = DataIntegrityCheck.getCodeFromName(check.getName());
        Set<String> checkCodes =
            checksByName.keySet().stream()
                .map(DataIntegrityCheck::getCodeFromName)
                .collect(Collectors.toSet());
        if (!checkCodes.contains(checkCode)) {
          DataIntegrityCheck dataIntegrityCheck = checksByName.putIfAbsent(check.getName(), check);
          if (dataIntegrityCheck != null) {
            log.warn(
                "Data Integrity Check `{}` not added as a check with that name already exists",
                check.getName());
          }
        } else
          log.warn(
              "Data Integrity Check `{}` not added as a check with the code `{}` already exists",
              check.getName(),
              check.getCode());
      };

  record DataIntegrityRecord(
      DataIntegrityYamlReader.ResourceLocation resourceLocation,
      String yamlFileChecks,
      String checksDir,
      Consumer<DataIntegrityCheck> adder,
      BinaryOperator<String> info,
      Function<String, Function<DataIntegrityCheck, DataIntegritySummary>> sqlToSummary,
      Function<String, Function<DataIntegrityCheck, DataIntegrityDetails>> sqlToDetails) {}
}
