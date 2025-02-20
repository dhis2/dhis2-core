const {module, inject} = angular.mock;
const {spy} = sinon;
describe('Filters: d2Filters', () => {

    beforeEach(module('d2Filters'));

    /*TODO: paginate filters */

    describe('d2Filter: trimquotesFilter', () => {
        let trimquotesFilter;
        beforeEach(inject(($injector) => {
            trimquotesFilter = $injector.get('trimquotesFilter');
        }));

        it('should trim the initial single and double quotes', function() {
            expect(trimquotesFilter('"testString"')).to.equal('testString');
            expect(trimquotesFilter('{{testString::')).to.equal('{{testString::');
        });
        it('should return the same input if the input is not a string', function() {
            expect(trimquotesFilter(25)).to.equal(25);
        });

    });

    describe('d2Filter: trimvariablequalifiers', () => {
        let trimvariablequalifiersFilter;
        beforeEach(inject(($injector) => {
            trimvariablequalifiersFilter = $injector.get('trimvariablequalifiersFilter');
        }));

        it('should trim the characters matching /^[#VCAvca]{ in the begnnind and /}', function() {
            let beginningCharsToFilter = ["V{","C{","A{",,"#{","v{",,"c{",,"a{"];
            beginningCharsToFilter.forEach(function(beginningString){
                expect(trimvariablequalifiersFilter(beginningString+'testString}')).to.equal('testString');
                expect(trimvariablequalifiersFilter(beginningString+'testStringTT')).to.equal('testStringTT');
            });
        });

        it('should return the same input if the input is not a string', function() {
            expect(trimvariablequalifiersFilter(25)).to.equal(25);
        });

    });

    describe('d2Filter: forLoop', () => {
        let forLoopFilter;

        beforeEach(inject(($injector) => {
            forLoopFilter = $injector.get('forLoopFilter');
        }));

        it('should return an array with elements from start to end', function() {
            expect(forLoopFilter(2,3,10).toString()).to.equal([3,4,5,6,7,8,9].toString());
        });
    });
});