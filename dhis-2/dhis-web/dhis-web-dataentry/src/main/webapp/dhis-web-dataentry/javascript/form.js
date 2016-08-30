
dhis2.util.namespace( 'dhis2.de' );
dhis2.util.namespace( 'dhis2.de.api' );
dhis2.util.namespace( 'dhis2.de.event' );
dhis2.util.namespace( 'dhis2.de.cst' );

// API / methods to be used externally from forms / scripts

/**
 * Returns an object representing the currently selected state of the UI.
 * Contains properties for "ds", "pe", "ou" and the identifier of each
 * category with matching identifier values of the selected option.
 */
dhis2.de.api.getSelections = function() {
	var sel = dhis2.de.getCurrentCategorySelections();
	sel["ds"] = $( '#selectedDataSetId' ).val(),
	sel["pe"] = $( '#selectedPeriodId').val(),
	sel["ou"] = dhis2.de.currentOrganisationUnitId;	
	return sel;
}

// whether current user has any organisation units
dhis2.de.emptyOrganisationUnits = false;

// Identifiers for which zero values are insignificant, also used in entry.js
dhis2.de.significantZeros = [];

// Array with associative arrays for each data element, populated in select.vm
dhis2.de.dataElements = [];

// Associative array with [indicator id, expression] for indicators in form,
// also used in entry.js
dhis2.de.indicatorFormulas = [];

// Array with associative arrays for each data set, populated in select.vm
dhis2.de.dataSets = [];

// Maps input field to optionSet
dhis2.de.optionSets = {};

// Associative array with identifier and array of assigned data sets
dhis2.de.dataSetAssociationSets = [];

// Associate array with mapping between organisation unit identifier and data
// set association set identifier
dhis2.de.organisationUnitAssociationSetMap = [];

// Default category combo uid
dhis2.de.defaultCategoryCombo = undefined;

// Category combinations for data value attributes
dhis2.de.categoryCombos = {};

// Categories for data value attributes
dhis2.de.categories = {};

// Array with keys {dataelementid}-{optioncomboid}-min/max with min/max values
dhis2.de.currentMinMaxValueMap = [];

// Indicates whether any data entry form has been loaded
dhis2.de.dataEntryFormIsLoaded = false;

// Indicates whether meta data is loaded
dhis2.de.metaDataIsLoaded = false;

// Currently selected organisation unit identifier
dhis2.de.currentOrganisationUnitId = null;

// Currently selected data set identifier
dhis2.de.currentDataSetId = null;

// Array with category objects, null if default category combo / no categories
dhis2.de.currentCategories = null;

// Current offset, next or previous corresponding to increasing or decreasing value
dhis2.de.currentPeriodOffset = 0;

// Current existing data value, prior to entry or modification
dhis2.de.currentExistingValue = null;

// Associative array with currently-displayed period choices, keyed by iso
dhis2.de.periodChoices = [];

// Username of user who marked the current data set as complete if any
dhis2.de.currentCompletedByUser = null;

// Instance of the StorageManager
dhis2.de.storageManager = new StorageManager();

// Indicates whether current form is multi org unit
dhis2.de.multiOrganisationUnit = false;

// Indicates whether multi org unit is enabled on instance
dhis2.de.multiOrganisationUnitEnabled = false;

// Simple object to see if we have tried to fetch children DS for a parent before
dhis2.de.fetchedDataSets = {};

// "organisationUnits" object inherited from ouwt.js

// Constants

dhis2.de.cst.defaultType = 'INTEGER';
dhis2.de.cst.defaultName = '[unknown]';
dhis2.de.cst.dropDownMaxItems = 30;
dhis2.de.cst.formulaPattern = /#\{.+?\}/g;
dhis2.de.cst.separator = '.';
dhis2.de.cst.valueMaxLength = 50000;
dhis2.de.cst.metaData = 'dhis2.de.cst.metaData';
dhis2.de.cst.dataSetAssociations = 'dhis2.de.cst.dataSetAssociations';

// Colors

dhis2.de.cst.colorGreen = '#b9ffb9';
dhis2.de.cst.colorYellow = '#fffe8c';
dhis2.de.cst.colorRed = '#ff8a8a';
dhis2.de.cst.colorOrange = '#ff6600';
dhis2.de.cst.colorWhite = '#fff';
dhis2.de.cst.colorGrey = '#ccc';
dhis2.de.cst.colorBorderActive = '#73ad72';
dhis2.de.cst.colorBorder = '#aaa';

// Form types

dhis2.de.cst.formTypeCustom = 'CUSTOM';
dhis2.de.cst.formTypeSection = 'SECTION';
dhis2.de.cst.formTypeMultiOrgSection = 'SECTION_MULTIORG';
dhis2.de.cst.formTypeDefault = 'DEFAULT';

// Events

dhis2.de.event.formLoaded = "dhis2.de.event.formLoaded";
dhis2.de.event.dataValuesLoaded = "dhis2.de.event.dataValuesLoaded";
dhis2.de.event.formReady = "dhis2.de.event.formReady";
dhis2.de.event.dataValueSaved = "dhis2.de.event.dataValueSaved";
dhis2.de.event.completed = "dhis2.de.event.completed";
dhis2.de.event.uncompleted = "dhis2.de.event.uncompleted";
dhis2.de.event.validationSucces = "dhis2.de.event.validationSuccess";
dhis2.de.event.validationError = "dhis2.de.event.validationError";

/**
 * Convenience method to be used from inside custom forms. When a function is
 * registered inside a form it will be loaded every time the form is loaded,
 * hence the need to unregister and the register the function.
 */
dhis2.de.on = function( event, fn )
{
    $( document ).off( event ).on( event, fn );
};

var DAO = DAO || {};

dhis2.de.getCurrentOrganisationUnit = function() 
{
    if ( $.isArray( dhis2.de.currentOrganisationUnitId ) ) 
    {
        return dhis2.de.currentOrganisationUnitId[0];
    }

    return dhis2.de.currentOrganisationUnitId;
};

DAO.store = new dhis2.storage.Store( {
    name: 'dhis2de',
    adapters: [ dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter ],
    objectStores: [ 'optionSets', 'forms' ]
} );

( function( $ ) {
    $.safeEach = function( arr, fn ) 
    {
        if ( arr )
        {
            $.each( arr, fn );
        }
    };
} )( jQuery );

$(document).bind('dhis2.online', function( event, loggedIn ) {
    if( loggedIn ) {
        if( dhis2.de.storageManager.hasLocalData() ) {
            var message = i18n_need_to_sync_notification
              + ' <button id="sync_button" type="button">' + i18n_sync_now + '</button>';

            setHeaderMessage(message);

            $('#sync_button').bind('click', dhis2.de.uploadLocalData);
        }
        else {
            if( dhis2.de.emptyOrganisationUnits ) {
                setHeaderMessage(i18n_no_orgunits);
            }
            else {
                setHeaderDelayMessage(i18n_online_notification);
            }
        }
    }
    else {
        var form = [
            '<form style="display:inline;">',
            '<label for="username">Username</label>',
            '<input name="username" id="username" type="text" style="width: 70px; margin-left: 10px; margin-right: 10px" size="10"/>',
            '<label for="password">Password</label>',
            '<input name="password" id="password" type="password" style="width: 70px; margin-left: 10px; margin-right: 10px" size="10"/>',
            '<button id="login_button" type="button">Login</button>',
            '</form>'
        ].join('');

        setHeaderMessage(form);
        dhis2.de.ajaxLogin();
    }
});

$(document).bind('dhis2.offline', function() {
    if( dhis2.de.emptyOrganisationUnits ) {
        setHeaderMessage(i18n_no_orgunits);
    }
    else {
        setHeaderMessage(i18n_offline_notification);
    }
});

/**
 * Page init. The order of events is:
 *
 * 1. Load ouwt 2. Load meta-data (and notify ouwt) 3. Check and potentially
 * download updated forms from server
 */
$( document ).ready( function()
{
    /**
     * Cache false necessary to prevent IE from caching by default.
     */
    $.ajaxSetup( {
        cache: false
    } );

    $( '#loaderSpan' ).show();

    $( '#orgUnitTree' ).one( 'ouwtLoaded', function( event, ids, names )
    {
        console.log( 'Ouwt loaded' );
                
        $.when( dhis2.de.getMultiOrgUnitSetting(), dhis2.de.loadMetaData(), dhis2.de.loadDataSetAssociations() ).done( function() {
        	dhis2.de.setMetaDataLoaded();
          organisationUnitSelected( ids, names );
        } );
    } );
} );

dhis2.de.shouldFetchDataSets = function( ids ) {
    if( !dhis2.de.multiOrganisationUnitEnabled ) {
        return false;
    }

    if( !$.isArray(ids) || ids.length == 0 || (ids.length > 0 && dhis2.de.fetchedDataSets[ids[0]]) ) {
        return false;
    }

    var c = organisationUnits[ids[0]].c;

    if( $.isArray(c) && c.length > 0 && dhis2.de.organisationUnitAssociationSetMap[c[0]] ) {
        return false;
    }

    return true;
};

dhis2.de.getMultiOrgUnitSetting = function()
{
  return $.ajax({
    url: '../api/systemSettings/multiOrganisationUnitForms',
    dataType: 'json',
    async: false,
    type: 'GET',
    success: function( data ) {
      dhis2.de.multiOrganisationUnitEnabled = data;
      selection.setIncludeChildren(data);
    }
  });
};

dhis2.de.ajaxLogin = function()
{
    $( '#login_button' ).bind( 'click', function()
    {
        var username = $( '#username' ).val();
        var password = $( '#password' ).val();

        $.post( '../dhis-web-commons-security/login.action', {
            'j_username' : username,
            'j_password' : password
        } ).success( function()
        {
            var ret = dhis2.availability.syncCheckAvailability();

            if ( !ret )
            {
                alert( i18n_ajax_login_failed );
            }
        } );
    } );
};

dhis2.de.loadMetaData = function()
{
    var def = $.Deferred();
	
    $.ajax( {
    	url: 'getMetaData.action',
    	dataType: 'json',
    	success: function( json )
	    {
	        sessionStorage[dhis2.de.cst.metaData] = JSON.stringify( json.metaData );
	    },
	    complete: function()
	    {
	        var metaData = JSON.parse( sessionStorage[dhis2.de.cst.metaData] );
	        dhis2.de.emptyOrganisationUnits = metaData.emptyOrganisationUnits;
	        dhis2.de.significantZeros = metaData.significantZeros;
	        dhis2.de.dataElements = metaData.dataElements;
	        dhis2.de.indicatorFormulas = metaData.indicatorFormulas;
	        dhis2.de.dataSets = metaData.dataSets;
	        dhis2.de.optionSets = metaData.optionSets;
	        dhis2.de.defaultCategoryCombo = metaData.defaultCategoryCombo;
	        dhis2.de.categoryCombos = metaData.categoryCombos;
	        dhis2.de.categories = metaData.categories;	        
	        def.resolve();
	    }
	} );
    
    return def.promise();
};

dhis2.de.loadDataSetAssociations = function()
{
	var def = $.Deferred();
	
	$.ajax( {
    	url: 'getDataSetAssociations.action',
    	dataType: 'json',
    	success: function( json )
	    {
	        sessionStorage[dhis2.de.cst.dataSetAssociations] = JSON.stringify( json.dataSetAssociations );
	    },
	    complete: function()
	    {
	        var metaData = JSON.parse( sessionStorage[dhis2.de.cst.dataSetAssociations] );
	        dhis2.de.dataSetAssociationSets = metaData.dataSetAssociationSets;
	        dhis2.de.organisationUnitAssociationSetMap = metaData.organisationUnitAssociationSetMap;	        
	        def.resolve();
	    }
	} );
	
	return def.promise();
};

