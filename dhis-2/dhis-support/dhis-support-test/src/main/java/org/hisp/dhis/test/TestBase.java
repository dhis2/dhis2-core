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
package org.hisp.dhis.test;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.common.DataDimensionType.DISAGGREGATION;
import static org.hisp.dhis.visualization.VisualizationType.PIVOT_TABLE;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryDimension;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboGenerateService;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitDescendants;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.design.Column;
import org.hisp.dhis.dashboard.design.Layout;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.Api;
import org.hisp.dhis.dataexchange.aggregate.Filter;
import org.hisp.dhis.dataexchange.aggregate.Source;
import org.hisp.dhis.dataexchange.aggregate.SourceParams;
import org.hisp.dhis.dataexchange.aggregate.SourceRequest;
import org.hisp.dhis.dataexchange.aggregate.Target;
import org.hisp.dhis.dataexchange.aggregate.TargetRequest;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.notifications.DataSetNotificationRecipient;
import org.hisp.dhis.dataset.notifications.DataSetNotificationTemplate;
import org.hisp.dhis.dataset.notifications.DataSetNotificationTrigger;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.fileresource.ExternalFileResource;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.hibernate.HibernateService;
import org.hisp.dhis.icon.Icon;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.MapViewRenderingStrategy;
import org.hisp.dhis.mapping.ThematicMapType;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorGroup;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsPeriodBoundaryType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramSection;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.setting.SessionUserSettings;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewType;
import org.hisp.dhis.test.api.TestCategoryMetadata;
import org.hisp.dhis.test.utils.Dxf2NamespaceResolver;
import org.hisp.dhis.test.utils.RelationshipUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityfilter.EntityQueryCriteria;
import org.hisp.dhis.trackedentityfilter.TrackedEntityFilter;
import org.hisp.dhis.trackerdataview.TrackerDataView;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.hisp.dhis.visualization.Visualization;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.xml.sax.InputSource;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@ActiveProfiles(profiles = {"test"})
public abstract class TestBase {
  protected static final String BASE_UID = "abcdefghij";

  protected static final String BASE_IN_UID = "inabcdefgh";
  protected static final String BASE_IN_TYPE_UID = "IntY123abg";

  protected static final String BASE_DE_UID = "deabcdefgh";

  protected static final String BASE_DS_UID = "dsabcdefgh";

  protected static final String BASE_OU_UID = "ouabcdefgh";

  protected static final String BASE_COC_UID = "cuabcdefgh";

  protected static final String BASE_USER_UID = "userabcdef";

  protected static final String BASE_USER_GROUP_UID = "ugabcdefgh";

  protected static final String BASE_PG_UID = "pgabcdefgh";

  protected static final String BASE_PR_UID = "prabcdefgh";

  protected static final String BASE_TE_UID = "teibcdefgh";

  protected static final String BASE_PREDICTOR_GROUP_UID = "predictorg";

  private static final String EXT_TEST_DIR =
      System.getProperty("user.home") + File.separator + "dhis2_test_dir";

  public static final String ADMIN_USER_UID = "M5zQapPyTZI";

  public static final String DEFAULT_USERNAME = "admin";

  public static final String DEFAULT_ADMIN_PASSWORD = "district";

  private static final String PROGRAM_RULE_VARIABLE = "ProgramRuleVariable";

  protected static final String FIRST_NAME = "FirstName";

  protected static final String SURNAME = "Surname";

  private static Date date;

  protected static final double DELTA = 0.01;
  private int categoryCounter = 1;

  // -------------------------------------------------------------------------
  // Service references
  // -------------------------------------------------------------------------

  protected UserService userService;

  @Autowired private UserSettingsService userSettingsService;

  protected RenderService renderService;

  @Autowired(required = false)
  protected CategoryService internalCategoryService;

  @Autowired(required = false)
  protected CategoryOptionComboGenerateService categoryOptionComboGenerateService;

  @Autowired protected HibernateService hibernateService;

  protected static CategoryService categoryService;

  @PostConstruct
  protected void initServices() {
    categoryService = internalCategoryService;
  }

  static {
    DateTime dateTime = new DateTime(1970, 1, 1, 0, 0);
    date = dateTime.toDate();
  }

  // -------------------------------------------------------------------------
  // Convenience methods
  // -------------------------------------------------------------------------

  public User getCurrentUser() {
    return userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
  }

  /**
   * Creates a date.
   *
   * @param year the year.
   * @param month the month.
   * @param day the day of month.
   * @return a date.
   */
  public static Date getDate(int year, int month, int day) {
    LocalDateTime dateTime = new LocalDateTime(year, month, day, 0, 0);
    return dateTime.toDate();
  }

  /**
   * Creates a date.
   *
   * @param s a string representation of a date
   * @return a date.
   */
  public static Date getDate(String s) {
    DateTime dateTime = new DateTime(s);
    return dateTime.toDate();
  }

  /**
   * Creates a date. Alias for {@code getDate}.
   *
   * @param year the year.
   * @param month the month.
   * @param day the day of month.
   * @return a date.
   */
  public static Date date(int year, int month, int day) {
    return getDate(year, month, day);
  }

  /**
   * Creates a date.
   *
   * @param day the day of the year.
   * @return a date.
   */
  public Date getDay(int day) {
    DateTime dataTime = DateTime.now();
    dataTime = dataTime.withTimeAtStartOfDay();
    dataTime = dataTime.withDayOfYear(day);

    return dataTime.toDate();
  }

  /**
   * Converts a {@link Date} into a {@link LocalDate}.
   *
   * @param date the {@link Date}
   * @return the {@link LocalDate} object
   * @throws NullPointerException if the given date is null
   */
  public LocalDate toLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  /**
   * Compares two collections for equality. This method does not check for the implementation type
   * of the collection in contrast to the native equals method. This is useful for black-box testing
   * where one will not know the implementation type of the returned collection for a method.
   *
   * @param actual the actual collection to check.
   * @param reference the reference objects to check against.
   * @return true if the collections are equal, false otherwise.
   */
  public static boolean equals(Collection<?> actual, Object... reference) {
    final Collection<Object> collection = new HashSet<>();

    Collections.addAll(collection, reference);

    if (actual == collection) {
      return true;
    }

    if (actual == null) {
      return false;
    }

    if (actual.size() != collection.size()) {
      log.warn(
          "Actual collection has different size compared to reference collection: "
              + actual.size()
              + " / "
              + collection.size());
      return false;
    }

    for (Object object : actual) {
      if (!collection.contains(object)) {
        log.warn("Object in actual collection not part of reference collection: " + object);
        return false;
      }
    }

    for (Object object : collection) {
      if (!actual.contains(object)) {
        log.warn("Object in reference collection not part of actual collection: " + object);
        return false;
      }
    }

    return true;
  }

  public static String message(Object expected) {
    return "Expected was: " + ((expected != null) ? "[" + expected.toString() + "]" : "[null]");
  }

  public static String message(Object expected, Object actual) {
    return message(expected)
        + " Actual was: "
        + ((actual != null) ? "[" + actual.toString() + "]" : "[null]");
  }

  /**
   * Asserts that a {@link IllegalQueryException} is thrown with the given {@link ErrorCode}.
   *
   * @param exception the {@link IllegalQueryException}.
   * @param errorCode the {@link ErrorCode}.
   */
  public static void assertIllegalQueryEx(IllegalQueryException exception, ErrorCode errorCode) {
    assertThat(errorCode, is(exception.getErrorCode()));
  }

