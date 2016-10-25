
$(function() {
    dhis2.contextmenu.makeContextMenu({
        menuId: 'contextMenu',
        menuItemActiveClass: 'contextMenuItemActive'
    });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateProgramRuleForm( context ) {
  location.href = 'showUpdateProgramRuleForm.action?id=' + context.id;
}

function showProgramRuleDetails( context ) {
    jQuery.getJSON( 'getProgramRule.action', { id: context.id },
        function ( json ) {
            setInnerHTML( 'nameField', json.programRule.name );	
            setInnerHTML( 'descriptionField', json.programRule.description );
            showDetails();
    });
}


// -----------------------------------------------------------------------------
// Delete Program Rule
// -----------------------------------------------------------------------------

function removeProgramRule(context)
{
    var result = window.confirm( i18n_confirm_delete + "\n\n" + context.name );
    if ( result )
    {
        $.ajax({
            type: "DELETE"
            ,url:  "../api/programRules/" + context.uid
            ,success: function(){

                jQuery( "tr#tr" + context.id ).remove();

                jQuery( "table.listTable tbody tr" ).removeClass( "listRow listAlternateRow" );
                jQuery( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
                jQuery( "table.listTable tbody tr:even" ).addClass( "listRow" );
                jQuery( "table.listTable tbody" ).trigger("update");

                setHeaderDelayMessage( i18n_delete_success );

            }
            ,error:  function(){}
        });
    }
}

// -----------------------------------------------------------------------------
// Program Rule Variables
// --

var program_DataElements = new Array();

function removeDuplicateOptions()
{
    $("#sourceFieldList tr").each(function(){
        var deName = $(this).find('td:first').html();
        var deId = $(this).find('input').attr("deId");
        if(deId){
            program_DataElements[deId] = deName;
        }
    });

    $("#programRuleVariableDiv #dataElementId option").each(function () {
        if(program_DataElements[this.value]) {
            $(this).remove();
        } else {
            program_DataElements[this.value] = this.text;
        }
    });
}

function getDataElementsByStage()
{
    var programStageField = $("#programRuleVariableDiv #programStageId");
    var programStageId = programStageField.val();
    var dataElementField = $("#programRuleVariableDiv #dataElementId");
    clearListById('programRuleVariableDiv #dataElementId');

    if( programStageId != "" )
    {
        $.ajax({
            type: 'GET'
            ,url: "../api/programStages/" + programStageId + ".json?fields=programStageDataElements[dataElement[id,name]]"
            ,dataType: "json"
            ,success: function(json){
                for( var i in json.programStageDataElements )
                {
                    var dataElement = json.programStageDataElements[i].dataElement;
                    dataElementField.append("<option value='" + dataElement.id + "'>" + dataElement.name + "</option>");
                }
                removeAvailableVariables();
            }
        });
    }
    else
    {
        for( var i in program_DataElements )
        {
            dataElementField.append("<option value='" + i + "'>" + program_DataElements[i] + "</option>");
        }
        removeAvailableVariables();
    }
}

function removeAvailableVariables()
{
    $("#sourceFieldList tr").find("input").each(function(){
        var deId = $(this).attr('deId');
        $("#programRuleVariableDiv #dataElementId option").each(function(){
            if( deId == $(this).attr('value') )
            {
                $(this).remove();
            }
        });
    });
}

function validateVariable( dataElementId, variableName )
{
    var valid = true;
    hideById('variableNameError');
    hideById('dataElementIdError');

    if( dataElementId == null || dataElementId == "" )
    {
        setInnerHTML('dataElementIdError', i18n_this_field_is_required);
        showById('dataElementIdError');
        return false;
    } 
    else if( variableName == "" )
    {
        setInnerHTML('variableNameError', i18n_this_field_is_required);
        showById('variableNameError');
        return false;
    } 

    $("#sourceFieldList tr").find("td:last").each(function(){
        if( variableName == $(this).find('input').attr('realValue') )
        {
            setInnerHTML('variableNameError', i18n_name_in_use);
            showById('variableNameError');
            valid = false;
            return;
        }
    });
    return valid;
}

function addProgramRuleVariable()
{
    var sourceType = getFieldValue('sourceType');

    if( sourceType == 'TEI_ATTRIBUTE' ){
        addProgramRuleAttrVariable();
    }
    else{
        addProgramRuleDEVariable();
    }
}

function addProgramRuleAttrVariable()
{
    var attributeId = getFieldValue('programRuleVariableDiv #attributeId');
    var variableName = getFieldValue('variableName');
    if( validateVariable( attributeId, variableName ) )
    {	
        hideById('variableNameError');

        var sourceType = getFieldValue('sourceType');
        var programStageId = getFieldValue('programStageId');
        var attributeName = $('#attributeId option:selected').text();
        var useOptionSetCode = $('#useOptionSetCode').is(":checked") === true;

        var json_Data = getAttrVariableJson( variableName, sourceType, attributeId, useOptionSetCode );

        var clazz = "listAlternateRow";
        if( $("#sourceFieldList tr").length % 2 == 0 )
        {
                clazz = "listRow";
        }

        var row = "<tr class='" + clazz + " newVariable' jsonData='" + json_Data + "'><td>" + attributeName + "</td><td>" + addAttrVariableButton( variableName, attributeId ) +"</td></tr>";
        $("#sourceFieldList").append(row);

        // Remove the data element from Add the Source field form. This data was used, so cannot be used for any variables.

        $('#programRuleVariableDiv #attributeId option[value="' + attributeId + '"]').remove();		
        $("#programRuleVariableDiv").dialog("close");
    }
	
}


function addProgramRuleDEVariable()
{
    var dataElementId = getFieldValue('programRuleVariableDiv #dataElementId');
    var variableName = getFieldValue('variableName');
    if( validateVariable( dataElementId, variableName ) )
    {	
        hideById('variableNameError');

        var sourceType = getFieldValue('sourceType');
        var programStageId = getFieldValue('programStageId');
        var dataElementName = $('#dataElementId option:selected').text();
        var useOptionSetCode = $('#useOptionSetCode').is(":checked") === true;

        var json_Data = getDEVariableJson( variableName, sourceType, dataElementId, programStageId, useOptionSetCode );

        var clazz = "listAlternateRow";
        if( $("#sourceFieldList tr").length % 2 == 0 )
        {
            clazz = "listRow";
        }

        var row = "<tr class='" + clazz + " newVariable' jsonData='" + json_Data + "'><td>" + dataElementName + "</td><td>" + addDEVariableButton( variableName, dataElementId ) +"</td></tr>";
        $("#sourceFieldList").append(row);

        // Remove the data element from Add the Source field form. This data was used, so cannot be used for any variables.

        $('#programRuleVariableDiv #dataElementId option[value="' + dataElementId + '"]').remove();		
        $("#programRuleVariableDiv").dialog("close");
    }
	
}

function addDEVariableButton( name, deId )
{
    return "<input type='button' deId='" + deId + "' realValue='" + name + "' value='#{" + name + "}' style='width:100%;' onclick='insertVariable(this)'/>";
}

function addAttrVariableButton( name, attributeId )
{
    return "<input type='button' attributeId='" + attributeId + "' realValue='" + name + "' value='A{" + name + "}' style='width:100%;' onclick='insertVariable(this)'/>";
}


function insertVariable(_this)
{
    insertTextCommon('condition', _this.value + " "); 
}

function getDEVariableJson( variableName, sourceType, dataElementId, programStageId, useOptionSetCode )
{
    var json_Data = '{ '
        + '"name": "' + variableName + '",'
        + '"programRuleVariableSourceType": "' +  sourceType + '",'
        + '"dataElement": { "id" : "' + dataElementId + '"},'
        + '"program": { "id" :"' + getFieldValue("programId") + '"},'
        + '"useCodeForOptionSet": "' + useOptionSetCode + '"'
        + (programStageId ? ',"programStage": { "id" :  "' + programStageId + '"}' : "")
        + '}';

    return json_Data;
}


function getAttrVariableJson( variableName, sourceType, attributeId, useOptionSetCode )
{
    var json_Data = '{ '
        + '"name": "' + variableName + '",'
        + '"programRuleVariableSourceType": "' +  sourceType + '",'
        + '"trackedEntityAttribute": { "id" : "' + attributeId + '"},'
        + '"useCodeForOptionSet": "' + useOptionSetCode + '",'
        + '"program": { "id" :"' + getFieldValue("programId") + '"}'
        + '}';

    return json_Data;
}

function closeVariableForm()
{
    $("#programRuleVariableDiv").dialog("close");
}

// --
// Program Rule Variables
// -----------------------------------------------------------------------------



// -----------------------------------------------------------------------------
// Add new - Program Rule
// --

// 0 : Program Rule List
// 1 : Add new
// 2 : Update 
var status = 0; 
function validateProgramRule()
{
    status = 0; 
    var valid = true;
    $("#actionTB tr").find(".actionList").each(function(){
        var sourceType = $(this).val();
        var contentCell = $(this).closest("tr").find(".content");
        if( contentCell.val() == "" && sourceType.indexOf("HIDE") < 0 )
        {
            var message = $(this).find('option:selected').attr("errorMessage");
            contentCell.css('background-color', 'pink');
            contentCell.attr('placeholder', message);
            unLockScreen();
            valid = false;
            return;
        }
    });

    if( valid ) {
        status = 1;
        addProgramRule();
    }
    return valid;
}

function addProgramRule()
{
    console.log('trying to save program rule');
    lockScreen();
    var json_Data = { 
        "name": getFieldValue('name'),
        "condition": getFieldValue('condition'),
        "description": getFieldValue('description'),
        "program":{ "id": getFieldValue('programId')}
    };

    var programRuleId = getFieldValue('programRuleId')
    var actionMethod = ( programRuleId === undefined ) ? "POST" : "PUT";
    var url = ( programRuleId === undefined ) ? "../api/programRules/" : "../api/programRules/" + programRuleId;

    $.ajax({
        type: actionMethod
        ,url: url
        ,dataType: "json"
        ,async: false
        ,contentType: "application/json"
        ,data: JSON.stringify(json_Data)
        ,success: function(data){
            console.log('data:  ', data);
            if( data.response && data.response.uid ){
                saveProgramRuleVariable();
                saveAction( data.response.uid );	
            }				
        }
        ,error:  function(){}
    });

    return false;
}


function saveProgramRuleVariable()
{
    $("#sourceFieldList tr.newVariable").each(function(){
        var json_Data = $(this).attr("jsonData");

        $.ajax({
            type: "POST"
            ,url: "../api/programRuleVariables/"
            ,dataType: "json"
            ,async: false
            ,contentType: "application/json"
            ,data: json_Data
            ,success: function(){}
            ,error:  function(){}
        });
    });
}

function saveAction( programRuleId )
{
    console.log('trying to save action..');
    $("#actionTB tr").each(function(){
        var row = $(this);
        var json_Data = { 
            "programRuleActionType": row.find(".actionList").val(),
            "programRule":{ "id":programRuleId },			
            "content": row.find(".content").val()
        };		

        var key = row.find(".actionDEs").val();

        if(key) {
            if(attributeList[key]){
                json_Data.trackedEntityAttribute = {id: key};
            }
            else {
                json_Data.dataElement = {id: key};
            }
        }
        
        if (row.find(".actionSections").val()) {
            json_Data.programStageSection = {id: row.find(".actionSections").val() };
        }

        var actionId = $(this).attr('id');		
        var actionMethod = ( actionId === undefined ) ? "POST" : "PUT";
        var url = ( actionId === undefined ) ? "../api/programRuleActions/" 
            : "../api/programRuleActions/" + actionId;

        $.ajax({
            type: actionMethod
            ,url: url
            ,dataType: "json"
            ,async: false
            ,contentType: "application/json"
            ,data: JSON.stringify(json_Data)
            ,success: function(){}
            ,error:  function(){}
        });
    });
}


$( document ).ajaxStop(function() {
    if( status == 1 )
    {
        status = 0;
        window.location.href='programRule.action?id=' + getFieldValue('programLocalId');
    }
}); 


// --
// Add new - Program Rule
// -----------------------------------------------------------------------------
function sourceTypeOnChange()
{
    var sourceType = getFieldValue("sourceType");
    if( sourceType === "DATAELEMENT_NEWEST_EVENT_PROGRAM" ){
        setFieldValue( "programStageId", "" );
        $("[name='deProgramStage']").hide();
        $("[name='deSourceType']").show();
        $("[name='teiAttrSourceType']").hide();		
    }
    else if( sourceType === "DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE" ){
        $("[name='deProgramStage']").show();
        $("[name='deSourceType']").show();
        $("[name='teiAttrSourceType']").hide();	
    }
    else if( sourceType === "DATAELEMENT_CURRENT_EVENT" ){
        setFieldValue( "programStageId", "" );
        $("[name='deProgramStage']").hide();
        $("[name='deSourceType']").show();
        $("[name='teiAttrSourceType']").hide();		
    }
    else if( sourceType === "DATAELEMENT_PREVIOUS_EVENT" ){
        setFieldValue( "programStageId", "" );
        $("[name='deProgramStage']").hide();
        $("[name='deSourceType']").show();
        $("[name='teiAttrSourceType']").hide();		
    }
    else if( sourceType === "TEI_ATTRIBUTE" ){
        setFieldValue( "programStageId", "" );
        $("[name='deProgramStage']").hide();
        $("[name='deSourceType']").hide();
        $("[name='teiAttrSourceType']").show();	
    }
}

function addSourceFieldForm()
{
    hideById('variableNameError');
    $("#programRuleVariableDiv").dialog({
        title: i18n_add_source_field,
        height: 230,
        width:450,
        modal: true,
        zIndex:99999
    });	
}

function addMoreAction()
{        
    var table = $("#actionTB");
    var dataElementSelector = "<select class='actionDEs' style='width:100%;' >";
    dataElementSelector += "<option value=''>" + i18n_none + "</option>";
    dataElementSelector += "<optgroup label='" + i18n_data_element_label + "'>";
    for( var i in program_DataElements )
    {
        dataElementSelector += "<option value='" + i + "'>" + program_DataElements[i] + "</option>";
    }
    dataElementSelector += "</optgroup>";
    dataElementSelector += "<optgroup label='" + i18n_program_attribute_label + "'>";
    for( var key in attributeList)
    {
        dataElementSelector += "<option value='" + key + "'>" + attributeList[key] + "</option>";
    }
    dataElementSelector += "</optgroup>";
    dataElementSelector += "</select>";

    var clazz = "class='listAlternateRow'";
    if( $("#actionTB tr").length % 2 == 0 )
    {
        clazz = "class='listRow'";
    }
    var row = "<tr " + clazz + ">"
        + "<td><select class='actionList' style='width:100%' onchange='actionListToggle(this)'>"
        + "	<option value='HIDEFIELD' errorMessage='" + i18n_please_enter_alert_message_when_hiding_a_field + "' >" + i18n_hide_field + "</option>"
        + "	<option value='SHOWWARNING' errorMessage='" + i18n_please_enter_warning_message + "' >" + i18n_show_warning + "</option>"
        + "	<option value='SHOWERROR' errorMessage='" + i18n_please_enter_error_message + "' >" + i18n_show_error + "</option>"
        + "     <option value='WARNINGONCOMPLETE' errorMessage='" + i18n_please_enter_warning_message + "' > " + i18n_show_warning_on_complete + "</option>"
        + "     <option value='ERRORONCOMPLETE' errorMessage='" + i18n_please_enter_error_message + "' >" + i18n_show_error_on_complete + "</option>"
        + "	<option value='HIDESECTION' errorMessage='" + i18n_please_enter_alert_message_when_hiding_a_section + "' >" + i18n_hide_section + "</option>"
        + "</select>"
        + "</td>"
        + "<td><input type='text' class='content' style='width:97%;'/></td>"
        + "<td><span class='deCell'>" + dataElementSelector + "</span><span class='sectionCell' style='display:none;'>" + sectionSelector + "</td>"
        + "<td><input class='small-button' type='button' value='-' onclick='javascript:removeActionRow(this)';></td>"
        + "</tr>";
    table.append(row);
}

function actionListToggle(_this)
{
    var selected = _this.value;
    var row = $(_this).closest('tr');
    if( selected == 'HIDESECTION' ){
        row.find('.deCell').hide();
        row.find('.sectionCell').show();
    }
    else{
        row.find('.sectionCell').hide();
        row.find('.deCell').show();
    }
}

function removeActionRow(_this)
{
    $(_this).closest('tr').remove();
}

function variableNameKeyPress(event)
{
    var key = event.keyCode || event.charCode || event.which;

    // Allow Numbers || A-Z || a-z
    if( ( key>=48 && key<=57 ) ||  ( key>=65 && key<=90 ) ||  ( key>=97 && key<=122 ) )
    {
        return true;
    }
    return false;
}

function showUpdateAttributeForm(context)
{
    window.location.href='showUpdateProgramRuleForm.action?id=' + context.id
}

function clearRuleFilter( programId ) {
    setFieldValue('key','');
    jQuery.cookie( "currentPage", null );
    jQuery.cookie( "currentKey", null );
    window.location.href = 'programRule.action?id=' + programId;
}