var util = require('./unit_test_cfg')

// 报表测试
describe('2、报表接口测试', function () {
    it('should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`reports/${util.unitTestParams.assetIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it(`should return a 200 response`, function (done) {
        //this.timeout(20000);
        util.API.get(`reports/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 20
            })
            //.set('limit', 20)
            .expect(200, done);
    })

    it('reports/ALL', function (done) {
        //this.timeout(20000);
        util.API.get(`reports/ALL`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 20
            })
            //.set('limit', 20)
            .expect(200, done);
    })
});