dhis2.de.setMetaDataLoaded = function()
{
    dhis2.de.metaDataIsLoaded = true;
    $( '#loaderSpan' ).hide();
    console.log( 'Meta-data loaded' );

    updateForms();
};

dhis2.de.discardLocalData = function() {
    if( confirm( i18n_remove_local_data ) ) {
        dhis2.de.storageManager.clearAllDataValues();
        hideHeaderMessage();
    }
};

dhis2.de.uploadLocalData = function()
{
    if ( !dhis2.de.storageManager.hasLocalData() )
    {
        return;
    }

    var dataValues = dhis2.de.storageManager.getAllDataValues();
    var completeDataSets = dhis2.de.storageManager.getCompleteDataSets();

    setHeaderWaitMessage( i18n_uploading_data_notification );

    var dataValuesArray = dataValues ? Object.keys( dataValues ) : [];
    var completeDataSetsArray = completeDataSets ? Object.keys( completeDataSets ) : [];

    function pushCompleteDataSets( array )
    {
        if ( array.length < 1 )
        {
            return;
        }

        var key = array[0];
        var value = completeDataSets[key];

        console.log( 'Uploaded complete data set: ' + key + ', with value: ' + value );

        $.ajax( {
            url: '../api/completeDataSetRegistrations',
            data: value,
            dataType: 'json',
            success: function( data, textStatus, jqXHR )
            {
            	dhis2.de.storageManager.clearCompleteDataSet( value );
                console.log( 'Successfully saved complete dataset with value: ' + value );
                ( array = array.slice( 1 ) ).length && pushCompleteDataSets( array );

                if ( array.length < 1 )
                {
                    setHeaderDelayMessage( i18n_sync_success );
                }
            },
            error: function( jqXHR, textStatus, errorThrown )
            {
            	if ( 409 == xhr.status || 500 == xhr.status ) // Invalid value or locked
            	{
            		// Ignore value for now TODO needs better handling for locking
            		
            		dhis2.de.storageManager.clearCompleteDataSet( value );
            	}
            	else // Connection lost during upload
        		{
                    var message = i18n_sync_failed
                        + ' <button id="sync_button" type="button">' + i18n_sync_now + '</button>'
                        + ' <button id="discard_button" type="button">' + i18n_discard + '</button>';

                    setHeaderMessage( message );

                    $( '#sync_button' ).bind( 'click', dhis2.de.uploadLocalData );
                    $( '#discard_button' ).bind( 'click', dhis2.de.discardLocalData );
        		}
            }
        } );
    }

    ( function pushDataValues( array )
    {
        if ( array.length < 1 )
        {
            setHeaderDelayMessage( i18n_online_notification );

            pushCompleteDataSets( completeDataSetsArray );

            return;
        }

        var key = array[0];
        var value = dataValues[key];

        if ( value !== undefined && value.value !== undefined && value.value.length > 254 )
        {
            value.value = value.value.slice(0, 254);
        }

        console.log( 'Uploading data value: ' + key + ', with value: ' + value );

        $.ajax( {
            url: '../api/dataValues',
            data: value,
            dataType: 'json',
            type: 'post',
            success: function( data, textStatus, xhr )
            {
            	dhis2.de.storageManager.clearDataValueJSON( value );
                console.log( 'Successfully saved data value with value: ' + value );
                ( array = array.slice( 1 ) ).length && pushDataValues( array );

                if ( array.length < 1 && completeDataSetsArray.length > 0 )
                {
                    pushCompleteDataSets( completeDataSetsArray );
                }
                else
                {
                    setHeaderDelayMessage( i18n_sync_success );
                }
            },
            error: function( xhr, textStatus, errorThrown )
            {
            	if ( 409 == xhr.status || 500 == xhr.status ) // Invalid value or locked
            	{
            		// Ignore value for now TODO needs better handling for locking
            		
            		dhis2.de.storageManager.clearDataValueJSON( value );
            	}
            	else // Connection lost during upload
            	{
	                var message = i18n_sync_failed
                    + ' <button id="sync_button" type="button">' + i18n_sync_now + '</button>'
                    + ' <button id="discard_button" type="button">' + i18n_discard + '</button>';

	                setHeaderMessage( message );

	                $( '#sync_button' ).bind( 'click', dhis2.de.uploadLocalData );
                  $( '#discard_button' ).bind( 'click', dhis2.de.discardLocalData );
            	}
            }
        } );
    } )( dataValuesArray );
};

dhis2.de.addEventListeners = function()
{
    $( '.entryfield' ).each( function( i )
    {
        var id = $( this ).attr( 'id' );

        // If entry field is a date picker, remove old target field, and change id
        if( /-dp$/.test( id ) )
        {
            var dpTargetId = id.substring( 0, id.length - 3 );
            $( '#' + dpTargetId ).remove();
            $( this ).attr( 'id', dpTargetId ).calendarsPicker( 'destroy' );
            id = dpTargetId;
        }

        var split = dhis2.de.splitFieldId( id );
        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;
        dhis2.de.currentOrganisationUnitId = split.organisationUnitId;

        var type = getDataElementType( dataElementId );

        $( this ).unbind( 'focus' );
        $( this ).unbind( 'blur' );
        $( this ).unbind( 'change' );
        $( this ).unbind( 'dblclick' );
        $( this ).unbind( 'keyup' );

        $( this ).focus( valueFocus );

        $( this ).blur( valueBlur );

        $( this ).change( function()
        {
            saveVal( dataElementId, optionComboId, id );
        } );

        $( this ).dblclick( function()
        {
            viewHist( dataElementId, optionComboId );
        } );

        $( this ).keyup( function( event )
        {
            keyPress( event, this );
        } );

        if ( type == 'DATE' )
        {
            // Fake event, needed for valueBlur / valueFocus when using date-picker
            var fakeEvent = {
                target: {
                    id: id + '-dp'
                }
            };

            dhis2.period.picker.createInstance( '#' + id, false, {
                onSelect: function() {
                    saveVal( dataElementId, optionComboId, id, fakeEvent.target.id );
                },
                onClose: function() {
                    valueBlur( fakeEvent );
                },
                onShow: function() {
                    valueFocus( fakeEvent );
                },
                minDate: null,
                maxDate: null
            } );
        }
    } );

    $( '.entryselect' ).each( function( i )
    {
        var id = $( this ).attr( 'id' );
        var split = dhis2.de.splitFieldId( id );

        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;       

        $( this ).change( function()
        {
            saveBoolean( dataElementId, optionComboId, id );
        } );
    } );

    $( '.entrytrueonly' ).each( function( i )
    {
        var id = $( this ).attr( 'id' );
        var split = dhis2.de.splitFieldId( id );

        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;

        $( this ).unbind( 'focus' );
        $( this ).unbind( 'change' );

        $( this ).focus( valueFocus );
        $( this ).blur( valueBlur );

        $( this ).change( function()
        {
            saveTrueOnly( dataElementId, optionComboId, id );
        } );
    } );

    $( '.commentlink' ).each( function( i )
    {
        var id = $( this ).attr( 'id' );
        var split = dhis2.de.splitFieldId( id );

        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;

        $( this ).unbind( 'click' );

        $( this ).attr( "src", "../images/comment.png" );
        $( this ).attr( "title", i18n_view_comment );

        $( this ).css( "cursor", "pointer" );

        $( this ).click( function()
        {
            viewHist( dataElementId, optionComboId );
        } );
    } );

    $( '.entryfileresource' ).each( function()
    {
        $( this ).fileEntryField();
    } );
}

dhis2.de.resetSectionFilters = function()
{
    $( '#filterDataSetSectionDiv' ).hide();
    $( '.formSection' ).show();
}

dhis2.de.clearSectionFilters = function()
{
    $( '#filterDataSetSection' ).children().remove();
    $( '#filterDataSetSectionDiv' ).hide();
    $( '.formSection' ).show();
}

dhis2.de.clearPeriod = function()
{
    clearListById( 'selectedPeriodId' );
    dhis2.de.clearEntryForm();
}

dhis2.de.clearEntryForm = function()
{
    $( '#contentDiv' ).html( '' );

    dhis2.de.currentPeriodOffset = 0;

    dhis2.de.dataEntryFormIsLoaded = false;

    $( '#completenessDiv' ).hide();
    $( '#infoDiv' ).hide();
}

dhis2.de.loadForm = function()
{
	var dataSetId = dhis2.de.currentDataSetId;
	
	dhis2.de.currentOrganisationUnitId = selection.getSelected()[0];

    if ( !dhis2.de.multiOrganisationUnit  )
    {
        dhis2.de.storageManager.formExists( dataSetId ).done( function( value ) 
        {           
	    	if ( value ) 
	    	{
	            console.log( 'Loading form locally: ' + dataSetId );
	
	            dhis2.de.storageManager.getForm( dataSetId ).done( function( html ) 
	            {
	                $( '#contentDiv' ).html( html );

	                if ( dhis2.de.dataSets[dataSetId].renderAsTabs )
	                {
	                    $( "#tabs" ).tabs();
	                }

	                dhis2.de.enableSectionFilter();	               
	                $( document ).trigger( dhis2.de.event.formLoaded, dhis2.de.currentDataSetId );
	
	                loadDataValues();
                    var table = $( '.sectionTable' );
                    table.floatThead({
                        position: 'auto',
                        top: 44,
                        zIndex: 9
                    });

                  dhis2.de.insertOptionSets();

	            } );
	        } 
	    	else {
                dhis2.de.storageManager.formExistsRemotely( dataSetId ).done( function( value ) {
                    console.log( 'Loading form remotely: ' + dataSetId );

       	            dhis2.de.storageManager.getForm( dataSetId ).done( function( html )
       	            {
       	                $( '#contentDiv' ).html( html );

       	                if ( dhis2.de.dataSets[dataSetId].renderAsTabs )
       	                {
       	                    $( "#tabs" ).tabs();
       	                }

       	                dhis2.de.enableSectionFilter();
       	                $( document ).trigger( dhis2.de.event.formLoaded, dhis2.de.currentDataSetId );

       	                loadDataValues();
       	                dhis2.de.insertOptionSets();
       	            } );
                });
            }
        } );
    }
    else
    {
        console.log( 'Loading form remotely: ' + dataSetId );

        $( '#contentDiv' ).load( 'loadForm.action', 
        {
            dataSetId : dataSetId,
            multiOrganisationUnit: dhis2.de.multiOrganisationUnit ? dhis2.de.getCurrentOrganisationUnit() : ''
        }, 
        function() 
        {
            if ( !dhis2.de.multiOrganisationUnit )
            {
                if ( dhis2.de.dataSets[dataSetId].renderAsTabs ) 
                {
                    $( "#tabs" ).tabs();
                }

                dhis2.de.enableSectionFilter();
            }
            else
            {
                $( '#currentOrganisationUnit' ).html( i18n_no_organisationunit_selected );
            }

            dhis2.de.insertOptionSets();
            loadDataValues();
        } );
    }
}

//------------------------------------------------------------------------------
// Section filter
//------------------------------------------------------------------------------

