/* global trackerCapture, angular */

trackerCapture.controller('RegistrationController', 
        function($rootScope,
                $scope,
                $location,
                $timeout,
                $modal,
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
                ModalService) {
    
    $scope.maxOptionSize = 30;
    
    $scope.today = DateUtils.getToday();
    $scope.trackedEntityForm = null;
    $scope.customForm = null;    
    $scope.selectedTei = {};
    $scope.tei = {};
    $scope.registrationMode = 'REGISTRATION';    
    $scope.hiddenFields = {};
    
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
    
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
    $scope.selectedEnrollment = {enrollmentDate: $scope.today, incidentDate: $scope.today, orgUnitName: $scope.selectedOrgUnit.name};   
            
    $scope.trackedEntities = {available: []};
    TEService.getAll().then(function(entities){
        $scope.trackedEntities.available = entities;   
        $scope.trackedEntities.selected = $scope.trackedEntities.available[0];
    });

    var getProgramRules = function(){
        $scope.trackedEntityForm = null;
        $scope.customForm = null;        
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
            //$scope.teiOriginal = angular.copy(args.selectedTei);  
        }
        
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
        AttributesFactory.getByProgram($scope.selectedProgram).then(function(atts){            
            $scope.attributes = TEIGridService.generateGridColumns(atts, null,false).columns;
            $scope.customFormExists = false;
            if($scope.selectedProgram && $scope.selectedProgram.id && $scope.selectedProgram.dataEntryForm && $scope.selectedProgram.dataEntryForm.htmlCode){
                $scope.customFormExists = true;
                $scope.trackedEntityForm = $scope.selectedProgram.dataEntryForm;  
                $scope.trackedEntityForm.attributes = $scope.attributes;
                $scope.trackedEntityForm.selectIncidentDatesInFuture = $scope.selectedProgram.selectIncidentDatesInFuture;
                $scope.trackedEntityForm.selectEnrollmentDatesInFuture = $scope.selectedProgram.selectEnrollmentDatesInFuture;
                $scope.trackedEntityForm.displayIncidentDate = $scope.selectedProgram.displayIncidentDate;
                $scope.customForm = CustomFormService.getForTrackedEntity($scope.trackedEntityForm, mode);
            }
        });
    }; 
    
    var goToDashboard = function(destination, teiId){
        //reset form
        $scope.selectedTei = {};
        $scope.selectedEnrollment = {enrollmentDate: $scope.today, incidentDate: $scope.today, orgUnitName: $scope.selectedOrgUnit.name};
        $scope.outerForm.submitted = false;
        $scope.outerForm.$setPristine();

        if(destination === 'DASHBOARD') {
            $location.path('/dashboard').search({tei: teiId,                                            
                                    program: $scope.selectedProgram ? $scope.selectedProgram.id: null});
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
        goToDashboard( destination ? destination : 'DASHBOARD', teiId );
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
                                var dhis2Events = EventUtils.autoGenerateEvents($scope.tei.trackedEntityInstance, $scope.selectedProgram, $scope.selectedOrgUnit, enrollment);
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
    
    $scope.registerEntity = function(destination){        

        //check for form validity
        $scope.outerForm.submitted = true;        
        if( $scope.outerForm.$invalid ){
            return false;
        }                   
        
        //form is valid, continue the registration
        //get selected entity        
        if(!$scope.selectedTei.trackedEntityInstance){
            $scope.selectedTei.trackedEntity = $scope.tei.trackedEntity = $scope.selectedProgram && $scope.selectedProgram.trackedEntity && $scope.selectedProgram.trackedEntity.id ? $scope.selectedProgram.trackedEntity.id : $scope.trackedEntities.selected.id;
            $scope.selectedTei.orgUnit = $scope.tei.orgUnit = $scope.selectedOrgUnit.id;
            $scope.selectedTei.attributes = $scope.selectedTei.attributes = [];
        }
        
        //get tei attributes and their values
        //but there could be a case where attributes are non-mandatory and
        //registration form comes empty, in this case enforce at least one value        
        
        var result = RegistrationService.processForm($scope.tei, $scope.selectedTei, $scope.attributesById);
        $scope.formEmpty = result.formEmpty;
        $scope.tei = result.tei;
        
        if($scope.formEmpty){//registration form is empty
            return false;
        }        
        performRegistration(destination);
    }; 
   
    
    var processRuleEffect = function(){        
        $scope.warningMessages = [];        
        angular.forEach($rootScope.ruleeffects['registration'], function (effect) {
            if (effect.trackedEntityAttribute) {
                //in the data entry controller we only care about the "hidefield", showerror and showwarning actions
                if (effect.action === "HIDEFIELD") {
                    if (effect.trackedEntityAttribute) {
                        if (effect.ineffect && $scope.selectedTei[effect.trackedEntityAttribute.id]) {
                            //If a field is going to be hidden, but contains a value, we need to take action;
                            if (effect.content) {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert(effect.content);
                            }
                            else {
                                //TODO: Alerts is going to be replaced with a proper display mecanism.
                                alert($scope.attributesById[effect.trackedEntityAttribute.id].name + "Was blanked out and hidden by your last action");
                            }

                            //Blank out the value:
                            $scope.selectedTei[effect.trackedEntityAttribute.id] = "";
                        }

                        $scope.hiddenFields[effect.trackedEntityAttribute.id] = effect.ineffect;
                    }
                    else {
                        $log.warn("ProgramRuleAction " + effect.id + " is of type HIDEFIELD, bot does not have an attribute defined");
                    }
                } else if (effect.action === "SHOWERROR") {
                    if (effect.trackedEntityAttribute) {                        
                        if(effect.ineffect) {
                            var dialogOptions = {
                                headerText: 'validation_error',
                                bodyText: effect.content + (effect.data ? effect.data : "")
                            };
                            DialogService.showDialog({}, dialogOptions);
                            $scope.selectedTei[effect.trackedEntityAttribute.id] = $scope.tei[effect.trackedEntityAttribute.id];
                        }
                    }
                    else {
                        $log.warn("ProgramRuleAction " + effect.id + " is of type HIDEFIELD, bot does not have an attribute defined");
                    }
                } else if (effect.action === "SHOWWARNING") {
                    if (effect.trackedEntityAttribute) {
                        if(effect.ineffect) {
                            var message = effect.content + (angular.isDefined(effect.data) ? effect.data : "");
                            $scope.warningMessages.push(message);
                            $scope.warningMessages[effect.trackedEntityAttribute.id] = message;
                        }
                    }
                    else {
                        $log.warn("ProgramRuleAction " + effect.id + " is of type HIDEFIELD, bot does not have an attribute defined");
                    }
                }
            }
        });
    };
    
    $scope.executeRules = function () {   
        var flag = {debug: true, verbose: false};
        
        //repopulate attributes with updated values
        $scope.selectedTei.attributes = [];
        
        angular.forEach($scope.attributes, function(metaAttribute){
            var newAttributeInArray = {attribute:metaAttribute.id,
                code:metaAttribute.code,
                displayName:metaAttribute.displayName,
                type:metaAttribute.valueType
            };
            if($scope.selectedTei[newAttributeInArray.attribute]){
                newAttributeInArray.value = $scope.selectedTei[newAttributeInArray.attribute];
            }
            
           $scope.selectedTei.attributes.push(newAttributeInArray);
        });
        
        if($scope.selectedProgram && $scope.selectedProgram.id){
            TrackerRulesExecutionService.executeRules($scope.allProgramRules, 'registration', null, null, $scope.selectedTei, $scope.selectedEnrollment, flag);
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
    $scope.$on('ruleeffectsupdated', function (event, args) {
        processRuleEffect(args.event);
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
        
        var modalOptions = {
            closeButtonText: 'no',
            actionButtonText: 'yes',
            headerText: 'cancel',
            bodyText: 'are_you_sure_to_cancel_registration'
        }
        ModalService.showModal({}, modalOptions).then(function(){
            cancelFunction();
        });
    }
});
