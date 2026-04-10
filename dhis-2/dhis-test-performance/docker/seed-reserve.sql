-- ===========================================================
-- Performance test seed data — ReserveValuesPerformanceTest
--
-- Reserved values: 1M rows (owneruid = 'perfTestUid')
--   1M expired
--
-- TrackedEntityAttributes: 10 total
--   - 'perfTestUid' : SEQUENTIAL, generated=true
--     (uid matches reservedvalue.owneruid for TEAV join)
--   - 'perfRandUid' : RANDOM, generated=true
--   - 'perfReg0001'..'perfReg0008' : regular, generated=false
--
-- TrackedEntities: 1M rows
--
-- TrackedEntityAttributeValues: 10M rows (10 per TE)
--   - 1M SEQUENTIAL (perfTestUid):
--       first 20K values match reservedvalue.value ('PERF-1'..'PERF-20000')
--       remaining 980K have unique non-matching values
--   - 1M RANDOM (perfRandUid): md5-derived values
--   - 8M regular (8 TEAs × 1M): simple text values
-- ===========================================================

-- ----------------------------------------
-- 1. Reserved values
-- ----------------------------------------
INSERT INTO reservedvalue (reservedvalueid, ownerobject, owneruid, key, value, expirydate, created)
SELECT
  nextval('reservedvalue_sequence'),
  'TRACKEDENTITYATTRIBUTE',
  'perfTestUid',
  'PERF-',
  'PERF-' || i,
  now() - interval '1 day',
  now()
FROM generate_series(1, 1000000) AS i;

-- ----------------------------------------
-- 2. TrackedEntityAttributes
-- ----------------------------------------
CREATE TEMP TABLE perf_tea (tea_type text, tea_id bigint);

WITH ins_seq AS (
  INSERT INTO trackedentityattribute (
    trackedentityattributeid, uid, name, shortname, valuetype, aggregationtype,
    created, lastupdated, generated, pattern,
    skipsynchronization, confidential, inherit, uniquefield,
    displayonvisitschedule, displayinlistnoprogram, orgunitscope
  ) VALUES (
    nextval('hibernate_sequence'),
    'perfTestUid',
    'Perf SEQUENTIAL Attribute', 'PerfSeqAttr',
    'TEXT', 'NONE',
    now(), now(),
    true, 'SEQUENTIAL(####)',
    false, false, false, false, false, false, false
  ) RETURNING trackedentityattributeid
),
ins_rand AS (
  INSERT INTO trackedentityattribute (
    trackedentityattributeid, uid, name, shortname, valuetype, aggregationtype,
    created, lastupdated, generated, pattern,
    skipsynchronization, confidential, inherit, uniquefield,
    displayonvisitschedule, displayinlistnoprogram, orgunitscope
  ) VALUES (
    nextval('hibernate_sequence'),
    'perfRandUid',
    'Perf RANDOM Attribute', 'PerfRandAttr',
    'TEXT', 'NONE',
    now(), now(),
    true, 'RANDOM(XXXXXXXX)',
    false, false, false, false, false, false, false
  ) RETURNING trackedentityattributeid
),
ins_reg AS (
  INSERT INTO trackedentityattribute (
    trackedentityattributeid, uid, name, shortname, valuetype, aggregationtype,
    created, lastupdated, generated,
    skipsynchronization, confidential, inherit, uniquefield,
    displayonvisitschedule, displayinlistnoprogram, orgunitscope
  )
  SELECT
    nextval('hibernate_sequence'),
    'perfReg' || lpad(i::text, 4, '0'),
    'Perf Regular Attribute ' || i, 'PerfRegAttr' || i,
    'TEXT', 'NONE',
    now(), now(),
    false,
    false, false, false, false, false, false, false
  FROM generate_series(1, 8) AS i
  RETURNING trackedentityattributeid
)
INSERT INTO perf_tea
  SELECT 'seq',  trackedentityattributeid FROM ins_seq
  UNION ALL
  SELECT 'rand', trackedentityattributeid FROM ins_rand
  UNION ALL
  SELECT 'reg',  trackedentityattributeid FROM ins_reg;

-- ----------------------------------------
-- 3. TrackedEntities (1M)
-- ----------------------------------------
CREATE TEMP TABLE perf_te (te_id bigint, rn bigint);

WITH ins AS (
  INSERT INTO trackedentity (
    trackedentityid, uid, created, lastupdated, deleted,
    lastsynchronized, organisationunitid, trackedentitytypeid
  )
  SELECT
    nextval('trackedentityinstance_sequence'),
    'PE' || lpad(i::text, 9, '0'),
    now(), now(),
    false,
    to_timestamp(0),
    (SELECT organisationunitid FROM organisationunit LIMIT 1),
    (SELECT trackedentitytypeid FROM trackedentitytype LIMIT 1)
  FROM generate_series(1, 1000000) AS i
  RETURNING trackedentityid
)
INSERT INTO perf_te
SELECT trackedentityid, row_number() OVER () FROM ins;

-- ----------------------------------------
-- 4. TrackedEntityAttributeValues (10M)
-- ----------------------------------------

-- 4a. SEQUENTIAL TEAVs (1M):
--     first 20K match reservedvalue ('PERF-1'..'PERF-20000') → exercised by TEAV join
--     remaining 980K use a non-matching scheme
INSERT INTO trackedentityattributevalue (
  trackedentityid, trackedentityattributeid, created, lastupdated, value
)
SELECT
  te.te_id,
  t.tea_id,
  now(), now(),
  CASE WHEN te.rn <= 20000
    THEN 'PERF-' || te.rn
    ELSE 'SEQ-'  || te.rn
  END
FROM perf_te te
CROSS JOIN (SELECT tea_id FROM perf_tea WHERE tea_type = 'seq') t;

-- 4b. RANDOM TEAVs (1M): md5-derived values
INSERT INTO trackedentityattributevalue (
  trackedentityid, trackedentityattributeid, created, lastupdated, value
)
SELECT
  te.te_id,
  t.tea_id,
  now(), now(),
  substring(md5(te.rn::text || 'R'), 1, 12)
FROM perf_te te
CROSS JOIN (SELECT tea_id FROM perf_tea WHERE tea_type = 'rand') t;

-- 4c. Regular TEAVs (8M: 1M per regular TEA)
INSERT INTO trackedentityattributevalue (
  trackedentityid, trackedentityattributeid, created, lastupdated, value
)
SELECT
  te.te_id,
  t.tea_id,
  now(), now(),
  'REG-' || te.rn
FROM perf_te te
CROSS JOIN (SELECT tea_id FROM perf_tea WHERE tea_type = 'reg') t;

-- ----------------------------------------
-- 5. Cleanup
-- ----------------------------------------
DROP TABLE perf_tea;
DROP TABLE perf_te;