dhis2.de.enableSectionFilter = function()
{
    var $sectionHeaders = $( '.formSection .cent h3' );
    dhis2.de.clearSectionFilters();

    if ( $sectionHeaders.size() > 1)
    {
        $( '#filterDataSetSection' ).append( "<option value='all'>" + i18n_show_all_sections + "</option>" );

        $sectionHeaders.each( function( idx, value ) 
        {
            $( '#filterDataSetSection' ).append( "<option value='" + idx + "'>" + value.innerHTML + "</option>" );
        } );

        $( '#filterDataSetSectionDiv' ).show();
    }
    else
    {
        $( '#filterDataSetSectionDiv' ).hide();
    }
}

dhis2.de.filterOnSection = function()
{
    var $filterDataSetSection = $( '#filterDataSetSection' );
    var value = $filterDataSetSection.val();

    if ( value == 'all' )
    {
        $( '.formSection' ).show();
    }
    else
    {
        $( '.formSection' ).hide();
        $( $( '.formSection' )[value] ).show();
    }
}

dhis2.de.filterInSection = function( $this )
{
    var $tbody = $this.closest('tbody').find("#sectionTable tbody");
    var thisTable = $tbody.parent().get(0);
    var $trTarget = $tbody.find( 'tr');

    if ( $this.val() == '' )
    {
        $trTarget.show();
    }
    else
    {
        var $trTargetChildren = $trTarget.find( 'td:first-child' );

        $trTargetChildren.each( function( idx, item ) 
        {
            var text1 = $this.val().toUpperCase();
            var text2 = $( item ).find( 'span' ).html().toUpperCase();

            if ( text2.indexOf( text1 ) >= 0 )
            {
                $( item ).parent().show();
            }
            else
            {
                $( item ).parent().hide();
            }
        } );
    }

    refreshZebraStripes( $tbody );
    $.each($( '.sectionTable' ), function(index, table){
        if(table == thisTable) return;
        $(table).trigger( 'reflow' );
    });
}

//------------------------------------------------------------------------------
// Supportive methods
//------------------------------------------------------------------------------

/**
 * Splits an id based on the multi org unit variable.
 */
dhis2.de.splitFieldId = function( id )
{
    var split = {};

    if ( dhis2.de.multiOrganisationUnit )
    {
        split.organisationUnitId = id.split( '-' )[0];
        split.dataElementId = id.split( '-' )[1];
        split.optionComboId = id.split( '-' )[2];
    }
    else
    {
        split.organisationUnitId = dhis2.de.getCurrentOrganisationUnit();
        split.dataElementId = id.split( '-' )[0];
        split.optionComboId = id.split( '-' )[1];
    }

    return split;
}

function refreshZebraStripes( $tbody )
{
    $tbody.find( 'tr:not([colspan]):visible:even' ).find( 'td:first-child' ).removeClass( 'reg alt' ).addClass( 'alt' );
    $tbody.find( 'tr:not([colspan]):visible:odd' ).find( 'td:first-child' ).removeClass( 'reg alt' ).addClass( 'reg' );
}

function getDataElementType( dataElementId )
{
	if ( dhis2.de.dataElements[dataElementId] != null )
	{
		return dhis2.de.dataElements[dataElementId];
	}

	console.log( 'Data element not present in data set, falling back to default type: ' + dataElementId );
	return dhis2.de.cst.defaultType;
}

function getDataElementName( dataElementId )
{
	var span = $( '#' + dataElementId + '-dataelement' );

	if ( span != null )
	{
		return span.text();
	}

    console.log( 'Data element not present in form, falling back to default name: ' + dataElementId );
	return dhis2.de.cst.defaultName;
}

function getOptionComboName( optionComboId )
{
	var span = $( '#' + optionComboId + '-optioncombo' );

	if ( span != null )
	{
		return span.text();
	}

    console.log( 'Category option combo not present in form, falling back to default name: ' + optionComboId );
	return dhis2.de.cst.defaultName;
}

// ----------------------------------------------------------------------------
// OrganisationUnit Selection
// -----------------------------------------------------------------------------

/**
 * Callback for changes in organisation unit selections.
 */
function organisationUnitSelected( orgUnits, orgUnitNames, children )
{
	if ( dhis2.de.metaDataIsLoaded == false )
	{
	    return false;
	}

  if( dhis2.de.shouldFetchDataSets(orgUnits) ) {
    dhis2.de.fetchDataSets( orgUnits[0] ).always(function() {
      selection.responseReceived();
    });

    return false;
  }

	dhis2.de.currentOrganisationUnitId = orgUnits[0];
    var organisationUnitName = orgUnitNames[0];

    $( '#selectedOrganisationUnit' ).val( organisationUnitName );
    $( '#currentOrganisationUnit' ).html( organisationUnitName );

    dhis2.de.getOrFetchDataSetList().then(function(data) {
        var dataSetList = data;

        $( '#selectedDataSetId' ).removeAttr( 'disabled' );

        var dataSetId = $( '#selectedDataSetId' ).val();
        var periodId = $( '#selectedPeriodId').val();

        clearListById( 'selectedDataSetId' );
        addOptionById( 'selectedDataSetId', '-1', '[ ' + i18n_select_data_set + ' ]' );

        var dataSetValid = false;
        var multiDataSetValid = false;

        $.safeEach( dataSetList, function( idx, item )
        {
        	if ( item )
        	{
	            addOptionById( 'selectedDataSetId', item.id, item.name );
	
	            if ( dataSetId == item.id )
	            {
	                dataSetValid = true;
	            }
        	}
        } );

        if ( children )
        {
            var childrenDataSets = getSortedDataSetListForOrgUnits( children );

            if ( childrenDataSets && childrenDataSets.length > 0 )
            {
                $( '#selectedDataSetId' ).append( '<optgroup label="' + i18n_childrens_forms + '">' );

                $.safeEach( childrenDataSets, function( idx, item )
                {
                    if ( dataSetId == item.id && dhis2.de.multiOrganisationUnit )
                    {
                        multiDataSetValid = true;
                    }

                    $( '<option />' ).attr( 'data-multiorg', true ).attr( 'value', item.id).html( item.name ).appendTo( '#selectedDataSetId' );
                } );

                $( '#selectDataSetId' ).append( '</optgroup>' );
            }
        }

        if ( !dhis2.de.multiOrganisationUnit && dataSetValid && dataSetId ) {
            $( '#selectedDataSetId' ).val( dataSetId ); // Restore selected data set

            if ( dhis2.de.inputSelected() && dhis2.de.dataEntryFormIsLoaded ) {
                dhis2.de.resetSectionFilters();
                showLoader();
                loadDataValues();
            }
        }
        else if ( dhis2.de.multiOrganisationUnit && multiDataSetValid && dataSetId ) {
            $( '#selectedDataSetId' ).val( dataSetId ); // Restore selected data set
            dataSetSelected();
        }
        else {
        	dhis2.de.multiOrganisationUnit = false;
            dhis2.de.currentDataSetId = null;

            dhis2.de.clearSectionFilters();
            dhis2.de.clearPeriod();
            dhis2.de.clearAttributes();
        }
    });

}

/**
 * Fetch data-sets for a orgUnit + data-sets for its children.
 *
 * @param {String} ou Organisation Unit ID to fetch data-sets for
 * @returns {$.Deferred}
 */
dhis2.de.fetchDataSets = function( ou )
{
    var def = $.Deferred();

    $.ajax({
        type: 'GET',
        url: '../api/organisationUnits/' + ou,
        data: {
            fields: 'id,dataSets[id],children[id,dataSets[id]]'
        }
    }).done(function(data) {
        dhis2.de._updateDataSets(data);

        data.children.forEach(function( item ) {
            dhis2.de._updateDataSets(item);
        });

        dhis2.de.fetchedDataSets[ou] = true;
        def.resolve(data);
    });

    return def.promise();
};

/**
 * Internal method that will go through all data-sets on the object and add them to
 * {@see dhis2.de.dataSetAssociationSets} and {@see dhis2.de.organisationUnitAssociationSetMap}.
 *
 * @param {Object} ou Object that matches the format id,dataSets[id].
 * @private
 */
dhis2.de._updateDataSets = function( ou ) {
    var dataSets = [];

    ou.dataSets.forEach(function( item ) {
        dataSets.push(item.id);
    });

    dhis2.de.dataSetAssociationSets[Object.keys(dhis2.de.dataSetAssociationSets).length] = dataSets;
    dhis2.de.organisationUnitAssociationSetMap[ou.id] = Object.keys(dhis2.de.dataSetAssociationSets).length - 1;
};

/**
 * Get a list of sorted data-sets for a orgUnit, if data-set list is empty, it will 
 * try and fetch data-sets from the server.
 *
 * @param {String} [ou] Organisation unit to fetch data-sets for
 * @returns {$.Deferred}
 */
dhis2.de.getOrFetchDataSetList = function( ou ) {
    var def = $.Deferred();

    var dataSets = getSortedDataSetList(ou);
    ou = ou || dhis2.de.getCurrentOrganisationUnit();

    if (dataSets.length > 0) {
        def.resolve(dataSets);
    } 
    else {
        dhis2.de.fetchDataSets(ou).then(function() {
            def.resolve(getSortedDataSetList(ou));
        });
    }

    /* TODO check if data sets are accessible for current user */
    
    return def.promise();
};

/**
 * Returns an array containing associative array elements with id and name
 * properties. The array is sorted on the element name property.
 */
function getSortedDataSetList( orgUnit )
{
    var associationSet = orgUnit !== undefined ? dhis2.de.organisationUnitAssociationSetMap[orgUnit] : dhis2.de.organisationUnitAssociationSetMap[dhis2.de.getCurrentOrganisationUnit()];
    var orgUnitDataSets = dhis2.de.dataSetAssociationSets[associationSet];

    var dataSetList = [];

    $.safeEach( orgUnitDataSets, function( idx, item ) 
    {
        var dataSetId = orgUnitDataSets[idx];
        
        if ( dhis2.de.dataSets[dataSetId] )
        {
	        var dataSetName = dhis2.de.dataSets[dataSetId].name;
	
	        var row = [];
	        row['id'] = dataSetId;
	        row['name'] = dataSetName;
	        dataSetList[idx] = row;
        }
    } );

    dataSetList.sort( function( a, b )
    {
        return a.name > b.name ? 1 : a.name < b.name ? -1 : 0;
    } );

    return dataSetList;
}

/**
 * Gets list of data sets for selected organisation units.
 */
function getSortedDataSetListForOrgUnits( orgUnits )
{
    var dataSetList = [];

    $.safeEach( orgUnits, function( idx, item )
    {
        dataSetList.push.apply( dataSetList, getSortedDataSetList(item) )
    } );

    var filteredDataSetList = [];

    $.safeEach( dataSetList, function( idx, item ) 
    {
        var formType = dhis2.de.dataSets[item.id].type;
        var found = false;

        $.safeEach( filteredDataSetList, function( i, el ) 
        {
            if( item.name == el.name )
            {
                found = true;
            }
        } );

        if ( !found && ( formType == dhis2.de.cst.formTypeSection || formType == dhis2.de.cst.formTypeDefault ) )
        {
            filteredDataSetList.push(item);
        }
    } );

    return filteredDataSetList;
}

// -----------------------------------------------------------------------------
// DataSet Selection
// -----------------------------------------------------------------------------

