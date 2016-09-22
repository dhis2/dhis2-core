/**
 * Form setup for add and update forms
 */
( function( $ ) {
    var params = $( '#params' );
    var focusedTextArea = $( '#subjectTemplate' );
    var recipientSelector = $( '#notificationRecipient' );
    var userGroup = $( '#userGroup' );
    var userGroupContainer = $( '#userGroupContainer' );
    var notificationTrigger = $( '#notificationTrigger' );
    var daysContainer = $( '#daysContainer' );
    var days = $('#days' );
    var subjectTemplateTextArea = $( '#subjectTemplate' );
    var messageTemplateTextArea = $( '#messageTemplate' );
    var saveButton = $( '#save' );
    var cancelButton = $( '#cancel' );
    var programId = $( '#programId' ).value;
    var programUid = $( '#programUid' ).value;

    var isUpdate = Boolean( $( '#isUpdate' ).value );

    var templateUid = isUpdate ? $( '#templateUid' ).value : undefined;

    // Event handlers

    subjectTemplateTextArea.onfocus = function() {
        focusedTextArea = subjectTemplateTextArea;
    };

    messageTemplateTextArea.onfocus = function() {
        focusedTextArea = messageTemplateTextArea;
    };

    // Click handlers

    recipientSelector.onchange = function( e ) {
        if ( recipientSelector.value === 'USER_GROUP' ) {
            userGroupContainer.style.display = 'table-row';
            userGroup.disabled = false;
        } else {
            userGroupContainer.style.display = 'none';
            userGroup.value = "";
            userGroup.disabled = true;
        }
    };

    notificationTrigger.onchange = function( e ) {
        if ( notificationTrigger.value === 'ENROLLMENT' || notificationTrigger.value == 'COMPLETION' ) {
            daysContainer.style.display = 'none';
        } else {
            daysContainer.style.display = 'table-row';
            days.value = undefined;
        }
    };

    params.ondblclick = function() {
        insertTextCommon( focusedTextArea.id, params.value );
    };

    cancelButton.onclick = function() {
        window.location.href = 'programNotification.action?id=' + programId;
    };

    saveButton.onclick = function() {
        save( isUpdate );
    };

    // Internal

    function getSelectedDeliveryChannels() {
        return Array.prototype.slice.call(
            document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
            .filter( function( cb ) { return cb.checked; } )
            .map( function( cb ) { return cb.value; } );
    };

    function getUserGroup() {
        var uid = $( '#userGroup' ).value || undefined;
        return ( uid === undefined ) ? undefined : { 'id' : uid };
    };

    function getScheduledDays() {
        return ( $( '#days' ).value || 0 ) * ( $( '#daysModifier' ).value );
    };

    function formAsJson() {
        return {
            name : $( '#name' ).value || '',
            notificationTrigger : $( '#notificationTrigger' ).value,
            relativeScheduledDays : getScheduledDays(),
            notificationRecipient : $( '#notificationRecipient' ).value,
            recipientUserGroup : getUserGroup(),
            deliveryChannels : getSelectedDeliveryChannels(),
            subjectTemplate : $( '#subjectTemplate' ).value,
            messageTemplate : $( '#messageTemplate' ).value
        };
    };

    function save( update ) {
        var jsonData = formAsJson();

        jQuery.ajax( {
            url : '../api/programNotificationTemplates',
            dataType : 'json',
            data : JSON.stringify( jsonData ),
            contentType : 'application/json',
            type : 'POST'
        } )
            .then( function( result ) {
                if ( !update ) {
                    return jQuery.ajax( {
                        url: '../api/programs/' + programUid + '/notificationTemplates/' + result.response.uid,
                        type: 'POST'
                    } );
                }
            } )
            .done( function( result ) {
                window.location.href = 'programNotification.action?id=' + programId;
            } )
            .fail( function( jqXhr, textStatus, error ) {
                var json = jQuery.parseJSON( jqXhr.responseText );
                var errorMessage = json.response.errorReports[0].message;

                setHeaderDelayMessage( errorMessage || ( textStatus + ': ' + error ) );
            } );
    };
} )( document.querySelector.bind( document ) );