  // -------------------------------------------------------------------------
  // Create object methods
  // -------------------------------------------------------------------------

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static AggregateDataExchange getAggregateDataExchange(char uniqueCharacter) {
    SourceParams sourceParams =
        new SourceParams()
            .setPeriodTypes(List.of(PeriodTypeEnum.MONTHLY, PeriodTypeEnum.QUARTERLY));

    SourceRequest sourceRequest =
        new SourceRequest()
            .setName("RequestA")
            .setVisualization("JHKuBWP20RO")
            .setDx(newArrayList("LrDpG50RAU9", "uR5HCiJhQ1w"))
            .setPe(newArrayList("202201", "202202"))
            .setOu(newArrayList("G9BuXqtNeeb", "jDgiLmYwPDm"))
            .setFilters(
                newArrayList(
                    new Filter()
                        .setDimension("MuTwGW0BI4o")
                        .setItems(newArrayList("v9oULMMdmzE", "eJHJ0bfDCEO")),
                    new Filter()
                        .setDimension("dAOgE7mgysJ")
                        .setItems(newArrayList("rbE2mZX86AA", "XjOFfrPwake"))))
            .setInputIdScheme(IdScheme.UID.name())
            .setOutputIdScheme(IdScheme.UID.name());

    Source source = new Source().setParams(sourceParams).setRequests(newArrayList(sourceRequest));

    Api api =
        new Api()
            .setUrl("https://play.dhis2.org/demo")
            .setUsername(DEFAULT_USERNAME)
            .setPassword(DEFAULT_ADMIN_PASSWORD);

    TargetRequest targetRequest = new TargetRequest().setIdScheme(IdScheme.UID.name());

    Target target = new Target().setApi(api).setType(TargetType.EXTERNAL).setRequest(targetRequest);

    AggregateDataExchange exchange = new AggregateDataExchange();
    exchange.setAutoFields();
    exchange.setName("DataExchange" + uniqueCharacter);
    exchange.setSource(source);
    exchange.setTarget(target);
    return exchange;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static DataElement createDataElement(char uniqueCharacter) {
    return createDataElement(uniqueCharacter, null);
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param categoryCombo The category combo.
   */
  public static DataElement createDataElement(char uniqueCharacter, CategoryCombo categoryCombo) {
    DataElement dataElement = new DataElement();
    dataElement.setAutoFields();

    dataElement.setUid(BASE_DE_UID + uniqueCharacter);
    dataElement.setName("DataElement" + uniqueCharacter);
    dataElement.setShortName("DataElementShort" + uniqueCharacter);
    dataElement.setCode("DataElementCode" + uniqueCharacter);
    dataElement.setDescription("DataElementDescription" + uniqueCharacter);
    dataElement.setValueType(ValueType.INTEGER);
    dataElement.setDomainType(DataElementDomain.AGGREGATE);
    dataElement.setAggregationType(AggregationType.SUM);
    dataElement.setZeroIsSignificant(false);

    if (categoryCombo != null) {
      dataElement.setCategoryCombo(categoryCombo);
    } else if (categoryService != null) {
      dataElement.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    }

    return dataElement;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param valueType The value type.
   * @param aggregationType The aggregation type.
   */
  public static DataElement createDataElement(
      char uniqueCharacter, ValueType valueType, AggregationType aggregationType) {
    DataElement dataElement = createDataElement(uniqueCharacter);
    dataElement.setValueType(valueType);
    dataElement.setAggregationType(aggregationType);
    return dataElement;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param valueType The value type.
   * @param aggregationType The aggregation type.
   * @param domainType The domain type.
   */
  public static DataElement createDataElement(
      char uniqueCharacter,
      ValueType valueType,
      AggregationType aggregationType,
      DataElementDomain domainType) {
    DataElement dataElement = createDataElement(uniqueCharacter);
    dataElement.setValueType(valueType);
    dataElement.setAggregationType(aggregationType);
    dataElement.setDomainType(domainType);

    return dataElement;
  }

  /**
   * @param categoryComboUniqueIdentifier A unique character to identify the category option combo.
   * @param categories the categories category options.
   * @return CategoryOptionCombo
   */
  public static CategoryCombo createCategoryCombo(
      char categoryComboUniqueIdentifier, Category... categories) {
    CategoryCombo categoryCombo =
        new CategoryCombo(
            "CategoryCombo" + categoryComboUniqueIdentifier, DataDimensionType.DISAGGREGATION);
    categoryCombo.setAutoFields();

    for (Category category : categories) {
      categoryCombo.getCategories().add(category);
    }

    return categoryCombo;
  }

  /**
   * @param identifier A unique string to identify the category option combo.
   * @param categories the categories category options.
   * @return CategoryOptionCombo
   */
  public static CategoryCombo createCategoryCombo(String identifier, Category... categories) {
    CategoryCombo categoryCombo =
        new CategoryCombo("CategoryCombo " + identifier, DataDimensionType.DISAGGREGATION);
    categoryCombo.setAutoFields();

    for (Category category : categories) {
      categoryCombo.getCategories().add(category);
    }

    return categoryCombo;
  }

  /**
   * Creates a {@see CategoryCombo} with name, uid, and categories.
   *
   * @param name desired name
   * @param uid desired uid
   * @param categories categories for this combo
   * @return {@see CategoryCombo}
   */
  public static CategoryCombo createCategoryCombo(String name, String uid, Category... categories) {
    CategoryCombo categoryCombo =
        new CategoryCombo(name, DISAGGREGATION, Arrays.asList(categories));
    categoryCombo.setAutoFields();
    categoryCombo.setUid(uid);
    return categoryCombo;
  }

  public static CategoryOptionCombo createCategoryOptionCombo() {
    CategoryOptionCombo coc = new CategoryOptionCombo();
    coc.setAutoFields();
    return coc;
  }

  /**
   * @param categoryComboUniqueIdentifier A unique character to identify the category combo.
   * @param categoryOptionUniqueIdentifiers Unique characters to identify the category options.
   * @return CategoryOptionCombo
   */
  public static CategoryOptionCombo createCategoryOptionCombo(
      char categoryComboUniqueIdentifier, char... categoryOptionUniqueIdentifiers) {
    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setAutoFields();

    categoryOptionCombo.setCategoryCombo(
        new CategoryCombo(
            "CategoryCombo" + categoryComboUniqueIdentifier, DataDimensionType.DISAGGREGATION));

    for (char identifier : categoryOptionUniqueIdentifiers) {
      categoryOptionCombo
          .getCategoryOptions()
          .add(new CategoryOption("CategoryOption" + identifier));
    }

    return categoryOptionCombo;
  }

  /**
   * Creates a {@see CategoryOptionCombo} with name, uid, and options.
   *
   * @param name desired name
   * @param uid desired uid
   * @param categoryCombo category combination for this option combo
   * @param categoryOptions category options for this option combo
   * @return {@see CategoryOptionCombo}
   */
  public static CategoryOptionCombo createCategoryOptionCombo(
      String name, String uid, CategoryCombo categoryCombo, CategoryOption... categoryOptions) {
    CategoryOptionCombo categoryOptionCombo =
        createCategoryOptionCombo(categoryCombo, categoryOptions);
    categoryOptionCombo.setName(name);
    categoryOptionCombo.setShortName(name);
    categoryOptionCombo.setUid(uid);
    return categoryOptionCombo;
  }

  /**
   * @param categoryCombo the category combo.
   * @param categoryOptions the category options.
   * @return CategoryOptionCombo
   *     <p>Note: All the Category Options (COs) should be added to the Category Option Combo (COC)
   *     before the COC is added to the COs. That way the hashCode for the COC is stable when it is
   *     added to the CO HashSets because the COC hashCode depends on its linked COs.
   */
  public static CategoryOptionCombo createCategoryOptionCombo(
      CategoryCombo categoryCombo, CategoryOption... categoryOptions) {
    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    categoryOptionCombo.setAutoFields();

    categoryOptionCombo.setCategoryCombo(categoryCombo);

    for (CategoryOption categoryOption : categoryOptions) {
      categoryOptionCombo.getCategoryOptions().add(categoryOption);
    }

    for (CategoryOption categoryOption : categoryOptions) {
      categoryOption.getCategoryOptionCombos().add(categoryOptionCombo);
    }

    return categoryOptionCombo;
  }

  public static CategoryOptionCombo createCategoryOptionCombo(char uniqueCharacter) {
    CategoryOptionCombo coc = new CategoryOptionCombo();
    coc.setAutoFields();

    coc.setUid(BASE_COC_UID + uniqueCharacter);
    coc.setName("CategoryOptionCombo" + uniqueCharacter);
    coc.setCode("CategoryOptionComboCode" + uniqueCharacter);

    return coc;
  }

  /**
   * @param categoryUniqueIdentifier A unique character to identify the category.
   * @param categoryOptions the category options.
   * @return Category
   */
  public static Category createCategory(
      char categoryUniqueIdentifier, CategoryOption... categoryOptions) {
    Category category =
        new Category("Category" + categoryUniqueIdentifier, DataDimensionType.DISAGGREGATION);
    category.setAutoFields();
    category.setShortName(category.getName());
    for (CategoryOption categoryOption : categoryOptions) {
      category.addCategoryOption(categoryOption);
    }

    return category;
  }

  /**
   * @param identifier A unique string to identify the category.
   * @param categoryOptions the category options.
   * @return Category
   */
  public static Category createCategory(String identifier, CategoryOption... categoryOptions) {
    Category category = new Category("Category" + identifier, DataDimensionType.DISAGGREGATION);
    category.setAutoFields();
    category.setShortName(category.getName());
    for (CategoryOption categoryOption : categoryOptions) {
      category.addCategoryOption(categoryOption);
    }

    return category;
  }

  /**
   * Creates a {@see Category} with name, uid, and options.
   *
   * @param name desired name
   * @param uid desired uid
   * @param categoryOptions options for this category
   * @return {@see Category}
   */
  public static Category createCategory(
      String name, String uid, CategoryOption... categoryOptions) {
    Category category = new Category(name, DISAGGREGATION, Arrays.asList(categoryOptions));
    category.setAutoFields();
    category.setShortName(name);
    category.setUid(uid);

    return category;
  }

  public static CategoryOption createCategoryOption(char uniqueIdentifier) {
    CategoryOption categoryOption = new CategoryOption("CategoryOption" + uniqueIdentifier);
    categoryOption.setAutoFields();

    return categoryOption;
  }

  /**
   * Creates a {@see CategoryOption} with name and uid.
   *
   * @param name desired name
   * @param uid desired uid
   * @return {@see CategoryOption}
   */
  public static CategoryOption createCategoryOption(String name, String uid) {
    CategoryOption categoryOption = new CategoryOption(name);
    categoryOption.setAutoFields();
    categoryOption.setUid(uid);

    return categoryOption;
  }

  /**
   * Creates a {@see CategoryDimension} with name and uid.
   *
   * @param dimension desired category
   * @return {@see CategoryDimension}
   */
  public static CategoryDimension createCategoryDimension(Category dimension) {
    CategoryDimension categoryDimension = new CategoryDimension();
    categoryDimension.setDimension(dimension);

    return categoryDimension;
  }

  /**
   * @param uniqueIdentifier A unique character to identify the category option group.
   * @param categoryOptions the category options.
   * @return CategoryOptionGroup
   */
  public static CategoryOptionGroup createCategoryOptionGroup(
      char uniqueIdentifier, CategoryOption... categoryOptions) {
    CategoryOptionGroup categoryOptionGroup =
        new CategoryOptionGroup("CategoryOptionGroup" + uniqueIdentifier);
    categoryOptionGroup.setShortName("ShortName" + uniqueIdentifier);
    categoryOptionGroup.setAutoFields();
    categoryOptionGroup.setDataDimensionType(DISAGGREGATION);

    categoryOptionGroup.setMembers(new HashSet<>());

    for (CategoryOption categoryOption : categoryOptions) {
      categoryOptionGroup.addCategoryOption(categoryOption);
    }

    return categoryOptionGroup;
  }

  /**
   * @param categoryGroupSetUniqueIdentifier A unique character to identify the category option
   *     group set.
   * @param categoryOptionGroups the category option groups.
   * @return CategoryOptionGroupSet
   */
  public static CategoryOptionGroupSet createCategoryOptionGroupSet(
      char categoryGroupSetUniqueIdentifier, CategoryOptionGroup... categoryOptionGroups) {
    CategoryOptionGroupSet categoryOptionGroupSet =
        new CategoryOptionGroupSet("CategoryOptionGroupSet" + categoryGroupSetUniqueIdentifier);
    categoryOptionGroupSet.setAutoFields();
    categoryOptionGroupSet.setDataDimensionType(DISAGGREGATION);

    for (CategoryOptionGroup categoryOptionGroup : categoryOptionGroups) {
      categoryOptionGroupSet.addCategoryOptionGroup(categoryOptionGroup);
    }

    return categoryOptionGroupSet;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static Attribute createAttribute(char uniqueCharacter) {
    Attribute attribute = new Attribute("Attribute" + uniqueCharacter, ValueType.TEXT);
    attribute.setAutoFields();

    return attribute;
  }

  public static Attribute createAttribute(String name, ValueType valueType) {
    Attribute attribute = new Attribute(name, valueType);
    attribute.setAutoFields();

    return attribute;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static DataElementGroup createDataElementGroup(char uniqueCharacter) {
    DataElementGroup group = new DataElementGroup();
    group.setAutoFields();

    group.setUid(BASE_UID + uniqueCharacter);
    group.setName("DataElementGroup" + uniqueCharacter);
    group.setShortName("DataElementGroup" + uniqueCharacter);
    group.setCode("DataElementCode" + uniqueCharacter);

    return group;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param dataElements Data elements to go in the group.
   */
  public static DataElementGroup createDataElementGroup(
      char uniqueCharacter, DataElement... dataElements) {
    DataElementGroup deg = createDataElementGroup(uniqueCharacter);

    Arrays.stream(dataElements).forEach(deg::addDataElement);

    return deg;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static DataElementGroupSet createDataElementGroupSet(char uniqueCharacter) {
    DataElementGroupSet groupSet = new DataElementGroupSet();
    groupSet.setAutoFields();

    groupSet.setUid(BASE_UID + uniqueCharacter);
    groupSet.setName("DataElementGroupSet" + uniqueCharacter);
    groupSet.setShortName(groupSet.getName());

    return groupSet;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static IndicatorType createIndicatorType(char uniqueCharacter) {
    IndicatorType type = new IndicatorType();
    type.setAutoFields();

    type.setUid(BASE_IN_TYPE_UID + uniqueCharacter);
    type.setName("IndicatorType" + uniqueCharacter);
    type.setFactor(100);

    return type;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param type The type.
   */
  public static Indicator createIndicator(char uniqueCharacter, IndicatorType type) {
    return createIndicator(uniqueCharacter, type, "Numerator", "Denominator");
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param type The type.
   * @param numerator The numerator.
   * @param denominator The denominator.
   */
  public static Indicator createIndicator(
      char uniqueCharacter, IndicatorType type, String numerator, String denominator) {
    Indicator indicator = new Indicator();
    indicator.setAutoFields();

    indicator.setUid(BASE_IN_UID + uniqueCharacter);
    indicator.setName("Indicator" + uniqueCharacter);
    indicator.setShortName("IndicatorShort" + uniqueCharacter);
    indicator.setCode("IndicatorCode" + uniqueCharacter);
    indicator.setDescription("IndicatorDescription" + uniqueCharacter);
    indicator.setAnnualized(false);
    indicator.setIndicatorType(type);
    indicator.setNumerator(numerator);
    indicator.setNumeratorDescription("NumeratorDescription");
    indicator.setDenominator(denominator);
    indicator.setDenominatorDescription("DenominatorDescription");

    return indicator;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static IndicatorGroup createIndicatorGroup(char uniqueCharacter) {
    IndicatorGroup group = new IndicatorGroup();
    group.setAutoFields();

    group.setUid(BASE_UID + uniqueCharacter);
    group.setName("IndicatorGroup" + uniqueCharacter);

    return group;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static IndicatorGroupSet createIndicatorGroupSet(char uniqueCharacter) {
    IndicatorGroupSet groupSet = new IndicatorGroupSet();
    groupSet.setAutoFields();

    groupSet.setUid(BASE_UID + uniqueCharacter);
    groupSet.setName("IndicatorGroupSet" + uniqueCharacter);

    return groupSet;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static Section createSection(
      char uniqueCharacter,
      DataSet dataSet,
      List<DataElement> dataElements,
      List<Indicator> indicators) {
    Section section = new Section("Section" + uniqueCharacter, dataSet, dataElements, Set.of());
    section.setAutoFields();
    section.getIndicators().addAll(indicators);
    return section;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static DataSet createDataSet(char uniqueCharacter) {
    DataSet dataSet = createDataSet(uniqueCharacter, null);
    dataSet.setPeriodType(new MonthlyPeriodType());

    return dataSet;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param periodType The period type.
   */
  public static DataSet createDataSet(char uniqueCharacter, PeriodType periodType) {
    return createDataSet(uniqueCharacter, periodType, null);
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param periodType The period type.
   * @param categoryCombo The category combo.
   */
  public static DataSet createDataSet(
      char uniqueCharacter, PeriodType periodType, CategoryCombo categoryCombo) {
    DataSet dataSet = new DataSet();
    dataSet.setAutoFields();

    dataSet.setUid(BASE_DS_UID + uniqueCharacter);
    dataSet.setName("DataSet" + uniqueCharacter);
    dataSet.setShortName("DataSetShort" + uniqueCharacter);
    dataSet.setCode("DataSetCode" + uniqueCharacter);
    dataSet.setPeriodType(periodType);

    if (categoryCombo != null) {
      dataSet.setCategoryCombo(categoryCombo);
    } else if (categoryService != null) {
      dataSet.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    }

    return dataSet;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static DataEntryForm createDataEntryForm(char uniqueCharacter) {
    return new DataEntryForm("DataEntryForm" + uniqueCharacter, "<p></p>");
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param html the form HTML content.
   */
  public static DataEntryForm createDataEntryForm(char uniqueCharacter, String html) {
    return new DataEntryForm("DataEntryForm" + uniqueCharacter, html);
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static OrganisationUnit createOrganisationUnit(char uniqueCharacter) {
    OrganisationUnit unit = new OrganisationUnit();
    unit.setAutoFields();
    unit.setUid(BASE_OU_UID + uniqueCharacter);
    unit.setName("OrganisationUnit" + uniqueCharacter);
    unit.setShortName("OrganisationUnitShort" + uniqueCharacter);
    unit.setCode("OrganisationUnitCode" + uniqueCharacter);
    unit.setOpeningDate(date);
    unit.setComment("Comment" + uniqueCharacter);
    unit.updatePath();
    return unit;
  }

  public static OrganisationUnit createOrganisationUnit(char uniqueCharacter, Geometry geometry) {
    OrganisationUnit unit = createOrganisationUnit(uniqueCharacter);
    unit.setGeometry(geometry);
    return unit;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param parent The parent.
   */
  public static OrganisationUnit createOrganisationUnit(
      char uniqueCharacter, OrganisationUnit parent) {
    OrganisationUnit unit = createOrganisationUnit(uniqueCharacter);
    unit.setParent(parent);
    parent.getChildren().add(unit);
    unit.updatePath();
    return unit;
  }

  /**
   * Deprecated, use {@code createOrganisationUnit(char,OrganisationUnit)}.
   *
   * @param name The name, short name and code of the organisation unit.
   */
  public static OrganisationUnit createOrganisationUnit(String name) {
    OrganisationUnit unit = createOrganisationUnit('Y');
    unit.setUid(CodeGenerator.generateUid());
    unit.setName(name);
    unit.setShortName(name);
    unit.setCode(name);
    unit.setComment("Comment " + name);
    return unit;
  }

  /**
   * Deprecated, use {@code createOrganisationUnit(char,OrganisationUnit)}.
   *
   * @param name The name, short name and code of the organisation unit.
   * @param parent The parent.
   */
  public static OrganisationUnit createOrganisationUnit(String name, OrganisationUnit parent) {
    OrganisationUnit unit = createOrganisationUnit(name);
    unit.setParent(parent);
    parent.getChildren().add(unit);
    unit.updatePath();
    return unit;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static OrganisationUnitGroup createOrganisationUnitGroup(char uniqueCharacter) {
    OrganisationUnitGroup group = new OrganisationUnitGroup();
    group.setAutoFields();
    group.setUid(BASE_UID + uniqueCharacter);
    group.setName("OrganisationUnitGroup" + uniqueCharacter);
    group.setShortName("OrganisationUnitGroupShort" + uniqueCharacter);
    group.setCode("OrganisationUnitGroupCode" + uniqueCharacter);
    return group;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   */
  public static OrganisationUnitGroupSet createOrganisationUnitGroupSet(char uniqueCharacter) {
    OrganisationUnitGroupSet groupSet = new OrganisationUnitGroupSet();
    groupSet.setAutoFields();
    groupSet.setName("OrganisationUnitGroupSet" + uniqueCharacter);
    groupSet.setShortName("OrganisationUnitGroupSet" + uniqueCharacter);
    groupSet.setCode("OrganisationUnitGroupSetCode" + uniqueCharacter);
    groupSet.setDescription("Description" + uniqueCharacter);
    groupSet.setCompulsory(true);
    return groupSet;
  }

  /**
   * @param type The PeriodType.
   * @param startDate The start date.
   */
  public static Period createPeriod(PeriodType type, Date startDate) {
    Period period = new Period();
    period.setAutoFields();

    period.setPeriodType(type);
    period.setStartDate(startDate);

    return period;
  }

  /**
   * @param type The PeriodType.
   * @param startDate The start date.
   * @param endDate The end date.
   */
  public static Period createPeriod(PeriodType type, Date startDate, Date endDate) {
    Period period = new Period();
    period.setAutoFields();

    period.setPeriodType(type);
    period.setStartDate(startDate);
    period.setEndDate(endDate);

    return period;
  }

  /**
   * @param isoPeriod the ISO period string.
   */
  public static Period createPeriod(String isoPeriod) {
    return PeriodType.getPeriodFromIsoString(isoPeriod);
  }

  /**
   * @param isoPeriod the ISO period strings.
   */
  public static List<Period> createPeriods(String... isoPeriod) {
    return Stream.of(isoPeriod)
        .map(PeriodType::getPeriodFromIsoString)
        .collect(Collectors.toList());
  }

  /**
   * @param startDate The start date.
   * @param endDate The end date.
   */
  public static Period createPeriod(Date startDate, Date endDate) {
    Period period = new Period();
    period.setAutoFields();

    period.setPeriodType(new MonthlyPeriodType());
    period.setStartDate(startDate);
    period.setEndDate(endDate);

    return period;
  }

  /**
   * Uses the given category option combo also as attribute option combo.
   *
   * @param dataElement The data element.
   * @param period The period.
   * @param source The source.
   * @param value The value.
   * @param categoryOptionCombo The category (and attribute) option combo.
   */
  public static DataValue createDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      String value,
      CategoryOptionCombo categoryOptionCombo) {
    DataValue dataValue = new DataValue();

    dataValue.setDataElement(dataElement);
    dataValue.setPeriod(period);
    dataValue.setSource(source);
    dataValue.setCategoryOptionCombo(categoryOptionCombo);
    dataValue.setAttributeOptionCombo(categoryOptionCombo);
    dataValue.setValue(value);
    dataValue.setComment("Comment");
    dataValue.setStoredBy("StoredBy");

    return dataValue;
  }

  /**
   * @param dataElement The data element.
   * @param period The period.
   * @param source The source.
   * @param categoryOptionCombo The category option combo.
   * @param attributeOptionCombo The attribute option combo.
   * @param value the value.
   * @param created the created date.
   * @param lastUpdated the last updated date.
   */
  public static DataValue createDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value,
      Date created,
      Date lastUpdated) {
    DataValue dataValue =
        createDataValue(
            dataElement, period, source, categoryOptionCombo, attributeOptionCombo, value);
    dataValue.setCreated(created);
    dataValue.setLastUpdated(lastUpdated);
    return dataValue;
  }

  /**
   * @param dataElement The data element.
   * @param period The period.
   * @param source The source.
   * @param value The value.
   * @param categoryOptionCombo The category option combo.
   * @param attributeOptionCombo The attribute option combo.
   */
  public static DataValue createDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value) {
    return createDataValue(
        dataElement, period, source, categoryOptionCombo, attributeOptionCombo, value, false);
  }

  /**
   * @param dataElement The data element.
   * @param period The period.
   * @param source The source.
   * @param value The value.
   * @param categoryOptionCombo The category option combo.
   * @param attributeOptionCombo The attribute option combo.
   * @param deleted Whether the data valeu is deleted.
   */
  public static DataValue createDataValue(
      DataElement dataElement,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo,
      String value,
      boolean deleted) {
    DataValue dataValue = new DataValue();

    dataValue.setDataElement(dataElement);
    dataValue.setPeriod(period);
    dataValue.setSource(source);
    dataValue.setCategoryOptionCombo(categoryOptionCombo);
    dataValue.setAttributeOptionCombo(attributeOptionCombo);
    dataValue.setValue(value);
    dataValue.setComment("Comment");
    dataValue.setStoredBy("StoredBy");
    dataValue.setCreated(new Date());
    dataValue.setLastUpdated(new Date());
    dataValue.setDeleted(deleted);

    return dataValue;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param operator The operator.
   * @param leftSide The left side expression.
   * @param rightSide The right side expression.
   * @param periodType The period-type.
   */
  public static ValidationRule createValidationRule(
      String uniqueCharacter,
      Operator operator,
      Expression leftSide,
      Expression rightSide,
      PeriodType periodType) {
    return createValidationRule(uniqueCharacter, operator, leftSide, rightSide, periodType, false);
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param operator The operator.
   * @param leftSide The left side expression.
   * @param rightSide The right side expression.
   * @param periodType The period-type.
   * @param skipFormValidation Skip when validating forms.
   */
  public static ValidationRule createValidationRule(
      String uniqueCharacter,
      Operator operator,
      Expression leftSide,
      Expression rightSide,
      PeriodType periodType,
      boolean skipFormValidation) {
    Assert.notNull(leftSide, "Left side expression must be specified");
    Assert.notNull(rightSide, "Rigth side expression must be specified");

    ValidationRule validationRule = new ValidationRule();
    validationRule.setAutoFields();

    validationRule.setName("ValidationRule" + uniqueCharacter);
    validationRule.setDescription("Description" + uniqueCharacter);
    validationRule.setOperator(operator);
    validationRule.setLeftSide(leftSide);
    validationRule.setRightSide(rightSide);
    validationRule.setPeriodType(periodType);
    validationRule.setSkipFormValidation(skipFormValidation);

    return validationRule;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param operator The operator.
   * @param leftSide The left side expression.
   * @param rightSide The right side expression.
   * @param periodType The period-type.
   */
  public static ValidationRule createValidationRule(
      char uniqueCharacter,
      Operator operator,
      Expression leftSide,
      Expression rightSide,
      PeriodType periodType) {
    return createValidationRule(
        Character.toString(uniqueCharacter), operator, leftSide, rightSide, periodType);
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @return ValidationRuleGroup
   */
  public static ValidationRuleGroup createValidationRuleGroup(char uniqueCharacter) {
    ValidationRuleGroup group = new ValidationRuleGroup();
    group.setAutoFields();

    group.setName("ValidationRuleGroup" + uniqueCharacter);
    group.setDescription("Description" + uniqueCharacter);

    return group;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param expressionString The expression string.
   */
  public static Expression createExpression2(char uniqueCharacter, String expressionString) {
    Expression expression = new Expression();

    expression.setExpression(expressionString);
    expression.setDescription("Description" + uniqueCharacter);

    return expression;
  }

  /**
   * Creates a Predictor
   *
   * @param output the data element where the predictor stores its predictions
   * @param combo the category option combo (or null) under which the predictors are stored
   * @param uniqueCharacter A unique character to identify the object.
   * @param generator The right side expression.
   * @param skipTest The skiptest expression
   * @param periodType The period-type.
   * @param organisationUnitLevel The organisation unit level to be evaluated by this rule.
   * @param sequentialSampleCount How many sequential past periods to sample.
   * @param annualSampleCount How many years of past periods to sample.
   * @param sequentialSkipCount How many periods in the current year to skip
   */
  public static Predictor createPredictor(
      DataElement output,
      CategoryOptionCombo combo,
      String uniqueCharacter,
      Expression generator,
      Expression skipTest,
      PeriodType periodType,
      OrganisationUnitLevel organisationUnitLevel,
      int sequentialSampleCount,
      int sequentialSkipCount,
      int annualSampleCount) {
    return createPredictor(
        output,
        combo,
        uniqueCharacter,
        generator,
        skipTest,
        periodType,
        Sets.newHashSet(organisationUnitLevel),
        sequentialSampleCount,
        sequentialSkipCount,
        annualSampleCount);
  }

  /**
   * Creates a Predictor
   *
   * @param output The data element where the predictor stores its predictions
   * @param combo The category option combo (or null) under which the predictors are stored
   * @param uniqueCharacter A unique character to identify the object.
   * @param generator The right side expression.
   * @param skipTest The skiptest expression
   * @param periodType The period-type.
   * @param organisationUnitLevels The organisation unit levels to be evaluated by this rule.
   * @param sequentialSampleCount How many sequential past periods to sample.
   * @param annualSampleCount How many years of past periods to sample.
   * @param sequentialSkipCount How many periods in the current year to skip
   */
  public static Predictor createPredictor(
      DataElement output,
      CategoryOptionCombo combo,
      String uniqueCharacter,
      Expression generator,
      Expression skipTest,
      PeriodType periodType,
      Set<OrganisationUnitLevel> organisationUnitLevels,
      int sequentialSampleCount,
      int sequentialSkipCount,
      int annualSampleCount) {
    Predictor predictor = new Predictor();
    predictor.setAutoFields();

    predictor.setOutput(output);
    predictor.setOutputCombo(combo);
    predictor.setName("Predictor" + uniqueCharacter);
    predictor.setShortName("Predictor" + uniqueCharacter);
    predictor.setDescription("Description" + uniqueCharacter);
    predictor.setGenerator(generator);
    predictor.setSampleSkipTest(skipTest);
    predictor.setPeriodType(periodType);
    predictor.setOrganisationUnitLevels(organisationUnitLevels);
    predictor.setOrganisationUnitDescendants(OrganisationUnitDescendants.DESCENDANTS);
    predictor.setSequentialSampleCount(sequentialSampleCount);
    predictor.setAnnualSampleCount(annualSampleCount);
    predictor.setSequentialSkipCount(sequentialSkipCount);

    return predictor;
  }

  /**
   * Creates a Predictor Group
   *
   * @param uniqueCharacter A unique character to identify the object.
   * @param predictors Predictors to add to the group.
   * @return PredictorGroup
   */
  public static PredictorGroup createPredictorGroup(char uniqueCharacter, Predictor... predictors) {
    PredictorGroup group = new PredictorGroup();
    group.setAutoFields();

    group.setName("PredictorGroup" + uniqueCharacter);
    group.setDescription("Description" + uniqueCharacter);
    group.setUid(BASE_PREDICTOR_GROUP_UID + uniqueCharacter);

    for (Predictor p : predictors) {
      group.addPredictor(p);
    }

    return group;
  }

  public static Legend createLegend(char uniqueCharacter, Double startValue, Double endValue) {
    Legend legend = new Legend();
    legend.setAutoFields();

    legend.setName("Legend" + uniqueCharacter);
    legend.setStartValue(startValue);
    legend.setEndValue(endValue);
    legend.setColor("Color" + uniqueCharacter);

    return legend;
  }

  public static LegendSet createLegendSet(char uniqueCharacter) {
    LegendSet legendSet = new LegendSet();
    legendSet.setAutoFields();

    legendSet.setName("LegendSet" + uniqueCharacter);

    return legendSet;
  }

  public static LegendSet createLegendSet(char uniqueCharacter, Legend... legends) {
    LegendSet legendSet = createLegendSet(uniqueCharacter);

    for (Legend legend : legends) {
      legendSet.getLegends().add(legend);
      legend.setLegendSet(legendSet);
    }

    return legendSet;
  }

  public static Visualization createVisualization(char uniqueCharacter) {
    Visualization visualization = new Visualization();
    visualization.setAutoFields();
    visualization.setName("Visualization" + uniqueCharacter);
    visualization.setType(PIVOT_TABLE);

    return visualization;
  }

  public static EventVisualization createEventVisualization(char uniqueCharacter, Program program) {
    EventVisualization eventVisualization = new EventVisualization("name-" + uniqueCharacter);
    eventVisualization.setAutoFields();
    eventVisualization.setProgram(program);
    eventVisualization.setName("EventVisualization" + uniqueCharacter);
    eventVisualization.setType(EventVisualizationType.LINE_LIST);

    return eventVisualization;
  }

  public static User makeUser(String uniqueCharacter) {
    return makeUser(uniqueCharacter, Lists.newArrayList());
  }

  private static final char[] USERNAME_CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();

  private static AtomicInteger uniqueCharCounter = new AtomicInteger(-1);

  private static String getNextUniqueChar() {
    int i = uniqueCharCounter.incrementAndGet();
    if (i >= USERNAME_CHARS.length - 1) {
      uniqueCharCounter.set(0);
    }
    return String.valueOf(USERNAME_CHARS[i]);
  }

  public static User makeUser(String uniqueCharacter, List<String> auths) {
    User user = new User();

    user.setUid(BASE_USER_UID + uniqueCharacter);

    user.setCreatedBy(user);

    user.setUsername(("username" + uniqueCharacter).toLowerCase());
    user.setPassword("password" + uniqueCharacter);

    if (auths != null && !auths.isEmpty()) {
      UserRole role = new UserRole();
      role.setName("Role_" + CodeGenerator.generateCode(5));
      auths.forEach(auth -> role.getAuthorities().add(auth));
      user.getUserRoles().add(role);
    }

    user.setFirstName("FirstName" + uniqueCharacter);
    user.setSurname("Surname" + uniqueCharacter);
    user.setEmail(("Email" + uniqueCharacter).toLowerCase());
    user.setPhoneNumber("PhoneNumber" + uniqueCharacter);
    user.setCode("UserCode" + uniqueCharacter);
    user.setAutoFields();

    return user;
  }

  public static MapView createMapView(String layer) {
    MapView mapView = new MapView();
    mapView.setAutoFields();

    mapView.setLayer(layer);
    mapView.setAggregationType(AggregationType.SUM);
    mapView.setThematicMapType(ThematicMapType.CHOROPLETH);
    mapView.setProgramStatus(EnrollmentStatus.COMPLETED);
    mapView.setOrganisationUnitSelectionMode(OrganisationUnitSelectionMode.DESCENDANTS);
    mapView.setRenderingStrategy(MapViewRenderingStrategy.SINGLE);
    mapView.setUserOrgUnitType(UserOrgUnitType.DATA_CAPTURE);
    mapView.setNoDataColor("#ddeeff");

    return mapView;
  }

  public static UserGroup createUserGroup(char uniqueCharacter, Set<User> users) {
    UserGroup userGroup = new UserGroup();
    userGroup.setAutoFields();

    userGroup.setUid(BASE_USER_GROUP_UID + uniqueCharacter);
    userGroup.setCode("UserGroupCode" + uniqueCharacter);
    userGroup.setName("UserGroup" + uniqueCharacter);
    userGroup.setMembers(users);

    return userGroup;
  }

  public static UserRole createUserRole(char uniqueCharacter, String... auths) {
    UserRole role = new UserRole();
    role.setAutoFields();

    role.setUid(BASE_UID + uniqueCharacter);
    role.setName("UserRole" + uniqueCharacter);

    for (String auth : auths) {
      role.getAuthorities().add(auth);
    }

    return role;
  }

  public static Program createProgram(char uniqueCharacter) {
    return createProgram(uniqueCharacter, null, null);
  }

  public static Program createProgramWithoutRegistration(char uniqueCharacter) {
    Program program = createProgram(uniqueCharacter, null, null);
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);

    return program;
  }

  public static Program createProgram(
      char uniqueCharacter, Set<ProgramStage> programStages, OrganisationUnit unit) {
    Set<OrganisationUnit> units = new HashSet<>();

    if (unit != null) {
      units.add(unit);
    }

    return createProgram(uniqueCharacter, programStages, null, units, null);
  }

  public static Program createProgram(
      char uniqueCharacter,
      Set<ProgramStage> programStages,
      Set<TrackedEntityAttribute> attributes,
      Set<OrganisationUnit> organisationUnits,
      CategoryCombo categoryCombo) {
    Program program = new Program();
    program.setAutoFields();
    program.setUid(BASE_PR_UID + uniqueCharacter);
    program.setName("Program" + uniqueCharacter);
    program.setCode("ProgramCode" + uniqueCharacter);
    program.setShortName("ProgramShort" + uniqueCharacter);
    program.setDescription("Description" + uniqueCharacter);
    program.setEnrollmentDateLabel("DateOfEnrollmentDescription");
    program.setIncidentDateLabel("DateOfIncidentDescription");
    program.setProgramType(ProgramType.WITH_REGISTRATION);

    if (programStages != null) {
      for (ProgramStage programStage : programStages) {
        programStage.setProgram(program);
        program.getProgramStages().add(programStage);
      }
    }

    if (attributes != null) {
      for (TrackedEntityAttribute attribute : attributes) {
        ProgramTrackedEntityAttribute ptea =
            new ProgramTrackedEntityAttribute(program, attribute, false, false);
        ptea.setAutoFields();

        program.getProgramAttributes().add(ptea);
      }
    }

    if (organisationUnits != null) {
      program.getOrganisationUnits().addAll(organisationUnits);
    }

    if (categoryCombo != null) {
      program.setCategoryCombo(categoryCombo);
    } else if (categoryService != null) {
      program.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    }

    return program;
  }

  public static Enrollment createEnrollment(
      Program program, TrackedEntity te, OrganisationUnit organisationUnit) {
    Enrollment enrollment = new Enrollment(new Date(), new Date(), te, program);
    enrollment.setAutoFields();
    enrollment.setTrackedEntity(te);
    enrollment.setOrganisationUnit(organisationUnit);
    return enrollment;
  }

  public static TrackerEvent createEvent(
      ProgramStage programStage, Enrollment enrollment, OrganisationUnit organisationUnit) {
    TrackerEvent event = new TrackerEvent();
    event.setAutoFields();
    event.setProgramStage(programStage);
    event.setEnrollment(enrollment);
    event.setOrganisationUnit(organisationUnit);
    if (categoryService != null) {
      event.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    }
    return event;
  }

  public static TrackerEvent createEvent(
      Enrollment enrollment,
      ProgramStage programStage,
      OrganisationUnit organisationUnit,
      Set<EventDataValue> dataValues) {
    TrackerEvent event = createEvent(programStage, enrollment, organisationUnit);
    event.setOccurredDate(new Date());
    event.setStatus(EventStatus.ACTIVE);
    event.setEventDataValues(dataValues);
    return event;
  }

  public static ProgramRule createProgramRule(char uniqueCharacter, Program parentProgram) {
    ProgramRule programRule = new ProgramRule();
    programRule.setAutoFields();

    programRule.setName("ProgramRule" + uniqueCharacter);
    programRule.setProgram(parentProgram);
    programRule.setCondition("true");

    return programRule;
  }

  public static ProgramRuleAction createProgramRuleAction(char uniqueCharacter) {
    ProgramRuleAction programRuleAction = new ProgramRuleAction();
    programRuleAction.setAutoFields();

    programRuleAction.setName("ProgramRuleAction" + uniqueCharacter);
    programRuleAction.setProgramRuleActionType(ProgramRuleActionType.HIDEFIELD);

    return programRuleAction;
  }

  public static ProgramRuleAction createProgramRuleAction(
      char uniqueCharacter, ProgramRule parentRule) {
    ProgramRuleAction programRuleAction = createProgramRuleAction(uniqueCharacter);
    programRuleAction.setProgramRule(parentRule);

    return programRuleAction;
  }

  public static ProgramRuleVariable createConstantProgramRuleVariable(
      char uniqueCharacter, Program parentProgram) {
    ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
    programRuleVariable.setAutoFields();

    programRuleVariable.setName(uniqueCharacter + "1234567890");
    programRuleVariable.setProgram(parentProgram);
    programRuleVariable.setSourceType(ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT);

    return programRuleVariable;
  }

  public static ProgramRuleVariable createProgramRuleVariable(
      char uniqueCharacter, Program parentProgram) {
    ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
    programRuleVariable.setAutoFields();

    programRuleVariable.setName(PROGRAM_RULE_VARIABLE + uniqueCharacter);
    programRuleVariable.setProgram(parentProgram);
    programRuleVariable.setSourceType(ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT);
    programRuleVariable.setValueType(ValueType.TEXT);

    return programRuleVariable;
  }

  public static ProgramRuleVariable createProgramRuleVariableWithSourceType(
      char uniqueCharacter,
      Program parentProgram,
      ProgramRuleVariableSourceType sourceType,
      ValueType valueType) {
    ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
    programRuleVariable.setAutoFields();

    programRuleVariable.setName(PROGRAM_RULE_VARIABLE + uniqueCharacter);
    programRuleVariable.setProgram(parentProgram);
    programRuleVariable.setSourceType(sourceType);
    programRuleVariable.setValueType(valueType);

    return programRuleVariable;
  }

  public static ProgramRuleVariable createProgramRuleVariableWithDataElement(
      char uniqueCharacter, Program parentProgram, DataElement dataElement) {
    ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
    programRuleVariable.setAutoFields();

    programRuleVariable.setName(PROGRAM_RULE_VARIABLE + uniqueCharacter);
    programRuleVariable.setProgram(parentProgram);
    programRuleVariable.setDataElement(dataElement);
    programRuleVariable.setSourceType(ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT);
    programRuleVariable.setValueType(dataElement.getValueType());

    return programRuleVariable;
  }

  public static ProgramRuleVariable createProgramRuleVariableWithTEA(
      char uniqueCharacter, Program parentProgram, TrackedEntityAttribute attribute) {
    ProgramRuleVariable programRuleVariable = new ProgramRuleVariable();
    programRuleVariable.setAutoFields();

    programRuleVariable.setName(PROGRAM_RULE_VARIABLE + uniqueCharacter);
    programRuleVariable.setProgram(parentProgram);
    programRuleVariable.setAttribute(attribute);
    programRuleVariable.setSourceType(ProgramRuleVariableSourceType.TEI_ATTRIBUTE);
    programRuleVariable.setValueType(attribute.getValueType());

    return programRuleVariable;
  }

  public static ProgramStage createProgramStage(char uniqueCharacter, Program program) {
    ProgramStage stage = createProgramStage(uniqueCharacter, 0, false);
    stage.setProgram(program);

    return stage;
  }

  public static ProgramStage createProgramStage(char uniqueCharacter, int minDays) {
    return createProgramStage(uniqueCharacter, minDays, false);
  }

  public static ProgramStage createProgramStage(
      char uniqueCharacter, int minDays, boolean repeatable) {
    ProgramStage programStage = new ProgramStage();
    programStage.setAutoFields();

    programStage.setUid(BASE_PG_UID + uniqueCharacter);
    programStage.setName("ProgramStage" + uniqueCharacter);
    programStage.setDescription("description" + uniqueCharacter);
    programStage.setMinDaysFromStart(minDays);
    programStage.setRepeatable(repeatable);

    return programStage;
  }

  public static ProgramStage createProgramStage(
      char uniqueCharacter, Set<DataElement> dataElements) {
    ProgramStage programStage = createProgramStage(uniqueCharacter, 0);

    if (dataElements != null) {
      int sortOrder = 1;

      for (DataElement dataElement : dataElements) {
        ProgramStageDataElement psd =
            createProgramStageDataElement(programStage, dataElement, sortOrder);
        psd.setAutoFields();

        programStage.getProgramStageDataElements().add(psd);
      }
    }

    return programStage;
  }

  public static ProgramStageDataElement createProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, Integer sortOrder) {
    ProgramStageDataElement psde =
        new ProgramStageDataElement(programStage, dataElement, false, sortOrder);
    psde.setAutoFields();

    return psde;
  }

  public static ProgramStageDataElement createProgramStageDataElement(
      ProgramStage programStage, DataElement dataElement, Integer sortOrder, boolean compulsory) {
    ProgramStageDataElement psde =
        new ProgramStageDataElement(programStage, dataElement, compulsory, sortOrder);
    psde.setAutoFields();

    return psde;
  }

  public static ProgramMessage createProgramMessage(
      String text,
      String subject,
      ProgramMessageRecipients recipients,
      ProgramMessageStatus status,
      Set<DeliveryChannel> channels) {

    ProgramMessage pm = new ProgramMessage();
    pm.setAutoFields();
    pm.setText(text);
    pm.setSubject(subject);
    pm.setRecipients(recipients);
    pm.setMessageStatus(status);
    pm.setDeliveryChannels(channels);

    return pm;
  }

  public static ProgramIndicator createProgramIndicator(
      char uniqueCharacter, Program program, String expression, String filter) {
    return createProgramIndicator(
        uniqueCharacter, AnalyticsType.EVENT, program, expression, filter);
  }

  public static ProgramIndicator createProgramIndicator(
      char uniqueCharacter,
      AnalyticsType analyticsType,
      Program program,
      String expression,
      String filter) {
    return createProgramIndicator(
        uniqueCharacter, analyticsType, program, expression, filter, null, 0);
  }

  public static ProgramIndicator createProgramIndicator(
      char uniqueCharacter,
      AnalyticsType analyticsType,
      Program program,
      String expression,
      String filter,
      PeriodType afterStartPeriodType,
      int afterStartPeriods) {
    ProgramIndicator indicator = new ProgramIndicator();
    indicator.setAutoFields();
    indicator.setName("Indicator" + uniqueCharacter);
    indicator.setShortName("IndicatorShort" + uniqueCharacter);
    indicator.setCode("IndicatorCode" + uniqueCharacter);
    indicator.setDescription("IndicatorDescription" + uniqueCharacter);
    indicator.setProgram(program);
    indicator.setExpression(expression);
    indicator.setAnalyticsType(analyticsType);
    indicator.setFilter(filter);
    if (categoryService != null) {
      indicator.setCategoryCombo(categoryService.getDefaultCategoryCombo());
      indicator.setAttributeCombo(categoryService.getDefaultCategoryCombo());
    }

    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    if (analyticsType == AnalyticsType.EVENT) {
      boundaries.add(
          new AnalyticsPeriodBoundary(
              AnalyticsPeriodBoundary.EVENT_DATE,
              AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
              null,
              0));
      boundaries.add(
          new AnalyticsPeriodBoundary(
              AnalyticsPeriodBoundary.EVENT_DATE,
              AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
              afterStartPeriodType,
              afterStartPeriods));
    } else if (analyticsType == AnalyticsType.ENROLLMENT) {
      boundaries.add(
          new AnalyticsPeriodBoundary(
              AnalyticsPeriodBoundary.ENROLLMENT_DATE,
              AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD,
              null,
              0));
      boundaries.add(
          new AnalyticsPeriodBoundary(
              AnalyticsPeriodBoundary.ENROLLMENT_DATE,
              AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD,
              afterStartPeriodType,
              afterStartPeriods));
    }

    for (AnalyticsPeriodBoundary boundary : boundaries) {
      boundary.setAutoFields();
    }

    indicator.setAnalyticsPeriodBoundaries(boundaries);

    return indicator;
  }

  public static ProgramStageSection createProgramStageSection(
      char uniqueCharacter, Integer sortOrder) {
    ProgramStageSection section = new ProgramStageSection();
    section.setAutoFields();
    section.setName("ProgramStageSection" + uniqueCharacter);
    section.setSortOrder(sortOrder);

    return section;
  }

  public static RelationshipType createMalariaCaseLinkedToPersonRelationshipType(
      char uniqueCharacter, Program program, TrackedEntityType trackedEntityType) {
    RelationshipConstraint eventConstraint = new RelationshipConstraint();
    eventConstraint.setProgram(program);
    eventConstraint.setTrackedEntityType(trackedEntityType);
    eventConstraint.setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    RelationshipConstraint teConstraint = new RelationshipConstraint();
    teConstraint.setProgram(program);
    teConstraint.setTrackedEntityType(trackedEntityType);
    teConstraint.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    RelationshipType relationshipType = createRelationshipType(uniqueCharacter);
    relationshipType.setName("Malaria case linked to person");
    relationshipType.setBidirectional(true);
    relationshipType.setFromConstraint(eventConstraint);
    relationshipType.setToConstraint(teConstraint);
    return relationshipType;
  }

  public static Relationship createTeToTeRelationship(
      TrackedEntity from, TrackedEntity to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riFrom.setRelationship(relationship);
    riTo.setTrackedEntity(to);
    riTo.setRelationship(relationship);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static Relationship createTeToEnrollmentRelationship(
      TrackedEntity from, Enrollment to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riTo.setEnrollment(to);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static Relationship createTeToEventRelationship(
      TrackedEntity from, TrackerEvent to, RelationshipType relationshipType) {
    Relationship relationship = new Relationship();
    RelationshipItem riFrom = new RelationshipItem();
    RelationshipItem riTo = new RelationshipItem();

    riFrom.setTrackedEntity(from);
    riTo.setTrackerEvent(to);

    relationship.setRelationshipType(relationshipType);
    relationship.setFrom(riFrom);
    relationship.setTo(riTo);
    relationship.setKey(RelationshipUtils.generateRelationshipKey(relationship));
    relationship.setInvertedKey(RelationshipUtils.generateRelationshipInvertedKey(relationship));

    relationship.setAutoFields();

    return relationship;
  }

  public static RelationshipType createPersonToPersonRelationshipType(
      char uniqueCharacter,
      Program program,
      TrackedEntityType trackedEntityType,
      boolean isBidirectional) {
    RelationshipConstraint teConstraintA = new RelationshipConstraint();
    teConstraintA.setProgram(program);
    teConstraintA.setTrackedEntityType(trackedEntityType);
    teConstraintA.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    RelationshipConstraint teConstraintB = new RelationshipConstraint();
    teConstraintB.setProgram(program);
    teConstraintB.setTrackedEntityType(trackedEntityType);
    teConstraintB.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    RelationshipType relationshipType = createRelationshipType(uniqueCharacter);
    relationshipType.setName("Person_to_person_" + uniqueCharacter);
    relationshipType.setBidirectional(isBidirectional);
    relationshipType.setFromConstraint(teConstraintA);
    relationshipType.setToConstraint(teConstraintB);
    return relationshipType;
  }

  public static RelationshipType createTeToEnrollmentRelationshipType(
      char uniqueCharacter,
      Program program,
      TrackedEntityType trackedEntityType,
      boolean isBidirectional) {
    RelationshipConstraint teConstraintA = new RelationshipConstraint();
    teConstraintA.setProgram(program);
    teConstraintA.setTrackedEntityType(trackedEntityType);
    teConstraintA.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    RelationshipConstraint teConstraintB = new RelationshipConstraint();
    teConstraintB.setProgram(program);
    teConstraintB.setTrackedEntityType(trackedEntityType);
    teConstraintB.setRelationshipEntity(RelationshipEntity.PROGRAM_INSTANCE);
    RelationshipType relationshipType = createRelationshipType(uniqueCharacter);
    relationshipType.setName("Tei_to_enrollment_" + uniqueCharacter);
    relationshipType.setBidirectional(isBidirectional);
    relationshipType.setFromConstraint(teConstraintA);
    relationshipType.setToConstraint(teConstraintB);
    return relationshipType;
  }

  public static RelationshipType createTeToEventRelationshipType(
      char uniqueCharacter,
      Program program,
      TrackedEntityType trackedEntityType,
      boolean isBidirectional) {
    RelationshipConstraint teConstraintA = new RelationshipConstraint();
    teConstraintA.setProgram(program);
    teConstraintA.setTrackedEntityType(trackedEntityType);
    teConstraintA.setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    RelationshipConstraint teConstraintB = new RelationshipConstraint();
    teConstraintB.setProgram(program);
    teConstraintB.setTrackedEntityType(trackedEntityType);
    teConstraintB.setRelationshipEntity(RelationshipEntity.PROGRAM_STAGE_INSTANCE);
    RelationshipType relationshipType = createRelationshipType(uniqueCharacter);
    relationshipType.setName("Tei_to_event_" + uniqueCharacter);
    relationshipType.setBidirectional(isBidirectional);
    relationshipType.setFromConstraint(teConstraintA);
    relationshipType.setToConstraint(teConstraintB);
    return relationshipType;
  }

  public static RelationshipType createRelationshipType(char uniqueCharacter) {
    RelationshipType relationshipType = new RelationshipType();

    RelationshipConstraint fromRelationShipConstraint = new RelationshipConstraint();
    fromRelationShipConstraint.setTrackerDataView(TrackerDataView.builder().build());

    RelationshipConstraint toRelationShipConstraint = new RelationshipConstraint();
    toRelationShipConstraint.setTrackerDataView(TrackerDataView.builder().build());

    relationshipType.setFromToName("from_" + uniqueCharacter);
    relationshipType.setToFromName("to_" + uniqueCharacter);
    relationshipType.setAutoFields();
    relationshipType.setName("RelationshipType_" + relationshipType.getUid());
    relationshipType.setFromConstraint(fromRelationShipConstraint);
    relationshipType.setToConstraint(toRelationShipConstraint);
    relationshipType.setBidirectional(true);
    return relationshipType;
  }

  public static TrackedEntityFilter createTrackedEntityFilter(char uniqueChar, Program program) {
    TrackedEntityFilter trackedEntityFilter = new TrackedEntityFilter();
    trackedEntityFilter.setAutoFields();
    trackedEntityFilter.setName("TrackedEntityType" + uniqueChar);
    trackedEntityFilter.setDescription("TrackedEntityType" + uniqueChar + " description");
    trackedEntityFilter.setProgram(program);
    trackedEntityFilter.setEntityQueryCriteria(new EntityQueryCriteria());
    return trackedEntityFilter;
  }

  public static TrackedEntityType createTrackedEntityType(char uniqueChar) {
    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setAutoFields();
    trackedEntityType.setName("TrackedEntityType" + uniqueChar);
    trackedEntityType.setShortName("TrackedEntityTypeShort" + uniqueChar);
    trackedEntityType.setDescription("TrackedEntityType" + uniqueChar + " description");

    return trackedEntityType;
  }

  public static TrackedEntity createTrackedEntity(
      OrganisationUnit organisationUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  public static TrackedEntity createTrackedEntity(
      char uniqueChar, OrganisationUnit organisationUnit, TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setUid(BASE_TE_UID + uniqueChar);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    return trackedEntity;
  }

  public static TrackedEntity createTrackedEntity(
      char uniqueChar,
      OrganisationUnit organisationUnit,
      TrackedEntityAttribute attribute,
      TrackedEntityType trackedEntityType) {
    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setAutoFields();
    trackedEntity.setOrganisationUnit(organisationUnit);
    trackedEntity.setTrackedEntityType(trackedEntityType);

    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setAttribute(attribute);
    attributeValue.setTrackedEntity(trackedEntity);
    attributeValue.setValue("Attribute" + uniqueChar);
    trackedEntity.getTrackedEntityAttributeValues().add(attributeValue);

    return trackedEntity;
  }

  public static TrackedEntityAttributeValue createTrackedEntityAttributeValue(
      char uniqueChar, TrackedEntity trackedEntity, TrackedEntityAttribute attribute) {
    TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue();
    attributeValue.setTrackedEntity(trackedEntity);
    attributeValue.setAttribute(attribute);
    attributeValue.setValue("Attribute" + uniqueChar);

    return attributeValue;
  }

  /**
   * @param uniqueChar A unique character to identify the object.
   * @return TrackedEntityAttribute
   */
  public static TrackedEntityAttribute createTrackedEntityAttribute(char uniqueChar) {
    TrackedEntityAttribute attribute = new TrackedEntityAttribute();
    attribute.setAutoFields();

    attribute.setName("Attribute" + uniqueChar);
    attribute.setShortName("AttributeShortName" + uniqueChar);
    attribute.setCode("AttributeCode" + uniqueChar);
    attribute.setDescription("Attribute" + uniqueChar);
    attribute.setValueType(ValueType.TEXT);
    attribute.setAggregationType(AggregationType.NONE);

    return attribute;
  }

  public static TrackedEntityAttribute createTrackedEntityAttribute(
      char uniqueChar, ValueType valueType) {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute(uniqueChar);
    attribute.setValueType(valueType);
    return attribute;
  }

  public static TrackedEntityTypeAttribute createTrackedEntityTypeAttribute(
      char uniqueChar, ValueType valueType) {
    return new TrackedEntityTypeAttribute(
        createTrackedEntityType(uniqueChar), createTrackedEntityAttribute(uniqueChar, valueType));
  }

  public static ProgramTrackedEntityAttribute createProgramTrackedEntityAttribute(
      Program program, TrackedEntityAttribute attribute) {
    ProgramTrackedEntityAttribute ptea = new ProgramTrackedEntityAttribute();
    ptea.setAutoFields();

    ptea.setProgram(program);
    ptea.setAttribute(attribute);

    return ptea;
  }

  /**
   * @param uniqueChar A unique character to identify the object.
   * @param content The content of the file
   * @return a fileResource object
   */
  public static FileResource createFileResource(char uniqueChar, byte[] content) {
    String filename = "filename" + uniqueChar;
    HashCode contentMd5 = Hashing.md5().hashBytes(content);
    String contentType = MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

    FileResource fileResource =
        new FileResource(
            filename,
            contentType,
            content.length,
            contentMd5.toString(),
            FileResourceDomain.DATA_VALUE);
    fileResource.setAssigned(false);
    fileResource.setCreated(new Date());
    fileResource.setAutoFields();

    return fileResource;
  }

  public static Icon createIcon(char uniqueChar, Set<String> keywords, FileResource fileResource) {

    Icon icon = new Icon();
    icon.setAutoFields();
    icon.setKey("iconKey" + uniqueChar);
    icon.setDescription("description");
    icon.setKeywords(keywords);
    icon.setFileResource(fileResource);
    icon.setCustom(true);

    return icon;
  }

  /**
   * @param uniqueChar A unique character to identify the object.
   * @param content The content of the file
   * @return an externalFileResource object
   */
  public static ExternalFileResource createExternalFileResource(char uniqueChar, byte[] content) {
    FileResource fileResource = createFileResource(uniqueChar, content);
    ExternalFileResource externalFileResource = new ExternalFileResource();

    externalFileResource.setFileResource(fileResource);
    fileResource.setAssigned(true);
    externalFileResource.setAccessToken(String.valueOf(uniqueChar));
    return externalFileResource;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param sql A query statement to retreive record/data from database.
   * @return a sqlView instance
   */
  public static SqlView createSqlView(char uniqueCharacter, String sql) {
    SqlView sqlView = new SqlView();
    sqlView.setAutoFields();

    sqlView.setName("SqlView" + uniqueCharacter);
    sqlView.setDescription("Description" + uniqueCharacter);
    sqlView.setSqlQuery(sql);
    sqlView.setType(SqlViewType.VIEW);
    sqlView.setCacheStrategy(CacheStrategy.RESPECT_SYSTEM_SETTING);

    return sqlView;
  }

  /**
   * @param uniqueCharacter A unique character to identify the object.
   * @param value The value for constant
   * @return a constant instance
   */
  public static Constant createConstant(char uniqueCharacter, double value) {
    Constant constant = new Constant();
    constant.setAutoFields();

    constant.setName("Constant" + uniqueCharacter);
    constant.setShortName(constant.getName());
    constant.setValue(value);

    return constant;
  }

  public static ProgramNotificationTemplate createProgramNotificationTemplate(
      String name, int days, NotificationTrigger trigger, ProgramNotificationRecipient recipient) {
    ProgramNotificationTemplate template =
        new ProgramNotificationTemplate(
            name, "Subject", "Message", trigger, recipient, Sets.newHashSet(), days, null, null);

    template.setAutoFields();

    return template;
  }

  public static ProgramNotificationTemplate createProgramNotificationTemplate(
      String name,
      int days,
      NotificationTrigger trigger,
      ProgramNotificationRecipient recipient,
      Date scheduledDate) {
    ProgramNotificationTemplate template =
        new ProgramNotificationTemplate(
            name, "Subject", "Message", trigger, recipient, Sets.newHashSet(), days, null, null);
    template.setAutoFields();

    return template;
  }

  public static DataSetNotificationTemplate createDataSetNotificationTemplate(
      String name,
      DataSetNotificationRecipient notificationRecipient,
      DataSetNotificationTrigger dataSetNotificationTrigger,
      Integer relativeScheduledDays,
      SendStrategy sendStrategy) {
    DataSetNotificationTemplate dst =
        new DataSetNotificationTemplate(
            newHashSet(),
            newHashSet(),
            "Message",
            notificationRecipient,
            dataSetNotificationTrigger,
            "Subject",
            null,
            relativeScheduledDays,
            sendStrategy);
    dst.setName(name);
    return dst;
  }

  public static ValidationNotificationTemplate createValidationNotificationTemplate(String name) {
    ValidationNotificationTemplate template = new ValidationNotificationTemplate();
    template.setAutoFields();

    template.setName(name);
    template.setSubjectTemplate("Subject");
    template.setMessageTemplate("Message");
    template.setNotifyUsersInHierarchyOnly(false);

    return template;
  }

  public static OptionSet createOptionSet(char uniqueCharacter) {
    OptionSet optionSet = new OptionSet();
    optionSet.setAutoFields();

    optionSet.setName("OptionSet" + uniqueCharacter);
    optionSet.setCode("OptionSetCode" + uniqueCharacter);

    return optionSet;
  }

  public static OptionSet createOptionSet(char uniqueCharacter, Option... options) {
    OptionSet optionSet = createOptionSet(uniqueCharacter);

    for (Option option : options) {
      optionSet.getOptions().add(option);
      option.setOptionSet(optionSet);
    }

    return optionSet;
  }

  public static Option createOption(char uniqueCharacter) {
    Option option = new Option();
    option.setAutoFields();

    option.setName("Option" + uniqueCharacter);
    option.setCode("OptionCode" + uniqueCharacter);

    return option;
  }

  public static Option createOption(String code) {
    Option option = new Option();
    option.setAutoFields();

    option.setName("Option" + code);
    option.setCode(code);

    return option;
  }

  public static void configureHierarchy(
      OrganisationUnit root,
      OrganisationUnit lvlOneLeft,
      OrganisationUnit lvlOneRight,
      OrganisationUnit lvlTwoLeftLeft,
      OrganisationUnit lvlTwoLeftRight) {
    root.getChildren().addAll(Sets.newHashSet(lvlOneLeft, lvlOneRight));
    lvlOneLeft.setParent(root);
    lvlOneRight.setParent(root);

    lvlOneLeft.getChildren().addAll(Sets.newHashSet(lvlTwoLeftLeft, lvlTwoLeftRight));
    lvlTwoLeftLeft.setParent(lvlOneLeft);
    lvlTwoLeftRight.setParent(lvlOneLeft);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  protected <T extends IdentifiableObject> T fromJson(String path, Class<T> klass) {
    Assert.notNull(renderService, "RenderService must be injected in test");

    try {
      return renderService.fromJson(new ClassPathResource(path).getInputStream(), klass);
    } catch (IOException ex) {
      log.error("An error occurred when deserializing from Json", ex);
    }

    return null;
  }

  /**
   * Injects the externalDir property of LocationManager to user.home/dhis2_test_dir.
   * LocationManager dependency must be retrieved from the context up front.
   *
   * @param locationManager The LocationManager to be injected with the external directory.
   */
  public void setExternalTestDir(LocationManager locationManager) {
    if (locationManager instanceof DefaultLocationManager) {
      ((DefaultLocationManager) locationManager).setExternalDir(EXT_TEST_DIR);
    }
  }

  /** Attempts to remove the external test directory. */
  public void removeExternalTestDir() {
    deleteDir(new File(EXT_TEST_DIR));
  }

  private boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();

      if (children != null) {
        for (String aChildren : children) {
          boolean success = deleteDir(new File(dir, aChildren));

          if (!success) {
            return false;
          }
        }
      }
    }

    return dir.delete();
  }

  // -------------------------------------------------------------------------
  // Allow xpath testing of DXF2
  // -------------------------------------------------------------------------

  protected String xpathTest(String xpathString, String xml) throws XPathExpressionException {
    InputSource source = new InputSource(new StringReader(xml));
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    xpath.setNamespaceContext(new Dxf2NamespaceResolver());

    return xpath.evaluate(xpathString, source);
  }

  /**
   * Creates a user and injects into the security context with username "username". Requires <code>
   * identifiableObjectManager</code> and <code>userService</code> to be injected into the test.
   *
   * @param allAuth whether to grant ALL authority to user.
   * @param auths authorities to grant to user.
   * @return the user.
   */
  protected User createUserAndInjectSecurityContext(boolean allAuth, String... auths) {
    return createUserAndInjectSecurityContext(null, allAuth, auths);
  }

  /**
   * Creates a user and injects into the security context with username "username". Requires <code>
   * identifiableObjectManager</code> and <code>userService</code> to be injected into the test.
   *
   * @param organisationUnits the organisation units of the user.
   * @param allAuth whether to grant the ALL authority to user.
   * @param auths authorities to grant to user.
   * @return the user.
   */
  protected User createUserAndInjectSecurityContext(
      Set<OrganisationUnit> organisationUnits, boolean allAuth, String... auths) {
    return createUserAndInjectSecurityContext(organisationUnits, null, allAuth, auths);
  }

  /**
   * Creates a user and injects into the security context with username "username". Requires <code>
   * identifiableObjectManager</code> and <code>userService</code> to be injected into the test.
   *
   * @param organisationUnits the organisation units of the user.
   * @param dataViewOrganisationUnits the data view organisation units of the user.
   * @param allAuth whether to grant the ALL authority to the user.
   * @param auths authorities to grant to the user.
   * @return the user.
   */
  protected User createUserAndInjectSecurityContext(
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      boolean allAuth,
      String... auths) {
    return createUserAndInjectSecurityContext(
        organisationUnits, dataViewOrganisationUnits, null, allAuth, auths);
  }

  /**
   * Creates a user and injects into the security context with username "username". Requires <code>
   * identifiableObjectManager</code> and <code>userService</code> to be injected into the test.
   *
   * <p>
   *
   * @param organisationUnits the organisation units of the user.
   * @param dataViewOrganisationUnits the data view organisation units of the user.
   * @param catDimensionConstraints the category dimension constraints of the user.
   * @param allAuth whether to grant the ALL authority to the user.
   * @param auths authorities to grant to the user. =======
   * @return the user.
   */
  protected User createUserAndInjectSecurityContext(
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      Set<Category> catDimensionConstraints,
      boolean allAuth,
      String... auths) {
    checkUserServiceWasInjected();

    Set<String> authorities = new HashSet<>();

    if (allAuth) {
      authorities.add(Authorities.ALL.toString());
    }

    if (auths != null) {
      authorities.addAll(Lists.newArrayList(auths));
    }

    String username = CodeGenerator.generateCode(16);
    UserRole group = new UserRole();
    group.setName(username);
    group.getAuthorities().addAll(authorities);
    userService.addUserRole(group);

    User user = makeUser(getNextUniqueChar());
    user.setUsername(username);
    user.getUserRoles().add(group);

    if (organisationUnits != null) {
      user.setOrganisationUnits(organisationUnits);
    }

    if (dataViewOrganisationUnits != null) {
      user.setDataViewOrganisationUnits(dataViewOrganisationUnits);
    }

    if (catDimensionConstraints != null) {
      user.setCatDimensionConstraints(catDimensionConstraints);
    }

    userService.addUser(user);

    injectSecurityContextUser(user);

    return user;
  }

  private void checkUserServiceWasInjected() {
    Assert.notNull(userService, "UserService must be injected in test");
  }

  protected User createUserWithId(String username, String uid, String... authorities) {
    return createUserInternal(username, Optional.of(uid), null, authorities);
  }

  protected User createUserWithAuthority(String username, Authorities... authorities) {
    return createUserWithAuth(username, Authorities.toStringArray(authorities));
  }

  protected User createUserWithAuth(String username, String... authorities) {
    return createUserInternal(username, Optional.empty(), null, authorities);
  }

  protected User createOpenIDUser(String username, String openIDIdentifier) {
    return createUserInternal(username, Optional.empty(), openIDIdentifier);
  }

  private User createUserInternal(
      String username, Optional<String> uid, String openIDIdentifier, String... authorities) {
    checkUserServiceWasInjected();

    UserRole userRole = createUserRole(username, authorities);
    userService.addUserRole(userRole);

    boolean present = uid.isPresent();
    User user = present ? createUser(username, uid.get()) : createUser(username);
    user.setEmail(username + "@dhis2.org");
    user.setUsername(username);
    user.setOpenId(openIDIdentifier);
    user.getUserRoles().add(userRole);

    if (!Strings.isNullOrEmpty(openIDIdentifier)) {
      user.setOpenId(openIDIdentifier);
      user.setExternalAuth(true);
    }

    userService.encodeAndSetPassword(user, DEFAULT_ADMIN_PASSWORD);

    userService.addUser(user);

    return user;
  }

  /**
   * Creates and persists a user with a random UID and given authorities and injects it into the
   * Spring security context.
   */
  protected final User createAndInjectRandomUser(String... authorities) {
    User user = createAndAddRandomUser(authorities);
    injectSecurityContextUser(user);
    return user;
  }

  /** Creates and persists a user with a random UID and given authorities. */
  protected User createAndAddRandomUser(String... authorities) {
    checkUserServiceWasInjected();

    String uid = CodeGenerator.generateUid();
    UserRole role = createUserRole("Superuser_Test_" + uid, authorities);
    role.setUid(uid);

    String username = DEFAULT_USERNAME + "_" + uid;
    String password = DEFAULT_ADMIN_PASSWORD;

    User user = createUser(username);
    user.setUuid(UUID.randomUUID());
    user.setUid(uid);
    user.setName("Admin" + "_" + uid);
    user.setUsername(username);
    user.setPassword(password);
    user.getUserRoles().add(role);

    user.setCreatedBy(user);
    role.setCreatedBy(user);
    userService.addUser(user);

    userService.encodeAndSetPassword(user, password);
    userService.updateUser(user);

    userService.addUserRole(role);

    return user;
  }

  protected void injectSecurityContextUser(User user) {
    if (user == null) {
      clearSecurityContext();
      return;
    }

    user = userService.getUser(user.getUid());
    UserDetails userDetails = UserDetails.fromUser(user);

    injectSecurityContext(userDetails);
  }

  public static void injectSecurityContextNoSettings(UserDetails currentUserDetails) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            currentUserDetails, "", currentUserDetails.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
  }

  public void injectSecurityContext(UserDetails currentUserDetails) {
    injectSecurityContextNoSettings(currentUserDetails);
    if (userSettingsService != null) {
      String username = currentUserDetails.getUsername();
      SessionUserSettings.put(username, userSettingsService.getUserSettings(username, true));
    }
  }

  public static void clearSecurityContext() {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context != null) {
      SecurityContextHolder.getContext().setAuthentication(null);
    }
    SecurityContextHolder.clearContext();
  }

  protected static String getStackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw, true);
    t.printStackTrace(pw);
    pw.flush();
    sw.flush();

    return sw.toString();
  }

  protected ProgramDataElementDimensionItem createProgramDataElement(char name) {
    Program pr = new Program();
    pr.setUid("P123456789" + name);
    pr.setCode("PCode" + name);

    DataElement de = new DataElement("Name" + name);
    de.setUid("D123456789" + name);
    de.setCode("DCode" + name);

    return new ProgramDataElementDimensionItem(pr, de);
  }

  protected void removeUserAccess(IdentifiableObject object) {
    object.getSharing().resetUserAccesses();
  }

  protected void removePublicAccess(IdentifiableObject object) {
    object.getSharing().setPublicAccess("--------");
  }

  protected void enableDataSharing(User user, IdentifiableObject object, String access) {
    object.getSharing().resetUserAccesses();

    UserAccess userAccess = new UserAccess();
    userAccess.setUser(user);
    userAccess.setAccess(access);

    object.getSharing().addUserAccess(userAccess);
  }

  protected void enableDataSharingWithUserGroup(
      UserGroup userGroup, IdentifiableObject object, String access) {
    object.getSharing().resetUserGroupAccesses();

    UserGroupAccess userGroupAccess = new UserGroupAccess();
    userGroupAccess.setUserGroup(userGroup);
    userGroupAccess.setAccess(access);

    object.getSharing().addUserGroupAccess(userGroupAccess);
  }

  private static User createUser(String username, String uid) {
    User user = new User();
    user.setCode(username);
    user.setFirstName(username);
    user.setSurname(username);
    user.setUid(uid);
    return user;
  }

  private static User createUser(String uniquePart) {
    User user = new User();
    user.setCode("Code" + uniquePart);
    user.setFirstName(FIRST_NAME + uniquePart);
    user.setSurname(SURNAME + uniquePart);
    return user;
  }

  protected static UserRole createUserRole(String name, String... authorities) {
    UserRole group = new UserRole();
    group.setCode(name);
    group.setName(name);
    group.setDescription(name);
    group.setAuthorities(Sets.newHashSet(authorities));
    return group;
  }

  protected final User addUser(String uniqueCharacter) {
    return addUser(uniqueCharacter, (Consumer<User>) null);
  }

  protected final <T> User addUser(String uniqueCharacter, BiConsumer<User, T> setter, T value) {
    return addUser(uniqueCharacter, user -> setter.accept(user, value));
  }

  protected final User addUser(String uniqueCharacter, OrganisationUnit... units) {
    return addUser(uniqueCharacter, user -> user.getOrganisationUnits().addAll(asList(units)));
  }

  protected final User addUser(String uniqueCharacter, UserRole... roles) {
    return addUser(uniqueCharacter, user -> user.getUserRoles().addAll(asList(roles)));
  }

  protected final User addUser(String uniqueCharacter, Consumer<User> consumer) {
    uniqueCharacter = uniqueCharacter.toLowerCase();

    User user = createUser(uniqueCharacter);
    user.setUsername("username" + uniqueCharacter);
    user.setEmail("email" + uniqueCharacter);
    if (consumer != null) {
      consumer.accept(user);
    }
    userService.addUser(user);
    return user;
  }

  protected final ProgramSection createProgramSection(char uniqueCharacter, Program program) {
    ProgramSection programSection = new ProgramSection();
    programSection.setProgram(program);
    programSection.setSortOrder(0);
    programSection.setName("ProgramSection" + uniqueCharacter);
    programSection.setAutoFields();
    return programSection;
  }

  private User persistUserAndRoles(User user) {
    for (UserRole role : user.getUserRoles()) {
      role.setName(CodeGenerator.generateUid());
      userService.addUserRole(role);
    }

    userService.addUser(user);

    return user;
  }

  protected User createAndAddUser(String userName) {
    return createAndAddUser(false, userName, null);
  }

  protected User createAndAddUserWithAuth(
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      Authorities... auths) {
    return createAndAddUser(
        organisationUnits, dataViewOrganisationUnits, Authorities.toStringArray(auths));
  }

  protected User createAndAddUser(
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      String... auths) {
    return createAndAddUser(
        false, CodeGenerator.generateUid(), organisationUnits, dataViewOrganisationUnits, auths);
  }

  protected User createAndAddUserWithAuth(
      String userName, OrganisationUnit orgUnit, Authorities... auths) {
    return createAndAddUser(userName, orgUnit, Authorities.toStringArray(auths));
  }

  protected User createAndAddUser(String userName, OrganisationUnit orgUnit, String... auths) {
    return createAndAddUser(false, userName, orgUnit, auths);
  }

  protected User createAndAddUser(
      boolean superUserFlag, String userName, OrganisationUnit orgUnit, String... auths) {
    return createAndAddUser(superUserFlag, userName, orgUnit, null, auths);
  }

  protected User createAndAddUser(
      boolean superUserFlag,
      String userName,
      OrganisationUnit orgUnit,
      OrganisationUnit dataViewOrganisationUnit,
      String... auths) {
    Set<OrganisationUnit> organisationUnits =
        orgUnit == null ? new HashSet<>() : newHashSet(orgUnit);
    Set<OrganisationUnit> dataViewOrganisationUnits =
        dataViewOrganisationUnit != null
            ? newHashSet(dataViewOrganisationUnit)
            : new HashSet<>(organisationUnits);
    User user =
        createUserAndRole(
            superUserFlag, userName, organisationUnits, dataViewOrganisationUnits, auths);

    persistUserAndRoles(user);

    return user;
  }

  protected User createAndAddUser(
      boolean superUserFlag,
      String userName,
      Set<OrganisationUnit> orgUnits,
      Set<OrganisationUnit> dataViewOrgUnits,
      String... auths) {
    User user =
        createUserAndRole(
            superUserFlag,
            userName,
            (orgUnits),
            dataViewOrgUnits != null ? (dataViewOrgUnits) : (orgUnits),
            auths);

    persistUserAndRoles(user);

    return user;
  }

  private User createUserAndRole(
      boolean superUserFlag,
      String username,
      Set<OrganisationUnit> organisationUnits,
      Set<OrganisationUnit> dataViewOrganisationUnits,
      String... auths) {
    UserRole userRole = new UserRole();
    userRole.setName("USER");
    userRole.setAutoFields();
    userRole.getAuthorities().addAll(Arrays.asList(auths));
    if (superUserFlag) {
      userRole.getAuthorities().add("ALL");
    }

    User user = new User();
    user.setUsername(username);
    user.getUserRoles().add(userRole);
    user.setFirstName("First name");
    user.setSurname("Last name");
    user.setOrganisationUnits(organisationUnits);
    user.setDataViewOrganisationUnits(dataViewOrganisationUnits);
    user.setAutoFields();
    user.setCreatedBy(user);

    return user;
  }

  /**
   * Used by setupAdminUser() in SpringIntegrationTestExtension.class, to set up the base admin user
   * for all tests.
   *
   * @return the admin user
   */
  protected User preCreateInjectAdminUser() {
    UserRole role = createUserRole("Superuser", "ALL");
    role.setUid("yrB6vc5Ip3r");

    User user = new User();
    user.setCode("Code" + DEFAULT_USERNAME);
    user.setUuid(UUID.fromString("6507f586-f154-4ec1-a25e-d7aa51de5216"));
    user.setUid(ADMIN_USER_UID);
    user.setFirstName(FIRST_NAME + DEFAULT_USERNAME);
    user.setSurname(SURNAME + DEFAULT_USERNAME);
    user.setUsername(DEFAULT_USERNAME);
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.getUserRoles().add(role);

    user.setCreatedBy(user);
    role.setCreatedBy(user);

    // I assume this is needed so we can save the user
    UserDetails currentUserDetails = userService.createUserDetails(user);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            currentUserDetails, DEFAULT_ADMIN_PASSWORD, List.of(new SimpleGrantedAuthority("ALL")));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    userService.addUser(user);

    user.getUserRoles().forEach(userRole -> userService.addUserRole(userRole));

    userService.encodeAndSetPassword(user, user.getPassword());
    userService.updateUser(user);

    // needed by tests like ControllerWithApiTokenAuthTestBase
    UserDetails userDetails = userService.createUserDetails(user);
    injectSecurityContext(userDetails);

    return user;
  }

  protected RelationshipType createRelTypeConstraint(
      RelationshipEntity from, RelationshipEntity to) {
    RelationshipType relType = new RelationshipType();
    relType.setUid(CodeGenerator.generateUid());
    RelationshipConstraint relationshipConstraintFrom = new RelationshipConstraint();
    relationshipConstraintFrom.setRelationshipEntity(from);
    RelationshipConstraint relationshipConstraintTo = new RelationshipConstraint();
    relationshipConstraintTo.setRelationshipEntity(to);

    relType.setFromConstraint(relationshipConstraintFrom);
    relType.setToConstraint(relationshipConstraintTo);

    return relType;
  }

  protected Dashboard createDashboard(char uniqueChar) {
    Dashboard dashboard = new Dashboard();
    dashboard.setName("Dashboard " + uniqueChar);
    dashboard.setUid(CodeGenerator.generateUid());
    return dashboard;
  }

  protected Layout createLayoutWithColumns(int numColumns) {
    Layout layout = new Layout();
    List<Column> columns = new ArrayList<>();
    for (int i = 0; i < numColumns; i++) {
      Column column = new Column();
      column.setIndex(i);
      column.setSpan(i);
      columns.add(column);
    }
    layout.setColumns(columns);
    return layout;
  }

  public static User createRandomAdminUserWithEntityManager(EntityManager entityManager) {
    UserRole role = createUserRole("Superuser_Test_" + CodeGenerator.generateUid(), "ALL");
    role.setUid(CodeGenerator.generateUid());

    entityManager.persist(role);

    User user = new User();
    user.setUid("A_" + CodeGenerator.generateUid().substring(2));
    user.setFirstName("Admin");
    user.setSurname("User");
    user.setUsername(DEFAULT_USERNAME + "_test_" + CodeGenerator.generateUid());
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.getUserRoles().add(role);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());

    entityManager.persist(user);

    return user;
  }

  /**
   * This test setup allows easy creation of more realistic {@link CategoryOptionCombo}s. It creates
   * multiple {@link CategoryOptionCombo}s that mirror how they are created in live code, (creating
   * {@link Category}s, {@link CategoryOption}s, {@link CategoryCombo}s and then invoking the
   * generation of {@link CategoryOptionCombo}s through the service). {@link CategoryOptionCombo}s
   * are always system-generated and never created in isolation, like most other resources. When
   * system-generated, they always have a {@link CategoryCombo} and {@link CategoryOption}s.
   *
   * @param identifier unique identifier to create different objects
   * @return record of created category types
   */
  protected TestCategoryMetadata setupCategoryMetadata(String identifier) {
    // 4 category options
    CategoryOption co1 =
        createCategoryOption(identifier + " " + categoryCounter++, CodeGenerator.generateUid());
    CategoryOption co2 =
        createCategoryOption(identifier + " " + categoryCounter++, CodeGenerator.generateUid());
    CategoryOption co3 =
        createCategoryOption(identifier + " " + categoryCounter++, CodeGenerator.generateUid());
    CategoryOption co4 =
        createCategoryOption(identifier + " " + categoryCounter++, CodeGenerator.generateUid());

    categoryService.addCategoryOption(co1);
    categoryService.addCategoryOption(co2);
    categoryService.addCategoryOption(co3);
    categoryService.addCategoryOption(co4);

    // 2 categories (each with 2 category options)
    Category cat1 = createCategory(identifier + " " + categoryCounter++, co1, co2);
    Category cat2 = createCategory(identifier + " " + categoryCounter++, co3, co4);
    categoryService.addCategory(cat1);
    categoryService.addCategory(cat2);

    // 1 category combo with 2 categories
    CategoryCombo cc1 = createCategoryCombo(identifier + " " + categoryCounter++, cat1, cat2);
    categoryService.addCategoryCombo(cc1);

    // should generate 4 category option combos ([co1,co3], [co1,co4], [co2,co3], [co2,co4])
    categoryOptionComboGenerateService.addAndPruneOptionCombos(cc1);

    CategoryOptionCombo coc1 = getCocWithOptions(co1.getName(), co3.getName());
    CategoryOptionCombo coc2 = getCocWithOptions(co1.getName(), co4.getName());
    CategoryOptionCombo coc3 = getCocWithOptions(co2.getName(), co3.getName());
    CategoryOptionCombo coc4 = getCocWithOptions(co2.getName(), co4.getName());

    return new TestCategoryMetadata(cc1, cat1, cat2, co1, co2, co3, co4, coc1, coc2, coc3, coc4);
  }

  private static CategoryOptionCombo getCocWithOptions(String co1, String co2) {
    List<CategoryOptionCombo> allCategoryOptionCombos =
        categoryService.getAllCategoryOptionCombos();

    return allCategoryOptionCombos.stream()
        .filter(
            coc -> {
              Set<String> categoryOptions =
                  coc.getCategoryOptions().stream()
                      .map(IdentifiableObject::getName)
                      .collect(Collectors.toSet());
              return categoryOptions.containsAll(List.of(co1, co2));
            })
        .toList()
        .get(0);
  }
}
