/* global trackerCapture, angular */

trackerCapture.controller('RegistrationController', 
        function($rootScope,
                $scope,
                $location,
                $timeout,
                $modal,
                $translate,
                orderByFilter,
                AttributesFactory,
                DHIS2EventFactory,
                TEService,
                CustomFormService,
                EnrollmentService,
                DialogService,
                CurrentSelection,
                MetaDataFactory,
                EventUtils,
                RegistrationService,
                DateUtils,
                SessionStorageService,
                TEIGridService,
                TrackerRulesFactory,
                TrackerRulesExecutionService,
                TCStorageService,
                ModalService) {
    
    $scope.maxOptionSize = 30;
    $scope.today = DateUtils.getToday();
    $scope.trackedEntityForm = null;
    $scope.customRegistrationForm = null;    
    $scope.selectedTei = {};
    $scope.tei = {};    
    $scope.warningMessages = [];
    $scope.hiddenFields = [];    
    $scope.assignedFields = [];
    $scope.errorMessages = {};
    $scope.hiddenSections = [];
    $scope.currentEvent = null;
    $scope.prStDes = null;
    $scope.registrationAndDataEntry = false;
    
    
    $scope.helpTexts = {};
    $scope.registrationMode = 'REGISTRATION';
    $scope.currentEvent = {};
    var flag = {debug: true, verbose: false};
    $rootScope.ruleeffects = {};

    $scope.attributesById = CurrentSelection.getAttributesById();
    if(!$scope.attributesById){
        $scope.attributesById = [];
        AttributesFactory.getAll().then(function(atts){
            angular.forEach(atts, function(att){
                $scope.attributesById[att.id] = att;
            });
            
            CurrentSelection.setAttributesById($scope.attributesById);
        });
    }
    
    //get ouLevels
    $scope.ouLevels = CurrentSelection.getOuLevels();
    if(!$scope.ouLevels){
        TCStorageService.currentStore.open().done(function(){
            TCStorageService.currentStore.getAll('ouLevels').done(function(response){
                var ouLevels = angular.isObject(response) ? orderByFilter(response, '-level').reverse() : [];
                CurrentSelection.setOuLevels(orderByFilter(ouLevels, '-level').reverse());
            });
        });
    }
    
    $scope.optionSets = CurrentSelection.getOptionSets();        
    if(!$scope.optionSets){
        $scope.optionSets = [];
        MetaDataFactory.getAll('optionSets').then(function(optionSets){
            angular.forEach(optionSets, function(optionSet){                        
                $scope.optionSets[optionSet.id] = optionSet;
            });
            CurrentSelection.setOptionSets($scope.optionSets);
        });
    }
    
    $scope.dataElementTranslations = CurrentSelection.getDataElementTranslations();        
    if(!$scope.dataElementTranslations){
        $scope.dataElementTranslations = [];
        MetaDataFactory.getAll('dataElements').then(function(des){
            angular.forEach(des, function(de){  
                $scope.dataElementTranslations[de.id] = de;
            });
            CurrentSelection.setDataElementTranslations($scope.dataElementTranslations);
        });
    }
    
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
    $scope.selectedEnrollment = {enrollmentDate: $scope.today, incidentDate: $scope.today, orgUnitName: $scope.selectedOrgUnit.displayName};   
            
    $scope.trackedEntities = {available: []};
    TEService.getAll().then(function(entities){
        $scope.trackedEntities.available = entities;   
        $scope.trackedEntities.selected = $scope.trackedEntities.available[0];
    });

    var getProgramRules = function(){
        $scope.trackedEntityForm = null;
        $scope.customRegistrationForm = null;        
        $scope.allProgramRules = {constants: [], programIndicators: {}, programValidations: [], programVariables: [], programRules: []};
        if( angular.isObject($scope.selectedProgram) && $scope.selectedProgram.id ){
            TrackerRulesFactory.getRules($scope.selectedProgram.id).then(function(rules){                    
                $scope.allProgramRules = rules;
            });
        }        
    };
    
    //watch for selection of program
    $scope.$watch('selectedProgram', function(newValue, oldValue) {
        if( newValue !== oldValue )
        {
            getProgramRules();
            
            if($scope.registrationMode === 'REGISTRATION'){
                $scope.getAttributes($scope.registrationMode);
            }
        }                
    }); 
    
    //listen to modes of registration
    $scope.$on('registrationWidget', function(event, args){
        $scope.selectedTei = {};
        $scope.tei = {};
        $scope.registrationMode = args.registrationMode;
        
        if($scope.registrationMode !== 'REGISTRATION'){
            $scope.selectedTei = args.selectedTei;            
            $scope.tei = angular.copy(args.selectedTei);             
        }
        
        $scope.teiOriginal = angular.copy($scope.tei); 
        
        if($scope.registrationMode === 'PROFILE'){
            $scope.selectedEnrollment = args.enrollment;
        }

        $scope.getAttributes($scope.registrationMode);
        
        if($scope.selectedProgram && $scope.selectedProgram.id){
            getProgramRules();
        }
    });
        
    $scope.getAttributes = function(_mode){        
        var mode = _mode ? _mode : 'ENROLLMENT';
        $scope.customRegistrationFormExists = false;
        $scope.customDataEntryForm = null;
        $scope.schedulingEnabled = true;
        AttributesFactory.getByProgram($scope.selectedProgram).then(function(atts){            
            $scope.attributes = TEIGridService.generateGridColumns(atts, null,false).columns;            
            if($scope.selectedProgram && $scope.selectedProgram.id ){                
                if( $scope.selectedProgram.dataEntryForm && $scope.selectedProgram.dataEntryForm.htmlCode){
                    $scope.customRegistrationFormExists = true;
                    $scope.trackedEntityForm = $scope.selectedProgram.dataEntryForm;  
                    $scope.trackedEntityForm.attributes = $scope.attributes;
                    $scope.trackedEntityForm.selectIncidentDatesInFuture = $scope.selectedProgram.selectIncidentDatesInFuture;
                    $scope.trackedEntityForm.selectEnrollmentDatesInFuture = $scope.selectedProgram.selectEnrollmentDatesInFuture;
                    $scope.trackedEntityForm.displayIncidentDate = $scope.selectedProgram.displayIncidentDate;
                    $scope.customRegistrationForm = CustomFormService.getForTrackedEntity($scope.trackedEntityForm, mode);
                }
                
                if( $scope.selectedProgram.programStages && $scope.selectedProgram.programStages[0] && $scope.selectedProgram.useFirstStageDuringRegistration && $scope.registrationMode === 'REGISTRATION'){
                    $scope.registrationAndDataEntry = true;
                    $scope.prStDes = [];
                    $scope.currentStage = $scope.selectedProgram.programStages[0];
                    $scope.currentEvent.event = 'SINGLE_EVENT';
                    $scope.currentEvent.providedElsewhere = {};
                    $scope.currentEvent.orgUnit = $scope.selectedOrgUnit.id;
                    $scope.currentEvent.program = $scope.selectedProgram.id;
                    $scope.currentEvent.programStage = $scope.currentStage.id;
                    $scope.currentEvent.enrollmentStatus = $scope.currentEvent.status = 'ACTIVE';
                    $scope.currentEvent.excecutionDateLabel = $scope.currentStage.excecutionDateLabel;     
                    $rootScope.ruleeffects[$scope.currentEvent.event] = {};
                    $scope.selectedEnrollment.status = 'ACTIVE';
                    angular.forEach($scope.currentStage.programStageDataElements, function (prStDe) {
                        var tx = $scope.dataElementTranslations[prStDe.dataElement.id];
                        prStDe.dataElement.displayFormName = tx && tx.displayFormName && tx.displayFormName !== "" ? tx.displayFormName : tx && tx.displayName ? tx.displayName : prStDe.dataElement.displayName;                        
                        $scope.prStDes[prStDe.dataElement.id] = prStDe;
                        if(prStDe.allowProvidedElsewhere){
                            $scope.allowProvidedElsewhereExists[$scope.currentStage.id] = true;
                        }
                    });
                    
                    $scope.customDataEntryForm = CustomFormService.getForProgramStage($scope.currentStage, $scope.prStDes);
                }                
            }
        });
    }; 
    
    var goToDashboard = function(destination, teiId){
        //reset form
        $scope.selectedTei = {};
        $scope.selectedEnrollment = {enrollmentDate: $scope.today, incidentDate: $scope.today, orgUnitName: $scope.selectedOrgUnit.displayName};
        $scope.outerForm.submitted = false;
        $scope.outerForm.$setPristine();

        if(destination === 'DASHBOARD') {
            $location.path('/dashboard').search({tei: teiId, program: $scope.selectedProgram ? $scope.selectedProgram.id: null});
        }
        else if (destination === 'SELF'){
            //notify user
            var dialogOptions = {
                    headerText: 'success',
                    bodyText: 'registration_complete'
                };
            DialogService.showDialog({}, dialogOptions);
            $scope.selectedTei = {};
            $scope.tei = {};
        }
    };
    
    var reloadProfileWidget = function(){
        var selections = CurrentSelection.get();
        CurrentSelection.set({tei: $scope.selectedTei, te: $scope.selectedTei.trackedEntity, prs: selections.prs, pr: $scope.selectedProgram, prNames: selections.prNames, prStNames: selections.prStNames, enrollments: selections.enrollments, selectedEnrollment: $scope.selectedEnrollment, optionSets: selections.optionSets});        
        $timeout(function() { 
            $rootScope.$broadcast('profileWidget', {});            
        }, 200);
    };
    
    var notifyRegistrtaionCompletion = function(destination, teiId){
        if($scope.registrationMode === 'ENROLLMENT'){
            broadcastTeiEnrolled();
        }
        else{
            goToDashboard( destination ? destination : 'DASHBOARD', teiId );  
        }
    };
    
    var performRegistration = function(destination){
        RegistrationService.registerOrUpdate($scope.tei, $scope.optionSets, $scope.attributesById).then(function(registrationResponse){
            var reg = registrationResponse.response ? registrationResponse.response : {};            
            if(reg.reference && reg.status === 'SUCCESS'){                
                $scope.tei.trackedEntityInstance = reg.reference;
                
                if( $scope.registrationMode === 'PROFILE' ){
                    reloadProfileWidget();
                    $rootScope.$broadcast('teiupdated', {});          
                }
                else{
                    if( $scope.selectedProgram ){
                        
                        //enroll TEI
                        var enrollment = {};
                        enrollment.trackedEntityInstance = $scope.tei.trackedEntityInstance;
                        enrollment.program = $scope.selectedProgram.id;
                        enrollment.status = 'ACTIVE';
                        enrollment.orgUnit = $scope.selectedOrgUnit.id;
                        enrollment.enrollmentDate = $scope.selectedEnrollment.enrollmentDate;
                        enrollment.incidentDate = $scope.selectedEnrollment.incidentDate === '' ? $scope.selectedEnrollment.enrollmentDate : $scope.selectedEnrollment.incidentDate;

                        EnrollmentService.enroll(enrollment).then(function(enrollmentResponse){
                            var en = enrollmentResponse.response && enrollmentResponse.response.importSummaries && enrollmentResponse.response.importSummaries[0] ? enrollmentResponse.response.importSummaries[0] : {};
                            if(en.reference && en.status === 'SUCCESS'){                                
                                enrollment.enrollment = en.reference;
                                $scope.selectedEnrollment = enrollment;
                                var availableEvent = $scope.currentEvent && $scope.currentEvent.event ? $scope.currentEvent : null;
                                var dhis2Events = EventUtils.autoGenerateEvents($scope.tei.trackedEntityInstance, $scope.selectedProgram, $scope.selectedOrgUnit, enrollment, availableEvent);
                                if(dhis2Events.events.length > 0){
                                    DHIS2EventFactory.create(dhis2Events).then(function(){
                                        notifyRegistrtaionCompletion(destination, $scope.tei.trackedEntityInstance);
                                    });
                                }else{
                                    notifyRegistrtaionCompletion(destination, $scope.tei.trackedEntityInstance);
                                } 
                            }
                            else{
                                //enrollment has failed
                                var dialogOptions = {
                                        headerText: 'enrollment_error',
                                        bodyText: enrollmentResponse.message
                                    };
                                DialogService.showDialog({}, dialogOptions);
                                return;                                                            
                            }
                        });
                    }
                    else{
                       notifyRegistrtaionCompletion(destination, $scope.tei.trackedEntityInstance); 
                    }
                }                
            }
            else{//update/registration has failed
                var dialogOptions = {
                        headerText: $scope.tei && $scope.tei.trackedEntityInstance ? 'update_error' : 'registration_error',
                        bodyText: registrationResponse.message
                    };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
        });
        
    };
    
    function broadcastTeiEnrolled(){
        $rootScope.$broadcast('teienrolled', {});
    }
    
    $scope.registerEntity = function(destination){
        //check for form validity
        $scope.outerForm.submitted = true;        
        if( $scope.outerForm.$invalid ){
            return false;
        }           
        
        if( $scope.registrationAndDataEntry ){
            $scope.outerDataEntryForm.submitted = true;        
            if( $scope.outerDataEntryForm.$invalid ){
                return false;
            }
        }         
        
        //form is valid, continue the registration
        //get selected entity        
        if(!$scope.selectedTei.trackedEntityInstance){
            $scope.selectedTei.trackedEntity = $scope.tei.trackedEntity = $scope.selectedProgram && $scope.selectedProgram.trackedEntity && $scope.selectedProgram.trackedEntity.id ? $scope.selectedProgram.trackedEntity.id : $scope.trackedEntities.selected.id;
            $scope.selectedTei.orgUnit = $scope.tei.orgUnit = $scope.selectedOrgUnit.id;
            $scope.selectedTei.attributes = $scope.tei.attributes = [];
        }
        
        //get tei attributes and their values
        //but there could be a case where attributes are non-mandatory and
        //registration form comes empty, in this case enforce at least one value        
        var result = RegistrationService.processForm($scope.tei, $scope.selectedTei, $scope.teiOriginal, $scope.attributesById);
        $scope.formEmpty = result.formEmpty;
        $scope.tei = result.tei;
        
        if($scope.formEmpty){//registration form is empty
            var dialogOptions = {
                    headerText: 'error',
                    bodyText: $translate.instant('form_is_empty_fill_at_least_one')
                };
            DialogService.showDialog({}, dialogOptions);
            return;
        } 

        performRegistration(destination);
    }; 
    
    $scope.executeRules = function () {        
        //repopulate attributes with updated values
        $scope.selectedTei.attributes = [];        
        angular.forEach($scope.attributes, function(metaAttribute){
            var newAttributeInArray = {attribute:metaAttribute.id,
                code:metaAttribute.code,
                displayName:metaAttribute.displayName,
                type:metaAttribute.valueType,
                value: $scope.selectedTei[metaAttribute.id]
            };
            
           $scope.selectedTei.attributes.push(newAttributeInArray);
        });
        
        if($scope.selectedProgram && $scope.selectedProgram.id){
            var eventExists = $scope.currentEvent && $scope.currentEvent.event;
            TrackerRulesExecutionService.executeRules($scope.allProgramRules, eventExists ? $scope.currentEvent : 'registration', eventExists ? {all: [$scope.currentEvent], byStage: [$scope.currentStage.id][$scope.currentEvent]} : null, $scope.prStDes, $scope.selectedTei, $scope.selectedEnrollment, flag);
        }        
    };
    
    //check if field is hidden
    $scope.isHidden = function (id) {
        //In case the field contains a value, we cant hide it. 
        //If we hid a field with a value, it would falsely seem the user was aware that the value was entered in the UI.        
        return $scope.selectedTei[id] ? false : $scope.hiddenFields[id];
    };
    
    $scope.teiValueUpdated = function(tei, field){
        $scope.executeRules();
    };
    
    //listen for rule effect changes
    $scope.$on('ruleeffectsupdated', function(event, args){
        if(args.event === "registration") {
            $scope.warningMessages = [];
            $scope.hiddenFields = [];    
            $scope.assignedFields = [];
            $scope.errorMessages = {};
            $scope.hiddenSections = [];

            var effectResult = TrackerRulesExecutionService.processRuleEffectAttribute(args.event, $scope.selectedTei, $scope.tei, $scope.currentEvent, {}, $scope.currentEvent, $scope.attributesById, $scope.hiddenFields, $scope.hiddenSections, $scope.warningMessages, $scope.assignedFields);        
            $scope.selectedTei = effectResult.selectedTei;
            $scope.currentEvent = effectResult.currentEvent;
            $scope.hiddenFields = effectResult.hiddenFields;
            $scope.hiddenSections = effectResult.hiddenSections;
            $scope.assignedFields = effectResult.assignedFields;
            $scope.warningMessages = effectResult.warningMessages;
        }
    });

    $scope.interacted = function(field) {        
        var status = false;
        if(field){            
            status = $scope.outerForm.submitted || field.$dirty;
        }
        return status;        
    };
    
    $scope.getTrackerAssociate = function(selectedAttribute, existingAssociateUid){
        var modalInstance = $modal.open({
            templateUrl: 'components/teiadd/tei-add.html',
            controller: 'TEIAddController',
            windowClass: 'modal-full-window',
            resolve: {
                relationshipTypes: function () {
                    return $scope.relationshipTypes;
                },
                addingRelationship: function(){
                    return false;
                },
                selections: function () {
                    return CurrentSelection.get();
                },
                selectedTei: function(){
                    return $scope.selectedTei;
                },
                selectedAttribute: function(){
                    return selectedAttribute;
                },
                existingAssociateUid: function(){
                    return existingAssociateUid;
                },
                selectedProgram: function(){
                    return $scope.selectedProgram;
                },
                relatedProgramRelationship: function(){
                    return $scope.relatedProgramRelationship;
                }
            }
        });
        modalInstance.result.then(function (res) {
            if(res && res.id){
                $scope.selectedTei[selectedAttribute.id] = res.id;
            }
        });
    };
    
    $scope.cancelRegistrationWarning = function(cancelFunction){        
        var result = RegistrationService.processForm($scope.tei, $scope.selectedTei, $scope.teiOriginal, $scope.attributesById);
        if( result.formChanged){
            var modalOptions = {
                closeButtonText: 'no',
                actionButtonText: 'yes',
                headerText: 'cancel',
                bodyText: 'are_you_sure_to_cancel_registration'
            };

            ModalService.showModal({}, modalOptions).then(function(){
                $scope.outerForm.$setPristine();
                cancelFunction();
            });
        }
        else{
            $scope.outerForm.$setPristine();
            cancelFunction();
        }        
    };
    
    $scope.showAttributeMap = function(obj, id){
        var lat = "",
            lng = "";
        if(obj[id] && obj[id].length > 0){
            var coordinates = obj[id].split(",");
            lng = coordinates[0];
            lat = coordinates[1];
        }
        var modalInstance = $modal.open({
            templateUrl: '../dhis-web-commons/angular-forms/map.html',
            controller: 'MapController',
            windowClass: 'modal-full-window',
            resolve: {
                location: function () {
                    return {lat: lat, lng: lng};
                }
            }
        });

        modalInstance.result.then(function (location) {
            if(angular.isObject(location)){
                obj[id] = location.lng + ',' + location.lat;
            }
        }, function () {
        });
    };
    
    $scope.showDataElementMap = function(obj, id){
        var lat = "",
            lng = "";
        if(obj[id] && obj[id].length > 0){
            var coordinates = obj[id].split(",");
            lng = coordinates[0];
            lat = coordinates[1];
        }
        var modalInstance = $modal.open({
            templateUrl: '../dhis-web-commons/angular-forms/map.html',
            controller: 'MapController',
            windowClass: 'modal-full-window',
            resolve: {
                location: function () {
                    return {lat: lat, lng: lng};
                }
            }
        });

        modalInstance.result.then(function (location) {
            if(angular.isObject(location)){
                obj[id] = location.lng + ',' + location.lat;
            }
        }, function () {
        });
    };
    
    $scope.saveDatavalue = function () {        
        $scope.executeRules();
    };    
});
