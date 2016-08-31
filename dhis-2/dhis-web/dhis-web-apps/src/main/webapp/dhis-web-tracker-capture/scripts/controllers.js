/* global angular */

'use strict';

/* Controllers */
var trackerCaptureControllers = angular.module('trackerCaptureControllers', [])

//Controller for settings page
.controller('SelectionController',
        function($rootScope,
                $scope,
                $modal,
                $location,
                $filter,
                $timeout,
                $translate,
                Paginator,
                SessionStorageService,
                MetaDataFactory,
                DateUtils,
                OrgUnitFactory,
                OperatorFactory,
                ProgramFactory,
                AttributesFactory,
                EntityQueryFactory,
                CurrentSelection,
                TEIGridService,
                TEIService) {  
    
    $scope.maxOptionSize = 30;
    
    //Selection
    $scope.ouModes = [{name: 'SELECTED'}, {name: 'CHILDREN'}, {name: 'DESCENDANTS'}, {name: 'ACCESSIBLE'}];         
    $scope.selectedOuMode = $scope.ouModes[0];    
    $scope.dashboardProgramId = ($location.search()).program;
    $scope.selectedOrgUnitId = ($location.search()).ou;
    $scope.treeLoaded = false;
    $scope.searchOuTree = false;
    $scope.teiListMode = {onlyActive: false};
    $scope.enrollmentStatus = 'ALL';
    
    //Paging
    $scope.pager = {pageSize: 50, page: 1, toolBarDisplay: 5};   
    
    //EntityList
    $scope.showTrackedEntityDiv = false;
    
    //Searching
    $scope.showSearchDiv = false;
    $scope.searchText = null;
    $scope.emptySearchText = false;
    $scope.searchFilterExists = false;   
    $scope.defaultOperators = OperatorFactory.defaultOperators;
    $scope.boolOperators = OperatorFactory.boolOperators;
    $scope.enrollment = {programStartDate: '', programEndDate: '', operator: $scope.defaultOperators[0]};
    $scope.searchMode = { listAll: 'LIST_ALL', freeText: 'FREE_TEXT', attributeBased: 'ATTRIBUTE_BASED' };    
    $scope.optionSets = null;
    $scope.attributesById = null;
    $scope.doSearch = true;
    
    //Registration
    $scope.showRegistrationDiv = false;    
    
    //watch for selection of org unit from tree
    $scope.$watch('selectedOrgUnit', function() {           

        if( angular.isObject($scope.selectedOrgUnit)){   
            
            $scope.searchingOrgUnit = $scope.selectedOrgUnit;
            
            SessionStorageService.set('SELECTED_OU', $scope.selectedOrgUnit);
            
            $scope.trackedEntityList = null;
            
            $scope.optionSets = CurrentSelection.getOptionSets();
            
            $scope.attributesById = CurrentSelection.getAttributesById();
            
            if(!$scope.attributesById){
                $scope.attributesById = [];
                MetaDataFactory.getAll('attributes').then(function(atts){                    
                    angular.forEach(atts, function(att){
                        $scope.attributesById[att.id] = att;
                    });
                    CurrentSelection.setAttributesById($scope.attributesById);
                });
            }
            
            if(!$scope.optionSets){
                $scope.optionSets = [];
                MetaDataFactory.getAll('optionSets').then(function(optionSets){
                    angular.forEach(optionSets, function(optionSet){  
                        $scope.optionSets[optionSet.id] = optionSet;
                    });
                    CurrentSelection.setOptionSets($scope.optionSets);
                });                
            }
            
            //Labels
            $scope.orgUnitLabel = $translate.instant('org_unit');
            $scope.listAllLabel = $translate.instant('list_all');
            $scope.registerLabel = $translate.instant('register');
            $scope.searchOusLabel = $translate.instant('locate_organisation_unit_by_name');
            $scope.printLabel = $translate.instant('print');
            $scope.searchLabel = $translate.instant('search');
            $scope.findLabel = $translate.instant('find');    
            $scope.advancedSearchLabel = $translate.instant('advanced_search');
            $scope.allEnrollmentsLabel = $translate.instant('all_enrollment');
            $scope.completedEnrollmentsLabel = $translate.instant('completed_enrollment');
            $scope.activeEnrollmentsLabel = $translate.instant('active_enrollment');
            $scope.cancelledEnrollmentsLabel = $translate.instant('cancelled_enrollment');
            $scope.searchCriteriaLabel = $translate.instant('type_your_search_criteria_here');
            $scope.programSelectLabel = $translate.instant('please_select_a_program');
            $scope.settingsLabel = $translate.instant('settings');
            $scope.showHideLabel = $translate.instant('show_hide_columns');
            $scope.listProgramsLabel = $translate.instant('list_programs');
            $scope.settingsLabel = $translate.instant('settings');
            
            $scope.loadPrograms($scope.selectedOrgUnit);
        }
    });
    
    //watch for changes in ou mode - mode could be selected without notifcation to grid column generator
    $scope.$watch('selectedOuMode.name', function() {
        if( $scope.selectedOuMode.name && angular.isObject($scope.gridColumns)){
            var continueLoop = true;
            for(var i=0; i<$scope.gridColumns.length && continueLoop; i++){
                if($scope.gridColumns[i].id === 'orgUnitName' && $scope.selectedOuMode.name !== 'SELECTED'){
                    $scope.gridColumns[i].show = true;
                    continueLoop = false;
                }
            }           
        }
    });    
    
    //watch for program feedback (this is when coming back from dashboard)
    if($scope.dashboardProgramId && $scope.dashboardProgramId !== 'null'){        
        ProgramFactory.get($scope.dashboardProgramId).then(function(program){
            $scope.selectedProgram = program;        
        });
    }
    
    //load programs associated with the selected org unit.
    $scope.loadPrograms = function(orgUnit) {
        
        $scope.selectedOrgUnit = orgUnit;
        
        if (angular.isObject($scope.selectedOrgUnit)) {   
            
            ProgramFactory.getProgramsByOu($scope.selectedOrgUnit, $scope.selectedProgram).then(function(response){
                $scope.programs = response.programs;
                $scope.selectedProgram = response.selectedProgram;
                $scope.trackedEntityList = null;
                $scope.selectedSearchMode = $scope.searchMode.listAll;
                $scope.processAttributes();                
                //$scope.search($scope.searchMode.listAll);
            });
        }        
    };
    
    $scope.getProgramAttributes = function(program){ 
        $scope.selectedProgram = program;
        $scope.trackedEntityList = null;
        $scope.processAttributes();              
    };
    
    $scope.processAttributes = function(){
        $scope.sortColumn = {};
        AttributesFactory.getByProgram($scope.selectedProgram).then(function(atts){
            $scope.attributes = AttributesFactory.generateAttributeFilters(atts);
            var grid = TEIGridService.generateGridColumns($scope.attributes, $scope.selectedOuMode.name);
            $scope.gridColumns = grid.columns;
            
            if($scope.showRegistrationDiv){
                $scope.doSearch = false;
            }

            if($scope.doSearch){
                $scope.search($scope.searchMode);
            } 
        });
    };
    
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
   
    //$scope.searchParam = {bools: []};
    $scope.search = function(mode){        
        $scope.selectedSearchMode = mode;
        $scope.emptySearchText = false;
        $scope.emptySearchAttribute = false;
        $scope.showRegistrationDiv = false;  
        $scope.showTrackedEntityDiv = false;        
        $scope.teiFetched = false;
        $scope.trackedEntityList = null;
        
        $scope.queryUrl = null;
        $scope.programUrl = null;
        $scope.attributeUrl = {url: null, hasValue: false};
    
        if($scope.selectedProgram){
            $scope.programUrl = 'program=' + $scope.selectedProgram.id;
        }        
        
        //check search mode
        if( $scope.selectedSearchMode === $scope.searchMode.freeText ){     

            if(!$scope.searchText){                
                $scope.emptySearchText = true;
                $scope.teiFetched = false;
                return;
            }       
            
            $scope.queryUrl = 'query=LIKE:' + $scope.searchText;            
            $scope.attributes = EntityQueryFactory.resetAttributesQuery($scope.attributes, $scope.enrollment);
            $scope.searchingOrgUnit = $scope.selectedOrgUnit;
        }
        
        if( $scope.selectedSearchMode === $scope.searchMode.attributeBased ){
            
            $scope.searchText = '';
            
            $scope.attributeUrl = EntityQueryFactory.getAttributesQuery($scope.attributes, $scope.enrollment);
            
            if(!$scope.attributeUrl.hasValue){
                $scope.emptySearchAttribute = true;
                $scope.teiFetched = false;
                return;
            }
            
            $scope.searchingOrgUnit = $scope.selectedSearchingOrgUnit && $scope.selectedSearchingOrgUnit.id ? $scope.selectedSearchingOrgUnit : $scope.selectedOrgUnit;
        }
        
        if( $scope.selectedSearchMode === $scope.searchMode.listAll ){
            $scope.searchText = '';            
            $scope.attributes = EntityQueryFactory.resetAttributesQuery($scope.attributes, $scope.enrollment);
            $scope.searchingOrgUnit = $scope.selectedOrgUnit;
        }
        
        $scope.doSearch = false;
        $scope.fetchTeis();
    };
    
    $scope.fetchTeis = function(){
        
        $scope.teiFetched = false;
        $scope.trackedEntityList = null;
        $scope.showTrackedEntityDiv = true;
        
        //get events for the specified parameters        
        TEIService.search($scope.searchingOrgUnit.id, 
                                            $scope.selectedOuMode.name,
                                            $scope.queryUrl,
                                            $scope.programUrl,
                                            $scope.attributeUrl.url,
                                            $scope.pager,
                                            true).then(function(data){            
            if( data.metaData && data.metaData.pager ){
                $scope.pager = data.metaData.pager;
                $scope.pager.toolBarDisplay = 5;

                Paginator.setPage($scope.pager.page);
                Paginator.setPageCount($scope.pager.pageCount);
                Paginator.setPageSize($scope.pager.pageSize);
                Paginator.setItemCount($scope.pager.total);                    
            }
            
            //process tei grid
            $scope.trackedEntityList = TEIGridService.format(data,false, $scope.optionSets, null);
            
            $scope.showSearchDiv = false;
            $scope.teiFetched = true;  
            $scope.doSearch = true;

            if(!$scope.sortColumn.id){                                      
                $scope.sortGrid({id: 'created', name: 'registration_date', valueType: 'date', displayInListNoProgram: false, showFilter: false, show: false});
            }
        });
    };
    
    $scope.jumpToPage = function(){
        if($scope.pager && $scope.pager.page && $scope.pager.pageCount && $scope.pager.page > $scope.pager.pageCount){
            $scope.pager.page = $scope.pager.pageCount;
        }
        $scope.search();
    };
    
    $scope.resetPageSize = function(){
        $scope.pager.page = 1;        
        $scope.search();
    };
    
    $scope.getPage = function(page){    
        $scope.pager.page = page;
        $scope.search();
    };
    
    $scope.clearEntities = function(){
        $scope.trackedEntityList = null;
    };
    
    $scope.showRegistration = function(){
        $scope.showRegistrationDiv = !$scope.showRegistrationDiv;
        
        if($scope.showRegistrationDiv){
            $scope.showTrackedEntityDiv = false;
            $scope.showSearchDiv = false;
            $timeout(function() { 
                $rootScope.$broadcast('registrationWidget', {registrationMode: 'REGISTRATION'});
            }, 200);
        }
        else{            
            $scope.doSearch = true;
            if(!$scope.trackedEntityList){
                if($scope.doSearch){
                    $scope.search($scope.searchMode);
                }
            }                        
            $scope.showTrackedEntityDiv = true;
        }
    };    
    
    $scope.showHideColumns = function(){
        $scope.hiddenGridColumns = 0;
        
        angular.forEach($scope.gridColumns, function(gridColumn){
            if(!gridColumn.show){
                $scope.hiddenGridColumns++;
            }
        });
        
        var modalInstance = $modal.open({
            templateUrl: 'views/column-modal.html',
            controller: 'ColumnDisplayController',
            resolve: {
                gridColumns: function () {
                    return $scope.gridColumns;
                },
                hiddenGridColumns: function(){
                    return $scope.hiddenGridColumns;
                }
            }
        });

        modalInstance.result.then(function (gridColumns) {
            $scope.gridColumns = gridColumns;
        }, function () {
        });
    };

    $scope.showDashboard = function(currentEntity){
        var sortedTei = $filter('orderBy')($scope.trackedEntityList.rows, function(tei) {
            return $scope.d2Sort(tei);
        }, $scope.reverse);
        
        var sortedTeiIds = [];
        angular.forEach(sortedTei, function(tei){
            sortedTeiIds.push(tei.id);
        });
        
        CurrentSelection.setSortedTeiIds(sortedTeiIds);        
        $location.path('/dashboard').search({tei: currentEntity.id,                                            
                                            program: $scope.selectedProgram ? $scope.selectedProgram.id: null});                                    
    };
       
    $scope.getHelpContent = function(){
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
    
    
    $scope.filterByEnrollmentStatus = function(status){
        if(status !== $scope.enrollmentStatus){            
            $scope.enrollmentStatus = status;                
            if($scope.enrollmentStatus === 'ALL'){
                $scope.programUrl = 'program=' + $scope.selectedProgram.id;                
            }
            else{
                $scope.programUrl = 'program=' + $scope.selectedProgram.id + '&programStatus=' + $scope.enrollmentStatus;
            }
            $scope.fetchTeis();
        }
    };
    
    //load programs for the selected orgunit (from tree)
    $scope.setSelectedSearchingOrgUnit = function(orgUnit){    
        $scope.selectedSearchingOrgUnit = orgUnit;
    };
});
