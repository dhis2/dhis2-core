-- After the fix of this bug https://jira.dhis2.org/browse/DHIS2-6837
-- it was necessary to clean up duplicated attribute values for data elements.
-- The script finds duplicates of attribute values of the same attribute for the same data element
-- and delete the row from `dataelementattributevalues` table and `attributevalue` table

WITH nonRepeatedIds AS (select min(av2.attributevalueid) as id from attributevalue av2 JOIN dataelementattributevalues deav2 ON av2.attributevalueid = deav2.attributevalueid
        group by deav2.dataelementid, av2.value, av2.attributeid),
    repeatedDataElementAttributeValues AS (select av.attributevalueid as id
                                from attributevalue av JOIN dataelementattributevalues deav ON av.attributevalueid = deav.attributevalueid
                                WHERE av.attributevalueid NOT IN (SELECT id FROM nonRepeatedIds)),
    deleteDataElementRepeatedAttributes AS (delete from  dataelementattributevalues where attributevalueid IN (select id FROM repeatedDataElementAttributeValues))

delete from attributevalue where attributevalueid IN (select id FROM repeatedDataElementAttributeValues);