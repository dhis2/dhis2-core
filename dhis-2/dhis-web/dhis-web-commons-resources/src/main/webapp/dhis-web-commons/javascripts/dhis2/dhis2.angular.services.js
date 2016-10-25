/* Pagination service */
/* global angular, dhis2, moment */

var d2Services = angular.module('d2Services', ['ngResource'])

/* Factory for loading translation strings */
.factory('i18nLoader', function ($q, $http, SessionStorageService, DHIS2URL) {

    var getTranslationStrings = function (locale) {
        var defaultUrl = 'i18n/i18n_app.properties';
        var url = '';
        if (locale === 'en' || !locale) {
            url = defaultUrl;
        }
        else {
            url = 'i18n/i18n_app_' + locale + '.properties';
        }

        var tx = {locale: locale};

        var promise = $http.get(url).then(function (response) {
            tx = {locale: locale, keys: dhis2.util.parseJavaProperties(response.data)};
            return tx;
        }, function () {

            var p = $http.get(defaultUrl).then(function (response) {
                tx = {locale: locale, keys: dhis2.util.parseJavaProperties(response.data)};
                return tx;
            });
            return p;
        });
        return promise;
    };

    var getLocale = function () {
        var locale = 'en';

        var promise = $http.get( DHIS2URL + '/me/profile.json').then(function (response) {
            SessionStorageService.set('USER_PROFILE', response.data);
            if (response.data && response.data.settings && response.data.settings.keyUiLocale) {
                locale = response.data.settings.keyUiLocale;
            }
            return locale;
        }, function () {
            return locale;
        });

        return promise;
    };
    return function () {
        var deferred = $q.defer(), translations;
        var userProfile = SessionStorageService.get('USER_PROFILE');
        if (userProfile && userProfile.settings && userProfile.settings.keyUiLocale) {
            getTranslationStrings(userProfile.settings.keyUiLocale).then(function (response) {
                translations = response.keys;
                deferred.resolve(translations);
            });
            return deferred.promise;
        }
        else {
            getLocale().then(function (locale) {
                getTranslationStrings(locale).then(function (response) {
                    translations = response.keys;
                    deferred.resolve(translations);
                });
            });
            return deferred.promise;
        }
    };
})

.service('AuthorityService', function () {
    var getAuthorities = function (roles) {
        var authority = {};
        if (roles && roles.userCredentials && roles.userCredentials.userRoles) {
            angular.forEach(roles.userCredentials.userRoles, function (role) {
                angular.forEach(role.authorities, function (auth) {
                    authority[auth] = true;
                });
            });
        }
        return authority;
    };

    return {
        getUserAuthorities: function (roles) {
            var auth = getAuthorities(roles);
            var authority = {};
            authority.canDeleteEvent = auth['F_TRACKED_ENTITY_DATAVALUE_DELETE'] || auth['ALL'] ? true : false;
            authority.canAddOrUpdateEvent = auth['F_TRACKED_ENTITY_DATAVALUE_ADD'] || auth['ALL'] ? true : false;
            authority.canSearchTei = auth['F_TRACKED_ENTITY_INSTANCE_SEARCH'] || auth['ALL'] ? true : false;
            authority.canDeleteTei = auth['F_TRACKED_ENTITY_INSTANCE_DELETE'] || auth['ALL'] ? true : false;
            authority.canRegisterTei = auth['F_TRACKED_ENTITY_INSTANCE_ADD'] || auth['ALL'] ? true : false;
            authority.canEnrollTei = auth['F_PROGRAM_ENROLLMENT'] || auth['ALL'] ? true : false;
            authority.canUnEnrollTei = auth['F_PROGRAM_UNENROLLMENT'] || auth['ALL'] ? true : false;
            authority.canAdministerDashboard = auth['F_PROGRAM_DASHBOARD_CONFIG_ADMIN'] || auth['ALL'] ? true : false;
            return authority;
        }
    };
})

/* Factory for loading external data */
.factory('ExternalDataFactory', function ($http) {

    return {
        get: function (fileName) {
            var promise = $http.get(fileName).then(function (response) {
                return response.data;
            });
            return promise;
        }
    };
})

/* service for wrapping sessionStorage '*/
.service('SessionStorageService', function ($window) {
    return {
        get: function (key) {
            return JSON.parse($window.sessionStorage.getItem(key));
        },
        set: function (key, obj) {
            $window.sessionStorage.setItem(key, JSON.stringify(obj));
        },
        clearAll: function () {
            for (var key in $window.sessionStorage) {
                $window.sessionStorage.removeItem(key);
            }
        }
    };
})

/* service for getting calendar setting */
.service('CalendarService', function (storage, $rootScope) {

    return {
        getSetting: function () {

            var dhis2CalendarFormat = {keyDateFormat: 'yyyy-MM-dd', keyCalendar: 'gregorian', momentFormat: 'YYYY-MM-DD'};
            var storedFormat = storage.get('SYSTEM_SETTING');
            if (angular.isObject(storedFormat) && storedFormat.keyDateFormat && storedFormat.keyCalendar) {
                if (storedFormat.keyCalendar === 'iso8601') {
                    storedFormat.keyCalendar = 'gregorian';
                }

                if (storedFormat.keyDateFormat === 'dd-MM-yyyy') {
                    dhis2CalendarFormat.momentFormat = 'DD-MM-YYYY';
                }

                dhis2CalendarFormat.keyCalendar = storedFormat.keyCalendar;
                dhis2CalendarFormat.keyDateFormat = storedFormat.keyDateFormat;
            }
            $rootScope.dhis2CalendarFormat = dhis2CalendarFormat;
            return dhis2CalendarFormat;
        }
    };
})

/* service for dealing with dates */
.service('DateUtils', function ($filter, CalendarService) {

    return {
        getDate: function (dateValue) {
            if (!dateValue) {
                return;
            }
            var calendarSetting = CalendarService.getSetting();
            dateValue = moment(dateValue, calendarSetting.momentFormat)._d;
            return Date.parse(dateValue);
        },
        format: function (dateValue) {
            if (!dateValue) {
                return;
            }

            var calendarSetting = CalendarService.getSetting();
            dateValue = moment(dateValue, calendarSetting.momentFormat)._d;
            dateValue = $filter('date')(dateValue, calendarSetting.keyDateFormat);
            return dateValue;
        },
        formatToHrsMins: function (dateValue) {
            var calendarSetting = CalendarService.getSetting();
            var dateFormat = 'YYYY-MM-DD @ hh:mm A';
            if (calendarSetting.keyDateFormat === 'dd-MM-yyyy') {
                dateFormat = 'DD-MM-YYYY @ hh:mm A';
            }
            return moment(dateValue).format(dateFormat);
        },
        formatToHrsMinsSecs: function (dateValue) {
            var calendarSetting = CalendarService.getSetting();
            var dateFormat = 'YYYY-MM-DD @ hh:mm:ss A';
            if (calendarSetting.keyDateFormat === 'dd-MM-yyyy') {
                dateFormat = 'DD-MM-YYYY @ hh:mm:ss A';
            }
            return moment(dateValue).format(dateFormat);
        },

        getToday: function () {
            var calendarSetting = CalendarService.getSetting();
            var tdy = $.calendars.instance(calendarSetting.keyCalendar).newDate();
            var today = moment(tdy._year + '-' + tdy._month + '-' + tdy._day, 'YYYY-MM-DD')._d;
            today = Date.parse(today);
            today = $filter('date')(today, calendarSetting.keyDateFormat);
            return today;
        },
        formatFromUserToApi: function (dateValue) {
            if (!dateValue) {
                return;
            }
            var calendarSetting = CalendarService.getSetting();
            dateValue = moment(dateValue, calendarSetting.momentFormat)._d;
            dateValue = Date.parse(dateValue);
            dateValue = $filter('date')(dateValue, 'yyyy-MM-dd');
            return dateValue;
        },
        formatFromApiToUser: function (dateValue) {
            if (!dateValue) {
                return;
            }
            var calendarSetting = CalendarService.getSetting();
            dateValue = moment(dateValue, 'YYYY-MM-DD')._d;
            return $filter('date')(dateValue, calendarSetting.keyDateFormat);
        },
        getDateAfterOffsetDays: function (offSetDays) {
            var date = new Date();
            date.setDate(date.getDate()+offSetDays);
            var calendarSetting = CalendarService.getSetting();
            var tdy = $.calendars.instance(calendarSetting.keyCalendar).fromJSDate(date);
            var dateAfterOffset = moment(tdy._year + '-' + tdy._month + '-' + tdy._day, 'YYYY-MM-DD')._d;
            dateAfterOffset = Date.parse(dateAfterOffset);
            dateAfterOffset = $filter('date')(dateAfterOffset, calendarSetting.keyDateFormat);
            return dateAfterOffset;
        }
    };
})

/* Service for option name<->code conversion */
.factory('OptionSetService', function() {
    return {
        getCode: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){
                    if( key === options[i].displayName){
                        return options[i].code;
                    }
                }
            }
            return key;
        },
        getName: function(options, key){
            if(options){
                for(var i=0; i<options.length; i++){
                    if( key === options[i].code){
                        return options[i].displayName;
                    }
                }
            }
            return key;
        }
    };
})

/* service for common utils */
.service('CommonUtils', function(DateUtils, OptionSetService, CurrentSelection, FileService){

    return {
        formatDataValue: function(event, val, obj, optionSets, destination){
            var fileNames = CurrentSelection.getFileNames();
            if(val &&
                obj.valueType === 'NUMBER' ||
                obj.valueType === 'INTEGER' ||
                obj.valueType === 'INTEGER_POSITIVE' ||
                obj.valueType === 'INTEGER_NEGATIVE' ||
                obj.valueType === 'INTEGER_ZERO_OR_POSITIVE'){
                if( dhis2.validation.isNumber(val)){
                    if(obj.valueType === 'NUMBER'){
                        val = parseFloat(val);
                    }else{
                        val = parseInt(val);
                    }
                }
            }
            if(val && obj.optionSetValue && obj.optionSet && obj.optionSet.id && optionSets[obj.optionSet.id] && optionSets[obj.optionSet.id].options  ){
                if(destination === 'USER'){
                    val = OptionSetService.getName(optionSets[obj.optionSet.id].options, String(val));
                }
                else{
                    val = OptionSetService.getCode(optionSets[obj.optionSet.id].options, val);
                }

            }
            if(val && obj.valueType === 'DATE'){
                if(destination === 'USER'){
                    val = DateUtils.formatFromApiToUser(val);
                }
                else{
                    val = DateUtils.formatFromUserToApi(val);
                }
            }
            if(obj.valueType === 'TRUE_ONLY'){

                if(destination === 'USER'){
                    val = val === 'true' ? true : '';
                }
                else{
                    val = val === true ? 'true' : '';
                }
            }
            if(event && val && destination === 'USER' && obj.valueType === 'FILE_RESOURCE'){
                FileService.get(val).then(function(response){
                    if(response && response.name){
                        if(!fileNames[event]){
                            fileNames[event] = [];
                        }
                        fileNames[event][obj.id] = response.name;
                        CurrentSelection.setFileNames( fileNames );
                    }
                });
            }
            return val;
        },
        displayBooleanAsYesNo: function(value, dataElement){
            if(angular.isUndefined(dataElement) || dataElement.valueType === "BOOLEAN"){
                if(value === "true" || value === true){
                    return "Yes";
                }
                else if(value === "false" || value === false){
                    return "No";
                }
            }
            return value;
        },
        userHasValidRole: function(obj, prop, userRoles){
        	if( !obj || !prop || !userRoles){
                return false;
        	}
        	for(var i=0; i < userRoles.length; i++){            
                if( userRoles[i].authorities && userRoles[i].authorities.indexOf('ALL') !== -1 ){
                    return true;
                }
                if( userRoles[i][prop] && userRoles[i][prop].length > 0 ){
                    for( var j=0; j< userRoles[i][prop].length; j++){
                        if( obj.id === userRoles[i][prop][j].id ){
                            return true;
                        }
                    }
                }
            }
            return false;            	
        }
    };
})

