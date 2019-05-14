var util = require('./unit_test_cfg')

// 统计
describe('6、统计接口测试', function () {
    var testParams = {
        deviceId: '7895cd70-171b-11e9-860f-856e86c94b8e',
        keys: '加速度',
        startTime: '1551369600000',
        endTime: '1556640000000'
    };

    it('获取物理量count信息 GET should return a 200 response', function (done) {
        // this.timeout(20000);
        util.API.get(`statistics/${testParams.deviceId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                keys: testParams.keys,
                startTime: testParams.startTime,
                endTime: testParams.endTime
            })
            //.expect(200, done);
            .end(function (err, resp) {
                var cntInfo = resp.body[0];
                //console.log(cntInfo);
                util.expect(cntInfo.key).to.be.equal(testParams.keys);
                done();
            });
    })

    it('获取物理量count信息 GET should return a 200 response', function (done) {
        // this.timeout(20000);
        util.API.get(`statistics/${testParams.deviceId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                keys: testParams.keys + 'ERROR',
                startTime: testParams.startTime,
                endTime: testParams.endTime
            })
            //.expect(200, done);
            .end(function (err, resp) {
                var cntInfo = resp.body[0];
                util.expect(cntInfo).to.be.equal(undefined);
                done();
            });
    })
});
