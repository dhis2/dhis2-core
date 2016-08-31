/* global trackerCapture, angular */

trackerCapture.controller('ProgramStatisticsController',
         function($scope,
                DateUtils,                
                EnrollmentService,
                ProgramFactory,
                DHIS2EventFactory) {
    $scope.today = DateUtils.getToday();
    
    $scope.ouModes = [{name: 'SELECTED'}, {name: 'CHILDREN'}, {name: 'DESCENDANTS'}, {name: 'ACCESSIBLE'}];         
    $scope.selectedOuMode = $scope.ouModes[0];
    $scope.report = {};
    $scope.maxOptionSize = 30;
    $scope.model = {};
    
    $scope.displayMode = {};
    $scope.printMode = false;
    
    //Paging
    $scope.pager = {pageSize: 50, page: 1, toolBarDisplay: 5};
    
    function resetParams(){
        $scope.reportStarted = false;
        $scope.dataReady = false;
    }
    //watch for selection of org unit from tree
    $scope.$watch('selectedOrgUnit', function() {      
        resetParams();
        $scope.model.selectedProgram = null;
        if( angular.isObject($scope.selectedOrgUnit)){        
            $scope.loadPrograms($scope.selectedOrgUnit);
        }
    });
    
    //load programs associated with the selected org unit.
    $scope.loadPrograms = function(orgUnit) {        
        $scope.selectedOrgUnit = orgUnit;        
        if (angular.isObject($scope.selectedOrgUnit)){
            ProgramFactory.getProgramsByOu($scope.selectedOrgUnit, $scope.model.selectedProgram).then(function(response){
                $scope.programs = response.programs;
                $scope.model.selectedProgram = response.selectedProgram;
            });
        }        
    };    
    
    //watch for selection of program
    $scope.$watch('model.selectedProgram', function() {   
        if( angular.isObject($scope.model.selectedProgram)){            
            resetParams();
        }
    });
    
    $scope.xFunction = function(){
        return function(d) {
            return d.key;
        };
    };
    
    $scope.yFunction = function(){
        return function(d){
            return d.y;
        };
    };

    $scope.generateReport = function(program, report, ouMode){
        
        $scope.model.selectedProgram = program;
        $scope.report = report;
        $scope.selectedOuMode = ouMode;

        //check for form validity
        $scope.outerForm.submitted = true;        
        if( $scope.outerForm.$invalid || !$scope.model.selectedProgram){
            return false;
        }
        
        $scope.dataReady = false;
        $scope.reportStarted = true;        
        

        $scope.enrollments = {active: 0, completed: 0, cancelled: 0};
        $scope.enrollmentList = [];
        EnrollmentService.getByStartAndEndDate($scope.model.selectedProgram.id,
                                        $scope.selectedOrgUnit.id, 
                                        $scope.selectedOuMode.name,
                                        DateUtils.formatFromUserToApi($scope.report.startDate), 
                                        DateUtils.formatFromUserToApi($scope.report.endDate)).then(function(data){

            if( data ) {
                $scope.totalEnrollment = data.enrollments.length;                                
                angular.forEach(data.enrollments, function(en){
                    $scope.enrollmentList[en.enrollment] = en;
                    if(en.status === 'ACTIVE'){
                        $scope.enrollments.active++;
                    }
                    else if(en.status === 'COMPLETED'){
                        $scope.enrollments.completed++;
                    }
                    else{
                        $scope.enrollments.cancelled++;
                    }
                });

                $scope.enrollmentStat = [{key: 'Completed', y: $scope.enrollments.completed},
                                        {key: 'Active', y: $scope.enrollments.active},
                                        {key: 'Cancelled', y: $scope.enrollments.cancelled}];

                DHIS2EventFactory.getByOrgUnitAndProgram($scope.selectedOrgUnit.id, $scope.selectedOuMode.name, $scope.model.selectedProgram.id, null, null).then(function(data){
                    
                    if( data ) {
                        $scope.dhis2Events = {completed: 0, active: 0, skipped: 0, overdue: 0, ontime: 0};
                        $scope.totalEvents = 0;
                        angular.forEach(data, function(ev){

                            if(ev.trackedEntityInstance && $scope.enrollmentList[ev.enrollment]){                        

                                $scope.totalEvents++;
                                if(ev.status === 'COMPLETED'){
                                    $scope.dhis2Events.completed++;
                                }
                                else if(ev.status === 'ACTIVE'){
                                    $scope.dhis2Events.active++;
                                }
                                else if(ev.status === 'SKIPPED'){
                                    $scope.dhis2Events.skipped++;
                                }                        
                                else{
                                    if(ev.dueDate && moment($scope.today).isAfter(DateUtils.formatFromApiToUser(ev.dueDate))){
                                        $scope.dhis2Events.overdue++;
                                    }
                                    else{
                                        $scope.dhis2Events.ontime++;
                                    }
                                }
                            }
                        });
                        $scope.eventStat = [{key: 'Completed', y: $scope.dhis2Events.completed},
                                            {key: 'Active', y: $scope.dhis2Events.active},
                                            {key: 'Skipped', y: $scope.dhis2Events.skipped},
                                            {key: 'Ontime', y: $scope.dhis2Events.overdue},
                                            {key: 'Overdue', y: $scope.dhis2Events.ontime}];         
                    }
                });            
            }
            
            $scope.reportStarted = false;
            $scope.dataReady = true; 
            
        });
    };    
});