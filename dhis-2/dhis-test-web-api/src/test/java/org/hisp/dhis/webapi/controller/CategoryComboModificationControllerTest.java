package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;

import org.hisp.dhis.webapi.json.domain.JsonCategoryOptionCombo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClient.Body;

class CategoryComboModificationControllerTest extends DhisControllerConvenienceTest
{
    @Autowired
    private DataValueService dataValueService;

    String testCatCombo;

    String dataElementId;

    String orgUnitId;
    String categoryColor;
    String categoryTaste;


    @Test
    void testModificationNoData() {
        setupTest();
        setTestCatComboModifiableProperties();
        //Remove a category
        assertStatus(
            HttpStatus.OK,
            PUT(
                "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
                "{ 'name' : 'COLOR AND TASTE', 'id' : '" + testCatCombo + "', "
                    + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} ]} "));
    }

    @Test
    void testModificationWithData() {
        setupTest();


        JsonObject response = GET("/categoryCombos/" +testCatCombo + "?fields=categoryOptionCombos[id]").content();
        JsonList<JsonCategoryOptionCombo> catOptionCombos =
            response.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
        String categoryOptionComboId = catOptionCombos.get(0).getId();

        String body =
            format(
                "{"
                    + "'dataElement':'%s',"
                    + "'categoryOptionCombo':'%s',"
                    + "'period':'20220102',"
                    + "'orgUnit':'%s',"
                    + "'value':'24',"
                    + "'comment':'OK'}",
                dataElementId, categoryOptionComboId, orgUnitId);

        assertStatus(HttpStatus.CREATED, POST("/dataValues", body));

        //We should not be able to remove a category here
        assertStatus(
            HttpStatus.CONFLICT,
            PUT(
                "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
                "{ 'name' : 'COLOR AND TASTE', 'id' : '" + testCatCombo + "', "
                    + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} ]} "));
    }

    void setTestCatComboModifiableProperties() {
        //Modify the initial name
        assertStatus(
            HttpStatus.OK,
            PUT(
                "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
                "{ 'name' : 'COLOR AND TASTE', 'id' : '" + testCatCombo + "', "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} , {'id' : '"
                    + categoryTaste
                    + "'}]} "));
        //Add a shortname
        assertStatus(
            HttpStatus.OK,
            PUT(
                "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
                "{ 'name' : 'COLOR AND TASTE', 'id' : '" + testCatCombo + "', "
                    + "'shortName': 'C_AND_T', "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} , {'id' : '"
                    + categoryTaste
                    + "'}]} "));
        //Skip totals
        assertStatus(
            HttpStatus.OK,
            PUT(
                "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
                "{ 'name' : 'COLOR AND TASTE', 'id' : '" + testCatCombo + "', "
                    + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} , {'id' : '"
                    + categoryTaste
                    + "'}]} "));
    }
    void setupTest() {
        String categoryOptionSour =
            assertStatus(
                HttpStatus.CREATED,
                POST("/categoryOptions", "{ 'name': 'Sour', 'shortName': 'Sour' }"));

        String categoryOptionRed =
            assertStatus(
                HttpStatus.CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

        categoryColor =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/categories",
                    "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ,"
                        + "'categoryOptions' : [{'id' : '"
                        + categoryOptionRed
                        + "'} ] }"));

        categoryTaste =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/categories",
                    "{ 'name': 'Taste', 'shortName': 'Taste', 'dataDimensionType': 'DISAGGREGATION' ,"
                        + "'categoryOptions' : [{'id' : '"
                        + categoryOptionSour
                        + "'} ] }"));

        testCatCombo =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/categoryCombos",
                    "{ 'name' : 'Taste and color', "
                        + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                        + "{'id' : '"
                        + categoryColor
                        + "'} , {'id' : '"
                        + categoryTaste
                        + "'}]} "));

        orgUnitId =
            assertStatus(
                HttpStatus.CREATED,
                POST(
                    "/organisationUnits/",
                    "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
        assertStatus(
            HttpStatus.OK,
            POST(
                "/users/{id}/organisationUnits",
                getCurrentUser().getUid(),
                Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));

        dataElementId =
            addDataElement("My data element", "DE1", ValueType.INTEGER, null, testCatCombo);
    }
}