/* service for dealing with custom form */
.service('CustomFormService', function ($translate, NotificationService) {

    return {
        getForProgramStage: function (programStage, programStageDataElements) {

            var htmlCode = programStage.dataEntryForm ? programStage.dataEntryForm.htmlCode : null;

            if (htmlCode) {
                var inputRegex = /<input.*?\/>/g,
                    match,
                    inputFields = [],
                    hasEventDate = false;

                while (match = inputRegex.exec(htmlCode)) {
                    inputFields.push(match[0]);
                }

                for (var i = 0; i < inputFields.length; i++) {
                    var inputField = inputFields[i];
                    
                    var inputElement = $.parseHTML(inputField);
                    var attributes = {};

                    $(inputElement[0].attributes).each(function () {
                        attributes[this.nodeName] = this.value;
                    });

                    var fieldId = '', newInputField;
                    if (attributes.hasOwnProperty('id')) {

                        if (attributes['id'] === 'executionDate') {
                            fieldId = 'eventDate';
                            hasEventDate = true;

                            //name needs to be unique so that it can be used for validation in angularjs
                            if (attributes.hasOwnProperty('name')) {
                                attributes['name'] = fieldId;
                            }

                            newInputField = '<span class="hideInPrint"><input type="text" ' +
                                this.getAttributesAsString(attributes) +
                                ' ng-model="currentEvent.' + fieldId + '"' +
                                ' input-field-id="' + fieldId + '"' +
                                ' d2-date ' +
                                ' d2-date-validator ' +
                                ' max-date="' + 0 + '"' +
                                ' placeholder="{{dhis2CalendarFormat.keyDateFormat}}" ' +
                                ' ng-class="getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id,true)"' +
                                ' blur-or-change="saveDatavalue(prStDes.' + fieldId + ')"' +
                                ' ng-required="{{true}}"></span><span class="not-for-screen"><input type="text" value={{currentEvent.' + fieldId + '}}></span>';
                        }
                        else {
                            fieldId = attributes['id'].substring(4, attributes['id'].length - 1).split("-")[1];

                            //name needs to be unique so that it can be used for validation in angularjs
                            if (attributes.hasOwnProperty('name')) {
                                attributes['name'] = fieldId;
                            }

                            var prStDe = programStageDataElements[fieldId];

                            if (prStDe && prStDe.dataElement && prStDe.dataElement.valueType) {

                                var commonInputFieldProperty = this.getAttributesAsString(attributes) +
                                    ' ng-model="currentEvent.' + fieldId + '" ' +
                                    ' input-field-id="' + fieldId + '"' +
                                    ' ng-disabled="isHidden(prStDes.' + fieldId + '.dataElement.id) || selectedEnrollment.status===\'CANCELLED\' || selectedEnrollment.status===\'COMPLETED\' || currentEvent[uid]==\'uid\' || currentEvent.editingNotAllowed "'+
                                    ' ng-required="{{prStDes.' + fieldId + '.compulsory}}" ';

                                
                                //check if dataelement has optionset
                                if (prStDe.dataElement.optionSetValue) {
                                    var optionSetId = prStDe.dataElement.optionSet.id;
                                    newInputField = '<span class="hideInPrint"><ui-select style="width: 100%;" theme="select2" ' + commonInputFieldProperty + ' on-select="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')" >' +
                                        '<ui-select-match ng-class="getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)" allow-clear="true" placeholder="' + $translate.instant('select_or_search') + '">{{$select.selected.displayName || $select.selected}}</ui-select-match>' +
                                        '<ui-select-choices ' +
                                        ' repeat="option.displayName as option in optionSets.' + optionSetId + '.options | filter: $select.search | limitTo:maxOptionSize">' +
                                        '<span ng-bind-html="option.displayName | highlight: $select.search">' +
                                        '</span>' +
                                        '</ui-select-choices>' +
                                        '</ui-select></span><span class="not-for-screen"><input type="text" value={{currentEvent.' + fieldId + '}}></span>';
                                }
                                else {
                                    //check data element type and generate corresponding angular input field
                                    if (prStDe.dataElement.valueType === "NUMBER" ||
                                        prStDe.dataElement.valueType === "INTEGER" ||
                                        prStDe.dataElement.valueType === "INTEGER_POSITIVE" ||
                                        prStDe.dataElement.valueType === "INTEGER_NEGATIVE" ||
                                        prStDe.dataElement.valueType === "INTEGER_ZERO_OR_POSITIVE") {
                                        newInputField = '<span class="hideInPrint"><input type="number" ' +
                                            ' d2-number-validator ' +
                                            ' ng-class="{{getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)}}" ' +
                                            ' number-type="' + prStDe.dataElement.valueType + '" ' +
                                            ' ng-blur="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')"' +
                                            commonInputFieldProperty + '></span><span class="not-for-screen"><input type="text" value={{currentEvent.' + fieldId + '}}></span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "BOOLEAN") {
                                    	newInputField = '<span class="hideInPrint"><d2-radio-button ' +
                                                                    ' dh-required="prStDes.' + fieldId + '.compulsory" ' +
                                                                    ' dh-disabled="isHidden(prStDes.' + fieldId + '.dataElement.id) || selectedEnrollment.status===\'CANCELLED\' || selectedEnrollment.status===\'COMPLETED\' || currentEvent[uid]==\'uid\' || currentEvent.editingNotAllowed" ' +
                                                                    ' dh-value="currentEvent.' + fieldId + '" ' +
                                                                    ' dh-name="foo" ' +
                                                                    ' dh-current-element="currentElement" ' +
                                                                    ' dh-event="currentEvent.event" ' +
                                                                    ' dh-id="prStDes.' + fieldId + '.dataElement.id" ' +
                                                                    ' dh-click="saveDatavalue(prStDes.' + fieldId + ', currentEvent, value )" >' +
                                                            ' </d2-radio-button></span> ' +
                                                            '<span class="not-for-screen">' +
                                                            	'<label class="radio-inline"><input type="radio" value="true" ng-model="currentEvent.' + fieldId +'">{{\'yes\' | translate}}</label>' +
                                                            	'<label class="radio-inline"><input type="radio" value="false" ng-model="currentEvent.' + fieldId + '">{{\'no\' | translate}}</label>' +
                                                            '</span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "DATE") {
                                        var maxDate = prStDe.allowFutureDate ? '' : 0;
                                        newInputField = '<span class="hideInPrint"><input type="text" ' +
                                            ' placeholder="{{dhis2CalendarFormat.keyDateFormat}}" ' +
                                            ' ng-class="{{getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)}}" ' +
                                            ' d2-date ' +
                                            ' d2-date-validator ' +
                                            ' max-date="' + maxDate + '"' +
                                            ' blur-or-change="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')"' +
                                            commonInputFieldProperty + ' ></span><span class="not-for-screen"><input type="text" value={{currentEvent.' + fieldId + '}}></span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "TRUE_ONLY") {
                                        newInputField = '<span class="hideInPrint"><input type="checkbox" ' +
                                            ' ng-class="{{getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)}}" ' +
                                            ' ng-change="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')"' +
                                            commonInputFieldProperty + ' ></span><span class="not-for-screen"><input type="checkbox" ng-checked={{currentEvent.' + fieldId + '}}></span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "LONG_TEXT") {
                                        newInputField = '<span class="hideInPrint"><textarea row="3" ' +
                                            ' ng-class="{{getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)}}" ' +
                                            ' ng-blur="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')"' +
                                            commonInputFieldProperty + '></textarea></span><span class="not-for-screen"><textarea row="3" value={{currentEvent.' + fieldId + '}}></textarea></span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "FILE_RESOURCE") {
                                        newInputField = '<span class="input-group hideInPrint">\n\
                                                        <span ng-if="currentEvent.' + fieldId + '">\n\
                                                            <a href ng-click="downloadFile(null, \'' + fieldId + '\', null)" title="fileNames[currentEvent.event][' + fieldId + ']" >{{fileNames[currentEvent.event][' + fieldId + '].length > 20 ? fileNames[currentEvent.event][' + fieldId + '].substring(0,20).concat(\'...\') : fileNames[currentEvent.event][' + fieldId + ']}}</a>\n\
                                                        </span>\n\
                                                        <span class="input-group-btn">\n\
                                                            <span class="btn btn-grp btn-file">\n\
                                                                <span ng-if="currentEvent.' + fieldId + '" title="{{\'delete\' | translate}}" d2-file-input-name="fileNames[currentEvent.event][' + fieldId + ']" d2-file-input-delete="currentEvent.' + fieldId + '">\n\
                                                                    <a href ng-click="deleteFile(\'' + fieldId + '\')"><i class="fa fa-trash alert-danger"></i></a>\n\
                                                                </span>\n\
                                                                <span ng-if="!currentEvent.' + fieldId + '" title="{{\'upload\' | translate}}" >\n\
                                                                    <i class="fa fa-upload"></i>\n\
                                                                    <input  type="file" \n\
                                                                            ' + this.getAttributesAsString(attributes) + '\n\
                                                                            input-field-id="' + fieldId + '"\n\
                                                                            d2-file-input-ps="currentStage"\n\
                                                                            d2-file-input="currentEvent"\n\
                                                                            d2-file-input-current-name="currentFileNames"\n\
                                                                            d2-file-input-name="fileNames">\n\
                                                                </span>\n\
                                                            </span>\n\
                                                        </span>\n\
                                                    </span>' 
                                                    '<span class="not-for-screen">' +
                                                    	'<input type="text" value={{currentEvent.' + fieldId + '}}' +
                                                    '</span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "COORDINATE") {
                                    	newInputField = '<span class="hideInPrint"><d2-map ' + 
                                    							' id=" ' + fieldId + '" ' +
						                                        ' d2-object="currentEvent" ' + 
						                                        ' d2-coordinate-format="\'TEXT\'" ' + 
						                                        ' d2-disabled="isHidden(prStDes.' + fieldId + '.dataElement.id) || selectedEnrollment.status===\'CANCELLED\' || selectedEnrollment.status===\'COMPLETED\' || currentEvent[uid]==\'uid\' || currentEvent.editingNotAllowed" ' +
					                                            ' d2-required="prStDes.' + fieldId + '.compulsory" ' +
						                                        ' d2-function="saveDatavalue(arg1)" ' +						                                        
						                                        ' d2-function-param-text="prStDes.' + fieldId + '" ' +
						                                        ' d2-function-param-coordinate="\'LATLNG\'" > ' +
						                                '</d2-map></span>' +
						                                '<span class="not-for-screen">' +
                                                    		'<input type="text" value={{currentEvent.' + fieldId + '}}' +
                                                    	'</span>';;
                                    }
                                    else if (prStDe.dataElement.valueType === "ORGANISATION_UNIT") {
                                    	newInputField = '<span class="hideInPrint"><d2-org-unit-tree ' +
					                                            ' selected-org-unit="selectedOrgUnit" ' +
					                                            ' id="{{prStDes.' + fieldId + '.dataElement.id}}" ' +
					                                            ' d2-object="currentEvent" ' +
					                                            ' d2-value="currentEvent.' + fieldId + '" ' +
					                                            ' d2-disabled="isHidden(prStDes.' + fieldId + '.dataElement.id) || selectedEnrollment.status===\'CANCELLED\' || selectedEnrollment.status===\'COMPLETED\' || currentEvent[uid]==\'uid\' || currentEvent.editingNotAllowed" ' +
					                                            ' d2-required="prStDes.' + fieldId + '.compulsory" ' +						                                            
					                                            ' d2-function="saveDatavalue(prStDes.' + fieldId + ', currentEvent, value )" >' +
					                                    ' </d2-org-unit-tree></span>' +
					                                    '<span class="not-for-screen">' +
                                                    		'<input type="text" value={{currentEvent.' + fieldId + '}}' +
                                                    	'</span>';
                                    }
                                    else if (prStDe.dataElement.valueType === "TEXT") {
                                        newInputField = '<span class="hideInPrint"><input type="text" ' +
                                            ' ng-class="{{getInputNotifcationClass(prStDes.' + fieldId + '.dataElement.id, true)}}" ' +
                                            ' ng-blur="saveDatavalue(prStDes.' + fieldId + ', outerForm.' + fieldId + ')"' +
                                            commonInputFieldProperty + '></span><span class="not-for-screen"><input type="text" value={{currentEvent.' + fieldId + '}}></span>';
                                    }
                                    else{
                                    	newInputField = ' {{"unsupported_value_type" | translate }}: ' + prStDe.dataElement.valueType;
                                    }
                                }
                            }
                            else{
                                NotificationService.showNotifcationDialog($translate.instant("error"),
                                    $translate.instant("custom_form_has_invalid_dataelement"));

                                return;
                            }
                            
                            
                        }
                        newInputField = newInputField + ' <span ng-messages="outerForm.' + fieldId + '.$error" class="required" ng-if="interacted(outerForm.' + fieldId + ')" ng-messages-include="../dhis-web-commons/angular-forms/error-messages.html"></span>';

                        htmlCode = htmlCode.replace(inputField, newInputField);

                    }
                }
                htmlCode = addPopOver(htmlCode, programStageDataElements);
                return {htmlCode: htmlCode, hasEventDate: hasEventDate};
            }
            return null;
        },
        getForTrackedEntity: function (trackedEntityForm, target) {
            if (!trackedEntityForm) {
                return null;
            }

            var htmlCode = trackedEntityForm.htmlCode ? trackedEntityForm.htmlCode : null;
            if (htmlCode) {

                var trackedEntityFormAttributes = [];
                angular.forEach(trackedEntityForm.attributes, function (att) {
                    trackedEntityFormAttributes[att.id] = att;
                });


                var inputRegex = /<input.*?\/>/g, match, inputFields = [];
                var hasProgramDate = false;
                while (match = inputRegex.exec(htmlCode)) {
                    inputFields.push(match[0]);
                }

                for (var i = 0; i < inputFields.length; i++) {
                    var inputField = inputFields[i];
                    var inputElement = $.parseHTML(inputField);
                    var attributes = {};

                    $(inputElement[0].attributes).each(function () {
                        attributes[this.nodeName] = this.value;
                    });

                    var attId = '', fieldName = '', newInputField, programId;
                    if (attributes.hasOwnProperty('attributeid')) {
                        attId = attributes['attributeid'];
                        fieldName = attId;
                        var att = trackedEntityFormAttributes[attId];

                        if (att) {
                            var attMaxDate = att.allowFutureDate ? '' : 0;
                            var isTrackerAssociate = att.valueType === 'TRACKER_ASSOCIATE';
                            var commonInputFieldProperty = ' name="' + fieldName + '"' +
                                ' element-id="' + i + '"' +
                                this.getAttributesAsString(attributes) +
                                ' d2-focus-next-on-enter' +
                                ' ng-model="selectedTei.' + attId + '" ' +
                                ' attribute-data="attributesById.' + attId + '" ' +
                                ' selected-program-id="selectedProgram.id" ' +
                                ' selected-tei-id="selectedTei.trackedEntityInstance" ' +
                                ' ng-disabled="editingDisabled || isHidden(attributesById.' + attId + '.id) || ' + isTrackerAssociate+ '|| attributesById.' + attId + '.generated"' +
                                ' d2-attribute-validator ' +
                                ' ng-required=" ' + att.mandatory + '" ';

                            //check if attribute has optionset
                            if (att.optionSetValue) {
                                var optionSetId = att.optionSet.id;
                                newInputField = '<span class="hideInPrint"><ui-select theme="select2" ' + commonInputFieldProperty + '  on-select="teiValueUpdated(selectedTei,\'' + attId + '\')" >' +
                                    '<ui-select-match style="width:100%;" allow-clear="true" placeholder="' + $translate.instant('select_or_search') + '">{{$select.selected.displayName || $select.selected}}</ui-select-match>' +
                                    '<ui-select-choices ' +
                                    'repeat="option.displayName as option in optionSets.' + optionSetId + '.options | filter: $select.search | limitTo:maxOptionSize">' +
                                    '<span ng-bind-html="option.displayName | highlight: $select.search"></span>' +
                                    '</ui-select-choices>' +
                                    '</ui-select></span><span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                            }
                            else {
                                //check attribute type and generate corresponding angular input field
                                if (att.valueType === "NUMBER" ||
                                		att.valueType === "INTEGER" ||
                                		att.valueType === "INTEGER_POSITIVE" ||
                                		att.valueType === "INTEGER_NEGATIVE" ||
                                		att.valueType === "INTEGER_ZERO_OR_POSITIVE" ) {
                                    newInputField = '<span class="hideInPrint"><input type="number"' +
                                        ' d2-number-validator ' +
                                        ' number-type="' + att.valueType + '" ' +
                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + ' ></span><span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "BOOLEAN") {
                                	newInputField = '<span class="hideInPrint"><d2-radio-button ' +
                                                            ' dh-required=" ' + (att.mandatory || att.unique) + '" ' +
                                                            ' dh-disabled="editingDisabled || isHidden(attributesById.' + attId + '.id) || ' + isTrackerAssociate + '"' +
                                                            ' dh-value="selectedTei.' + attId + '" ' +
                                                            ' dh-name="foo" ' +
                                                            ' dh-current-element="currentElement" ' +
                                                            ' dh-event="currentEvent.event" ' +
                                                            ' dh-id="' + attId + '" >' +
                                                    ' </d2-radio-button></span>' +
                                                    '<span class="not-for-screen">' +
                                                        '<label class="radio-inline"><input type="radio" value="true" ng-model="selectedTei.' + attId + '">{{\'yes\' | translate}}</label>' +
                                                        '<label class="radio-inline"><input type="radio" value="false" ng-model="selectedTei.' + attId + '">{{\'no\' | translate}}</label>' +
                                                    '</span>';
                                }
                                else if (att.valueType === "DATE") {
                                    newInputField = '<span class="hideInPrint"><input  type="text"' +
                                        ' placeholder="{{dhis2CalendarFormat.keyDateFormat}}" ' +
                                        ' max-date=" ' + attMaxDate + ' " ' +
                                        ' d2-date' +
                                        ' blur-or-change="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + ' ></span>' +
                                        '<span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "TRUE_ONLY") {
                                    newInputField = '<span class="hideInPrint"><input type="checkbox" ' +
                                        ' ng-change="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + ' ></span>' +
                                        '<span class="not-for-screen"><input type="checkbox" ng-checked={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "EMAIL") {
                                    newInputField = '<span class="hideInPrint"><input type="email"' +
                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + ' >' +
                                        '<span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "TRACKER_ASSOCIATE") {
                                	newInputField = '<span class="input-group hideInPrint"> ' +
                                                                        ' <input type="text" ' +
                                                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                                                        commonInputFieldProperty + ' >' +
                                                                        '<span class="input-group-btn input-group-btn-no-width"> ' +
                                                            '<button class="btn btn-grp default-btn-height" type="button" ' + 
                                                                ' title="{{\'add\' | translate}} {{attributesById.' + attId + '.displayName}}" ' +
                                                                ' ng-if="!selectedTei.' + attId + '" ' +
                                                                ' ng-class="{true: \'disable-clicks\'} [editingDisabled]" ' +
                                                                ' ng-click="getTrackerAssociate(attributesById.' + attId + ', selectedTei.' + attId + ')" >' +
                                                                '<i class="fa fa-external-link"></i> ' +
                                                            '</button> ' + 
                                                            '<button class="btn btn-grp default-btn-height" type="button" ' + 
                                                                ' title="{{\'remove\' | translate}} {{attributesById.' + attId + '.displayName}}" ' +
                                                                ' ng-if="selectedTei.' + attId + '" ' +
                                                                ' ng-class="{true: \'disable-clicks\'} [editingDisabled]" ' +
                                                                ' ng-click="selectedTei.' + attId + ' = null" >' +
                                                                '<i class="fa fa-trash-o"></i> ' +
                                                            '</button> ' + 
                                                        '</span>'+
                                                    '</span>'+
                                                    '<span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "COORDINATE") {
                                	newInputField = '<span class="hideInPrint"><d2-map ' + 
                            								' id=" ' + attId + '" ' +
						                                    ' d2-object="selectedTei" ' +  
						                                    ' d2-value="selectedTei.' + attId + '" ' +
						                                    ' d2-required=" ' + (att.mandatory || att.unique) + '" ' +
					                                        ' d2-disabled="editingDisabled || isHidden(attributesById.' + attId + '.id) || ' + isTrackerAssociate+ ' || attributesById.' + attId + '.generated"' +
						                                    ' d2-coordinate-format="\'TEXT\'" > ' +
						                            '</d2-map></span>'+
                                                                            '<span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "ORGANISATION_UNIT") {
                                	newInputField = '<span class="hideInPrint"><d2-org-unit-tree ' +
				                                            ' selected-org-unit="selectedOrgUnit" ' +
				                                            ' id=" ' + attId + '" ' +
				                                            ' d2-object="selectedTei" ' +  
						                                    ' d2-value="selectedTei.' + attId + '" ' +
						                                    ' d2-required=" ' + (att.mandatory || att.unique) + '" ' +
					                                        ' d2-disabled="editingDisabled || isHidden(attributesById.' + attId + '.id) || ' + isTrackerAssociate+ ' || attributesById.' + attId + '.generated"' +
					                                        ' d2-function="teiValueUpdated()" >' +
				                                    ' </d2-org-unit-tree></span>'+
                                                                    '<span class="not-for-screen"><input type="text" value={{selectedTei.' + attId + '}}></span>';
                                }
                                else if (att.valueType === "LONG_TEXT") {
                                    newInputField = '<span><textarea row ="3" ' +
                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + ' ></textarea></span>';
                                }
                                else if (att.valueType === "TEXT") {
                                    newInputField = '<input type="text" ' +
                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + '>';
                                }
                                else if (att.valueType === "PHONE_NUMBER") {
                                    newInputField = '<input type="text" ' +
                                        ' ng-blur="teiValueUpdated(selectedTei,\'' + attId + '\')" ' +
                                        commonInputFieldProperty + '>';
                                }
                                else {                                	
                                    newInputField = ' {{"unsupported_value_type" | translate }} ' + att.valueType;
                                }                                
                            }
                        }
                        else{
                            NotificationService.showNotifcationDialog($translate.instant("error"),
                                $translate.instant("custom_form_has_invalid_attribute"));
                            return;
                        }
                    }

                    if (attributes.hasOwnProperty('programid')) {
                        hasProgramDate = true;
                        programId = attributes['programid'];
                        if (programId === 'enrollmentDate') {
                            fieldName = 'dateOfEnrollment';
                            var enMaxDate = trackedEntityForm.selectEnrollmentDatesInFuture ? '' : 0;
                            newInputField = '<input type="text"' +
                                ' name="' + fieldName + '"' +
                                ' element-id="' + i + '"' +
                                this.getAttributesAsString(attributes) +
                                ' d2-focus-next-on-enter' +
                                ' placeholder="{{dhis2CalendarFormat.keyDateFormat}}" ' +
                                ' ng-model="selectedEnrollment.dateOfEnrollment" ' +
                                ' ng-disabled="\'' + target + '\' === \'PROFILE\'"' +
                                ' d2-date' +
                                ' max-date="' + enMaxDate + '"' +
                                ' ng-required="true">';
                        }
                        if (programId === 'dateOfIncident' && trackedEntityForm.displayIncidentDate) {
                            fieldName = 'dateOfIncident';
                            var inMaxDate = trackedEntityForm.selectIncidentDatesInFuture ? '' : 0;
                            newInputField = '<input type="text"' +
                                ' name="' + fieldName + '"' +
                                ' element-id="' + i + '"' +
                                this.getAttributesAsString(attributes) +
                                ' d2-focus-next-on-enter' +
                                ' placeholder="{{dhis2CalendarFormat.keyDateFormat}}" ' +
                                ' ng-model="selectedEnrollment.dateOfIncident" ' +
                                ' ng-disabled="\'' + target + '\' === \'PROFILE\'"' +
                                ' d2-date ' +
                                ' max-date="' + inMaxDate + '">';
                        }
                    }

                    newInputField = newInputField + ' <span ng-messages="outerForm.' + fieldName + '.$error" class="required" ng-if="interacted(outerForm.' + fieldName + ')" ng-messages-include="../dhis-web-commons/angular-forms/error-messages.html"></span>';

                    htmlCode = htmlCode.replace(inputField, newInputField);
                }
                htmlCode = addPopOver(htmlCode, trackedEntityFormAttributes);
                return {htmlCode: htmlCode, hasProgramDate: hasProgramDate};
            }
            return null;
        },
        getAttributesAsString: function (attributes) {
            if (attributes) {
                var attributesAsString = '';
                for (var prop in attributes) {
                    if (prop !== 'value') {
                        attributesAsString += prop + '="' + attributes[prop] + '" ';
                    }
                }
                return attributesAsString;
            }
            return null;
        }
    };
    /* This function inserts the d2-pop-over attributes into the tags containing d2-input-label attribute to
     * add description and url popover to those tags */
    function addPopOver(htmlCodeToInsertPopOver, popOverContent) {

        var inputRegex = /<span.*?\/span>/g;
        var match, tagToInsertPopOver, tagWithPopOver;
        var htmlCode = htmlCodeToInsertPopOver;
        while (match = inputRegex.exec(htmlCodeToInsertPopOver)) {
            if (match[0].indexOf("d2-input-label") > -1) {
                tagToInsertPopOver = match[0];
                tagWithPopOver = insertPopOverSpanToTag(tagToInsertPopOver, popOverContent);
                htmlCode = htmlCode.replace(tagToInsertPopOver,tagWithPopOver);
            }
        }
        return htmlCode;

    }

    function insertPopOverSpanToTag(tagToInsertPopOverSpan, popOverContent)  {

        var attribute, attributes, fieldId, description, url, element, attValue;
        var popOverSpanElement, tagWithPopOverSpan;

        element = $(tagToInsertPopOverSpan);
        attributes = element[0].attributes;

        for (var index = 0; index < attributes.length; index++) {
            if (attributes[index].name === "d2-input-label") {
                attValue = attributes[index].value;
                break;
            }
        }
        if (attValue) {
            popOverSpanElement = $('<span></span>');
            popOverSpanElement.attr("d2-pop-over","");
            popOverSpanElement.attr("details","{{'details'| translate}}");
            popOverSpanElement.attr("trigger","click");
            popOverSpanElement.attr("placement","right");
            popOverSpanElement.attr("class","popover-label");

            if (attValue.indexOf("attributeId.") > -1) {
                fieldId = attValue.split(".")[1];
                description = popOverContent[fieldId].description ? "'" + popOverContent[fieldId].description + "'" :
                    "undefined";
                popOverSpanElement.attr("content","{description: " + description + "}");
                popOverSpanElement.attr("template","attribute-details.html");

            } else {
                fieldId = attValue.split("-")[1];
                description = popOverContent[fieldId].dataElement.description ? "'" +
                popOverContent[fieldId].dataElement.description + "'" : "undefined";
                url = popOverContent[fieldId].dataElement.url ? "'" +
                popOverContent[fieldId].dataElement.url + "'" : "undefined";
                popOverSpanElement.attr("content","{description: " + description + ", url:" + url + "}");
                popOverSpanElement.attr("template","dataelement-details.html");
            }
            popOverSpanElement.html("<a href title=\"{{'details'| translate}}\" class=\"wrap-text\" tabindex=\"-1\">" +element.html() + "</a>");
            element.html(popOverSpanElement[0].outerHTML.replace('d2-pop-over=""','d2-pop-over'));
            tagWithPopOverSpan = element[0].outerHTML;
        }
        return tagWithPopOverSpan;
    }
})

