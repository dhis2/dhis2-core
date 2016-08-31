/* global trackerCapture, angular */

trackerCapture.controller('EnrollmentController',
        function($rootScope,
                $scope,  
                $location,
                $timeout,
                DateUtils,
                SessionStorageService,
                CurrentSelection,
                OrgUnitService,
                EnrollmentService,
                ModalService) {
    
    $scope.today = DateUtils.getToday();
    $scope.selectedOrgUnit = SessionStorageService.get('SELECTED_OU');    
    
    //listen for the selected items
    var selections = {};
    $scope.$on('selectedItems', function(event, args) {   
        $scope.attributes = [];
        $scope.historicalEnrollments = [];
        $scope.showEnrollmentDiv = false;
        $scope.showEnrollmentHistoryDiv = false;
        $scope.hasEnrollmentHistory = false;
        $scope.selectedEnrollment = null;
        $scope.currentEnrollment = null;
        $scope.newEnrollment = {};
        
        selections = CurrentSelection.get();        
        processSelectedTei();       
        
        $scope.selectedEntity = selections.te;
        $scope.selectedProgram = selections.pr;
        $scope.optionSets = selections.optionSets;
        $scope.programs = selections.prs;
        var selectedEnrollment = selections.selectedEnrollment;
        $scope.enrollments = selections.enrollments;
        $scope.programExists = args.programExists;
        $scope.programNames = selections.prNames;
        $scope.programStageNames = selections.prStNames;
        $scope.attributesById = CurrentSelection.getAttributesById();
        
        if($scope.selectedProgram){
            
            $scope.stagesById = [];        
            angular.forEach($scope.selectedProgram.programStages, function(stage){
                $scope.stagesById[stage.id] = stage;
            });
            
            angular.forEach($scope.enrollments, function(enrollment){
                if(enrollment.program === $scope.selectedProgram.id ){
                    if(enrollment.status === 'ACTIVE'){
                        selectedEnrollment = enrollment;
                        $scope.currentEnrollment = enrollment;
                    }
                    if(enrollment.status === 'CANCELLED' || enrollment.status === 'COMPLETED'){
                        $scope.historicalEnrollments.push(enrollment);
                        $scope.hasEnrollmentHistory = true;
                    }
                }
            });
            
            if(selectedEnrollment){
                $scope.selectedEnrollment = selectedEnrollment;
                $scope.loadEnrollmentDetails(selectedEnrollment);
            }
            else{
                $scope.selectedEnrollment = null;
                $scope.broadCastSelections('dashboardWidgets');
            }
        }
        else{
            $scope.broadCastSelections('dashboardWidgets');
        }        
    });
    
    $scope.loadEnrollmentDetails = function(enrollment){
        $scope.showEnrollmentHistoryDiv = false;
        $scope.selectedEnrollment = enrollment;
        
        if($scope.selectedEnrollment.enrollment && $scope.selectedEnrollment.orgUnit){
            if($scope.selectedEnrollment.orgUnit !== $scope.selectedOrgUnit.id) {
                OrgUnitService.open().then(function(){
                    OrgUnitService.get($scope.selectedEnrollment.orgUnit).then(function(ou){
                        if(ou){
                            $scope.selectedEnrollment.orgUnitName = $scope.selectedOrgUnit.name;
                        }                                                       
                    });           
                });
            }
            else{
                $scope.selectedEnrollment.orgUnitName = $scope.selectedOrgUnit.name;
            }
            $scope.broadCastSelections('dashboardWidgets');
        }
    };
        
    $scope.showNewEnrollment = function(){
        
        $scope.showEnrollmentDiv = !$scope.showEnrollmentDiv;
        
        $timeout(function() { 
            $rootScope.$broadcast('enrollmentEditing', {enrollmentEditing: $scope.showEnrollmentDiv});
        }, 200);
            
        if($scope.showEnrollmentDiv){
            
            $scope.showEnrollmentHistoryDiv = false;
            
            //load new enrollment details
            $scope.selectedEnrollment = {orgUnitName: $scope.selectedOrgUnit.name};            
            $scope.loadEnrollmentDetails($scope.selectedEnrollment);
            
            $timeout(function() { 
                $rootScope.$broadcast('registrationWidget', {registrationMode: 'ENROLLMENT', selectedTei: $scope.selectedTei});
            }, 200);
        }
        else{
            hideEnrollmentDiv();
        }
    };
       
    $scope.showEnrollmentHistory = function(){
        
        $scope.showEnrollmentHistoryDiv = !$scope.showEnrollmentHistoryDiv;
        
        if($scope.showEnrollmentHistoryDiv){
            $scope.selectedEnrollment = null;
            $scope.showEnrollmentDiv = false;
            
            $scope.broadCastSelections('dashboardWidgets');
        }
    };
    
    $scope.broadCastSelections = function(listeners){
        var selections = CurrentSelection.get();
        var tei = selections.tei;
        
        CurrentSelection.set({tei: tei, te: $scope.selectedEntity, prs: $scope.programs, pr: $scope.selectedProgram, prNames: $scope.programNames, prStNames: $scope.programStageNames, enrollments: $scope.enrollments, selectedEnrollment: $scope.selectedEnrollment, optionSets: $scope.optionSets});
        $timeout(function() { 
            $rootScope.$broadcast(listeners, {});
        }, 200);
    };
    
    var processSelectedTei = function(){
        $scope.selectedTei = angular.copy(selections.tei);
        angular.forEach($scope.selectedTei.attributes, function(att){
            $scope.selectedTei[att.attribute] = att.value;
        });
        delete $scope.selectedTei.attributes;
    };
    
    var hideEnrollmentDiv = function(){
        
        /*currently the only way to cancel enrollment window is by going through
         * the main dashboard controller. Here I am mixing program and programId, 
         * as I didn't want to refetch program from server, the main dashboard
         * has already fetched the programs. With the ID passed to it, it will
         * pass back the actual program than ID. 
         */
        processSelectedTei();
        $scope.selectedProgram = ($location.search()).program;
        $scope.broadCastSelections('mainDashboard'); 
    };
    
    $scope.terminateEnrollment = function(){        

        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'terminate',
            headerText: 'terminate_enrollment',
            bodyText: 'are_you_sure_to_terminate_enrollment'
        };

        ModalService.showModal({}, modalOptions).then(function(result){            
            EnrollmentService.cancel($scope.selectedEnrollment).then(function(data){                
                $scope.selectedEnrollment.status = 'CANCELLED';
                $scope.loadEnrollmentDetails($scope.selectedEnrollment);                
            });
        });
    };
    
    $scope.completeEnrollment = function(){        

        var modalOptions = {
            closeButtonText: 'cancel',
            actionButtonText: 'complete',
            headerText: 'complete_enrollment',
            bodyText: 'are_you_sure_to_complete_enrollment'
        };

        ModalService.showModal({}, modalOptions).then(function(result){            
            EnrollmentService.complete($scope.selectedEnrollment).then(function(data){                
                $scope.selectedEnrollment.status = 'COMPLETED';
                $scope.loadEnrollmentDetails($scope.selectedEnrollment);                
            });
        });
    };
    
    $scope.markForFollowup = function(){
        $scope.selectedEnrollment.followup = !$scope.selectedEnrollment.followup; 
        EnrollmentService.update($scope.selectedEnrollment).then(function(data){         
        });
    };
});
