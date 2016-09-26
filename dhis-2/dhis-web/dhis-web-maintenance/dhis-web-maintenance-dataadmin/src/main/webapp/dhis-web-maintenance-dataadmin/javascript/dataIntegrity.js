$( document ).ready( function() {
    showLoader();

    $.ajax({
        url: '../api/dataIntegrity',
        method: 'POST',
        success: pollDataIntegrityCheckFinished,
        error: function( xhr, txtStatus, err ) {
            showErrorMessage( "Data integrity checks cannot be run. Request failed.", 3 );
            throw Error( xhr.responseText );
        }
    } );
} );

var checkFinishedTimeout = null;

function pollDataIntegrityCheckFinished() {
    pingNotifications( 'DATAINTEGRITY', 'notificationsTable', function() {
        $.getJSON( "../api/system/taskSummaries/dataintegrity", {}, function( json ) {
            hideLoader();
            $( "#di-title" ).hide();
            $( "#di-completed" ).show();
            populateIntegrityItems( json );
            clearTimeout( checkFinishedTimeout );
        } );
    } );
    checkFinishedTimeout = setTimeout( "pollDataIntegrityCheckFinished()", 1500 );
}

function populateIntegrityItems( json ) {

    // Render functions

    var asMap = function( obj, lineBreak ) {
        var violationsText = "";

        for ( var o in obj ) {
            if ( obj.hasOwnProperty( o ) ) {
                violationsText += o + ": " + obj[o];
                violationsText += "<br>" + ( !!lineBreak ? "<br>" : "" );
            }
        }

        return violationsText;
    };

    var asListList = function( obj, lineBreak ) {
        var violationsText = "";

        obj.forEach( function( o ) {
            o.forEach( function( s ) {
                violationsText += s + ", ";
            } );
            violationsText += "<br>" + ( !!lineBreak ? "<br>" : "" );
        } );

        return violationsText;
    };

    var asMapList = function( obj, lineBreak ) {
        var violationsText = "";

        for ( var o in obj ) {
            if( obj.hasOwnProperty( o ) ) {
                violationsText += o + ": ";
                obj[o].forEach( function( s ) {
                    violationsText += s + ", ";
                } );
                violationsText += "<br>" + ( !!lineBreak ? "<br>" : "" );
            }
        }

        return violationsText;
    };

    var asList = function( list, lineBreak ) {
        var violationsText = "";

        list.forEach( function( violation ) {
            violationsText += violation + "<br>" + ( !!lineBreak ? "<br>" : "" );
        } );

        return violationsText;
    };

    // Displays an item using the selected renderer function

    var displayViolation = function( obj, id, lineBreak, renderFunc ) {

        var $button = $( "#" + id + "Button" );
        var $container = $( "#" + id + "Div" );

        if ( typeof obj !== "undefined" ) {
            $button
                .attr( { src: "../images/down.png", title: "View violations" } )
                .css( { cursor: "pointer" } )
                .click( function() { $container.slideToggle( "fast" ); } );

            $container.html( renderFunc( obj, lineBreak ) );

        } else {
            $button.attr({ src: "../images/check.png", title: "No violations" } );
        }

        $container.hide();
    };

    // Display each reported item

    displayViolation( json.dataElementsWithoutDataSet, "dataElementsWithoutDataSet", false, asList );
    displayViolation( json.dataElementsWithoutGroups, "dataElementsWithoutGroups", false, asList );
    displayViolation( json.dataElementsViolatingExclusiveGroupSets, "dataElementsViolatingExclusiveGroupSets", true, asMapList );
    displayViolation( json.dataElementsInDataSetNotInForm, "dataElementsInDataSetNotInForm", true, asMapList );
    displayViolation( json.invalidCategoryCombos, "invalidCategoryCombos", false, asList );
    displayViolation( json.dataElementsAssignedToDataSetsWithDifferentPeriodTypes, "dataElementsAssignedToDataSetsWithDifferentPeriodTypes", true, asMapList );
    displayViolation( json.dataSetsNotAssignedToOrganisationUnits, "dataSetsNotAssignedToOrganisationUnits", false, asList );
    displayViolation( json.indicatorsWithIdenticalFormulas, "indicatorsWithIdenticalFormulas", false, asListList );
    displayViolation( json.indicatorsWithoutGroups, "indicatorsWithoutGroups", false, asList );
    displayViolation( json.invalidIndicatorNumerators, "invalidIndicatorNumerators", true, asMap );
    displayViolation( json.invalidIndicatorDenominators, "invalidIndicatorDenominators", true, asMap );
    displayViolation( json.indicatorsViolatingExclusiveGroupSets, "indicatorsViolatingExclusiveGroupSets", true, asMapList );
    displayViolation( json.duplicatePeriods, "duplicatePeriods", false, asList );
    displayViolation( json.organisationUnitsWithCyclicReferences, "organisationUnitsWithCyclicReferences", false, asList );
    displayViolation( json.orphanedOrganisationUnits, "orphanedOrganisationUnits", false, asList );
    displayViolation( json.organisationUnitsWithoutGroups, "organisationUnitsWithoutGroups", false, asList );
    displayViolation( json.organisationUnitsViolatingExclusiveGroupSets, "organisationUnitsViolatingExclusiveGroupSets", true, asMapList );
    displayViolation( json.organisationUnitGroupsWithoutGroupSets, "organisationUnitGroupsWithoutGroupSets", false, asList );
    displayViolation( json.validationRulesWithoutGroups, "validationRulesWithoutGroups", false, asList );
    displayViolation( json.invalidValidationRuleLeftSideExpressions, "invalidValidationRuleLeftSideExpressions", true, asMapList );
    displayViolation( json.invalidValidationRuleRightSideExpressions, "invalidValidationRuleRightSideExpressions", true, asMapList );
}