/* Context menu for grid*/
.service('ContextMenuSelectedItem', function () {
    this.selectedItem = '';

    this.setSelectedItem = function (selectedItem) {
        this.selectedItem = selectedItem;
    };

    this.getSelectedItem = function () {
        return this.selectedItem;
    };
})

/* Modal service for user interaction */
.service('ModalService', ['$modal', function ($modal) {

    var modalDefaults = {
        backdrop: true,
        keyboard: true,
        modalFade: true,
        templateUrl: 'views/modal.html'
    };

    var modalOptions = {
        closeButtonText: 'Close',
        actionButtonText: 'OK',
        headerText: 'Proceed?',
        bodyText: 'Perform this action?'
    };

    this.showModal = function (customModalDefaults, customModalOptions) {
        if (!customModalDefaults)
            customModalDefaults = {};
        customModalDefaults.backdrop = 'static';
        return this.show(customModalDefaults, customModalOptions);
    };

    this.show = function (customModalDefaults, customModalOptions) {
        //Create temp objects to work with since we're in a singleton service
        var tempModalDefaults = {};
        var tempModalOptions = {};

        //Map angular-ui modal custom defaults to modal defaults defined in service
        angular.extend(tempModalDefaults, modalDefaults, customModalDefaults);

        //Map modal.html $scope custom properties to defaults defined in service
        angular.extend(tempModalOptions, modalOptions, customModalOptions);

        if (!tempModalDefaults.controller) {
            tempModalDefaults.controller = function ($scope, $modalInstance) {
                $scope.modalOptions = tempModalOptions;
                $scope.modalOptions.ok = function (result) {
                    $modalInstance.close(result);
                };
                $scope.modalOptions.close = function (result) {
                    $modalInstance.dismiss('cancel');
                };
            };
        }

        return $modal.open(tempModalDefaults).result;
    };

}])

/* Dialog service for user interaction */
.service('DialogService', ['$modal', function ($modal) {

    var dialogDefaults = {
        backdrop: true,
        keyboard: true,
        backdropClick: true,
        modalFade: true,
        templateUrl: 'views/dialog.html'
    };

    var dialogOptions = {
        closeButtonText: 'close',
        actionButtonText: 'ok',
        headerText: 'dhis2_tracker',
        bodyText: 'Perform this action?'
    };

    this.showDialog = function (customDialogDefaults, customDialogOptions) {
        if (!customDialogDefaults)
            customDialogDefaults = {};
        customDialogDefaults.backdropClick = false;
        return this.show(customDialogDefaults, customDialogOptions);
    };

    this.show = function (customDialogDefaults, customDialogOptions) {
        //Create temp objects to work with since we're in a singleton service
        var tempDialogDefaults = {};
        var tempDialogOptions = {};

        //Map angular-ui modal custom defaults to modal defaults defined in service
        angular.extend(tempDialogDefaults, dialogDefaults, customDialogDefaults);

        //Map modal.html $scope custom properties to defaults defined in service
        angular.extend(tempDialogOptions, dialogOptions, customDialogOptions);

        if (!tempDialogDefaults.controller) {
            tempDialogDefaults.controller = function ($scope, $modalInstance) {
                $scope.dialogOptions = tempDialogOptions;
                $scope.dialogOptions.ok = function (result) {
                    $modalInstance.close(result);
                };
            };
        }

        return $modal.open(tempDialogDefaults).result;
    };

}])
.service('NotificationService', function (DialogService) {
    this.showNotifcationDialog = function(errorMsgheader, errorMsgBody){
        var dialogOptions = {
            headerText: errorMsgheader,
            bodyText: errorMsgBody
        };
        DialogService.showDialog({}, dialogOptions);
    };

    this.showNotifcationWithOptions = function(dialogDefaults, dialogOptions){
        DialogService.showDialog(dialogDefaults, dialogOptions);
    };

})
.service('Paginator', function () {
    this.page = 1;
    this.pageSize = 50;
    this.itemCount = 0;
    this.pageCount = 0;
    this.toolBarDisplay = 5;

    this.setPage = function (page) {
        if (page > this.getPageCount()) {
            return;
        }

        this.page = page;
    };

    this.getPage = function () {
        return this.page;
    };

    this.setPageSize = function (pageSize) {
        this.pageSize = pageSize;
    };

    this.getPageSize = function () {
        return this.pageSize;
    };

    this.setItemCount = function (itemCount) {
        this.itemCount = itemCount;
    };

    this.getItemCount = function () {
        return this.itemCount;
    };

    this.setPageCount = function (pageCount) {
        this.pageCount = pageCount;
    };

    this.getPageCount = function () {
        return this.pageCount;
    };

    this.setToolBarDisplay = function (toolBarDisplay) {
        this.toolBarDisplay = toolBarDisplay;
    };

    this.getToolBarDisplay = function () {
        return this.toolBarDisplay;
    };

    this.lowerLimit = function () {
        var pageCountLimitPerPageDiff = this.getPageCount() - this.getToolBarDisplay();

        if (pageCountLimitPerPageDiff < 0) {
            return 0;
        }

        if (this.getPage() > pageCountLimitPerPageDiff + 1) {
            return pageCountLimitPerPageDiff;
        }

        var low = this.getPage() - (Math.ceil(this.getToolBarDisplay() / 2) - 1);

        return Math.max(low, 0);
    };
})