/**
 * Callback for changes in data set list. For previous selection to be valid and
 * the period selection to remain, the period type of the previous data set must
 * equal the current data set, and the allow future periods property of the previous
 * data set must equal the current data set or the current period offset must not
 * be in the future.
 */
function dataSetSelected()
{
    var previousDataSetValid = ( dhis2.de.currentDataSetId && dhis2.de.currentDataSetId != -1 );
    var previousDataSet = !!previousDataSetValid ? dhis2.de.dataSets[dhis2.de.currentDataSetId] : undefined;
    var previousPeriodType = previousDataSet ? previousDataSet.periodType : undefined;
    var previousOpenFuturePeriods = previousDataSet ? previousDataSet.openFuturePeriods : 0;

    dhis2.de.currentDataSetId = $( '#selectedDataSetId' ).val();
    
    if ( dhis2.de.currentDataSetId && dhis2.de.currentDataSetId != -1 )
    {
        $( '#selectedPeriodId' ).removeAttr( 'disabled' );
        $( '#prevButton' ).removeAttr( 'disabled' );
        $( '#nextButton' ).removeAttr( 'disabled' );

        var periodType = dhis2.de.dataSets[dhis2.de.currentDataSetId].periodType;
        var openFuturePeriods = dhis2.de.dataSets[dhis2.de.currentDataSetId].openFuturePeriods;

        var previousSelectionValid = !!( periodType == previousPeriodType && openFuturePeriods == previousOpenFuturePeriods );
        
        dhis2.de.currentCategories = dhis2.de.getCategories( dhis2.de.currentDataSetId );

        dhis2.de.setAttributesMarkup();   

        dhis2.de.multiOrganisationUnit = !!$( '#selectedDataSetId :selected' ).data( 'multiorg' );

        if ( dhis2.de.inputSelected() && previousSelectionValid )
        {
            showLoader();
            dhis2.de.loadForm();
        }
        else
        {
        	dhis2.de.currentPeriodOffset = 0;
        	displayPeriods();
        	dhis2.de.clearSectionFilters();
            dhis2.de.clearEntryForm();
        }
    }
    else
    {
        $( '#selectedPeriodId').val( "" );
        $( '#selectedPeriodId' ).attr( 'disabled', 'disabled' );
        $( '#prevButton' ).attr( 'disabled', 'disabled' );
        $( '#nextButton' ).attr( 'disabled', 'disabled' );

        dhis2.de.clearEntryForm();
        dhis2.de.clearAttributes();
    }
}

// -----------------------------------------------------------------------------
// Period Selection
// -----------------------------------------------------------------------------

/**
 * Callback for changes in period select list.
 */
function periodSelected()
{
    var periodName = $( '#selectedPeriodId :selected' ).text();

    $( '#currentPeriod' ).html( periodName );
    
    dhis2.de.setAttributesMarkup();
    
    if ( dhis2.de.inputSelected() )
    {    	
        showLoader();

        if ( dhis2.de.dataEntryFormIsLoaded )
        {
            loadDataValues();
        }
        else
        {
            dhis2.de.loadForm();
        }
    }
    else
    {
        dhis2.de.clearEntryForm();
    }
}

/**
 * Handles the onClick event for the next period button.
 */
function nextPeriodsSelected()
{
	var openFuturePeriods = !!( dhis2.de.currentDataSetId && dhis2.de.dataSets[dhis2.de.currentDataSetId].openFuturePeriods );
	
    if ( dhis2.de.currentPeriodOffset < 0 || openFuturePeriods )
    {
    	dhis2.de.currentPeriodOffset++;
        displayPeriods();
    }
}

/**
 * Handles the onClick event for the previous period button.
 */
function previousPeriodsSelected()
{
	dhis2.de.currentPeriodOffset--;
    displayPeriods();
}

/**
 * Generates the period select list options.
 */
function displayPeriods()
{
    var dataSetId = $( '#selectedDataSetId' ).val();
    var periodType = dhis2.de.dataSets[dataSetId].periodType;
    var openFuturePeriods = dhis2.de.dataSets[dataSetId].openFuturePeriods;
    var dsStartDate = dhis2.de.dataSets[dataSetId].startDate;
    var dsEndDate = dhis2.de.dataSets[dataSetId].endDate;
    var periods = dhis2.period.generator.generateReversedPeriods( periodType, dhis2.de.currentPeriodOffset );

    periods = dhis2.period.generator.filterOpenPeriods( periodType, periods, openFuturePeriods, dsStartDate, dsEndDate );
    
    clearListById( 'selectedPeriodId' );

    if ( periods.length > 0 )
    {
    	addOptionById( 'selectedPeriodId', "", '[ ' + i18n_select_period + ' ]' );
    }
    else
    {
    	addOptionById( 'selectedPeriodId', "", i18n_no_periods_click_prev_year_button );
    }

    dhis2.de.periodChoices = [];

    $.safeEach( periods, function( idx, item ) 
    {
        addOptionById( 'selectedPeriodId', item.iso, item.name );
        dhis2.de.periodChoices[ item.iso ] = item;
    } );
}

//------------------------------------------------------------------------------
// Attributes / Categories Selection
//------------------------------------------------------------------------------

/**
* Returns an array of category objects for the given data set identifier. Categories
* are looked up using the category combo of the data set. Null is returned if
* the given data set has the default category combo.
*/
dhis2.de.getCategories = function( dataSetId )
{
	var dataSet = dhis2.de.dataSets[dataSetId];
	
	if ( !dataSet || !dataSet.categoryCombo || dhis2.de.defaultCategoryCombo === dataSet.categoryCombo ) {
		return null;
	}

	var categoryCombo = dhis2.de.categoryCombos[dataSet.categoryCombo];
	
	var categories = [];
	
	$.safeEach( categoryCombo.categories, function( idx, cat ) {
		var category = dhis2.de.categories[cat];
		categories.push( category );
	} );
	
	return categories;
};

/**
 * Indicates whether all present categories have been selected. True is returned
 * if no categories are present. False is returned if less selections have been
 * made thant here are categories present.
 */
dhis2.de.categoriesSelected = function()
{
	if ( !dhis2.de.currentCategories || dhis2.de.currentCategories.length == 0 ) {
		return true; // No categories present which can be selected
	}
	
	var options = dhis2.de.getCurrentCategoryOptions();
	
	if ( !options || options.length < dhis2.de.currentCategories.length ) {
		return false; // Less selected options than categories present
	}
	
	return true;
};

/**
* Returns attribute category combo identifier. Based on the dhis2.de.currentDataSetId 
* global variable. Returns null if there is no current data set or if current 
* data set has the default category combo.
*/
dhis2.de.getCurrentCategoryCombo = function()
{
	var dataSet = dhis2.de.dataSets[dhis2.de.currentDataSetId];
	
	if ( !dataSet || !dataSet.categoryCombo || dhis2.de.defaultCategoryCombo === dataSet.categoryCombo ) {
		return null;
	}
	
	return dataSet.categoryCombo;
};

/**
* Returns an array of the currently selected attribute category option identifiers. 
* Based on the dhis2.de.currentCategories global variable. Returns null if there 
* are no current categories.
*/
dhis2.de.getCurrentCategoryOptions = function()
{
	if ( !dhis2.de.currentCategories || dhis2.de.currentCategories.length == 0 ) {
		return null;
	}
	
	var options = [];
	
	$.safeEach( dhis2.de.currentCategories, function( idx, category ) {
		var option = $( '#category-' + category.id ).val();
		
		if ( option && option != -1 ) {
			options.push( option );
		}
	} );
	
	return options;
};

/**
 * Returns an object for the currently selected attribute category options
 * with properties for the identifiers of each category and matching values
 * for the identifier of the selected category option. Returns an empty
 * object if there are no current categories.
 */
dhis2.de.getCurrentCategorySelections = function()
{
	var selections = {};
	
	if ( !dhis2.de.currentCategories || dhis2.de.currentCategories.length == 0 ) {
		return selections;
	}
		
	$.safeEach( dhis2.de.currentCategories, function( idx, category ) {
		var option = $( '#category-' + category.id ).val();
		
		if ( option && option != -1 ) {
			selections[category.id] = option;
		}
	} );
	
	return selections;
}

/**
 * Returns a query param value for the currently selected category options where
 * each option is separated by the ; character.
 */
dhis2.de.getCurrentCategoryOptionsQueryValue = function()
{
	if ( !dhis2.de.getCurrentCategoryOptions() ) {
		return null;
	}
	
	var value = '';
	
	$.safeEach( dhis2.de.getCurrentCategoryOptions(), function( idx, option ) {
		value += option + ';';
	} );
	
	if ( value ) {
		value = value.slice( 0, -1 );
	}
	
	return value;
}

/**
 * Tests to see if a category option is valid during a period.
 * 
 * TODO proper date comparison
 */
dhis2.de.optionValidWithinPeriod = function( option, period )
{
    return ( !option.start || option.start <= dhis2.de.periodChoices[ period ].endDate )
        && ( !option.end || option.end >= dhis2.de.periodChoices[ period ].startDate )
}

/**
 * Tests to see if attribute category option is valid for the selected org unit.
 */
dhis2.de.optionValidForSelectedOrgUnit = function( option )
{
    var isValid = true;

    if (option.ous && option.ous.length) {
        isValid = false;
        var path = organisationUnits[dhis2.de.getCurrentOrganisationUnit()].path;
        $.safeEach(option.ous, function (idx, uid) {
            if (path.indexOf(uid) >= 0) {
                isValid = true;
                return false;
            }
        });
    }

    return isValid;
}

/**
 * Sets the markup for attribute selections.
 */
dhis2.de.setAttributesMarkup = function()
{
    var attributeMarkup = dhis2.de.getAttributesMarkup();
    $( '#attributeComboDiv' ).html( attributeMarkup );
}

/**
* Returns markup for drop down boxes to be put in the selection box for the
* given categories. The empty string is returned if no categories are given.
*
* TODO check for category option validity for selected organisation unit.
*/
dhis2.de.getAttributesMarkup = function()
{
	var html = '';

    var period = $( '#selectedPeriodId' ).val();

    var options = dhis2.de.getCurrentCategoryOptions();

	if ( !dhis2.de.currentCategories || dhis2.de.currentCategories.length == 0 || !period ) {
		return html;
	}
	
	$.safeEach( dhis2.de.currentCategories, function( idx, category ) {
		html += '<div class="selectionBoxRow">';
		html += '<div class="selectionLabel">' + category.name + '</div>&nbsp;';
		html += '<select id="category-' + category.id + '" class="selectionBoxSelect" onchange="dhis2.de.attributeSelected(\'' + category.id + '\')">';
		html += '<option value="-1">[ ' + i18n_select_option + ' ]</option>';

		$.safeEach( category.options, function( idx, option ) {
			if ( dhis2.de.optionValidWithinPeriod( option, period ) && dhis2.de.optionValidForSelectedOrgUnit( option ) ) {
				var selected = ( $.inArray( option.id, options ) != -1 ) ? " selected" : "";
				html += '<option value="' + option.id + '"' + selected + '>' + option.name + '</option>';
			}
		} );
		
		html += '</select>';
		html += '</div>';
	} );

	return html;
};

/**
 * Clears the markup for attribute select lists.
 */
dhis2.de.clearAttributes = function()
{
	$( '#attributeComboDiv' ).html( '' );
};

/**
 * Callback for changes in attribute select lists.
 */
