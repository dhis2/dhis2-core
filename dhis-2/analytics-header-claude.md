# DHIS2 Analytics Headers Flow Analysis

This document explains how headers work in DHIS2 analytics for Enrollment and Events queries.

## Overview

When querying the analytics platform (events and enrollments endpoints), clients can specify a `headers` parameter to control which fields are returned:

```
IpHINAT79UW.json?headers=ouname,enrollmentdate&dimension=ou:ImspTQPwCqd
```

The `headers` parameter:
1. **Filters** the response to only include specified columns
2. **Orders** the response columns in the requested sequence

## Request Flow

```
HTTP Request with headers parameter
    ↓
QueryCriteria.headers = {Set<String>}
    ↓
EventDataQueryRequest.headers = {Set<String>}
    ↓
DefaultEventDataQueryService.withHeaders()
    ↓
EventQueryParams.headers = {LinkedHashSet<String>}
    ↓
Service creates full Grid with all possible headers
    ↓
HeaderHelper.addCommonHeaders() adds dimension/period/item headers
    ↓
ResponseHelper.applyHeaders() calls grid.retainColumns(params.getHeaders())
    ↓
ListGrid.retainColumns() removes unwanted columns and reorders remaining ones
    ↓
JSON Response with only requested headers
```

## Key Components

### 1. Request Parameter Parsing

Headers are parsed from query parameters as comma-separated values.

**EnrollmentAnalyticsQueryCriteria.java** (lines 86-90):
```java
/**
 * This parameter selects the headers to be returned as part of the response. The implementation
 * for this Set will be LinkedHashSet as the ordering is important.
 */
private Set<String> headers = new HashSet<>();
```

The same pattern exists in `EventsAnalyticsQueryCriteria.java` (lines 116-120).

### 2. Headers Flow to EventDataQueryRequest

Controllers convert criteria to `EventDataQueryRequest`.

**EventDataQueryRequest.java**:
- For Events (line 272): `.headers(criteria.getHeaders())`
- For Enrollments (line 353): `.headers(criteria.getHeaders())`

### 3. Headers Storage in EventQueryParams

**EventQueryParams.java** (lines 150-153):
```java
/**
 * The headers to be returned. Does not make sense to be repeated and should keep ordering, hence
 * a {@link LinkedHashSet}.
 */
protected Set<String> headers = new LinkedHashSet<>();
```

The `LinkedHashSet` preserves insertion order, which is critical for response column ordering.

### 4. Grid Creation with Headers

#### EnrollmentAggregateService (lines 128-131):
```java
private Grid createGridWithHeaders() {
  return new ListGrid()
      .addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false));
}
```

#### EventQueryService (lines 202-304):
Creates the grid with all standard headers (event, program stage, enrollment, tracked entity, etc.).

### 5. Adding Common Headers

**HeaderHelper.java** (lines 53-128) adds dimension, period, and query item headers:
```java
public static void addCommonHeaders(
    Grid grid, EventQueryParams params, List<DimensionalObject> periods) {

  for (DimensionalObject dimension : params.getDimensions()) {
    grid.addHeader(
        new GridHeader(
            dimension.getDimension(), dimension.getDimensionDisplayName(), TEXT, false, true));
  }
  // ... adds period headers, query item headers ...
}
```

## Filtering Mechanism (Critical)

### ResponseHelper.applyHeaders()

**ResponseHelper.java** (lines 61-65):
```java
public static void applyHeaders(Grid grid, EventQueryParams params) {
  if (params.hasHeaders()) {
    grid.retainColumns(params.getHeaders());
  }
}
```

Called from:
- `EnrollmentAggregateService` (line 118)
- `EventQueryService` (line 129)

### ListGrid.retainColumns()

**ListGrid.java** (lines 1045-1061):
```java
public void retainColumns(Set<String> headers) {
  if (headers != null && !headers.isEmpty()) {
    List<String> exclusions = getHeaders().stream().map(GridHeader::getName).collect(toList());
    exclusions.removeAll(headers);  // Calculate which headers to remove

    for (String headerToExclude : exclusions) {
      int headerIndex = getIndexOfHeader(headerToExclude);
      boolean hasHeader = headerIndex != -1;

      if (hasHeader) {
        removeColumn(getHeaders().get(headerIndex));
      }
    }

    repositionColumns(repositionHeaders(new ArrayList<>(headers)));  // Reorder remaining columns
  }
}
```

This method:
1. Identifies columns to exclude (those not in the requested headers)
2. Removes excluded columns
3. Reorders remaining columns to match the requested order

### Column Repositioning

The `repositionHeaders` method (lines 1064-1084) reorders headers to match the requested order.

The `repositionColumns` method (lines 1087-1124) reorders row data accordingly, including updating row context metadata with correct column indices.

## JSON Response Structure

The final JSON response includes a `headers` array that reflects the filtered and ordered columns:

```json
{
  "headers": [
    {"name": "ouname", "column": "Organisation unit name", "valueType": "TEXT", ...},
    {"name": "enrollmentdate", "column": "Enrollment date", "valueType": "DATETIME", ...}
  ],
  "rows": [
    ["District A", "2021-01-15"],
    ["District B", "2021-02-20"]
  ]
}
```

## Key Design Insights

1. **LinkedHashSet for Order Preservation**: Headers are stored in `LinkedHashSet` to preserve the order specified in the request.

2. **Lazy Filtering**: The grid is built with all possible headers first, then filtered at the end after data is added. This ensures all necessary metadata is available during processing.

3. **Data Alignment**: The `repositionColumns` method ensures row data is aligned with the new header order, including updating row context data with correct column indices.

4. **Consistent Flow**: Headers flow through the entire system (Criteria → Request → Params → Grid operations) without transformation, preserving the user's requested header list.

## Key Files Reference

| Component | File Path |
|-----------|-----------|
| Enrollment Query Criteria | `dhis-api/src/main/java/org/hisp/dhis/common/EnrollmentAnalyticsQueryCriteria.java` |
| Events Query Criteria | `dhis-api/src/main/java/org/hisp/dhis/common/EventsAnalyticsQueryCriteria.java` |
| Event Query Params | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/EventQueryParams.java` |
| Event Data Query Service | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/data/DefaultEventDataQueryService.java` |
| Enrollment Aggregate Service | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/data/EnrollmentAggregateService.java` |
| Event Query Service | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/data/EventQueryService.java` |
| Response Helper | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/data/ResponseHelper.java` |
| Header Helper | `dhis-services/dhis-service-analytics/src/main/java/org/hisp/dhis/analytics/event/data/HeaderHelper.java` |
| List Grid | `dhis-support/dhis-support-system/src/main/java/org/hisp/dhis/system/grid/ListGrid.java` |