.service('GridColumnService', function ($http, $q, DHIS2URL, $translate, SessionStorageService, NotificationService) {
    var GRIDCOLUMNS_URL = DHIS2URL+'/userDataStore/gridColumns/';
    return {
        columnExists: function (cols, id) {
            var colExists = false;
            if (!angular.isObject(cols) || !id || angular.isObject(cols) && !cols.length) {
                return colExists;
            }

            for (var i = 0; i < cols.length && !colExists; i++) {
                if (cols[i].id === id) {
                    colExists = true;
                }
            }
            return colExists;
        },
        set: function (gridColumns, name) {
            var deferred = $q.defer();
            var httpMessage = {
                method: "put",
                url: GRIDCOLUMNS_URL + name,
                data: {"gridColumns": gridColumns},
                headers: {'Content-Type': 'application/json;charset=UTF-8'}
            };

            $http(httpMessage).then(function (response) {
                deferred.resolve(response.data);
            },function (error) {
                httpMessage.method = "post";
                $http(httpMessage).then(function (response) {
                    deferred.resolve(response.data);
                }, function (error) {
                    if (error && error.data) {
                        deferred.resolve(error.data);
                    } else {
                        deferred.resolve(null);
                    }
                });
            });
            return deferred.promise;
        },
        get: function (name) {
            var promise = $http.get(GRIDCOLUMNS_URL+name).then(function (response) {
                if (response && response.data && response.data.gridColumns) {
                    SessionStorageService.set(name, {id:name, columns:response.data.gridColumns});
                    return response.data.gridColumns;
                } else {
                    NotificationService.showNotifcationDialog($translate.instant("error"), $translate.instant("gridColumns_invalid"));
                    return null;
                }
            }, function (error) {
                var gridColumnsFromSessionStore = SessionStorageService.get(name);
                if (gridColumnsFromSessionStore && gridColumnsFromSessionStore.columns) {
                    return gridColumnsFromSessionStore.columns;
                }
                return null;
            });
            return promise;
        }
    };
})