dhis2.de.attributeSelected = function( categoryId )
{
	if ( dhis2.de.inputSelected() ) {    	
        showLoader();

        if ( dhis2.de.dataEntryFormIsLoaded ) {
            loadDataValues();
        }
        else {
            dhis2.de.loadForm();
        }
    }
    else
    {
        dhis2.de.clearEntryForm();
    }
};

// -----------------------------------------------------------------------------
// Form
// -----------------------------------------------------------------------------

/**
 * Indicates whether all required inpout selections have been made.
 */
dhis2.de.inputSelected = function()
{
    var dataSetId = $( '#selectedDataSetId' ).val();
    var periodId = $( '#selectedPeriodId').val();

	if (
	    dhis2.de.currentOrganisationUnitId &&
	    dataSetId && dataSetId != -1 &&
	    periodId && periodId != "" &&
	    dhis2.de.categoriesSelected() ) {
		return true;
	}

	return false;
};

function loadDataValues()
{
    $( '#completeButton' ).removeAttr( 'disabled' );
    $( '#undoButton' ).attr( 'disabled', 'disabled' );
    $( '#infoDiv' ).css( 'display', 'none' );

    dhis2.de.currentOrganisationUnitId = selection.getSelected()[0];

    getAndInsertDataValues();
    displayEntryFormCompleted();
}

function clearFileEntryFields() {
    var $fields = $( '.entryfileresource' );
    $fields.find( '.upload-fileinfo-name' ).text( '' );
    $fields.find( '.upload-fileinfo-size' ).text( '' );

    $fields.find( '.upload-field' ).css( 'background-color', dhis2.de.cst.colorWhite );
    $fields.find( 'input' ).val( '' );
    
    $('.select2-container').select2("val", "");
}

function getAndInsertDataValues()
{
    var periodId = $( '#selectedPeriodId').val();
    var dataSetId = $( '#selectedDataSetId' ).val();

    // Clear existing values and colors, grey disabled fields

    $( '.entryfield' ).val( '' );
    $( '.entryselect' ).removeAttr( 'checked' );
    $( '.entrytrueonly' ).removeAttr( 'checked' );

    $( '.entryfield' ).css( 'background-color', dhis2.de.cst.colorWhite ).css( 'border', '1px solid ' + dhis2.de.cst.colorBorder );
    $( '.entryselect' ).css( 'background-color', dhis2.de.cst.colorWhite ).css( 'border', '1px solid ' + dhis2.de.cst.colorBorder );
    $( '.indicator' ).css( 'background-color', dhis2.de.cst.colorWhite ).css( 'border', '1px solid ' + dhis2.de.cst.colorBorder );
    $( '.entrytrueonly' ).css( 'background-color', dhis2.de.cst.colorWhite );

    clearFileEntryFields();


    $( '[name="min"]' ).html( '' );
    $( '[name="max"]' ).html( '' );

    $( '.entryfield' ).filter( ':disabled' ).css( 'background-color', dhis2.de.cst.colorGrey );

    var params = {
		periodId : periodId,
        dataSetId : dataSetId,
        organisationUnitId : dhis2.de.getCurrentOrganisationUnit(),
        multiOrganisationUnit: dhis2.de.multiOrganisationUnit
    };

    var cc = dhis2.de.getCurrentCategoryCombo();
    var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
    
    if ( cc && cp )
    {
    	params.cc = cc;
    	params.cp = cp;
    }
    
    $.ajax( {
    	url: 'getDataValues.action',
    	data: params,
	    dataType: 'json',
	    error: function() // offline
	    {
	    	$( '#completenessDiv' ).show();
	    	$( '#infoDiv' ).hide();
	    	
	    	var json = getOfflineDataValueJson( params );
	    	
	    	insertDataValues( json );
	    },
	    success: function( json ) // online
	    {
	    	insertDataValues( json );
        },
        complete: function()
        {
            $( '.indicator' ).attr( 'readonly', 'readonly' );
            $( '.dataelementtotal' ).attr( 'readonly', 'readonly' );
            $( document ).trigger( dhis2.de.event.dataValuesLoaded, dhis2.de.currentDataSetId );
        }
	} );
}

function getOfflineDataValueJson( params )
{
	var dataValues = dhis2.de.storageManager.getDataValuesInForm( params );
	var complete = dhis2.de.storageManager.hasCompleteDataSet( params );
	
	var json = {};
	json.dataValues = new Array();
	json.locked = false;
	json.complete = complete;
	json.date = "";
	json.storedBy = "";
		
	for ( var i = 0; i < dataValues.length; i++ )
	{
		var dataValue = dataValues[i];
		
		json.dataValues.push( { 
			'id': dataValue.de + '-' + dataValue.co,
			'val': dataValue.value
		} );
	}
	
	return json;
}

function insertDataValues( json )
{
    var dataValueMap = []; // Reset
    dhis2.de.currentMinMaxValueMap = []; // Reset
    
	if ( json.locked )
	{
        $( '#contentDiv input').attr( 'readonly', 'readonly' );
        $( '#contentDiv textarea').attr( 'readonly', 'readonly' );
        $( '.sectionFilter').removeAttr( 'disabled' );
        $( '#completenessDiv' ).hide();
		setHeaderDelayMessage( i18n_dataset_is_locked );
	}
	else
	{
        $( '#contentDiv input' ).removeAttr( 'readonly' );
        $( '#contentDiv textarea' ).removeAttr( 'readonly' );
		$( '#completenessDiv' ).show();
	}
	
    // Set data values, works for selects too as data value=select value

    $.safeEach( json.dataValues, function( i, value )
    {
        var fieldId = '#' + value.id + '-val';
        var commentId = '#' + value.id + '-comment';

        if ( $( fieldId ).length > 0 ) // Set values
        {
            if ( $( fieldId ).attr( 'name' ) == 'entrytrueonly' && 'true' == value.val ) 
            {
                $( fieldId ).attr( 'checked', true );
            }
            else if ( $( fieldId ).attr( 'name' ) == 'entryoptionset' )
            {
                dhis2.de.setOptionNameInField( fieldId, value );
            }
            else if ( $( fieldId ).attr( 'class' ) == 'entryselect' )
            {                
                var fId = fieldId.substring(1, fieldId.length);
    
                if( value.val == 'true' )
                {
                    $('input[id=' + fId + ']')[1].checked = true;
                }
                else if ( value.val == 'false')
                {
                    $('input[id=' + fId + ']')[2].checked = true;
                }
                else{
                    $('input[id=' + fId + ']')[0].checked = true;
                }
            }
            else if ( $( fieldId ).attr( 'class' ) == 'entryfileresource' )
            {
                var $field = $( fieldId );

                $field.find( 'input[class="entryfileresource-input"]' ).val( value.val );

                var split = dhis2.de.splitFieldId( value.id );

                var dvParams = {
                    'de': split.dataElementId,
                    'co': split.optionComboId,
                    'ou': split.organisationUnitId,
                    'pe': $( '#selectedPeriodId' ).val()
                };

                var name = "", size = "";

                if ( value.fileMeta )
                {
                    name = value.fileMeta.name;
                    size = '(' + filesize( value.fileMeta.size ) + ')';
                }
                else
                {
                    name = i18n_loading_file_info_failed;
                }

                var $filename = $field.find( '.upload-fileinfo-name' );

                $( '<a>', {
                    text: name,
                    title: name,
                    target: '_blank',
                    href: "../api/dataValues/files?" + $.param( dvParams )
                } ).appendTo( $filename );

                $field.find( '.upload-fileinfo-size' ).text( size );
            }
            else 
            {
                $( fieldId ).val( value.val );
            }
        }
        
        if ( 'true' == value.com ) // Set active comments
        {
            if ( $( commentId ).length > 0 )
            {
                $( commentId ).attr( 'src', '../images/comment_active.png' );
            }
            else if ( $( fieldId ).length > 0 )
            {
                $( fieldId ).css( 'border-color', dhis2.de.cst.colorBorderActive )
            }
        }
        
        dataValueMap[value.id] = value.val;

        dhis2.period.picker.updateDate(fieldId);
    } );

    // Set min-max values and colorize violation fields

    if ( !json.locked ) 
    {
        $.safeEach( json.minMaxDataElements, function( i, value )
        {
            var minId = value.id + '-min';
            var maxId = value.id + '-max';

            var valFieldId = '#' + value.id + '-val';

            var dataValue = dataValueMap[value.id];

            if ( dataValue && ( ( value.min && new Number( dataValue ) < new Number(
                value.min ) ) || ( value.max && new Number( dataValue ) > new Number( value.max ) ) ) )
            {
                $( valFieldId ).css( 'background-color', dhis2.de.cst.colorOrange );
            }

            dhis2.de.currentMinMaxValueMap[minId] = value.min;
            dhis2.de.currentMinMaxValueMap[maxId] = value.max;
        } );
    }

    // Update indicator values in form

    dhis2.de.updateIndicators();
    dhis2.de.updateDataElementTotals();

    // Set completeness button

    if ( json.complete && !json.locked)
    {
        $( '#completeButton' ).attr( 'disabled', 'disabled' );
        $( '#undoButton' ).removeAttr( 'disabled' );

        if ( json.storedBy )
        {
            $( '#infoDiv' ).show();
            $( '#completedBy' ).html( json.storedBy );
            $( '#completedDate' ).html( json.date );

            dhis2.de.currentCompletedByUser = json.storedBy;
        }
    }
    else
    {
        $( '#completeButton' ).removeAttr( 'disabled' );
        $( '#undoButton' ).attr( 'disabled', 'disabled' );
        $( '#infoDiv' ).hide();
    }

    if ( json.locked ) 
    {
        $( '#contentDiv input' ).css( 'backgroundColor', '#eee' );
        $( '.sectionFilter' ).css( 'backgroundColor', '#fff' );
    }
}

function displayEntryFormCompleted()
{
    dhis2.de.addEventListeners();

    $( '#validationButton' ).removeAttr( 'disabled' );
    $( '#validateButton' ).removeAttr( 'disabled' );

    dhis2.de.dataEntryFormIsLoaded = true;
    hideLoader();
    
    $( document ).trigger( dhis2.de.event.formReady, dhis2.de.currentDataSetId );
}

function valueFocus( e )
{
    var id = e.target.id;
    var value = e.target.value;

    var split = dhis2.de.splitFieldId( id );
    var dataElementId = split.dataElementId;
    var optionComboId = split.optionComboId;
    dhis2.de.currentOrganisationUnitId = split.organisationUnitId;
    dhis2.de.currentExistingValue = value;

    var dataElementName = getDataElementName( dataElementId );
    var optionComboName = getOptionComboName( optionComboId );
    var organisationUnitName = organisationUnits[dhis2.de.getCurrentOrganisationUnit()].n;

    $( '#currentOrganisationUnit' ).html( organisationUnitName );
    $( '#currentDataElement' ).html( dataElementName + ' ' + optionComboName );

    $( '#' + dataElementId + '-cell' ).addClass( 'currentRow' );
}

function valueBlur( e )
{
    var id = e.target.id;

    var split = dhis2.de.splitFieldId( id );
    var dataElementId = split.dataElementId;

    $( '#' + dataElementId + '-cell' ).removeClass( 'currentRow' );
}

function keyPress( event, field )
{
    var key = event.keyCode || event.charCode || event.which;

    var focusField = ( key == 13 || key == 40 ) ? getNextEntryField( field )
            : ( key == 38 ) ? getPreviousEntryField( field ) : false;

    if ( focusField )
    {
        focusField.focus();
    }
}

