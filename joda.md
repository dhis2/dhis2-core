# Joda Time to java.time Migration

## Summary

Replaced Joda Time date formatting in `WriteInstantStdSerializer` and `WriteDateStdSerializer` with
`java.time.DateTimeFormatter`.

**Performance improvements:**

* Eliminated unnecessary conversions: `Instant → Date → Joda DateTime` now `Instant → format`
* Reduced object allocations per serialization call
* Removed Joda Time overhead visible in profiler (`BasicChronology.getYearInfo`)
* Simpler, more maintainable code using standard JDK APIs

**Behavior preserved:** Both implementations use system default timezone via `ZoneId.systemDefault()`.

**Jackson best practices compliance:**

* Static `DateTimeFormatter` field is thread-safe (formatter is immutable)
* Jackson reuses single serializer instance per type across threads
* Simple `gen.writeString(formatter.format(value))` pattern matches Jackson's own serializers
* See `jackson-serializer-best-practices.md` for detailed analysis

## Date Serializers Timezone Behavior

### Old Joda Time Implementation

**Code:** `dhis-2/dhis-api/src/main/java/org/hisp/dhis/util/DateUtils.java:183`

```java
ISO8601_NO_TZ.print(new DateTime(date))
```

**Behavior:**

* The `DateTime(Object instant)` constructor constructs the DateTime using the ISO calendar in
  the **default time zone**
* The default timezone is derived from the `user.timezone` system property, falling back to JDK's
  `TimeZone.getDefault()`

**Source:** [Joda-Time DateTime API Documentation](https://www.joda.org/joda-time/apidocs/org/joda/time/DateTime.html)

### New java.time Implementation

**Code:** `WriteInstantStdSerializer` and `WriteDateStdSerializer`

```java
DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
```

**Behavior:**

* `ZoneId.systemDefault()` uses `TimeZone.getDefault()` - the JVM's default timezone
* Maintains the exact same behavior as the Joda Time implementation

### Conclusion

Both implementations use the system's default timezone. The migration maintains identical
behavior while eliminating unnecessary conversions:

* **Old:** `Instant` → `Date` → `Joda DateTime` → `format`
* **New:** `Instant` → `format` (direct)
* **Old:** `Date` → `Joda DateTime` → `format`
* **New:** `Date` → `Instant` → `format` (one conversion)