/* Service for uploading/downloading file */
.service('FileService', function ($http, DHIS2URL) {

    return {
        get: function (uid) {
            var promise = $http.get(DHIS2URL + '/fileResources/' + uid).then(function (response) {
                return response.data;
            } ,function(error) {
                return null;
            });
            return promise;
        },
        download: function (fileName) {
            var promise = $http.get(fileName).then(function (response) {
                return response.data;
            }, function(error) {
                return null;
            });
            return promise;
        },
        upload: function(file){
            var formData = new FormData();
            formData.append('file', file);
            var headers = {transformRequest: angular.identity, headers: {'Content-Type': undefined}};
            var promise = $http.post(DHIS2URL + '/fileResources', formData, headers).then(function(response){
                return response.data;
            },function(error) {
               return null;
            });
            return promise;
        }
    };
})
/* Returns a function for getting rules for a specific program */
.factory('RulesFactory', function($q,MetaDataFactory,$filter){
    var staticReplacements = 
                        [{regExp:new RegExp("([^\w\d])(and)([^\w\d])","gi"), replacement:"$1&&$3"},
                        {regExp:new RegExp("([^\w\d])(or)([^\w\d])","gi"), replacement:"$1||$3"},
                        {regExp:new RegExp("V{execution_date}","g"), replacement:"V{event_date}"}];

    var performStaticReplacements = function(expression) {
        angular.forEach(staticReplacements, function(staticReplacement) {
            expression = expression.replace(staticReplacement.regExp, staticReplacement.replacement);
        });

        return expression;
    };

    return{        
        loadRules : function(programUid){            
            var def = $q.defer();            
            MetaDataFactory.getAll('constants').then(function(constants) {
                MetaDataFactory.getByProgram('programIndicators',programUid).then(function(pis){                    
                    var variables = [];
                    var programRules = [];
                    angular.forEach(pis, function(pi){
                        if(pi.displayInForm){
                            var newAction = {
                                    id:pi.id,
                                    content:pi.displayDescription ? pi.displayDescription : pi.displayName,
                                    data:pi.expression,
                                    programRuleActionType:'DISPLAYKEYVALUEPAIR',
                                    location:'indicators'
                                };
                            var newRule = {
                                    displayName:pi.displayName,
                                    id: pi.id,
                                    shortname:pi.shortname,
                                    code:pi.code,
                                    program:pi.program,
                                    description:pi.description,
                                    condition:pi.filter ? pi.filter : 'true',
                                    programRuleActions: [newAction]
                                };

                            programRules.push(newRule);

                            var variablesInCondition = newRule.condition.match(/[A#]{\w+.?\w*}/g);
                            var variablesInData = newAction.data.match(/[A#]{\w+.?\w*}/g);
                            var valueCountPresent = newRule.condition.indexOf("V{value_count}") >= 0 
                                                            || newAction.data.indexOf("V{value_count}") >= 0;
                            var positiveValueCountPresent = newRule.condition.indexOf("V{zero_pos_value_count}") >= 0
                                                            || newAction.data.indexOf("V{zero_pos_value_count}") >= 0;
                            var variableObjectsCurrentExpression = [];

                            var pushDirectAddressedVariable = function(variableWithCurls) {
                                var variableName = $filter('trimvariablequalifiers')(variableWithCurls);
                                var variableNameParts = variableName.split('.');

                                var newVariableObject;

                                if(variableNameParts.length === 2) {
                                    //this is a programstage and dataelement specification. translate to program variable:
                                    newVariableObject = {
                                        displayName:variableName,
                                        programRuleVariableSourceType:'DATAELEMENT_CURRENT_EVENT',
                                        dataElement:variableNameParts[1],
                                        program:programUid,
                                        useCodeForOptionSet:true
                                    };
                                }
                                else if(variableNameParts.length === 1)
                                {
                                    //This is an attribute - let us translate to program variable:
                                    newVariableObject = {
                                        displayName:variableName,
                                        programRuleVariableSourceType:'TEI_ATTRIBUTE',
                                        trackedEntityAttribute:variableNameParts[0],
                                        program:programUid,
                                        useCodeForOptionSet:true
                                    };
                                }
                                
                                variables.push(newVariableObject);

                                return newVariableObject;

                            };

                            angular.forEach(variablesInCondition, function(variableInCondition) {
                                var pushed = pushDirectAddressedVariable(variableInCondition);
                            });

                            angular.forEach(variablesInData, function(variableInData) {
                                var pushed = pushDirectAddressedVariable(variableInData);

                                //We only count the number of values in the data part of the rule
                                //(Called expression in program indicators)
                                variableObjectsCurrentExpression.push(pushed);
                            });

                            //Change expression or data part of the rule to match the program rules execution model
                            if(valueCountPresent) {
                                var valueCountText;
                                angular.forEach(variableObjectsCurrentExpression, function(variableCurrentRule) {
                                   if(valueCountText) {
                                       //This is not the first value in the value count part of the expression. 
                                       valueCountText +=  ' + d2:count(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                   else
                                   {
                                       //This is the first part value in the value count expression:
                                       valueCountText = '(d2:count(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                });
                                //To finish the value count expression we need to close the paranthesis:
                                valueCountText += ')';

                                //Replace all occurrences of value counts in both the data and expression:
                                newRule.condition = newRule.condition.replace(new RegExp("V{value_count}", 'g'),valueCountText);
                                newAction.data = newAction.data.replace(new RegExp("V{value_count}", 'g'),valueCountText);
                            }
                            if(positiveValueCountPresent) {
                                var zeroPosValueCountText;
                                angular.forEach(variableObjectsCurrentExpression, function(variableCurrentRule) {
                                   if(zeroPosValueCountText) {
                                       //This is not the first value in the value count part of the expression. 
                                       zeroPosValueCountText +=  '+ d2:countifzeropos(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                   else
                                   {
                                       //This is the first part value in the value count expression:
                                       zeroPosValueCountText = '(d2:countifzeropos(\'' + variableCurrentRule.displayName + '\')';
                                   }
                                });
                                //To finish the value count expression we need to close the paranthesis:
                                zeroPosValueCountText += ')';

                                //Replace all occurrences of value counts in both the data and expression:
                                newRule.condition = newRule.condition.replace(new RegExp("V{zero_pos_value_count}", 'g'),zeroPosValueCountText);
                                newAction.data = newAction.data.replace(new RegExp("V{zero_pos_value_count}", 'g'),zeroPosValueCountText);
                            }

                            newAction.data = performStaticReplacements(newAction.data);
                            newRule.condition = performStaticReplacements(newRule.condition);
                        }
                    });

                    var programIndicators = {rules:programRules, variables:variables};

                    MetaDataFactory.getByProgram('programValidations',programUid).then(function(programValidations){                    
                        MetaDataFactory.getByProgram('programRuleVariables',programUid).then(function(programVariables){                    
                            MetaDataFactory.getByProgram('programRules',programUid).then(function(prs){
                                var programRules = [];
                                angular.forEach(prs, function(rule){
                                    rule.actions = [];
                                    rule.programStageId = rule.programStage && rule.programStage.id ? rule.programStage.id : null;
                                    programRules.push(rule);
                                });                                
                                def.resolve({constants: constants, programIndicators: programIndicators, programValidations: programValidations, programVariables: programVariables, programRules: programRules});
                            });
                        });
                    });
                }); 
            });                        
            return def.promise;
        }
    };  
})
/* service for building variables based on the data in users fields */
.service('VariableService', function(DateUtils,OptionSetService,$filter,$log){
    var processSingleValue = function(processedValue,valueType){
        //First clean away single or double quotation marks at the start and end of the variable name.
        processedValue = $filter('trimquotes')(processedValue);

        //Append single quotation marks in case the variable is of text or date type:
        if(valueType === 'LONG_TEXT' || valueType === 'TEXT' || valueType === 'DATE' || valueType === 'OPTION_SET') {
            if(processedValue) {
                processedValue = "'" + processedValue + "'";
            } else {
                processedValue = "''";
            }
        }
        else if(valueType === 'BOOLEAN' || valueType === 'TRUE_ONLY') {
            if(processedValue && eval(processedValue)) {
                processedValue = true;
            }
            else {
                processedValue = false;
            }
        }
        else if(valueType === "INTEGER" || valueType === "NUMBER" || valueType === "INTEGER_POSITIVE" || valueType === "INTEGER_NEGATIVE" || valueType === "INTEGER_ZERO_OR_POSITIVE" || valueType === "PERCENTAGE") {
            if(processedValue) {
                processedValue = Number(processedValue);
            } else {
                processedValue = 0;
            }
        }
        else{
            $log.warn("unknown datatype:" + valueType);
        }

        return processedValue;
    };

    var pushVariable = function(variables, variablename, varValue, allValues, varType, variablefound, variablePrefix, variableEventDate, useCodeForOptionSet) {

        var processedValues = [];

        angular.forEach(allValues, function(alternateValue) {
            processedValues.push(processSingleValue(alternateValue,varType));
        });

        variables[variablename] = {
            variableValue:processSingleValue(varValue, varType),
            useCodeForOptionSet:useCodeForOptionSet,
            variableType:varType,
            hasValue:variablefound,
            variableEventDate:variableEventDate,
            variablePrefix:variablePrefix,
            allValues:processedValues
        };
        return variables;
    };
    
    var getDataElementValueOrCode = function(useCodeForOptionSet, event, dataElementId, allDes, optionSets) {
        return useCodeForOptionSet && allDes && allDes[dataElementId].dataElement.optionSet ? 
                                            OptionSetService.getCode(optionSets[allDes[dataElementId].dataElement.optionSet.id].options, event[dataElementId])
                                            : event[dataElementId];
    };

    return {
        processValue: function(value, type) {
            return processSingleValue(value,type);
        },
        getDataElementValueOrCode: function(useCodeForOptionSet, event, dataElementId, allDes, optionSets) {
            return getDataElementValueOrCode(useCodeForOptionSet, event, dataElementId, allDes, optionSets);
        },
        getVariables: function(allProgramRules, executingEvent, evs, allDes, selectedEntity, selectedEnrollment, optionSets) {

            var variables = {};

            var programVariables = allProgramRules.programVariables;

            programVariables = programVariables.concat(allProgramRules.programIndicators.variables);

            angular.forEach(programVariables, function(programVariable) {
                var dataElementId = programVariable.dataElement;
                if(programVariable.dataElement && programVariable.dataElement.id) {
                    dataElementId = programVariable.dataElement.id;
                }

                var trackedEntityAttributeId = programVariable.trackedEntityAttribute;
                if(programVariable.trackedEntityAttribute && programVariable.trackedEntityAttribute.id) {
                    trackedEntityAttributeId = programVariable.trackedEntityAttribute.id;
                }

                var programStageId = programVariable.programStage;
                if(programVariable.programStage && programVariable.programStage.id) {
                    programStageId = programVariable.programStage.id;
                }

                var valueFound = false;
                //If variable evs is not defined, it means the rules is run before any events is registered, skip the types that require an event
                if(programVariable.programRuleVariableSourceType === "DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE" && evs && evs.byStage){
                    if(programStageId) {
                        var allValues = [];
                        angular.forEach(evs.byStage[programStageId], function(event) {
                            if(event[dataElementId] !== null) {
                                if(angular.isDefined(event[dataElementId])
                                        && event[dataElementId] !== ""){
                                    var value = getDataElementValueOrCode(programVariable.useCodeForOptionSet, event, dataElementId, allDes, optionSets);
                                            
                                    allValues.push(value);
                                    valueFound = true;
                                    variables = pushVariable(variables, programVariable.displayName, value, allValues, allDes[dataElementId].dataElement.valueType, valueFound, '#', event.eventDate, programVariable.useCodeForOptionSet);
                                }
                            }
                        });
                    } else {
                        $log.warn("Variable id:'" + programVariable.id + "' name:'" + programVariable.displayName
                            + "' does not have a programstage defined,"
                            + " despite that the variable has sourcetype DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE" );
                    }
                }
                else if(programVariable.programRuleVariableSourceType === "DATAELEMENT_NEWEST_EVENT_PROGRAM" && evs){
                    var allValues = [];
                    angular.forEach(evs.all, function(event) {
                        if(angular.isDefined(event[dataElementId])
                            && event[dataElementId] !== null 
                            && event[dataElementId] !== ""){
                            var value = getDataElementValueOrCode(programVariable.useCodeForOptionSet, event, dataElementId, allDes, optionSets);
                                    
                            allValues.push(value);
                            valueFound = true;
                            variables = pushVariable(variables, programVariable.displayName, value, allValues, allDes[dataElementId].dataElement.valueType, valueFound, '#', event.eventDate, programVariable.useCodeForOptionSet);
                        }
                    });
                }
                else if(programVariable.programRuleVariableSourceType === "DATAELEMENT_CURRENT_EVENT" && evs){
                    if(angular.isDefined(executingEvent[dataElementId])
                        && executingEvent[dataElementId] !== null 
                        && executingEvent[dataElementId] !== ""){
                        var value = getDataElementValueOrCode(programVariable.useCodeForOptionSet, executingEvent, dataElementId, allDes, optionSets);
                            
                        valueFound = true;
                        variables = pushVariable(variables, programVariable.displayName, value, null, allDes[dataElementId].dataElement.valueType, valueFound, '#', executingEvent.eventDate, programVariable.useCodeForOptionSet );
                    }
                }
                else if(programVariable.programRuleVariableSourceType === "DATAELEMENT_PREVIOUS_EVENT" && evs){
                    //Only continue checking for a value if there is more than one event.
                    if(evs.all && evs.all.length > 1) {
                        var allValues = [];
                        var previousvalue = null;
                        var previousEventDate = null;
                        var currentEventPassed = false;
                        for(var i = 0; i < evs.all.length; i++) {
                            //Store the values as we iterate through the stages
                            //If the event[i] is not the current event, it is older(previous). Store the previous value if it exists
                            if(!currentEventPassed && evs.all[i] !== executingEvent &&
                                angular.isDefined(evs.all[i][dataElementId])
                                && evs.all[i][dataElementId] !== "") {
                                previousvalue = getDataElementValueOrCode(programVariable.useCodeForOptionSet, evs.all[i], dataElementId, allDes, optionSets);
                                previousEventDate = evs.all[i].eventDate;
                                allValues.push(value);
                                valueFound = true;
                            }
                            else if(evs.all[i] === executingEvent) {
                                //We have iterated to the newest event - store the last collected variable value - if any is found:
                                if(valueFound) {
                                    variables = pushVariable(variables, programVariable.displayName, previousvalue, allValues, allDes[dataElementId].dataElement.valueType, valueFound, '#', previousEventDate, programVariable.useCodeForOptionSet);
                                }
                                //Set currentEventPassed, ending the iteration:
                                currentEventPassed = true;
                            }
                        }
                    }
                }
                else if(programVariable.programRuleVariableSourceType === "TEI_ATTRIBUTE"){
                    angular.forEach(selectedEntity.attributes , function(attribute) {
                        if(!valueFound) {
                            if(attribute.attribute === trackedEntityAttributeId
                                    && angular.isDefined(attribute.value)
                                    && attribute.value !== null
                                    && attribute.value !== "") {
                                valueFound = true;
                                //In registration, the attribute type is found in .type, while in data entry the same data is found in .valueType.
                                //Handling here, but planning refactor in registration so it will always be .valueType
                                variables = pushVariable(variables, 
                                    programVariable.displayName, 
                                    programVariable.useCodeForOptionSet ? (angular.isDefined(attribute.optionSetCode) ? attribute.optionSetCode : attribute.value) : attribute.value,
                                    null, 
                                    attribute.type ? attribute.type : attribute.valueType, valueFound, 
                                    'A', 
                                    '',
                                    programVariable.useCodeForOptionSet);
                            }
                        }
                    });
                }
                else if(programVariable.programRuleVariableSourceType === "CALCULATED_VALUE"){
                    //We won't assign the calculated variables at this step. The rules execution will calculate and assign the variable.
                }
                else {
                    //If the rules was executed without events, we ended up in this else clause as expected, as most of the variables require an event to be mapped
                    if(evs)
                    {
                        //If the rules was executed and events was supplied, we should have found an if clause for the the source type, and not ended up in this dead end else.
                        $log.warn("Unknown programRuleVariableSourceType:" + programVariable.programRuleVariableSourceType);
                    }
                }


                if(!valueFound){
                    //If there is still no value found, assign default value:
                    if(dataElementId && allDes) {
                        var dataElement = allDes[dataElementId];
                        if( dataElement ) {
                            variables = pushVariable(variables, programVariable.displayName, "", null, dataElement.dataElement.valueType, false, '#', '', programVariable.useCodeForOptionSet );
                        }
                        else {
                            $log.warn("Variable #{" + programVariable.displayName + "} is linked to a dataelement that is not part of the program");
                            variables = pushVariable(variables, programVariable.displayName, "", null, "TEXT",false, '#', '', programVariable.useCodeForOptionSet );
                        }
                    }
                    else if (programVariable.trackedEntityAttribute) {
                        //The variable is an attribute, set correct prefix and a blank value
                        variables = pushVariable(variables, programVariable.displayName, "", null, "TEXT",false, 'A', '', programVariable.useCodeForOptionSet );
                    }
                    else {
                        //Fallback for calculated(assigned) values:
                        variables = pushVariable(variables, programVariable.displayName, "", null, "TEXT",false, '#', '', programVariable.useCodeForOptionSet );
                    }
                }
            });

            //add context variables:
            //last parameter "valuefound" is always true for event date
            variables = pushVariable(variables, 'current_date', DateUtils.getToday(), null, 'DATE', true, 'V', '', false );

            variables = pushVariable(variables, 'event_date', executingEvent.eventDate, null, 'DATE', true, 'V', '', false );
            variables = pushVariable(variables, 'due_date', executingEvent.dueDate, null, 'DATE', true, 'V', '' );
            variables = pushVariable(variables, 'event_count', evs ? evs.all.length : 0, null, 'INTEGER', true, 'V', '', false );

            variables = pushVariable(variables, 'enrollment_date', selectedEnrollment ? selectedEnrollment.enrollmentDate : '', null, 'DATE', selectedEnrollment ? true : false, 'V', '', false );
            variables = pushVariable(variables, 'enrollment_id', selectedEnrollment ? selectedEnrollment.enrollment : '', null, 'TEXT',  selectedEnrollment ? true : false, 'V', '', false );
            variables = pushVariable(variables, 'event_id', executingEvent ? executingEvent.event : '', null, 'TEXT',  executingEvent ? true : false, 'V', executingEvent ? executingEvent.eventDate : false, false);

            variables = pushVariable(variables, 'incident_date', selectedEnrollment ? selectedEnrollment.incidentDate : '', null, 'DATE',  selectedEnrollment ? true : false, 'V', '', false);
            variables = pushVariable(variables, 'enrollment_count', selectedEnrollment ? 1 : 0, null, 'INTEGER', true, 'V', '', false);
            variables = pushVariable(variables, 'tei_count', selectedEnrollment ? 1 : 0, null, 'INTEGER', true, 'V', '', false);

            //Push all constant values:
            angular.forEach(allProgramRules.constants, function(constant){
                variables = pushVariable(variables, constant.id, constant.value, null, 'INTEGER', true, 'C', '', false);
            });

            return variables;
        }
    };
})

/* service for executing tracker rules and broadcasting results */
.service('TrackerRulesExecutionService', function($translate, VariableService, DateUtils, NotificationService, DHIS2EventFactory, RulesFactory, CalendarService, OptionSetService, $rootScope, $q, $log, $filter, orderByFilter){
    var NUMBER_OF_EVENTS_IN_SCOPE = 10;

    //Variables for storing scope and rules in memory from rules execution to rules execution:
    var allProgramRules = false; 
    var crossEventRulesExist = false;
    var lastEventId = null;
    var lastEventDate = null;
    var lastProgramId = null;
    var eventScopeExceptCurrent = false;

    var replaceVariables = function(expression, variablesHash){
        //replaces the variables in an expression with actual variable values.

        //Check if the expression contains program rule variables at all(any curly braces):
        if(expression.indexOf('{') !== -1) {
            //Find every variable name in the expression;
            var variablespresent = expression.match(/[A#CV]{\w+.?\w*}/g);
            //Replace each matched variable:
            angular.forEach(variablespresent, function(variablepresent) {
                //First strip away any prefix and postfix signs from the variable name:
                variablepresent = variablepresent.replace("#{","").replace("A{","").replace("C{","").replace("V{","").replace("}","");

                if(angular.isDefined(variablesHash[variablepresent])) {
                    //Replace all occurrences of the variable name(hence using regex replacement):
                    expression = expression.replace(new RegExp( variablesHash[variablepresent].variablePrefix + "{" + variablepresent + "}", 'g'),
                        variablesHash[variablepresent].variableValue);
                }
                else {
                    $log.warn("Expression " + expression + " conains variable " + variablepresent
                        + " - but this variable is not defined." );
                }
            });
        }

        //Check if the expression contains environment  variables
        if(expression.indexOf('V{') !== -1) {
            //Find every variable name in the expression;
            var variablespresent = expression.match(/V{\w+.?\w*}/g);
            //Replace each matched variable:
            angular.forEach(variablespresent, function(variablepresent) {
                //First strip away any prefix and postfix signs from the variable name:
                variablepresent = variablepresent.replace("V{","").replace("}","");

                if(angular.isDefined(variablesHash[variablepresent]) &&
                    variablesHash[variablepresent].variablePrefix === 'V') {
                    //Replace all occurrences of the variable name(hence using regex replacement):
                    expression = expression.replace(new RegExp("V{" + variablepresent + "}", 'g'),
                        variablesHash[variablepresent].variableValue);
                }
                else {
                    $log.warn("Expression " + expression + " conains context variable " + variablepresent
                        + " - but this variable is not defined." );
                }
            });
        }

        //Check if the expression contains attribute variables:
        if(expression.indexOf('A{') !== -1) {
            //Find every attribute in the expression;
            var variablespresent = expression.match(/A{\w+.?\w*}/g);
            //Replace each matched variable:
            angular.forEach(variablespresent, function(variablepresent) {
                //First strip away any prefix and postfix signs from the variable name:
                variablepresent = variablepresent.replace("A{","").replace("}","");

                if(angular.isDefined(variablesHash[variablepresent]) &&
                    variablesHash[variablepresent].variablePrefix === 'A') {
                    //Replace all occurrences of the variable name(hence using regex replacement):
                    expression = expression.replace(new RegExp("A{" + variablepresent + "}", 'g'),
                        variablesHash[variablepresent].variableValue);
                }
                else {
                    $log.warn("Expression " + expression + " conains attribute " + variablepresent
                        + " - but this attribute is not defined." );
                }
            });
        }

        //Check if the expression contains constants
        if(expression.indexOf('C{') !== -1) {
            //Find every constant in the expression;
            var variablespresent = expression.match(/C{\w+.?\w*}/g);
            //Replace each matched variable:
            angular.forEach(variablespresent, function(variablepresent) {
                //First strip away any prefix and postfix signs from the variable name:
                variablepresent = variablepresent.replace("C{","").replace("}","");

                if(angular.isDefined(variablesHash[variablepresent]) &&
                    variablesHash[variablepresent].variablePrefix === 'C') {
                    //Replace all occurrences of the variable name(hence using regex replacement):
                    expression = expression.replace(new RegExp("C{" + variablepresent + "}", 'g'),
                        variablesHash[variablepresent].variableValue);
                }
                else {
                    $log.warn("Expression " + expression + " conains constant " + variablepresent
                        + " - but this constant is not defined." );
                }
            });
        }

        return expression;
    };

    var runDhisFunctions = function(expression, variablesHash, flag){
        //Called from "runExpression". Only proceed with this logic in case there seems to be dhis function calls: "d2:" is present.
        if(angular.isDefined(expression) && expression.indexOf("d2:") !== -1){
            var dhisFunctions = [{name:"d2:daysBetween",parameters:2},
                {name:"d2:weeksBetween",parameters:2},
                {name:"d2:monthsBetween",parameters:2},
                {name:"d2:yearsBetween",parameters:2},
                {name:"d2:floor",parameters:1},
                {name:"d2:modulus",parameters:2},
                {name:"d2:concatenate"},
                {name:"d2:addDays",parameters:2},
                {name:"d2:zing",parameters:1},
                {name:"d2:oizp",parameters:1},
                {name:"d2:count",parameters:1},
                {name:"d2:countIfZeroPos",parameters:1},
                {name:"d2:countIfValue",parameters:2},
                {name:"d2:ceil",parameters:1},
                {name:"d2:round",parameters:1},
                {name:"d2:hasValue",parameters:1},
                {name:"d2:lastEventDate",parameters:1},
                {name:"d2:validatePattern",parameters:2},
                {name:"d2:addControlDigits",parameters:1},
                {name:"d2:checkControlDigits",parameters:1},
                {name:"d2:left",parameters:2},
                {name:"d2:right",parameters:2},
                {name:"d2:substring",parameters:3},
                {name:"d2:split",parameters:3},
                {name:"d2:length",parameters:1}];
            var continueLooping = true;
            //Safety harness on 10 loops, in case of unanticipated syntax causing unintencontinued looping
            for(var i = 0; i < 10 && continueLooping; i++ ) {
                var expressionUpdated = false;
                var brokenExecution = false;
                angular.forEach(dhisFunctions, function(dhisFunction){
                    //Select the function call, with any number of parameters inside single quotations, or number parameters witout quotations
                    var regularExFunctionCall = new RegExp(dhisFunction.name + "\\( *(([\\d/\\*\\+\\-%\.]+)|( *'[^']*'))*( *, *(([\\d/\\*\\+\\-%\.]+)|'[^']*'))* *\\)",'g');
                    var callsToThisFunction = expression.match(regularExFunctionCall);
                    angular.forEach(callsToThisFunction, function(callToThisFunction){
                        //Remove the function name and paranthesis:
                        var justparameters = callToThisFunction.replace(/(^[^\(]+\()|\)$/g,"");
                        //Remove white spaces before and after parameters:
                        justparameters = justparameters.trim();
                        //Then split into single parameters:
                        var parameters = justparameters.match(/(('[^']+')|([^,]+))/g);

                        //Show error if no parameters is given and the function requires parameters,
                        //or if the number of parameters is wrong.
                        if(angular.isDefined(dhisFunction.parameters)){
                            //But we are only checking parameters where the dhisFunction actually has a defined set of parameters(concatenate, for example, does not have a fixed number);
                            var numParameters = parameters ? parameters.length : 0;
                            
                            if(numParameters !== dhisFunction.parameters){
                                $log.warn(dhisFunction.name + " was called with the incorrect number of parameters");
                                
                                //Mark this function call as broken:
                                brokenExecution = true;
                            }
                        }

                        //In case the function call is nested, the parameter itself contains an expression, run the expression.
                        if(!brokenExecution && angular.isDefined(parameters) && parameters !== null) {
                            for (var i = 0; i < parameters.length; i++) {
                                parameters[i] = runExpression(parameters[i],dhisFunction.name,"parameter:" + i, flag, variablesHash);
                            }
                        }

                        //Special block for d2:weeksBetween(*,*) - add such a block for all other dhis functions.
                        if(brokenExecution) {
                            //Function call is not possible to evaluate, remove the call:
                            expression = expression.replace(callToThisFunction, "false");
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:daysBetween") {
                            var firstdate = $filter('trimquotes')(parameters[0]);
                            var seconddate = $filter('trimquotes')(parameters[1]);
                            firstdate = moment(firstdate);
                            seconddate = moment(seconddate);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, seconddate.diff(firstdate,'days'));
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:weeksBetween") {
                            var firstdate = $filter('trimquotes')(parameters[0]);
                            var seconddate = $filter('trimquotes')(parameters[1]);
                            firstdate = moment(firstdate);
                            seconddate = moment(seconddate);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, seconddate.diff(firstdate,'weeks'));
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:monthsBetween") {
                            var firstdate = $filter('trimquotes')(parameters[0]);
                            var seconddate = $filter('trimquotes')(parameters[1]);
                            firstdate = moment(firstdate);
                            seconddate = moment(seconddate);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, seconddate.diff(firstdate,'months'));
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:yearsBetween") {
                            var firstdate = $filter('trimquotes')(parameters[0]);
                            var seconddate = $filter('trimquotes')(parameters[1]);
                            firstdate = moment(firstdate);
                            seconddate = moment(seconddate);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, seconddate.diff(firstdate,'years'));
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:floor") {
                            var floored = Math.floor(parameters[0]);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, floored);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:modulus") {
                            var dividend = Number(parameters[0]);
                            var divisor = Number(parameters[1]);
                            var rest = dividend % divisor;
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, rest);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:concatenate") {
                            var returnString = "'";
                            for (var i = 0; i < parameters.length; i++) {
                                returnString += parameters[i];
                            }
                            returnString += "'";
                            expression = expression.replace(callToThisFunction, returnString);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:addDays") {
                            var date = $filter('trimquotes')(parameters[0]);
                            var daystoadd = $filter('trimquotes')(parameters[1]);
                            var newdate = DateUtils.format( moment(date, CalendarService.getSetting().momentFormat).add(daystoadd, 'days') );
                            var newdatestring = "'" + newdate + "'";
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, newdatestring);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:zing") {
                            var number = parameters[0];
                            if( number < 0 ) {
                                number = 0;
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, number);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:oizp") {
                            var number = parameters[0];
                            var output = 1;
                            if( number < 0 ) {
                                output = 0;
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, output);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:count") {
                            var variableName = parameters[0];
                            var variableObject = variablesHash[variableName];
                            var count = 0;
                            if(variableObject)
                            {
                                if(variableObject.hasValue){
                                    if(variableObject.allValues)
                                    {
                                        count = variableObject.allValues.length;
                                    } else {
                                        //If there is a value found for the variable, the count is 1 even if there is no list of alternate values
                                        //This happens for variables of "DATAELEMENT_CURRENT_STAGE" and "TEI_ATTRIBUTE"
                                        count = 1;
                                    }
                                }
                            }
                            else
                            {
                                $log.warn("could not find variable to count: " + variableName);
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, count);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:countIfZeroPos") {
                            var variableName = $filter('trimvariablequalifiers') (parameters[0]);
                            var variableObject = variablesHash[variableName];

                            var count = 0;
                            if(variableObject)
                            {
                                if( variableObject.hasValue ) {
                                    if(variableObject.allValues && variableObject.allValues.length > 0)
                                    {
                                        for(var i = 0; i < variableObject.allValues.length; i++)
                                        {
                                            if(variableObject.allValues[i] >= 0) {
                                                count++;
                                            }
                                        }
                                    }
                                    else {
                                        //The variable has a value, but no list of alternates. This means we only compare the elements real value
                                        if(variableObject.variableValue >= 0) {
                                            count = 1;
                                        }
                                    }
                                }
                            }
                            else
                            {
                                $log.warn("could not find variable to countifzeropos: " + variableName);
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, count);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:countIfValue") {
                            var variableName = parameters[0];
                            var variableObject = variablesHash[variableName];

                            var valueToCompare = VariableService.processValue(parameters[1],variableObject.variableType);

                            var count = 0;
                            if(variableObject)
                            {
                                if( variableObject.hasValue )
                                {
                                    if( variableObject.allValues )
                                    {
                                        for(var i = 0; i < variableObject.allValues.length; i++)
                                        {
                                            if(valueToCompare === variableObject.allValues[i]) {
                                                count++;
                                            }
                                        }
                                    } else {
                                        //The variable has a value, but no list of alternates. This means we compare the standard variablevalue
                                        if(valueToCompare === variableObject.variableValue) {
                                            count = 1;
                                        }
                                    }

                                }
                            }
                            else
                            {
                                $log.warn("could not find variable to countifvalue: " + variableName);
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, count);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:ceil") {
                            var ceiled = Math.ceil(parameters[0]);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, ceiled);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:round") {
                            var rounded = Math.round(parameters[0]);
                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, rounded);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:hasValue") {
                            var variableName = parameters[0];
                            var variableObject = variablesHash[variableName];
                            var valueFound = false;
                            if(variableObject)
                            {
                                if(variableObject.hasValue){
                                    valueFound = true;
                                }
                            }
                            else
                            {
                                $log.warn("could not find variable to check if has value: " + variableName);
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, valueFound);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:lastEventDate") {
                            var variableName = parameters[0];
                            var variableObject = variablesHash[variableName];
                            var valueFound = "''";
                            if(variableObject)
                            {
                                if(variableObject.variableEventDate){
                                    valueFound = VariableService.processValue(variableObject.variableEventDate, 'DATE');
                                }
                                else {
                                    $log.warn("no last event date found for variable: " + variableName);
                                }
                            }
                            else
                            {
                                $log.warn("could not find variable to check last event date: " + variableName);
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, valueFound);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:validatePattern") {
                            var inputToValidate = parameters[0].toString();
                            var pattern = parameters[1];
                            var regEx = new RegExp(pattern,'g');
                            var match = inputToValidate.match(regEx);
                            
                            var matchFound = false;
                            if(match !== null && inputToValidate === match[0]) {
                                matchFound = true;
                            }

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, matchFound);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:addControlDigits") {

                            var baseNumber = parameters[0];
                            var baseDigits = baseNumber.split('');
                            var error = false;

                            var firstDigit = 0;
                            var secondDigit = 0;

                            if(baseDigits && baseDigits.length < 10 ) {
                                var firstSum = 0;
                                var baseNumberLength = baseDigits.length;
                                //weights support up to 9 base digits:
                                var firstWeights = [3,7,6,1,8,9,4,5,2];
                                for(var i = 0; i < baseNumberLength && !error; i++) {
                                    firstSum += parseInt(baseDigits[i]) * firstWeights[i];
                                }
                                firstDigit = firstSum % 11;

                                //Push the first digit to the array before continuing, as the second digit is a result of the
                                //base digits and the first control digit.
                                baseDigits.push(firstDigit);
                                //Weights support up to 9 base digits plus first control digit:
                                var secondWeights = [5,4,3,2,7,6,5,4,3,2];
                                var secondSum = 0;
                                for(var i = 0; i < baseNumberLength + 1 && !error; i++) {
                                    secondSum += parseInt(baseDigits[i]) * secondWeights[i];
                                }
                                secondDigit = secondSum % 11;

                                if(firstDigit === 10) {
                                    $log.warn("First control digit became 10, replacing with 0");
                                    firstDigit = 0;
                                }
                                if(secondDigit === 10) {
                                    $log.warn("Second control digit became 10, replacing with 0");
                                    secondDigit = 0;
                                }
                            }
                            else
                            {
                                $log.warn("Base nuber not well formed(" + baseNumberLength + " digits): " + baseNumber);
                            }

                            if(!error) {
                                //Replace the end evaluation of the dhis function:
                                expression = expression.replace(callToThisFunction, baseNumber + firstDigit + secondDigit);
                                expressionUpdated = true;
                            }
                            else
                            {
                                //Replace the end evaluation of the dhis function:
                                expression = expression.replace(callToThisFunction, baseNumber);
                                expressionUpdated = true;
                            }
                        }
                        else if(dhisFunction.name === "d2:checkControlDigits") {
                            $log.warn("checkControlDigits not implemented yet");

                            //Replace the end evaluation of the dhis function:
                            expression = expression.replace(callToThisFunction, parameters[0]);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:left") {
                            var string = String(parameters[0]);
                            var numChars = string.length < parameters[1] ? string.length : parameters[1];
                            var returnString =  string.substring(0,numChars);
                            returnString = VariableService.processValue(returnString, 'TEXT');
                            expression = expression.replace(callToThisFunction, returnString);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:right") {
                            var string = String(parameters[0]);
                            var numChars = string.length < parameters[1] ? string.length : parameters[1];
                            var returnString =  string.substring(string.length - numChars, string.length);
                            returnString = VariableService.processValue(returnString, 'TEXT');
                            expression = expression.replace(callToThisFunction, returnString);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:substring") {
                            var string = String(parameters[0]);
                            var startChar = string.length < parameters[1] - 1 ? -1 : parameters[1];
                            var endChar = string.length < parameters[2] ? -1 : parameters[2];
                            if(startChar < 0 || endChar < 0) {
                                expression = expression.replace(callToThisFunction, "''");
                                expressionUpdated = true;
                            } else {
                                var returnString =  string.substring(startChar, endChar);
                                returnString = VariableService.processValue(returnString, 'TEXT');
                                expression = expression.replace(callToThisFunction, returnString);
                                expressionUpdated = true;
                            }
                        }
                        else if(dhisFunction.name === "d2:split") {
                            var string = String(parameters[0]);
                            var splitArray = string.split(parameters[1]);
                            var returnPart = "";
                            if (splitArray.length >= parameters[2]) {
                                returnPart = splitArray[parameters[2]];
                            }
                            returnPart = VariableService.processValue(returnPart, 'TEXT');
                            expression = expression.replace(callToThisFunction, returnPart);
                            expressionUpdated = true;
                        }
                        else if(dhisFunction.name === "d2:length") {
                            expression = expression.replace(callToThisFunction, String(parameters[0]).length);
                            expressionUpdated = true;
                        }
                    });
                });

                //We only want to continue looping until we made a successful replacement,
                //and there is still occurrences of "d2:" in the code. In cases where d2: occur outside
                //the expected d2: function calls, one unneccesary iteration will be done and the
                //successfulExecution will be false coming back here, ending the loop. The last iteration
                //should be zero to marginal performancewise.
                if(expressionUpdated && expression.indexOf("d2:") !== -1) {
                    continueLooping = true;
                } else {
                    continueLooping = false;
                }
            }
        }

        return expression;
    };

    var runExpression = function(expression, beforereplacement, identifier, flag, variablesHash ){
        //determine if expression is true, and actions should be effectuated
        //If DEBUG mode, use try catch and report errors. If not, omit the heavy try-catch loop.:
        var answer = false;
        if(flag && flag.debug) {
            try{

                var dhisfunctionsevaluated = runDhisFunctions(expression, variablesHash, flag);
                answer = eval(dhisfunctionsevaluated);

                if(flag.verbose)
                {
                    $log.info("Expression with id " + identifier + " was successfully run. Original condition was: " + beforereplacement + " - Evaluation ended up as:" + expression + " - Result of evaluation was:" + answer);
                }
            }
            catch(e)
            {
                $log.warn("Expression with id " + identifier + " could not be run. Original condition was: " + beforereplacement + " - Evaluation ended up as:" + expression + " - error message:" + e);
            }
        }
        else {
            //Just run the expression. This is much faster than the debug route: http://jsperf.com/try-catch-block-loop-performance-comparison
            var dhisfunctionsevaluated = runDhisFunctions(expression, variablesHash, flag);
            answer = eval(dhisfunctionsevaluated);
        }
        return answer;
    };

    var determineValueType = function(value) {
        var valueType = 'TEXT';
        if(value === 'true' || value === 'false') {
            valueType = 'BOOLEAN';
        }
        else if(angular.isNumber(value) || !isNaN(value)) {
            if(value % 1 !== 0) {
                valueType = 'NUMBER';
            }
            else {
                valueType = 'INTEGER';
            }
        }
        return valueType;
    };

    var performCreateEventAction = function(effect, selectedEntity, selectedEnrollment, currentEvents,executingEvent, programStage){
        var valArray = [];
        if(effect.data) {
            valArray = effect.data.split(',');
            var newEventDataValues = [];
            var idList = {active:false};

            angular.forEach(valArray, function(value) {
                var valParts = value.split(':');                
                if(valParts && valParts.length >= 1) {
                    var valId = valParts[0];

                    //Check wether one or more fields is marked as the id to use for comparison purposes:
                    if(valId.trim().substring(0, 4) === "[id]") {
                        valId = valId.substring(4,valId.length);
                        idList[valId] = true;
                        idList.active = true;
                    }

                    var valVal = "";
                    if(valParts.length > 1) {
                        valVal = valParts[1];
                    }
                    var valueType = determineValueType(valVal);

                    var processedValue = VariableService.processValue(valVal, valueType);
                    processedValue = $filter('trimquotes')(processedValue);
                    newEventDataValues.push({dataElement:valId,value:processedValue});
                    newEventDataValues[valId] = processedValue;
                }
            });

            var valuesAlreadyExists = false;
            angular.forEach(currentEvents, function(currentEvent) {
                var misMatch = false;
                angular.forEach(newEventDataValues, function(value) {
                    var valueFound = false;
                    angular.forEach(currentEvent.dataValues, function(currentDataValue) {
                        //Only count as mismatch if there is no particular ID to use, or the current field is part of the same ID
                        if(!idList.active || idList[currentDataValue.dataElement]){
                            if(currentDataValue.dataElement === value.dataElement) {
                                valueFound = true;
                                //Truthy comparison is needed to avoid false negatives for differing variable types:
                                if( currentDataValue.value != newEventDataValues[value.dataElement] ) {
                                    misMatch = true;
                                }
                            }
                        }
                    });
                    //Also treat no value found as a mismatch, but when ID fields is set, only concider ID fields
                    if((!idList.active || idList[value.dataElement] ) && !valueFound) {
                        misMatch = true;
                    }
                });
                if(!misMatch) {
                    //if no mismatches on this point, the exact same event already exists, and we dont create it.
                    valuesAlreadyExists = true;
                }
            });

            if(!valuesAlreadyExists) {
                var eventDate = DateUtils.getToday();
                var dueDate = DateUtils.getToday();

                var newEvent = {
                    trackedEntityInstance: selectedEnrollment.trackedEntityInstance,
                    program: selectedEnrollment.program,
                    programStage: effect.programStage.id,
                    enrollment: selectedEnrollment.enrollment,
                    orgUnit: selectedEnrollment.orgUnit,
                    dueDate: dueDate,
                    eventDate: eventDate,
                    notes: [],
                    dataValues: newEventDataValues,
                    status: 'ACTIVE',
                    event: dhis2.util.uid()
                };

                if(programStage && programStage.dontPersistOnCreate){
                    newEvent.notPersisted = true;
                    newEvent.executingEvent = executingEvent;
                    $rootScope.$broadcast("eventcreated", { event:newEvent });
                }
                else{
                    DHIS2EventFactory.create(newEvent).then(function(result){
                       $rootScope.$broadcast("eventcreated", { event:newEvent });
                    }); 
                }
                //1 event created
                return 1;
            }
            else
            {
                //no events created
                return 0;
            }
        } else {
            $log.warn("Cannot create event with empty content.");
        }
    };
    
    var internalExecuteRules = function(allProgramRules, executingEvent, evs, allDataElements, selectedEntity, selectedEnrollment, optionSets, flag) {
        if(allProgramRules) {
            var variablesHash = {};

            //Concatenate rules produced by indicator definitions into the other rules:
            var rules = $filter('filter')(allProgramRules.programRules, {programStageId: null});

            if(executingEvent && executingEvent.programStage){
                if(!rules) {
                    rules = [];
                }
                rules = rules.concat($filter('filter')(allProgramRules.programRules, {programStageId: executingEvent.programStage}));
            }
            if(!rules) {
                rules = [];
            }
            rules = rules.concat(allProgramRules.programIndicators.rules);

            //Run rules in priority - lowest number first(priority null is last)
            rules = orderByFilter(rules, 'priority');

            variablesHash = VariableService.getVariables(allProgramRules, executingEvent, evs, allDataElements, selectedEntity, selectedEnrollment, optionSets);

            if(angular.isObject(rules) && angular.isArray(rules)){
                //The program has rules, and we want to run them.
                //Prepare repository unless it is already prepared:
                if(angular.isUndefined( $rootScope.ruleeffects ) ) {
                    $rootScope.ruleeffects = {};
                }

                var ruleEffectKey = executingEvent.event ? executingEvent.event : executingEvent;
                if( executingEvent.event && angular.isUndefined( $rootScope.ruleeffects[ruleEffectKey] )){
                    $rootScope.ruleeffects[ruleEffectKey] = {};
                }

                if(!angular.isObject(executingEvent) && angular.isUndefined( $rootScope.ruleeffects[ruleEffectKey] )){
                    $rootScope.ruleeffects[ruleEffectKey] = {};
                }

                var updatedEffectsExits = false;
                var eventsCreated = 0;

                angular.forEach(rules, function(rule) {
                    var ruleEffective = false;

                    var expression = rule.condition;
                    //Go through and populate variables with actual values, but only if there actually is any replacements to be made(one or more "$" is present)
                    if(expression) {
                        if(expression.indexOf('{') !== -1) {
                            expression = replaceVariables(expression, variablesHash);
                        }
                        //run expression:
                        ruleEffective = runExpression(expression, rule.condition, "rule:" + rule.id, flag, variablesHash);
                    } else {
                        $log.warn("Rule id:'" + rule.id + "'' and name:'" + rule.name + "' had no condition specified. Please check rule configuration.");
                    }

                    angular.forEach(rule.programRuleActions, function(action){
                        //In case the effect-hash is not populated, add entries
                        if(angular.isUndefined( $rootScope.ruleeffects[ruleEffectKey][action.id] )){
                            $rootScope.ruleeffects[ruleEffectKey][action.id] =  {
                                id:action.id,
                                location:action.location,
                                action:action.programRuleActionType,
                                dataElement:action.dataElement,
                                trackedEntityAttribute:action.trackedEntityAttribute,
                                programStage: action.programStage,
                                programIndicator: action.programIndicator,
                                programStageSection: action.programStageSection && action.programStageSection.id ? action.programStageSection.id : null,
                                content:action.content,
                                data:action.data,
                                ineffect:undefined
                            };
                        }

                        //In case the rule is effective and contains specific data,
                        //the effect be refreshed from the variables list.
                        //If the rule is not effective we can skip this step
                        if(ruleEffective && action.data)
                        {
                            //Preserve old data for comparison:
                            var oldData = $rootScope.ruleeffects[ruleEffectKey][action.id].data;

                            //The key data might be containing a dollar sign denoting that the key data is a variable.
                            //To make a lookup in variables hash, we must make a lookup without the dollar sign in the variable name
                            //The first strategy is to make a direct lookup. In case the "data" expression is more complex, we have to do more replacement and evaluation.

                            var nameWithoutBrackets = action.data.replace('#{','').replace('}','');
                            if(angular.isDefined(variablesHash[nameWithoutBrackets]))
                            {
                                //The variable exists, and is replaced with its corresponding value
                                $rootScope.ruleeffects[ruleEffectKey][action.id].data =
                                    variablesHash[nameWithoutBrackets].variableValue;
                            }
                            else if(action.data.indexOf('{') !== -1 || action.data.indexOf('d2:') !== -1)
                            {
                                //Since the value couldnt be looked up directly, and contains a curly brace or a dhis function call,
                                //the expression was more complex than replacing a single variable value.
                                //Now we will have to make a thorough replacement and separate evaluation to find the correct value:
                                $rootScope.ruleeffects[ruleEffectKey][action.id].data = replaceVariables(action.data, variablesHash);
                                //In a scenario where the data contains a complex expression, evaluate the expression to compile(calculate) the result:
                                $rootScope.ruleeffects[ruleEffectKey][action.id].data = runExpression($rootScope.ruleeffects[ruleEffectKey][action.id].data, action.data, "action:" + action.id, flag, variablesHash);
                            }

                            if(oldData !== $rootScope.ruleeffects[ruleEffectKey][action.id].data) {
                                updatedEffectsExits = true;
                            }
                        }

                        //Update the rule effectiveness if it changed in this evaluation;
                        if($rootScope.ruleeffects[ruleEffectKey][action.id].ineffect !== ruleEffective)
                        {
                            //There is a change in the rule outcome, we need to update the effect object.
                            updatedEffectsExits = true;
                            $rootScope.ruleeffects[ruleEffectKey][action.id].ineffect = ruleEffective;
                        }

                        //In case the rule is of type CREATEEVENT, run event creation:
                        if($rootScope.ruleeffects[ruleEffectKey][action.id].action === "CREATEEVENT" && $rootScope.ruleeffects[ruleEffectKey][action.id].ineffect){
                            if(evs && evs.byStage){
                                if($rootScope.ruleeffects[ruleEffectKey][action.id].programStage) {
                                    var createdNow = performCreateEventAction($rootScope.ruleeffects[ruleEffectKey][action.id], selectedEntity, selectedEnrollment, evs.byStage[$rootScope.ruleeffects[ruleEffectKey][action.id].programStage.id]);
                                    eventsCreated += createdNow;
                                } else {
                                    $log.warn("No programstage defined for CREATEEVENT action: " + action.id);
                                }
                            } else {
                                $log.warn("Events to evaluate for CREATEEVENT action: " + action.id + ". Could it have been triggered at the wrong time or during registration?");
                            }
                        }
                        //In case the rule is of type "assign variable" and the rule is effective,
                        //the variable data result needs to be applied to the correct variable:
                        else if($rootScope.ruleeffects[ruleEffectKey][action.id].action === "ASSIGN" && $rootScope.ruleeffects[ruleEffectKey][action.id].ineffect){
                            //from earlier evaluation, the data portion of the ruleeffect now contains the value of the variable to be assigned.
                            //the content portion of the ruleeffect defines the name for the variable, when the qualidisers are removed:
                            var variabletoassign = $rootScope.ruleeffects[ruleEffectKey][action.id].content ?
                                $rootScope.ruleeffects[ruleEffectKey][action.id].content.replace("#{","").replace("}","") : null;

                            if(variabletoassign || !angular.isDefined(variablesHash[variabletoassign])){
                                //If a variable is mentioned in the content of the rule, but does not exist in the variables hash, show a warning:
                                $log.warn("Variable " + variabletoassign + " was not defined.");
                            }

                            if(variablesHash[variabletoassign] &&
                                variablesHash[variabletoassign].variableValue !== updatedValue){
                                //If the variable was actually updated, we assume that there is an updated ruleeffect somewhere:
                                updatedEffectsExits = true;
                                
                                var updatedValue = $rootScope.ruleeffects[ruleEffectKey][action.id].data;
                                
                                var valueType = determineValueType(updatedValue);
                                
                                if($rootScope.ruleeffects[ruleEffectKey][action.id].dataElement) {
                                    updatedValue = VariableService.getDataElementValueOrCode(variablesHash[variabletoassign].useCodeForOptionSet, executingEvent, $rootScope.ruleeffects[ruleEffectKey][action.id].dataElement.id, allDataElements, optionSets);
                                }
                                updatedValue = VariableService.processValue(updatedValue, valueType);

                                variablesHash[variabletoassign] = {
                                    variableValue:updatedValue,
                                    variableType:valueType,
                                    hasValue:true,
                                    variableEventDate:'',
                                    variablePrefix:'#',
                                    allValues:[updatedValue]
                                };
                            }
                        }
                    });
                });

                //Broadcast rules finished if there was any actual changes to the event.
                if(updatedEffectsExits){
                    $rootScope.$broadcast("ruleeffectsupdated", { event: ruleEffectKey, eventsCreated:eventsCreated });
                }
            }

            return true;
        }
    };
    
    var internalProcessEvent = function(event) {
        event.eventDate = DateUtils.formatFromApiToUser(event.eventDate);
        
        angular.forEach(event.dataValues, function(dataValue) {
            event[dataValue.dataElement] = dataValue.value;
        });
        return event;
    };

    var internalGetOrLoadScope = function(currentEvent,programStageId,orgUnitId) {        
        if(crossEventRulesExist) {
            //If crossEventRulesExist, we need to get a scope that contains more than the current event.
            if(lastEventId !== currentEvent.event 
                    || lastEventDate !== currentEvent.eventDate 
                    || !eventScopeExceptCurrent) {
                //The scope might need updates, as the parameters of the event has changed

                lastEventId = currentEvent.event;
                lastEventDate = currentEvent.eventDate;

                
                var pager = {pageSize: NUMBER_OF_EVENTS_IN_SCOPE};
                var ordering = {field:"eventDate",direction:"desc"};
                var filterings = [{field:"programStage", value:programStageId}];
                return DHIS2EventFactory.getByFilters(orgUnitId, pager, true, ordering, filterings).then(function(newestEvents) {
                    filterings.push({field:"dueDate",value:lastEventDate});
                    return DHIS2EventFactory.getByFilters(orgUnitId, pager, true, ordering, filterings).then(function(previousEvents) {
                        eventScopeExceptCurrent = [];
                        var eventIdDictionary = {};
                        var allEventsWithPossibleDuplicates = newestEvents.events.concat(previousEvents.events);
                        angular.forEach(allEventsWithPossibleDuplicates, function(eventInScope) {
                            if(currentEvent.event !== eventInScope.event 
                                    && !eventIdDictionary[eventInScope.event]) {
                                //Add event and update dictionary to avoid duplicates:
                                eventScopeExceptCurrent.push(internalProcessEvent(eventInScope));
                                eventIdDictionary[eventInScope.event] = true;
                            }
                        });

                        //make a sorted list of all events to pass to rules execution service:
                        var allEventsInScope = eventScopeExceptCurrent.concat([currentEvent]);
                        allEventsInScope = orderByFilter(allEventsInScope, '-eventDate').reverse();
                        var byStage = {};
                        byStage[currentEvent.programStage] = allEventsInScope;
                        return {all: allEventsInScope, byStage:byStage};
                    });
                });   
            }
            else
            {
                //make a sorted list of all events to pass to rules execution service:
                var allEvents = eventScopeExceptCurrent.concat([currentEvent]);
                allEvents = orderByFilter(allEvents, '-eventDate').reverse();
                var byStage = {};
                byStage[currentEvent.programStage] = allEvents;
                return $q.when({all: allEvents, byStage:byStage});
            }
        }
        else
        {
            //return a scope containing only the current event
            var byStage = {};
            byStage[currentEvent.programStage] = [currentEvent];
            return $q.when({all: [currentEvent], byStage:byStage});
        }
    };
    var internalGetOrLoadRules = function(programId) {
        //If no rules is stored in memory, or this service is being called in the context of a different program, get the rules again:
        if(allProgramRules === false || lastProgramId !== programId)
        {
            return RulesFactory.loadRules(programId).then(function(rules){                    
                allProgramRules = rules;
                lastProgramId = programId;

                //Check if any of the rules is using any source type thar requires a bigger event scope
                crossEventRulesExist = false;
                if(rules.programVariables && rules.programVariables.length) {
                    for(var i = 0; i < rules.programVariables.length; i ++) {
                        if( rules.programVariables[i].programRuleVariableSourceType ===
                                "DATAELEMENT_NEWEST_EVENT_PROGRAM" ||
                            rules.programVariables[i].programRuleVariableSourceType ===
                                "DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE" ||
                            rules.programVariables[i].programRuleVariableSourceType ===
                                "DATAELEMENT_PREVIOUS_EVENT")
                        {
                            crossEventRulesExist = true;
                        }
                    }
                }

                return rules;
            });  
        }
        else
        {
            return $q.when(allProgramRules);
        }
    };
    return {
        executeRules: function(allProgramRules, executingEvent, evs, allDataElements, selectedEntity, selectedEnrollment, optionSets, flags) {
            internalExecuteRules(allProgramRules, executingEvent, evs, allDataElements, selectedEntity, selectedEnrollment, optionSets, flags);
        },
        loadAndExecuteRulesScope: function(currentEvent, programId, programStageId, programStageDataElements, optionSets, orgUnitId, flags){
            internalGetOrLoadRules(programId).then(function(rules) {
                internalGetOrLoadScope(currentEvent,programStageId,orgUnitId).then(function(scope) {
                    internalExecuteRules(rules, currentEvent, scope, programStageDataElements, null, null, optionSets, flags);
                });
            });
        },
        processRuleEffectsForTrackedEntityAttributes: function(context, currentTei, teiOriginalValues, attributesById, optionSets ) {
            var hiddenFields = {};
            var assignedFields = {};
            var hiddenSections = {};
            var warningMessages = [];
            
            angular.forEach($rootScope.ruleeffects[context], function (effect) {
                if (effect.ineffect) {
                    if (effect.action === "HIDEFIELD" && effect.trackedEntityAttribute) {
                        if (currentTei[effect.trackedEntityAttribute.id]) {
                            //If a field is going to be hidden, but contains a value, we need to take action;
                            if (effect.content) {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert(effect.content);
                            }
                            else {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert(attributesById[effect.trackedEntityAttribute.id].displayName + " was blanked out and hidden by your last action");
                            }

                            //Blank out the value:
                            currentTei[effect.trackedEntityAttribute.id] = "";
                        }

                        hiddenFields[effect.trackedEntityAttribute.id] = true;
                    } else if (effect.action === "SHOWERROR" && effect.trackedEntityAttribute) {
                        if(effect.ineffect) {
                            var headerText =  $translate.instant('validation_error');
                            var bodyText = effect.content + (effect.data ? effect.data : "");

                            NotificationService.showNotifcationDialog(headerText, bodyText);
                            if( effect.trackedEntityAttribute ) {
                                currentTei[effect.trackedEntityAttribute.id] = teiOriginalValues[effect.trackedEntityAttribute.id];
                            }
                        }
                    } else if (effect.action === "SHOWWARNING" && effect.trackedEntityAttribute) {
                        if(effect.ineffect) {
                            var message = effect.content + (angular.isDefined(effect.data) ? effect.data : "");
                            warningMessages.push(message);

                            if( effect.trackedEntityAttribute ) {
                                warningMessages[effect.trackedEntityAttribute.id] = message;
                            }
                        }
                    }
                    else if (effect.action === "ASSIGN" && effect.trackedEntityAttribute) {
                        var processedValue = $filter('trimquotes')(effect.data);

                        if(attributesById[effect.trackedEntityAttribute.id]
                                && attributesById[effect.trackedEntityAttribute.id].optionSet) {
                            processedValue = OptionSetService.getName(
                                    optionSets[attributesById[effect.trackedEntityAttribute.id].optionSet.id].options, processedValue)
                        }

                        processedValue = processedValue === "true" ? true : processedValue;
                        processedValue = processedValue === "false" ? false : processedValue;

                        //For "ASSIGN" actions where we have a dataelement, we save the calculated value to the dataelement:
                        currentTei[effect.trackedEntityAttribute.id] = processedValue;
                        assignedFields[effect.trackedEntityAttribute.id] = true;
                    }
                }
            });
            return {currentTei: currentTei, hiddenFields: hiddenFields, hiddenSections: hiddenSections, warningMessages: warningMessages, assignedFields: assignedFields};
        },
        processRuleEffectsForEvent: function(eventId, currentEvent, currentEventOriginalValues, prStDes, optionSets ) {
            var hiddenFields = {};
            var assignedFields = {};
            var hiddenSections = {};
            var warningMessages = [];
            
            angular.forEach($rootScope.ruleeffects[eventId], function (effect) {
                if (effect.ineffect) {
                    if (effect.action === "HIDEFIELD" && effect.dataElement) {
                        if(currentEvent[effect.dataElement.id]) {
                            //If a field is going to be hidden, but contains a value, we need to take action;
                            if(effect.content) {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert(effect.content);
                            }
                            else {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert(prStDes[effect.dataElement.id].dataElement.formName + "Was blanked out and hidden by your last action");
                            }

                        }
                        
                        hiddenFields[effect.dataElement.id] = true;
                    }
                    else if(effect.action === "HIDESECTION") {
                        if(effect.programStageSection){
                            hiddenSections[effect.programStageSection] = effect.programStageSection;
                        }
                    }
                    else if(effect.action === "SHOWERROR" && effect.dataElement.id){
                        var headerTxt =  $translate.instant('validation_error');
                        var bodyTxt = effect.content + (effect.data ? effect.data : "");
                        NotificationService.showNotifcationDialog(headerTxt, bodyTxt);

                        currentEvent[effect.dataElement.id] = currentEventOriginalValues[effect.dataElement.id];
                    }
                    else if(effect.action === "SHOWWARNING"){
                        warningMessages.push(effect.content + (effect.data ? effect.data : ""));
                    }
                    else if (effect.action === "ASSIGN" && effect.dataElement) {
                        var processedValue = $filter('trimquotes')(effect.data);

                        if(prStDes[effect.dataElement.id] 
                                && prStDes[effect.dataElement.id].dataElement.optionSet) {
                            processedValue = OptionSetService.getName(
                                    optionSets[prStDes[effect.dataElement.id].dataElement.optionSet.id].options, processedValue)
                        }

                        processedValue = processedValue === "true" ? true : processedValue;
                        processedValue = processedValue === "false" ? false : processedValue;

                        currentEvent[effect.dataElement.id] = processedValue;
                        assignedFields[effect.dataElement.id] = true;
                    }
                }
            });
        
            return {currentEvent: currentEvent, hiddenFields: hiddenFields, hiddenSections: hiddenSections, warningMessages: warningMessages, assignedFields: assignedFields};
        },
        processRuleEffectAttribute: function(context, selectedTei, tei, currentEvent, currentEventOriginialValue, affectedEvent, attributesById, prStDes, hiddenFields, hiddenSections, warningMessages, assignedFields, optionSets){
            //Function used from registration controller to process effects for the tracked entity instance and for the events in the same operation
            var teiAttributesEffects = this.processRuleEffectsForTrackedEntityAttributes(context, selectedTei, tei, attributesById, optionSets );
            teiAttributesEffects.selectedTei = teiAttributesEffects.currentTei;
            
            if(context === "SINGLE_EVENT" && currentEvent && prStDes ) {
                var eventEffects = this.processRuleEffectsForEvent("SINGLE_EVENT", currentEvent, currentEventOriginialValue, prStDes, optionSets);
                teiAttributesEffects.warningMessages = angular.extend(teiAttributesEffects.warningMessages,eventEffects.warningMessages);
                teiAttributesEffects.hiddenFields = angular.extend(teiAttributesEffects.hiddenFields,eventEffects.hiddenFields);
                teiAttributesEffects.hiddenSections = angular.extend(teiAttributesEffects.hiddenSections,eventEffects.hiddenSections);
                teiAttributesEffects.assignedFields = angular.extend(teiAttributesEffects.assignedFields,eventEffects.assignedFields);
                teiAttributesEffects.currentEvent = eventEffects.currentEvent;
            }
            
            return teiAttributesEffects;
        }
    };
})

/* service for dealing with events */
.service('DHIS2EventService', function(){
    return {
        //for simplicity of grid display, events were changed from
        //event.datavalues = [{dataElement: dataElement, value: value}] to
        //event[dataElement] = value
        //now they are changed back for the purpose of storage.
        reconstructEvent: function(event, programStageDataElements){
            var e = {};

            e.event         = event.event;
            e.status        = event.status;
            e.program       = event.program;
            e.programStage  = event.programStage;
            e.orgUnit       = event.orgUnit;
            e.eventDate     = event.eventDate;

            var dvs = [];
            angular.forEach(programStageDataElements, function(prStDe){
                if(event.hasOwnProperty(prStDe.dataElement.id)){
                    dvs.push({dataElement: prStDe.dataElement.id, value: event[prStDe.dataElement.id]});
                }
            });

            e.dataValues = dvs;

            if(event.coordinate){
                e.coordinate = {latitude: event.coordinate.latitude ? event.coordinate.latitude : '',
                    longitude: event.coordinate.longitude ? event.coordinate.longitude : ''};
            }

            return e;
        },
        refreshList: function(eventList, currentEvent){
            if(!eventList || !eventList.length){
                return;
            }
            var continueLoop = true;
            for(var i=0; i< eventList.length && continueLoop; i++){
                if(eventList[i].event === currentEvent.event ){
                    eventList[i] = currentEvent;
                    continueLoop = false;
                }
            }
            return eventList;
        }
    };
})

/* current selections */
.service('CurrentSelection', function(){
    this.currentSelection = {};
    this.relationshipInfo = {};
    this.optionSets = null;
    this.attributesById = null;
    this.ouLevels = null;
    this.sortedTeiIds = [];
    this.selectedTeiEvents = null;
    this.relationshipOwner = {};
    this.selectedTeiEvents = [];
    this.fileNames = [];
    this.location = null;
    this.advancedSearchOptions = null;
	this.trackedEntities = null;

    this.set = function(currentSelection){
        this.currentSelection = currentSelection;
    };
    this.get = function(){
        return this.currentSelection;
    };

    this.setRelationshipInfo = function(relationshipInfo){
        this.relationshipInfo = relationshipInfo;
    };
    this.getRelationshipInfo = function(){
        return this.relationshipInfo;
    };

    this.setOptionSets = function(optionSets){
        this.optionSets = optionSets;
    };
    this.getOptionSets = function(){
        return this.optionSets;
    };

    this.setAttributesById = function(attributesById){
        this.attributesById = attributesById;
    };
    this.getAttributesById = function(){
        return this.attributesById;
    };

    this.setOuLevels = function(ouLevels){
        this.ouLevels = ouLevels;
    };
    this.getOuLevels = function(){
        return this.ouLevels;
    };

    this.setSortedTeiIds = function(sortedTeiIds){
        this.sortedTeiIds = sortedTeiIds;
    };
    this.getSortedTeiIds = function(){
        return this.sortedTeiIds;
    };

    this.setSelectedTeiEvents = function(selectedTeiEvents){
        this.selectedTeiEvents = selectedTeiEvents;
    };
    this.getSelectedTeiEvents = function(){
        return this.selectedTeiEvents;
    };

    this.setRelationshipOwner = function(relationshipOwner){
        this.relationshipOwner = relationshipOwner;
    };
    this.getRelationshipOwner = function(){
        return this.relationshipOwner;
    };

    this.setFileNames = function(fileNames){
        this.fileNames = fileNames;
    };
    this.getFileNames = function(){
        return this.fileNames;
    };
    
    this.setLocation = function(location){
        this.location = location;
    };
    this.getLocation = function(){
        return this.location;
    };
    
    this.setAdvancedSearchOptions = function (searchOptions) {
        this.advancedSearchOptions = searchOptions;
    };
    this.getAdvancedSearchOptions = function () {
        return this.advancedSearchOptions;
    };

    this.setTrackedEntities = function (trackedEntities) {
        this.trackedEntities = trackedEntities;
    };
    this.getTrackedEntities = function () {
        return this.trackedEntities;
    };

    this.setSortColumn = function (sortColumn) {
        if (this.advancedSearchOptions) {
            this.advancedSearchOptions.sortColumn = sortColumn;
        }
    };

    this.setColumnReverse = function (reverseSortStatus) {
        if (this.advancedSearchOptions) {
            this.advancedSearchOptions.reverse = reverseSortStatus;
        }
    };

    this.setGridColumns = function (gridColumns) {
        if (this.advancedSearchOptions) {
            this.advancedSearchOptions.gridColumns = gridColumns;
        }
    }
})

.service('AuditHistoryDataService', function( $http, $translate, NotificationService, DHIS2URL ) {
    this.getAuditHistoryData = function(dataId, dataType ) {
        var url="";
        if (dataType === "attribute") {
            url="/audits/trackedEntityAttributeValue?tei="+dataId+"&skipPaging=true";
            
        } else {
            url="/audits/trackedEntityDataValue?psi="+dataId+"&skipPaging=true";
        }

        var promise = $http.get(DHIS2URL + url).then(function( response ) {
            return response.data;
        }, function( response ) {
            if( response && response.data && response.data.status === 'ERROR' ) {
                var headerText = response.data.status;
                var bodyText = response.data.message ? response.data.message : $translate.instant('unable_to_fetch_data_from_server');
                NotificationService.showNotifcationDialog(headerText, bodyText);
            }
        });
        return promise;
    }
})

/* Factory for fetching OrgUnit */
.factory('OrgUnitFactory', function($http, DHIS2URL, $q, SessionStorageService) {
    var orgUnit, orgUnitPromise, rootOrgUnitPromise,orgUnitTreePromise;
    return {
        getChildren: function(uid){
            if( orgUnit !== uid ){
                orgUnitPromise = $http.get( DHIS2URL + '/organisationUnits/'+ uid + '.json?fields=id,path,children[id,displayName,level,children[id]]&paging=false' ).then(function(response){
                    orgUnit = uid;
                    return response.data;
                });
            }
            return orgUnitPromise;
        },
        get: function(uid){
            if( orgUnit !== uid ){
                orgUnitPromise = $http.get( DHIS2URL + '/organisationUnits/'+ uid + '.json?fields=id,displayName,level,path' ).then(function(response){
                    orgUnit = uid;
                    return response.data;
                });
            }
            return orgUnitPromise;
        },
        getViewTreeRoot: function(){
            if(!rootOrgUnitPromise){
                var url = DHIS2URL + '/me.json?fields=organisationUnits[id,displayName,level,path,children[id,displayName,level,children[id]]],dataViewOrganisationUnits[id,displayName,level,path,children[id,displayName,level,children[id]]]&paging=false';
                rootOrgUnitPromise = $http.get( url ).then(function(response){
                    response.data.organisationUnits = response.data.dataViewOrganisationUnits && response.data.dataViewOrganisationUnits.length > 0 ? response.data.dataViewOrganisationUnits : response.data.organisationUnits;
                    delete response.data.dataViewOrganisationUnits;
                    return response.data;
                });
            }
            return rootOrgUnitPromise;
        },
        getSearchTreeRoot: function(){
            if(!rootOrgUnitPromise){
                var url = DHIS2URL + '/me.json?fields=organisationUnits[id,displayName,level,path,children[id,displayName,level,children[id]]],teiSearchOrganisationUnits[id,displayName,level,path,children[id,displayName,level,children[id]]]&paging=false';
                rootOrgUnitPromise = $http.get( url ).then(function(response){
                    response.data.organisationUnits = response.data.teiSearchOrganisationUnits && response.data.teiSearchOrganisationUnits.length > 0 ? response.data.teiSearchOrganisationUnits : response.data.organisationUnits;
                    delete response.data.teiSearchOrganisationUnits;
                    return response.data;
                });
            }
            return rootOrgUnitPromise;
        },
        getOrgUnits: function(uid,fieldUrl){
            var url = DHIS2URL + '/organisationUnits.json?filter=id:eq:'+uid+'&'+fieldUrl+'&paging=false';
            orgUnitTreePromise = $http.get(url).then(function(response){
                return response.data;
            });
            return orgUnitTreePromise;
        },
        getOrgUnit: function(uid) {
            var def = $q.defer();
            var selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
            if (selectedOrgUnit) {
                def.resolve(selectedOrgUnit);
            } else if (uid) {
                this.get(uid).then(function (response) {
                    if (response.organisationUnits && response.organisationUnits[0]) {
                        def.resolve({
                            displayName: response.organisationUnits[0].displayName,
                            id: response.organisationUnits[0].id
                        });
                    } else if (response && response.id) {
                        def.resolve(response);
                    } else {
                        def.resolve(null);
                    }
                });
            } else {
                def.resolve(null);
            }
            return def.promise;
        }
    };
});