function getNextEntryField( field )
{
    var index = field.getAttribute( 'tabindex' );

    field = $( 'input[name="entryfield"][tabindex="' + ( ++index ) + '"]' );

    while ( field )
    {
        if ( field.is( ':disabled' ) || field.is( ':hidden' ) )
        {
            field = $( 'input[name="entryfield"][tabindex="' + ( ++index ) + '"]' );
        }
        else
        {
            return field;
        }
    }
}

function getPreviousEntryField( field )
{
    var index = field.getAttribute( 'tabindex' );

    field = $( 'input[name="entryfield"][tabindex="' + ( --index ) + '"]' );

    while ( field )
    {
        if ( field.is( ':disabled' ) || field.is( ':hidden' ) )
        {
            field = $( 'input[name="entryfield"][tabindex="' + ( --index ) + '"]' );
        }
        else
        {
            return field;
        }
    }
}

// -----------------------------------------------------------------------------
// Data completeness
// -----------------------------------------------------------------------------

function registerCompleteDataSet()
{
	if ( !confirm( i18n_confirm_complete ) )
	{
		return false;
    }
	
	dhis2.de.validate( true, function() 
    {
        var params = dhis2.de.storageManager.getCurrentCompleteDataSetParams();

        var cc = dhis2.de.getCurrentCategoryCombo();
        var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
        
        if ( cc && cp )
        {
        	params.cc = cc;
        	params.cp = cp;
        }
        
        dhis2.de.storageManager.saveCompleteDataSet( params );
	
	    $.ajax( {
	    	url: '../api/completeDataSetRegistrations',
	    	data: params,
	        dataType: 'json',
	        type: 'post',
	    	success: function( data, textStatus, xhr )
	        {
                $( document ).trigger( dhis2.de.event.completed, [ dhis2.de.currentDataSetId, params ] );
	    		disableCompleteButton();
	    		dhis2.de.storageManager.clearCompleteDataSet( params );
	        },
		    error:  function( xhr, textStatus, errorThrown )
		    {
		    	if ( 409 == xhr.status || 500 == xhr.status ) // Invalid value or locked
	        	{
	        		setHeaderMessage( xhr.responseText );
	        	}
	        	else // Offline, keep local value
	        	{
                    $( document ).trigger( dhis2.de.event.completed, [ dhis2.de.currentDataSetId, params ] );
	        		disableCompleteButton();
	        		setHeaderMessage( i18n_offline_notification );
	        	}
		    }
	    } );
	} );
}

function undoCompleteDataSet()
{
	if ( !confirm( i18n_confirm_undo ) )
	{
		return false;
	}

    var params = dhis2.de.storageManager.getCurrentCompleteDataSetParams();

    var cc = dhis2.de.getCurrentCategoryCombo();
    var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
    
    var params = 
    	'?ds=' + params.ds +
    	'&pe=' + params.pe +
    	'&ou=' + params.ou + 
    	'&multiOu=' + params.multiOu;

    if ( cc && cp )
    {
    	params += '&cc=' + cc;
    	params += '&cp=' + cp;
    }
        
    $.ajax( {
    	url: '../api/completeDataSetRegistrations' + params,
    	dataType: 'json',
    	type: 'delete',
    	success: function( data, textStatus, xhr )
        {
          $( document ).trigger( dhis2.de.event.uncompleted, dhis2.de.currentDataSetId );
          disableUndoButton();
          dhis2.de.storageManager.clearCompleteDataSet( params );
        },
        error: function( xhr, textStatus, errorThrown )
        {
        	if ( 409 == xhr.status || 500 == xhr.status ) // Invalid value or locked
        	{
        		setHeaderMessage( xhr.responseText );
        	}
        	else // Offline, keep local value
        	{
                $( document ).trigger( dhis2.de.event.uncompleted, dhis2.de.currentDataSetId );
        		disableUndoButton();
        		setHeaderMessage( i18n_offline_notification );
        	}

    		dhis2.de.storageManager.clearCompleteDataSet( params );
        }
    } );
}

function disableUndoButton()
{
    $( '#completeButton' ).removeAttr( 'disabled' );
    $( '#undoButton' ).attr( 'disabled', 'disabled' );
}

function disableCompleteButton()
{
    $( '#completeButton' ).attr( 'disabled', 'disabled' );
    $( '#undoButton' ).removeAttr( 'disabled' );
}

function displayUserDetails()
{
	if ( dhis2.de.currentCompletedByUser )
	{
		var url = '../dhis-web-commons-ajax-json/getUser.action';

		$.getJSON( url, { username: dhis2.de.currentCompletedByUser }, function( json ) 
		{
			$( '#userFullName' ).html( json.user.firstName + ' ' + json.user.surname );
			$( '#userUsername' ).html( json.user.username );
			$( '#userEmail' ).html( json.user.email );
			$( '#userPhoneNumber' ).html( json.user.phoneNumber );
			$( '#userOrganisationUnits' ).html( joinNameableObjects( json.user.organisationUnits ) );
			$( '#userUserRoles' ).html( joinNameableObjects( json.user.roles ) );

			$( '#completedByDiv' ).dialog( {
	        	modal : true,
	        	width : 350,
	        	height : 350,
	        	title : 'User'
	    	} );
		} );
	}
}

// -----------------------------------------------------------------------------
// Validation
// -----------------------------------------------------------------------------

/**
 * Executes all validation checks.
 * 
 * @param ignoreValidationSuccess indicates whether no dialog should be display
 *        if validation is successful.
 * @param successCallback the function to execute if validation is successful.                                  
 */
dhis2.de.validate = function( ignoreValidationSuccess, successCallback )
{
	var compulsoryCombinationsValid = dhis2.de.validateCompulsoryCombinations();
	
	// Check for compulsory combinations and return false if violated
	
	if ( !compulsoryCombinationsValid )
	{
    	var html = '<h3>' + i18n_validation_result + ' &nbsp;<img src="../images/warning_small.png"></h3>' +
        	'<p class="bold">' + i18n_all_values_for_data_element_must_be_filled + '</p>';
		
    	dhis2.de.displayValidationDialog( html, 300 );
	
		return false;
	}

	// Check for validation rules and whether complete is only allowed if valid
	
	var successHtml = '<h3>' + i18n_validation_result + ' &nbsp;<img src="../images/success_small.png"></h3>' +
		'<p class="bold">' + i18n_successful_validation + '</p>';

	var validCompleteOnly = dhis2.de.dataSets[dhis2.de.currentDataSetId].validCompleteOnly;

    var cc = dhis2.de.getCurrentCategoryCombo();
    var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();

    var params = dhis2.de.storageManager.getCurrentCompleteDataSetParams();

    if ( cc && cp )
    {
        params.cc = dhis2.de.getCurrentCategoryCombo();
        params.cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
    }

    $( '#validationDiv' ).load( 'validate.action', params, function( response, status, xhr ) {
    	var success = null;
    	
        if ( status == 'error' && !ignoreValidationSuccess )
        {
            window.alert( i18n_operation_not_available_offline );
            success = true;  // Accept if offline
        }
        else
        {
        	var hasViolations = isDefined( response ) && $.trim( response ).length > 0;
        	var success = !( hasViolations && validCompleteOnly );
        	
        	if ( hasViolations )
        	{
        		dhis2.de.displayValidationDialog( response, 500 );
        	}
        	else if ( !ignoreValidationSuccess )
        	{
        		dhis2.de.displayValidationDialog( successHtml, 200 );
        	}        	
        }
        
        if ( success && $.isFunction( successCallback ) )
        {
        	successCallback.call();
        }
        
        if ( success )
        {
        	$( document ).trigger( dhis2.de.event.validationSucces, dhis2.de.currentDataSetId );
        }
        else
    	{
        	$( document ).trigger( dhis2.de.event.validationError, dhis2.de.currentDataSetId );
    	}
    } );
}

/**
 * Displays the validation dialog.
 * 
 * @param html the html content to display in the dialog.
 * @param height the height of the dialog.
 */
dhis2.de.displayValidationDialog = function( html, height )
{
	height = isDefined( height ) ? height : 500;
	
	$( '#validationDiv' ).html( html );
	
    $( '#validationDiv' ).dialog( {
        modal: true,
        title: 'Validation',
        width: 920,
        height: height
    } );
}

/**
 * Validates that all category option combinations have all values or no values
 * per data element given that the fieldCombinationRequired is true for the 
 * current data set.
 */
dhis2.de.validateCompulsoryCombinations = function()
{
	var fieldCombinationRequired = dhis2.de.dataSets[dhis2.de.currentDataSetId].fieldCombinationRequired;
	
    if ( fieldCombinationRequired )
    {
        var violations = false;

        $( '.entryfield' ).add( '[name="entryselect"]' ).each( function( i )
        {
            var id = $( this ).attr( 'id' );

            var split = dhis2.de.splitFieldId( id );
            var dataElementId = split.dataElementId;
            var hasValue = $.trim( $( this ).val() ).length > 0;
            
            if ( hasValue )
            {
            	$selector = $( '[name="entryfield"][id^="' + dataElementId + '-"]' ).
            		add( '[name="entryselect"][id^="' + dataElementId + '-"]' );
				
                $selector.each( function( i )
                {
                    if ( $.trim( $( this ).val() ).length == 0 )
                    {
                        violations = true;						
                        $selector.css( 'background-color', dhis2.de.cst.colorRed );						
                        return false;
                    }
                } );
            }
        } );
		
        if ( violations )
        {
            return false;
        }
    }
	
	return true;
}

// -----------------------------------------------------------------------------
// History
// -----------------------------------------------------------------------------

function displayHistoryDialog( operandName )
{
    $( '#historyDiv' ).dialog( {
        modal: true,
        title: operandName,
        width: 580,
        height: 660
    } );
}

function viewHist( dataElementId, optionComboId )
{
    var periodId = $( '#selectedPeriodId').val();

	if ( dataElementId && optionComboId && periodId && periodId != -1 )
	{
	    var dataElementName = getDataElementName( dataElementId );
	    var optionComboName = getOptionComboName( optionComboId );
	    var operandName = dataElementName + ' ' + optionComboName;
	
	    var params = {
    		dataElementId : dataElementId,
	        optionComboId : optionComboId,
	        periodId : periodId,
	        organisationUnitId : dhis2.de.getCurrentOrganisationUnit()
	    };

	    var cc = dhis2.de.getCurrentCategoryCombo();
	    var cp = dhis2.de.getCurrentCategoryOptionsQueryValue();
	    
	    if ( cc && cp )
	    {
	    	params.cc = cc;
	    	params.cp = cp;
	    }
	    
	    $( '#historyDiv' ).load( 'viewHistory.action', params, 
	    function( response, status, xhr )
	    {
	        if ( status == 'error' )
	        {
	            window.alert( i18n_operation_not_available_offline );
	        }
	        else
	        {
	            displayHistoryDialog( operandName );
	        }
	    } );
	}
}

function closeCurrentSelection()
{
    $( '#currentSelection' ).fadeOut();
}

// -----------------------------------------------------------------------------
// Local storage of forms
// -----------------------------------------------------------------------------

function updateForms()
{
    DAO.store.open()
        .then(purgeLocalForms)
        .then(updateExistingLocalForms)
        .then(downloadRemoteForms)
        .then(dhis2.de.loadOptionSets)
        .done( function() {
        	dhis2.availability.startAvailabilityCheck();
        	console.log( 'Started availability check' );
          selection.responseReceived();
        } );
}

