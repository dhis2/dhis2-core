/**
 * Form setup for add and update forms
 */
( function( qs ) {
    jQuery( document ).ready( function() {

        var focusedTextArea = qs( '#subjectTemplate' );
        var recipientSelector = qs( '#notificationRecipient' );
        var userGroup = qs( '#userGroup' );
        var userGroupContainer = qs( '#userGroupContainer' );

        var notificationRecipient = qs('#notificationRecipient');
        var subjectTemplateContainer = qs( '#subjectTemplateContainer' );
        var messageTemplateContainer = qs( '#messageTemplateContainer' );
        var sc = qs ('#subjectTemplate');
        var mc = qs ('#messageTemplate');

        var sendStrategy = qs('#sendStrategy');


        var paramsContainerForCompletion = qs( '#paramsContainerForCompletion' );
        var paramsContainerForSchedule = qs( '#paramsContainerForSchedule' );

        var paramsForCompletion = qs( '#paramsForCompletion' );
        var paramsForSchedule = qs( '#paramsForSchedule' );


        var programAttributeContainer=qs('#programAttributeContainer');
        var programAttribute = qs('#programAttribute');

        var dataSetList = qs('#dataSets');

        var notificationTrigger = qs( '#notificationTrigger' );
        var daysContainer = qs( '#daysContainer' );
        var days = qs( '#days' );

        var sendStrategyContainer = qs('#sendStrategyContainer');

        var deliveryChannelsContainer = qs( '#deliveryChannelsContainer' );
        var subjectTemplateTextArea = qs( '#subjectTemplate' );
        var messageTemplateTextArea = qs( '#messageTemplate' );
        var saveButton = qs( '#save' );
        var cancelButton = qs( '#cancel' );

        var isUpdate = JSON.parse( qs( '#isUpdate' ).value );

        var templateUid = isUpdate ? qs( '#templateUid' ).value : undefined;

        if ( !isUpdate )
        {
            paramsContainerForCompletion.style.display = 'table-row';
            subjectTemplateContainer.style.display = 'table-row';
            messageTemplateContainer.style.display = 'table-row';
            deliveryChannelsContainer.style.display = 'table-row';
        }

        // Event handlers

        subjectTemplateTextArea.addEventListener( "focus", function( e ) {
            focusedTextArea = subjectTemplateTextArea;
        } );

        messageTemplateTextArea.addEventListener( "focus", function ( e ) {
            focusedTextArea = messageTemplateTextArea;
        } );

        // Click handlers


        qs('#sendStrategy').addEventListener( "change", function( e ) {
            var strategy = qs('#sendStrategy').value;

            if ( strategy === 'COLLECTIVE_SUMMARY' ) {

                clearDeliveryChannels();

                userGroupContainer.style.display = 'table-row';
                userGroup.disabled = false;

                notificationRecipient.value = 'USER_GROUP';
                notificationRecipient.disabled = true;

                sc.value= " ";
                mc.value= " ";
                subjectTemplateContainer.style.display = 'none';
                messageTemplateContainer.style.display = 'none';
                paramsContainerForSchedule.style.display = 'none';
                deliveryChannelsContainer.style.display = 'none';




            } else {

                changesForSingleStrategy();
            }
        });

        recipientSelector.addEventListener( "change", function( e ) {
            var recipient = recipientSelector.value;

            if ( recipient === 'USER_GROUP' ) {
                userGroupContainer.style.display = 'table-row';
                userGroup.disabled = false;
            } else {
                userGroupContainer.style.display = 'none';
                userGroup.value = "";
                userGroup.disabled = true;
            }

            if ( isExternalRecipient( recipient ) ) {
                deliveryChannelsContainer.style.display = 'table-row';
            } else {
                deliveryChannelsContainer.style.display = 'none';
                clearDeliveryChannels();
            }
        });

        notificationTrigger.addEventListener( "change", function( e ) {
            if ( notificationTrigger.value == 'COMPLETION' ) {
                daysContainer.style.display = 'none';
                sendStrategyContainer.style.display = 'none';

                paramsContainerForCompletion.style.display = 'table-row';
                paramsContainerForSchedule.style.display = 'none';
                notificationRecipient.disabled = false;
                subjectTemplateContainer.style.display = 'table-row';
                messageTemplateContainer.style.display = 'table-row';

            } else {
                daysContainer.style.display = 'table-row';
                days.value = undefined;
                sendStrategyContainer.style.display = 'table-row';
                sendStrategy.value = 'SINGLE_NOTIFICATION';


                paramsContainerForSchedule.style.display = 'table-row';
                paramsContainerForCompletion.style.display = 'none';
            }
        } );

        paramsContainerForCompletion.addEventListener( "dblclick", function() {
            insertTextCommon( focusedTextArea.id, paramsForCompletion.value );
        } );

        paramsContainerForSchedule.addEventListener( "dblclick", function() {
            insertTextCommon( focusedTextArea.id, paramsForSchedule.value );
        } );


        cancelButton.addEventListener( "click", returnToListing );

        saveButton.addEventListener( "click", function() {
            var json = formAsJson();
            isUpdate ? update( json ) : save( json );
        } );

        // Internal

        function changesForSingleStrategy() {

            notificationRecipient.disabled = false;
            subjectTemplateContainer.style.display = 'table-row';
            messageTemplateContainer.style.display = 'table-row';
            paramsContainerForSchedule.style.display = 'table-row';

        }
        function isExternalRecipient( recipient ) {
            return recipient === 'ORGANISATION_UNIT_CONTACT';
        }

        function returnToListing() {
                window.location.href = 'dataSetNotifications.action';
        }

        function getSelectedDeliveryChannels() {
            return Array.prototype.slice.call(
                document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
                .filter( function( cb ) { return cb.checked; } )
                .map( function( cb ) { return cb.value; } );
        }

        function clearDeliveryChannels() {
            Array.prototype.slice.call(
                document.forms[ 'deliveryChannelsForm' ].elements[ 'deliveryChannels[]' ] )
                .forEach( function( cb ) {
                    cb.checked = false;
                } );
        }

        function getDataSets()
        {
            var fld = dataSetList;
            var values = [];
            for (var i = 0; i < fld.options.length; i++) {
                if (fld.options[i].selected) {

                    values.push( { 'id' : fld.options[i].value } );
                }
            }

            return values;
        }

        function getUserGroup() {
            var uid = qs( '#userGroup' ).value || undefined;
            return ( uid === undefined ) ? undefined : { 'id' : uid };
        }


        function getScheduledDays() {
            return ( days.value || 0 ) * ( qs( '#daysModifier' ).value );
        }


        function formAsJson() {
            return {
                name : qs( '#name' ).value || '',
                notificationTrigger : qs( '#notificationTrigger' ).value,
                relativeScheduledDays : getScheduledDays(),
                notificationRecipient : qs( '#notificationRecipient' ).value,
                recipientUserGroup : getUserGroup(),
                dataSets: getDataSets(),
                deliveryChannels : getSelectedDeliveryChannels(),
                subjectTemplate : qs( '#subjectTemplate' ).value,
                messageTemplate : qs( '#messageTemplate' ).value,
                sendStrategy : qs('#sendStrategy').value
            };
        }

        function update( json ) {
            jQuery.ajax( {
                url : '../api/dataSetNotificationTemplates/' + templateUid,
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : ' application/json',
                type : 'PUT'
            } ).done( returnToListing ).fail( onSaveFail );
        }

        function save( json ) {
            jQuery.ajax( {
                url : '../api/dataSetNotificationTemplates',
                dataType : 'json',
                data : JSON.stringify( json ),
                contentType : 'application/json',
                type : 'POST'
            } ).then( function( result ) {
                returnToListing();

            } ).fail( function( jqXhr, textStatus, error ) {
                    onSaveFail( jqXhr, textStatus, error );
                } );
        }


        function onSaveFail( jqXhr, textStatus, error ) {
            var json = jQuery.parseJSON( jqXhr.responseText );
            var errorMessage = json.response.errorReports[0].message;

            setHeaderDelayMessage( errorMessage || ( textStatus + ': ' + error ) );
        }
    } );
} )( document.querySelector.bind( document ) );