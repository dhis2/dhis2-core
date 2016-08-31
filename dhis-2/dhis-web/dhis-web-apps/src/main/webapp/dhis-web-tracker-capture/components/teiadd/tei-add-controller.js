/* global trackerCapture, angular */

trackerCapture.controller('TEIAddController', 
    function($scope, 
            $rootScope,
            $translate,
            $modalInstance, 
            DateUtils,
            CurrentSelection,
            OperatorFactory,
            AttributesFactory,
            EntityQueryFactory,
            OrgUnitFactory,
            ProgramFactory,
            MetaDataFactory,
            TEIService,
            TEIGridService,
            DialogService,
            Paginator,
            SessionStorageService,
            relationshipTypes,
            selectedProgram,
            relatedProgramRelationship,
            selections,
            selectedAttribute,
            existingAssociateUid,
            addingRelationship,
            selectedTei){
    
    $scope.maxOptionSize = 30;
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
    
    $scope.today = DateUtils.getToday();
    $scope.relationshipTypes = relationshipTypes;
    $scope.addingRelationship = addingRelationship;
    $scope.selectedAttribute = selectedAttribute;
    $scope.selectedProgram = selectedProgram;
    $scope.relatedProgramRelationship = relatedProgramRelationship;
    $scope.mainTei = selectedTei;    
    $scope.attributesById = CurrentSelection.getAttributesById();
    $scope.addingTeiAssociate = false;
    
    $scope.searchOuTree = false;
    $scope.orgUnitLabel = $translate.instant('org_unit');
    
    $scope.selectedRelationship = {};
    $scope.relationship = {};
    
    var invalidTeis = [];
    invalidTeis.push($scope.mainTei.trackedEntityInstance);
    if($scope.mainTei.relationships){
        angular.forEach($scope.mainTei.relationships, function(rel){
            invalidTeis.push(rel.trackedEntityInstanceB);
        });
    }    
    
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
    $scope.selectedEnrollment = {enrollmentDate: $scope.today, incidentDate: $scope.today, orgUnitName: $scope.selectedOrgUnit.name};

    //Selections
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
    $scope.selectedTeiForDisplay = angular.copy($scope.mainTei);
    $scope.ouModes = [{name: 'SELECTED'}, {name: 'CHILDREN'}, {name: 'DESCENDANTS'}, {name: 'ACCESSIBLE'}];         
    $scope.selectedOuMode = $scope.ouModes[0];
    
    //Paging
    $scope.pager = {pageSize: 50, page: 1, toolBarDisplay: 5}; 
    
    //Searching
    $scope.showAdvancedSearchDiv = false;
    $scope.searchText = {value: null};
    $scope.emptySearchText = false;
    $scope.searchFilterExists = false;   
    $scope.defaultOperators = OperatorFactory.defaultOperators;
    $scope.boolOperators = OperatorFactory.boolOperators;
    $scope.selectedTrackedEntity = null;
    
    $scope.trackedEntityList = null; 
    $scope.enrollment = {programStartDate: '', programEndDate: '', operator: $scope.defaultOperators[0]};
   
    $scope.searchMode = {listAll: 'LIST_ALL', freeText: 'FREE_TEXT', attributeBased: 'ATTRIBUTE_BASED'};
    $scope.selectedSearchMode = $scope.searchMode.listAll;
    
    if($scope.addingRelationship){
        $scope.teiAddLabel = $translate.instant('add_relationship');
        $scope.programs = selections.prs;
        CurrentSelection.setRelationshipOwner($scope.mainTei);
    }
    else{        
        $scope.teiAddLabel = $scope.selectedAttribute && $scope.selectedAttribute.name ? $scope.selectedAttribute.name : $translate.instant('tracker_associate');
        $scope.addingTeiAssociate = true;
        ProgramFactory.getProgramsByOu($scope.selectedOrgUnit, $scope.selectedProgram).then(function(response){
            $scope.programs = response.programs;
            $scope.selectedProgram = response.selectedProgram;
        });
        
        if(existingAssociateUid){
            TEIService.get(existingAssociateUid, $scope.optionSets, $scope.attributesById).then(function(data){
                $scope.selectedTeiForDisplay = data;
            });
        }
        else{
            $scope.selectedTeiForDisplay = null;
        }
        
        CurrentSelection.setRelationshipOwner({});
        
        if($scope.selectedAttribute && $scope.selectedAttribute.trackedEntity && $scope.selectedAttribute.trackedEntity.id){
            $scope.selectedTrackedEntity = $scope.selectedAttribute.trackedEntity;
        }
    }
    
    if(angular.isObject($scope.programs) && $scope.programs.length === 1){
        $scope.selectedProgramForRelative = $scope.programs[0];        
    } 
    
    if($scope.selectedProgram){
        if($scope.selectedProgram.relatedProgram && $scope.relatedProgramRelationship){
            angular.forEach($scope.programs, function(pr){
                if(pr.id === $scope.selectedProgram.relatedProgram.id){
                    $scope.selectedProgramForRelative = pr;
                }
            });
        }
        
        if($scope.selectedProgram.relationshipType){
            angular.forEach($scope.relationshipTypes, function(rel){
                if(rel.id === $scope.selectedProgram.relationshipType.id){
                    $scope.relationship.selected = rel;
                }
            });
        }
    }
    
    //watch for selection of relationship
    $scope.$watch('relationship.selected', function() {        
        if( angular.isObject($scope.relationship.selected)){
            $scope.selectedRelationship = {aIsToB: $scope.relationship.selected.aIsToB, bIsToA: $scope.relationship.selected.bIsToA};  
        }
    });
    
    function resetFields(){
        
        $scope.teiForRelationship = null;
        $scope.teiFetched = false;    
        $scope.emptySearchText = false;
        $scope.emptySearchAttribute = false;
        $scope.showAdvancedSearchDiv = false;
        $scope.showRegistrationDiv = false;  
        $scope.showTrackedEntityDiv = false;
        $scope.trackedEntityList = null; 
        $scope.teiCount = null;

        $scope.queryUrl = null;
        $scope.programUrl = null;
        $scope.attributeUrl = {url: null, hasValue: false};
        $scope.sortColumn = {};
    }
    
    //listen for selections
    $scope.$on('relationship', function(event, args){
        if(args.result === 'SUCCESS'){
            var relationshipInfo = CurrentSelection.getRelationshipInfo();
            $scope.teiForRelationship = relationshipInfo.tei;
            $scope.addRelationship(); 
        }
        
        if(args.result === 'CANCEL'){
            $scope.showRegistration();
        }        
    });
    
    //sortGrid
    $scope.sortGrid = function(gridHeader){
        if ($scope.sortColumn && $scope.sortColumn.id === gridHeader.id){
            $scope.reverse = !$scope.reverse;
            return;
        }        
        $scope.sortColumn = gridHeader;
        if($scope.sortColumn.valueType === 'date'){
            $scope.reverse = true;
        }
        else{
            $scope.reverse = false;    
        }
    };
    
    $scope.d2Sort = function(tei){        
        if($scope.sortColumn && $scope.sortColumn.valueType === 'date'){            
            var d = tei[$scope.sortColumn.id];         
            return DateUtils.getDate(d);
        }
        return tei[$scope.sortColumn.id];
    };
    
    $scope.search = function(mode){ 
        
        resetFields();
        
        $scope.selectedSearchMode = mode;        
   
        if($scope.selectedProgramForRelative){
            $scope.programUrl = 'program=' + $scope.selectedProgramForRelative.id;
        }        
        
        //check search mode
        if( $scope.selectedSearchMode === $scope.searchMode.freeText ){ 
            
            if(!$scope.searchText.value){                
                $scope.emptySearchText = true;
                $scope.teiFetched = false;   
                $scope.teiCount = null;
                return;
            }       

            $scope.queryUrl = 'query=LIKE:' + $scope.searchText.value;                     
        }
        
        if( $scope.selectedSearchMode === $scope.searchMode.attributeBased ){            
            $scope.searchText.value = null;
            $scope.attributeUrl = EntityQueryFactory.getAttributesQuery($scope.attributes, $scope.enrollment);
            
            if(!$scope.attributeUrl.hasValue && !$scope.selectedProgramForRelative){
                $scope.emptySearchAttribute = true;
                $scope.teiFetched = false;   
                $scope.teiCount = null;
                return;
            }
        }
        
        if( $scope.addingTeiAssociate ){            
            if(!$scope.selectedTrackedEntity){
                var dialogOptions = {
                    headerText: 'searching_error',
                    bodyText: $translate.instant('no_enity_for_tracker_associate_attribute')
                };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
            else{
                $scope.programUrl = $scope.programUrl ? $scope.programUrl + '&trackedEntity=' + $scope.selectedTrackedEntity.id : 'trackedEntity=' + $scope.selectedTrackedEntity.id;
            }
        }
        
        $scope.fetchTei();
    };
    
    $scope.fetchTei = function(){

        //get events for the specified parameters
        TEIService.search($scope.selectedOrgUnit.id, 
                                            $scope.selectedOuMode.name,
                                            $scope.queryUrl,
                                            $scope.programUrl,
                                            $scope.attributeUrl.url,
                                            $scope.pager,
                                            true).then(function(data){
            //$scope.trackedEntityList = data;            
            if(data.rows){
                $scope.teiCount = data.rows.length;
            }                    
            
            if( data.metaData.pager ){
                $scope.pager = data.metaData.pager;
                $scope.pager.toolBarDisplay = 5;

                Paginator.setPage($scope.pager.page);
                Paginator.setPageCount($scope.pager.pageCount);
                Paginator.setPageSize($scope.pager.pageSize);
                Paginator.setItemCount($scope.pager.total);                    
            }
            
            //process tei grid
            $scope.trackedEntityList = TEIGridService.format(data,false, $scope.optionSets, invalidTeis);
            $scope.showTrackedEntityDiv = true;
            $scope.teiFetched = true;
            
            if(!$scope.sortColumn.id){                                      
                $scope.sortGrid({id: 'created', name: $translate.instant('registration_date'), valueType: 'date', displayInListNoProgram: false, showFilter: false, show: false});
            }
            
        });
    };
    
    //set attributes as per selected program
    $scope.setAttributesForSearch = function(program){
        
        $scope.selectedProgramForRelative = program;
        AttributesFactory.getByProgram($scope.selectedProgramForRelative).then(function(atts){
            $scope.attributes = atts;
            $scope.attributes = AttributesFactory.generateAttributeFilters(atts);
            $scope.gridColumns = $scope.generateGridColumns($scope.attributes);
        });
        
        
        $scope.search( $scope.selectedSearchMode );        
    }; 
    
    $scope.setAttributesForSearch( $scope.selectedProgramForRelative );
    
    $scope.jumpToPage = function(){
        if($scope.pager && $scope.pager.page && $scope.pager.pageCount && $scope.pager.page > $scope.pager.pageCount){
            $scope.pager.page = $scope.pager.pageCount;
        }
        
        $scope.search($scope.selectedSearchMode);
    };
    
    $scope.resetPageSize = function(){
        $scope.pager.page = 1;        
        $scope.search($scope.selectedSearchMode);
    };
    
    $scope.getPage = function(page){
        $scope.pager.page = page;
        $scope.search($scope.selectedSearchMode);
    };

    //generate grid columns from teilist attributes
    $scope.generateGridColumns = function(attributes){

        var columns = attributes ? angular.copy(attributes) : [];
       
        //also add extra columns which are not part of attributes (orgunit for example)
        columns.push({id: 'orgUnitName', name: 'Organisation unit', type: 'TEXT', displayInListNoProgram: false});
        columns.push({id: 'created', name: 'Registration date', type: 'TEXT', displayInListNoProgram: false});
        
        //generate grid column for the selected program/attributes
        angular.forEach(columns, function(column){
            if(column.id === 'orgUnitName' && $scope.selectedOuMode.name !== 'SELECTED'){
                column.show = true;
            }
            
            if(column.displayInListNoProgram){
                column.show = true;
            }           
           
            if(column.type === 'date'){
                $scope.filterText[column.id]= {start: '', end: ''};
            }
        });        
        return columns;        
    };   
    
    $scope.showHideSearch = function(simpleSearch){
        $scope.showAdvancedSearchDiv = simpleSearch ? false : !$scope.showAdvancedSearchDiv;
        $scope.showTrackedEntityDiv = !$scope.showAdvancedSearchDiv;        
    };
    
    $scope.showRegistration = function(){
        $scope.showRegistrationDiv = !$scope.showRegistrationDiv;        
        $scope.showTrackedEntityDiv = !$scope.showRegistrationDiv;        
    };
    
    $scope.close = function () {
        $modalInstance.close($scope.mainTei.relationships ? $scope.mainTei.relationships : []);
        $rootScope.showAddRelationshipDiv = !$rootScope.showAddRelationshipDiv;
    };
    
    $scope.setRelationshipSides = function(side){
        if(side === 'A'){            
            $scope.selectedRelationship.bIsToA = $scope.selectedRelationship.aIsToB === $scope.relationship.selected.aIsToB ? $scope.relationship.selected.bIsToA : $scope.relationship.selected.aIsToB;
        }
        if(side === 'B'){
            $scope.selectedRelationship.aIsToB = $scope.selectedRelationship.bIsToA === $scope.relationship.selected.bIsToA ? $scope.relationship.selected.aIsToB : $scope.relationship.selected.bIsToA;
        }
    };
    
    $scope.assignRelationship = function(relativeTei){
        $scope.teiForRelationship = relativeTei;
        $rootScope.showAddRelationshipDiv = !$rootScope.showAddRelationshipDiv;
    };    
    
    $scope.back = function(){
        $scope.teiForRelationship = null;
        $rootScope.showAddRelationshipDiv = !$rootScope.showAddRelationshipDiv;
    };
    
    $scope.addRelationship = function(){
        if($scope.addingRelationship){
            if($scope.mainTei && $scope.teiForRelationship && $scope.relationship.selected){            
                var tei = angular.copy($scope.mainTei);
                var relationship = {};
                relationship.relationship = $scope.relationship.selected.id;
                relationship.displayName = $scope.relationship.selected.name;
                relationship.relative = {};
                
                relationship.trackedEntityInstanceA = $scope.selectedRelationship.aIsToB === $scope.relationship.selected.aIsToB ? $scope.mainTei.trackedEntityInstance : $scope.teiForRelationship.id;
                relationship.trackedEntityInstanceB = $scope.selectedRelationship.bIsToA === $scope.relationship.selected.bIsToA ? $scope.teiForRelationship.id : $scope.mainTei.trackedEntityInstance;

                tei.relationships = [];
                angular.forEach($scope.mainTei.relationships, function(rel){
                    tei.relationships.push({relationship: rel.relationship, displayName: rel.displayName, trackedEntityInstanceA: rel.trackedEntityInstanceA, trackedEntityInstanceB: rel.trackedEntityInstanceB});
                });
                tei.relationships.push(relationship);

                TEIService.update(tei, $scope.optionSets, $scope.attributesById).then(function(response){
                    if(response.response && response.response.status !== 'SUCCESS'){//update has failed
                        var dialogOptions = {
                                headerText: 'relationship_error',
                                bodyText: response.message
                            };
                        DialogService.showDialog({}, dialogOptions);
                        return;
                    }

                    relationship.relative.processed = true;
                    relationship.relative.attributes = $scope.teiForRelationship;

                    if($scope.mainTei.relationships){
                        $scope.mainTei.relationships.push(relationship);
                    }
                    else{
                        $scope.mainTei.relationships = [relationship];
                    }

                    $modalInstance.close($scope.mainTei.relationships);
                });
            }
            else{
                var dialogOptions = {
                        headerText: 'relationship_error',
                        bodyText: $translate.instant('selected_tei_is_invalid')
                    };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
        }
        else{
            if($scope.teiForRelationship && $scope.teiForRelationship.id){
                $modalInstance.close($scope.teiForRelationship);
            }
            else{
                var dialogOptions = {
                        headerText: 'tracker_associate_error',
                        bodyText: $translate.instant('selected_tei_is_invalid')
                    };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
                
        }                
    };
    
    //Get orgunits for the logged in user
    OrgUnitFactory.getSearchTreeRoot().then(function(response) {  
        $scope.orgUnits = response.organisationUnits;
        angular.forEach($scope.orgUnits, function(ou){
            ou.show = true;
            angular.forEach(ou.children, function(o){                    
                o.hasChildren = o.children && o.children.length > 0 ? true : false;
            });            
        });
    });
    
    //expand/collapse of search orgunit tree
    $scope.expandCollapse = function(orgUnit) {
        if( orgUnit.hasChildren ){            
            //Get children for the selected orgUnit
            OrgUnitFactory.get(orgUnit.id).then(function(ou) {                
                orgUnit.show = !orgUnit.show;
                orgUnit.hasChildren = false;
                orgUnit.children = ou.organisationUnits[0].children;                
                angular.forEach(orgUnit.children, function(ou){                    
                    ou.hasChildren = ou.children && ou.children.length > 0 ? true : false;
                });                
            });           
        }
        else{
            orgUnit.show = !orgUnit.show;   
        }        
    };
   
    //load programs for the selected orgunit (from tree)
    $scope.setSelectedSearchingOrgUnit = function(orgUnit){    
        $scope.selectedSearchingOrgUnit = orgUnit;
    };
})

.controller('TEIRegistrationController', 
        function($rootScope,
                $scope,
                $timeout,
                AttributesFactory,
                MetaDataFactory,
                TrackerRulesFactory,
                CustomFormService,
                TEService,
                EnrollmentService,
                DialogService,
                CurrentSelection,
                DateUtils,
                RegistrationService,
                SessionStorageService) {
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');
    $scope.enrollment = {enrollmentDate: '', incidentDate: ''};    
    $scope.attributesById = CurrentSelection.getAttributesById();
    $scope.maxOptionSize = 30;
    
    var selections = CurrentSelection.get();
    $scope.programs = selections.prs;
    
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
    
    var assignInheritance = function(){        
        if($scope.addingRelationship){
            $scope.selectedTei = {};
            var t = angular.copy( CurrentSelection.getRelationshipOwner() );
            angular.forEach(t.attributes, function(att){
                t[att.attribute] = att.value;
            });
            
            angular.forEach($scope.attributes, function(att){
                if(att.inherit && t[att.id]){
                    $scope.selectedTei[att.id] = t[att.id];
                }
            });
            t = {};
        }
        else{
            $scope.selectedTei = {};
        }
    };
    
    var getRules = function(){
        $scope.allProgramRules = {constants: [], programIndicators: {}, programValidations: [], programVariables: [], programRules: []};
        if( angular.isObject($scope.selectedProgramForRelative) && $scope.selectedProgramForRelative.id ){
            TrackerRulesFactory.getRules($scope.selectedProgramForRelative.id).then(function(rules){                    
                $scope.allProgramRules = rules;
            });
        }
    };
  
    
    if(angular.isObject($scope.programs) && $scope.programs.length === 1){
        $scope.selectedProgramForRelative = $scope.programs[0];
        AttributesFactory.getByProgram($scope.selectedProgramForRelative).then(function(atts){
            $scope.attributes = atts;            
            assignInheritance();
            getRules();
        });
    }
    
    //watch for selection of program
    $scope.$watch('selectedProgramForRelative', function() {        
        $scope.trackedEntityForm = null;
        $scope.customForm = null;
        $scope.customFormExists = false;
        
        AttributesFactory.getByProgram($scope.selectedProgramForRelative).then(function(atts){
            $scope.attributes = atts;                        
            if($scope.selectedProgramForRelative && $scope.selectedProgramForRelative.id && $scope.selectedProgramForRelative.dataEntryForm && $scope.selectedProgramForRelative.dataEntryForm.htmlCode){
                $scope.customFormExists = true;
                $scope.trackedEntityForm = $scope.selectedProgramForRelative.dataEntryForm;  
                $scope.trackedEntityForm.attributes = $scope.attributes;
                $scope.trackedEntityForm.selectIncidentDatesInFuture = $scope.selectedProgramForRelative.selectIncidentDatesInFuture;
                $scope.trackedEntityForm.selectEnrollmentDatesInFuture = $scope.selectedProgramForRelative.selectEnrollmentDatesInFuture;
                $scope.trackedEntityForm.displayIncidentDate = $scope.selectedProgramForRelative.displayIncidentDate;
                $scope.customForm = CustomFormService.getForTrackedEntity($scope.trackedEntityForm, 'RELATIONSHIP');
            }

            assignInheritance();
            getRules();                
        });
        
    }); 
            
    $scope.trackedEntities = {available: []};
    TEService.getAll().then(function(entities){
        $scope.trackedEntities.available = entities;   
        $scope.trackedEntities.selected = $scope.trackedEntities.available[0];
    });
    
    $scope.registerEntity = function(){
        
        //check for form validity
        $scope.outerForm.submitted = true;
        if( $scope.outerForm.$invalid ){
            return false;
        }
        
        //form is valid, continue the registration
        //get selected entity
        var selectedTrackedEntity = $scope.trackedEntities.selected.id; 
        if($scope.selectedProgramForRelative){
            selectedTrackedEntity = $scope.selectedProgramForRelative.trackedEntity.id;
        }
        
        //get tei attributes and their values
        //but there could be a case where attributes are non-mandatory and
        //registration form comes empty, in this case enforce at least one value
        $scope.tei = {trackedEntity: selectedTrackedEntity, orgUnit: $scope.selectedOrgUnit.id};
        var result = RegistrationService.processForm($scope.tei, $scope.selectedTei, $scope.attributesById);
        $scope.formEmpty = result.formEmpty;
        $scope.tei = result.tei;
                
        if($scope.formEmpty){//registration form is empty
            return false;
        }
        
        RegistrationService.registerOrUpdate($scope.tei, $scope.optionSets, $scope.attributesById).then(function(registrationResponse){
            var reg = registrationResponse.response ? registrationResponse.response : {};
            if(reg.reference && reg.status === 'SUCCESS'){                
                $scope.tei.trackedEntityInstance = $scope.tei.id = reg.reference; 
                
                //registration is successful and check for enrollment
                if($scope.selectedProgramForRelative){    
                    //enroll TEI
                    var enrollment = {};
                        enrollment.trackedEntityInstance = $scope.tei.trackedEntityInstance;
                        enrollment.program = $scope.selectedProgramForRelative.id;
                        enrollment.status = 'ACTIVE';
                        enrollment.orgUnit = $scope.selectedOrgUnit.id;
                        enrollment.enrollmentDate = $scope.selectedEnrollment.enrollmentDate;
                        enrollment.incidentDate = $scope.selectedEnrollment.incidentDate === '' ? $scope.selectedEnrollment.enrollmentDate : $scope.selectedEnrollment.incidentDate;
                    EnrollmentService.enroll(enrollment).then(function(enrollmentResponse){
                        var en = enrollmentResponse.response && enrollmentResponse.response.importSummaries && enrollmentResponse.response.importSummaries[0] ? enrollmentResponse.response.importSummaries[0] : {};
                        if(en.reference && en.status === 'SUCCESS'){                            
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
            }
            else{
                //registration has failed
                var dialogOptions = {
                        headerText: 'registration_error',
                        bodyText: registrationResponse.message
                    };
                DialogService.showDialog({}, dialogOptions);
                return;
            }
            
            $timeout(function(){
                $scope.selectedEnrollment.enrollmentDate = '';
                $scope.selectedEnrollment.incidentDate =  '';
                $scope.outerForm.submitted = false; 
                $scope.broadCastSelections();                
            }, 100);        
            
        });
    };
    
    $scope.broadCastSelections = function(){
        if($scope.tei){
            angular.forEach($scope.tei.attributes, function(att){
                $scope.tei[att.attribute] = att.value;
            });

            $scope.tei.orgUnitName = $scope.selectedOrgUnit.name;
            $scope.tei.created = DateUtils.formatFromApiToUser(new Date());
            
            CurrentSelection.setRelationshipInfo({tei: $scope.tei});
            
            $timeout(function() { 
                $rootScope.$broadcast('relationship', {result: 'SUCCESS'});
            }, 100);
        }        
    };
    
    $scope.interacted = function(field) {
        var status = false;
        if(field){            
            status = $scope.outerForm.submitted || field.$dirty;
        }
        return status;        
    };
});