function purgeLocalForms()
{
    var def = $.Deferred();

    dhis2.de.storageManager.getAllForms().done(function( formIds ) {
        var keys = [];

        $.safeEach( formIds, function( idx, item )
        {
            if ( dhis2.de.dataSets[item] == null )
            {
                keys.push(item);
            	dhis2.de.storageManager.deleteFormVersion( item );
                console.log( 'Deleted locally stored form: ' + item );
            }
        } );

        def.resolve();

        console.log( 'Purged local forms' );
    });

    return def.promise();
}

function updateExistingLocalForms()
{
    var def = $.Deferred();

    dhis2.de.storageManager.getAllForms().done(function( formIds ) {
        var formVersions = dhis2.de.storageManager.getAllFormVersions();

        $.safeEach( formIds, function( idx, item )
        {
            var ds = dhis2.de.dataSets[item];
            var remoteVersion = ds ? ds.version : null;
            var localVersion = formVersions[item];

            if ( remoteVersion == null || localVersion == null || remoteVersion != localVersion )
            {
                dhis2.de.storageManager.downloadForm( item, remoteVersion )
            }
        } );

        def.resolve();
    });

    return def.promise();
}

function downloadRemoteForms()
{
    var def = $.Deferred();
    var chain = [];

    $.safeEach( dhis2.de.dataSets, function( idx, item )
    {
        var remoteVersion = item.version;

        if ( !item.skipOffline )
        {
            dhis2.de.storageManager.formExists( idx ).done(function( value ) {
                if( !value ) {
                    chain.push(dhis2.de.storageManager.downloadForm( idx, remoteVersion ));
                }
            });
        }
    } );

    $.when.apply($, chain).then(function() {
        def.resolve();
    });

    return def.promise();
}

// -----------------------------------------------------------------------------
// StorageManager
// -----------------------------------------------------------------------------

/**
 * This object provides utility methods for localStorage and manages data entry
 * forms and data values.
 */
function StorageManager()
{
    var KEY_FORM_VERSIONS = 'formversions';
    var KEY_DATAVALUES = 'datavalues';
    var KEY_COMPLETEDATASETS = 'completedatasets';

    /**
     * Gets the content of a data entry form.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @return the content of a data entry form.
     */
    this.getForm = function( dataSetId )
    {
        var def = $.Deferred();

        DAO.store.get( "forms", dataSetId ).done( function( form ) {
            if( typeof form !== 'undefined' ) {
                def.resolve( form.data );
            } else {
                dhis2.de.storageManager.loadForm( dataSetId ).done(function( data ) {
                    def.resolve( data );
                }).fail(function() {
                    def.resolve( "A form with that ID is not available. Please clear browser cache and try again." );
                });
            }
        });

        return def.promise();
    };

    /**
     * Returns an array of the identifiers of all forms.
     *
     * @return array with form identifiers.
     */
    this.getAllForms = function()
    {
        var def = $.Deferred();

        DAO.store.getKeys( "forms" ).done( function( keys ) {
            def.resolve( keys );
        });

        return def.promise();
    };

    /**
     * Indicates whether a form exists.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @return true if a form exists, false otherwise.
     */
    this.formExists = function( dataSetId )
    {
        var def = $.Deferred();

        DAO.store.contains( "forms", dataSetId ).done( function( found ) {
            def.resolve( found );
        });

        return def.promise();
    };

    /**
     * Indicates whether a form exists remotely.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @return true if a form exists, false otherwise.
     */
    this.formExistsRemotely = function( dataSetId )
    {
        var def = $.Deferred();

        $.ajax({
            url: '../api/dataSets/' + dataSetId,
            accept: 'application/json',
            type: 'GET'
        }).done(function() {
            def.resolve( true );
        }).fail(function() {
            def.resolve( false );
        });

        return def.promise();
    };

    /**
     * Loads a form directly from the server, does not try to save it in the
     * browser (so that it doesn't interfere with any current downloads).
     *
     * @param dataSetId
     * @returns {*}
     */
    this.loadForm = function( dataSetId )
    {
        return $.ajax({
            url: 'loadForm.action',
            data: {
                dataSetId: dataSetId
            },
            dataType: 'text'
        });
    };

    /**
     * Downloads the form for the data set with the given identifier from the
     * remote server and saves the form locally. Potential existing forms with
     * the same identifier will be overwritten. Updates the form version.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @param formVersion the version of the form of the remote data set.
     */
    this.downloadForm = function( dataSetId, formVersion )
    {
        var def = $.Deferred();
        
        console.log( 'Starting download of form: ' + dataSetId );

        $.ajax( {
            url: 'loadForm.action',
            data:
            {
                dataSetId : dataSetId
            },
            dataSetId: dataSetId,
            formVersion: formVersion,
            dataType: 'text',
            success: function( data )
            {
                var dataSet = {
                    id: dataSetId,
                    version: formVersion,
                    data: data
                };

                DAO.store.set( "forms", dataSet ).done(function() {
                    console.log( 'Successfully stored form: ' + dataSetId );
                    def.resolve();
                });

            	dhis2.de.storageManager.saveFormVersion( this.dataSetId, this.formVersion );
            }
        } );

        return def.promise();
    };

    /**
     * Saves a version for a form.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @param formVersion the version of the form.
     */
    this.saveFormVersion = function( dataSetId, formVersion )
    {
        var formVersions = {};

        if ( localStorage[KEY_FORM_VERSIONS] != null )
        {
            formVersions = JSON.parse( localStorage[KEY_FORM_VERSIONS] );
        }

        formVersions[dataSetId] = formVersion;

        try
        {
            localStorage[KEY_FORM_VERSIONS] = JSON.stringify( formVersions );

          console.log( 'Successfully stored form version: ' + dataSetId );
        } 
        catch ( e )
        {
          console.log( 'Max local storage quota reached, ignored form version: ' + dataSetId );
        }
    };

    /**
     * Returns the version of the form of the data set with the given
     * identifier.
     *
     * @param dataSetId the identifier of the data set of the form.
     * @return the form version.
     */
    this.getFormVersion = function( dataSetId )
    {
        if ( localStorage[KEY_FORM_VERSIONS] != null )
        {
            var formVersions = JSON.parse( localStorage[KEY_FORM_VERSIONS] );

            return formVersions[dataSetId];
        }

        return null;
    };

    /**
     * Deletes the form version of the data set with the given identifier.
     *
     * @param dataSetId the identifier of the data set of the form.
     */
    this.deleteFormVersion = function( dataSetId )
    {
    	if ( localStorage[KEY_FORM_VERSIONS] != null )
        {
            var formVersions = JSON.parse( localStorage[KEY_FORM_VERSIONS] );

            if ( formVersions[dataSetId] != null )
            {
                delete formVersions[dataSetId];
                localStorage[KEY_FORM_VERSIONS] = JSON.stringify( formVersions );
            }
        }
    };

    this.getAllFormVersions = function()
    {
        return localStorage[KEY_FORM_VERSIONS] != null ? JSON.parse( localStorage[KEY_FORM_VERSIONS] ) : null;
    };

    /**
     * Saves a data value.
     *
     * @param dataValue The datavalue and identifiers in json format.
     */
    this.saveDataValue = function( dataValue )
    {
        var id = this.getDataValueIdentifier( dataValue.de, 
        		dataValue.co, dataValue.pe, dataValue.ou );

        var dataValues = {};

        if ( localStorage[KEY_DATAVALUES] != null )
        {
            dataValues = JSON.parse( localStorage[KEY_DATAVALUES] );
        }

        dataValues[id] = dataValue;

        try
        {
            localStorage[KEY_DATAVALUES] = JSON.stringify( dataValues );

          console.log( 'Successfully stored data value' );
        } 
        catch ( e )
        {
          console.log( 'Max local storage quota reached, not storing data value locally' );
        }
    };

    /**
     * Gets the value for the data value with the given arguments, or null if it
     * does not exist.
     *
     * @param de the data element identifier.
     * @param co the category option combo identifier.
     * @param pe the period identifier.
     * @param ou the organisation unit identifier.
     * @return the value for the data value with the given arguments, null if
     *         non-existing.
     */
    this.getDataValue = function( de, co, pe, ou )
    {
        var id = this.getDataValueIdentifier( de, co, pe, ou );

        if ( localStorage[KEY_DATAVALUES] != null )
        {
            var dataValues = JSON.parse( localStorage[KEY_DATAVALUES] );

            return dataValues[id];
        }

        return null;
    };
    
    /**
     * Returns the data values for the given period and organisation unit 
     * identifiers as an array.
     * 
     * @param json object with periodId and organisationUnitId properties.
     */
    this.getDataValuesInForm = function( json )
    {
    	var dataValues = this.getDataValuesAsArray();
    	var valuesInForm = new Array();
    	
		for ( var i = 0; i < dataValues.length; i++ )
		{
			var val = dataValues[i];
			
			if ( val.pe == json.periodId && val.ou == json.organisationUnitId )
			{
				valuesInForm.push( val );
			}
		}
    	
    	return valuesInForm;
    }

    /**
     * Removes the given dataValue from localStorage.
     *
     * @param dataValue The datavalue and identifiers in json format.
     */
    this.clearDataValueJSON = function( dataValue )
    {
        this.clearDataValue( dataValue.de, dataValue.co, dataValue.pe,
                dataValue.ou );
    };

    /**
     * Removes the given dataValue from localStorage.
     *
     * @param de the data element identifier.
     * @param co the category option combo identifier.
     * @param pe the period identifier.
     * @param ou the organisation unit identifier.
     */
    this.clearDataValue = function( de, co, pe, ou )
    {
        var id = this.getDataValueIdentifier( de, co, pe, ou );
        var dataValues = this.getAllDataValues();

        if ( dataValues != null && dataValues[id] != null )
        {
            delete dataValues[id];
            localStorage[KEY_DATAVALUES] = JSON.stringify( dataValues );
        }
    };

    /**
     * Returns a JSON associative array where the keys are on the form <data
     * element id>-<category option combo id>-<period id>-<organisation unit
     * id> and the data values are the values.
     *
     * @return a JSON associative array.
     */
    this.getAllDataValues = function()
    {
        return localStorage[KEY_DATAVALUES] != null ? JSON.parse( localStorage[KEY_DATAVALUES] ) : null;
    };

    this.clearAllDataValues = function()
    {
        localStorage[KEY_DATAVALUES] = "";
    };

    /**
     * Returns all data value objects in an array. Returns an empty array if no
     * data values exist. Items in array are guaranteed not to be undefined.
     */
    this.getDataValuesAsArray = function()
    {
    	var values = new Array();
    	var dataValues = this.getAllDataValues();
    	
    	if ( undefined === dataValues )
    	{
    		return values;
    	}
    	
    	for ( i in dataValues )
    	{
    		if ( dataValues.hasOwnProperty( i ) && undefined !== dataValues[i] )
    		{
    			values.push( dataValues[i] );
    		}
    	}
    	
    	return values;
    }

    /**
     * Generates an identifier.
     */
    this.getDataValueIdentifier = function( de, co, pe, ou )
    {
        return de + '-' + co + '-' + pe + '-' + ou;
    };

    /**
     * Generates an identifier.
     */
    this.getCompleteDataSetId = function( json )
    {
        return json.ds + '-' + json.pe + '-' + json.ou;
    };

    /**
     * Returns current state in data entry form as associative array.
     *
     * @return an associative array.
     */
    this.getCurrentCompleteDataSetParams = function()
    {
        var params = {
            'ds': $( '#selectedDataSetId' ).val(),
            'pe': $( '#selectedPeriodId').val(),
            'ou': dhis2.de.getCurrentOrganisationUnit(),
            'multiOu': dhis2.de.multiOrganisationUnit
        };

        return params;
    };

    /**
     * Gets all complete data set registrations as JSON.
     *
     * @return all complete data set registrations as JSON.
     */
    this.getCompleteDataSets = function()
    {
        if ( localStorage[KEY_COMPLETEDATASETS] != null )
        {
            return JSON.parse( localStorage[KEY_COMPLETEDATASETS] );
        }

        return null;
    };

    /**
     * Saves a complete data set registration.
     *
     * @param json the complete data set registration as JSON.
     */
    this.saveCompleteDataSet = function( json )
    {
        var completeDataSets = this.getCompleteDataSets();
        var completeDataSetId = this.getCompleteDataSetId( json );

        if ( completeDataSets != null )
        {
            completeDataSets[completeDataSetId] = json;
        }
        else
        {
            completeDataSets = {};
            completeDataSets[completeDataSetId] = json;
        }

        try
        {
        	localStorage[KEY_COMPLETEDATASETS] = JSON.stringify( completeDataSets );
        	
        	log( 'Successfully stored complete registration' );
        }
        catch ( e )
        {
        	log( 'Max local storage quota reached, not storing complete registration locally' );
        }
    };
    
    /**
     * Indicates whether a complete data set registration exists for the given
     * argument.
     * 
     * @param json object with periodId, dataSetId, organisationUnitId properties.
     */
    this.hasCompleteDataSet = function( json )
    {
    	var id = this.getCompleteDataSetId( json );
    	var registrations = this.getCompleteDataSets();
    	
        if ( null != registrations && undefined !== registrations && undefined !== registrations[id] )
        {
            return true;
        }
    	
    	return false;
    }

    /**
     * Removes the given complete data set registration.
     *
     * @param json the complete data set registration as JSON.
     */
    this.clearCompleteDataSet = function( json )
    {
        var completeDataSets = this.getCompleteDataSets();
        var completeDataSetId = this.getCompleteDataSetId( json );

        if ( completeDataSets != null )
        {
            delete completeDataSets[completeDataSetId];

            if ( completeDataSets.length > 0 )
            {
                localStorage.removeItem( KEY_COMPLETEDATASETS );
            }
            else
            {
                localStorage[KEY_COMPLETEDATASETS] = JSON.stringify( completeDataSets );
            }
        }
    };

    /**
     * Indicates whether there exists data values or complete data set
     * registrations in the local storage.
     *
     * @return true if local data exists, false otherwise.
     */
    this.hasLocalData = function()
    {
        var dataValues = this.getAllDataValues();
        var completeDataSets = this.getCompleteDataSets();

        if ( dataValues == null && completeDataSets == null )
        {
            return false;
        }
        else if ( dataValues != null )
        {
            if ( Object.keys( dataValues ).length < 1 )
            {
                return false;
            }
        }
        else if ( completeDataSets != null )
        {
            if ( Object.keys( completeDataSets ).length < 1 )
            {
                return false;
            }
        }

        return true;
    };
}

