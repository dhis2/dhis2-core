-- Enforce NOT NULL on trackedentityattributevalue.value.
--
-- A tracked entity attribute value row is meaningful only because of its value, so a row with a
-- NULL value carries no information and is not a state the application produces: the tracker
-- importer deletes the row when a value is cleared (it never stores NULL), and the legacy
-- TrackedEntityAttributeValueService skips the save when the value is NULL. The dual-column
-- encryption storage that historically left `value` NULL (with the ciphertext in `encryptedvalue`)
-- was removed in 2.44 (V2_44_6__remove_confidential_from_tea.sql). The only remaining NULL rows are
-- therefore legacy/orphan data, which we remove before adding the constraint. Empty string ('') is
-- intentionally left allowed.
DELETE FROM trackedentityattributevalue
WHERE value IS NULL;

ALTER TABLE trackedentityattributevalue
ALTER COLUMN value SET NOT NULL;