// -----------------------------------------------------------------------------
// Option set
// -----------------------------------------------------------------------------

/**
 * Inserts the name of the option set in the input field with the given identifier.
 * The option set input fields should use the name as label and code as value to
 * be saved.
 * 
 * @fieldId the identifier of the field on the form #deuid-cocuid-val.
 * @value the value with properties id (deuid-cocuid) and val (option name).
 */
dhis2.de.setOptionNameInField = function( fieldId, value )
{
  var id = value.id;

  if(value.id.split("-").length == 3)
  {
    id = id.substr(12);
  }

	var optionSetUid = dhis2.de.optionSets[id].uid;

	DAO.store.get( 'optionSets', optionSetUid ).done( function( obj ) {
		if ( obj && obj.optionSet && obj.optionSet.options ) {			
			$.each( obj.optionSet.options, function( inx, option ) {
				if ( option && option.code == value.val ) {
          option.id = option.code;
          option.text = option.name;
          $( fieldId ).select2("val", option.text );
					return false;
				}
			} );
		}		
	} );
};

/**
 * Performs a search for options for the option set with the given identifier based
 * on the given query. If query is null, the first MAX options for the option set
 * is used. Checks and uses option set from local store, if not fetches option
 * set from server.
 */
dhis2.de.searchOptionSet = function( uid, query, success ) 
{
	var noneVal = '[No value]';
	
    if ( window.DAO !== undefined && window.DAO.store !== undefined ) {
        DAO.store.get( 'optionSets', uid ).done( function ( obj ) {
            if ( obj && obj.optionSet ) {
                var options = [];

                if ( query == null || query == '' || query == noneVal ) {
                    options = obj.optionSet.options.slice( 0, dhis2.de.cst.dropDownMaxItems - 1 );
                } 
                else {
                    query = query.toLowerCase();

                    for ( var idx=0, len = obj.optionSet.options.length; idx < len; idx++ ) {
                        var item = obj.optionSet.options[idx];

                        if ( options.length >= dhis2.de.cst.dropDownMaxItems ) {
                            break;
                        }

                        if ( item.name.toLowerCase().indexOf( query ) != -1 ) {
                            options.push( item );
                        }
                    }
                }
                
                if ( options && options.length > 0 ) {
                	options.push( { name: noneVal, code: '' } );
                }

                success( $.map( options, function ( item ) {
                    return {
                        label: item.name,
                        id: item.code
                    };
                } ) );
            }
            else {
                dhis2.de.getOptions( uid, query, success );
            }
        } );
    } 
    else {
        dhis2.de.getOptions( uid, query, success );
    }
};

/**
 * Retrieves options from server. Provides result as jquery ui structure to the
 * given jquery ui success callback.
 */
dhis2.de.getOptions = function( uid, query, success ) 
{
    return $.ajax( {
        url: '../api/optionSets/' + uid + '.json?links=false&q=' + query,
        dataType: "json",
        cache: false,
        type: 'GET',
        success: function ( data ) {
            success( $.map( data.options, function ( item ) {
                return {
                    label: item.name,
                    id: item.code
                };
            } ) );
        }
    } );
};

/**
 * Loads option sets from server into local store.
 */
dhis2.de.loadOptionSets = function() 
{
    var options = _.uniq( _.values( dhis2.de.optionSets ), function( item ) {
        return item.uid;
    }); // Array of objects with uid and v

    var uids = [];

    var deferred = $.Deferred();
    var promise = deferred.promise();

    _.each( options, function ( item, idx ) {
        if ( uids.indexOf( item.uid ) == -1 ) {
            DAO.store.get( 'optionSets', item.uid ).done( function( obj ) {
                if( !obj || !obj.optionSet || !obj.optionSet.version || !item.v || obj.optionSet.version !== item.v ) {
                    promise = promise.then( function () {
                        return $.ajax( {
                            url: '../api/optionSets/' + item.uid + '.json?fields=:all,options[:all]',
                            type: 'GET',
                            cache: false
                        } ).done( function ( data ) {
                            console.log( 'Successfully stored optionSet: ' + item.uid );

                            var obj = {};
                            obj.id = item.uid;
                            obj.optionSet = data;
                            DAO.store.set( 'optionSets', obj );
                        } );
                    } );

                    uids.push( item.uid );
                }
            });
        }
    } );

    promise = promise.then( function () {
    } );

    deferred.resolve();
};

/**
 * Inserts option sets in the appropriate input fields.
 */
dhis2.de.insertOptionSets = function() 
{
    $( '.entryoptionset').each( function( idx, item ) {
        
        var fieldId = item.id;
        
        var split = dhis2.de.splitFieldId( fieldId );

        var dataElementId = split.dataElementId;
        var optionComboId = split.optionComboId;
        
    	var optionSetKey = dhis2.de.splitFieldId( item.id );
        var s2prefix = 's2id_';        
        optionSetKey.dataElementId = optionSetKey.dataElementId.indexOf(s2prefix) != -1 ? optionSetKey.dataElementId.substring(s2prefix.length, optionSetKey.dataElementId.length) : optionSetKey.dataElementId;
        
        if ( dhis2.de.multiOrganisationUnit ) {
        	item = optionSetKey.organisationUnitId + '-' + optionSetKey.dataElementId + '-' + optionSetKey.optionComboId;
        } 
        else {
        	item = optionSetKey.dataElementId + '-' + optionSetKey.optionComboId;
        }
        
        item = item + '-val';
        optionSetKey = optionSetKey.dataElementId + '-' + optionSetKey.optionComboId;
        var optionSetUid = dhis2.de.optionSets[optionSetKey].uid;
        
        DAO.store.get( 'optionSets', optionSetUid ).done( function( obj ) {
		if ( obj && obj.optionSet && obj.optionSet.options ) {

                    $.each( obj.optionSet.options, function( inx, option ) {
                        option.text = option.name;
                        option.id = option.code;
                    } );
                    
                    $("#" + item).select2({
                        placeholder: i18n_select_option ,
                        allowClear: true,
                        dataType: 'json',
                        data: obj.optionSet.options
                    }).on("change", function(e){
                        saveVal( dataElementId, optionComboId, fieldId );
                    });
		}		
	} );        
    } );
};

/**
 * Applies the autocomplete widget on the given input field using the option set
 * with the given identifier.
 */
dhis2.de.autocompleteOptionSetField = function( idField, optionSetUid ) 
{
    var input = jQuery( '#' + idField );

    if ( !input ) {
        return;
    }

    input.autocomplete( {
        delay: 0,
        minLength: 0,
        source: function ( request, response ) {
            dhis2.de.searchOptionSet( optionSetUid, input.val(), response );
        },
        select: function ( event, ui ) {
            input.val( ui.item.id );
            input.autocomplete( 'close' );
            input.change();
        },
        change: function( event, ui ) {
            if( ui.item == null ) {
                $( this ).val("");
                $( this ).focus();
            }
        }
    } ).addClass( 'ui-widget' );

    input.data( 'ui-autocomplete' )._renderItem = function ( ul, item ) {
        return $( '<li></li>' )
            .data( 'item.autocomplete', item )
            .append( '<a>' + item.label + '</a>' )
            .appendTo( ul );
    };

    var wrapper = this.wrapper = $( '<span style="width:200px">' )
        .addClass( 'ui-combobox' )
        .insertAfter( input );

    var button = $( '<a style="width:20px; margin-bottom:1px; height:20px;">' )
        .attr( 'tabIndex', -1 )
        .attr( 'title', i18n_show_all_items )
        .appendTo( wrapper )
        .button( {
            icons: {
                primary: 'ui-icon-triangle-1-s'
            },
            text: false
        } )
        .addClass( 'small-button' )
        .click( function () {
            if ( input.autocomplete( 'widget' ).is( ':visible' ) ) {
                input.autocomplete( 'close' );
                return;
            }
            $( this ).blur();
            input.autocomplete( 'search', '' );
            input.focus();
        } );
};

// -----------------------------------------------------------------------------
// Various
// -----------------------------------------------------------------------------

function printBlankForm()
{
	$( '#contentDiv input, select' ).css( 'display', 'none' );
	window.print();
	$( '#contentDiv input, select' ).css( 'display', '' );	
}
