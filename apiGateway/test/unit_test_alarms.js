var util = require('./unit_test_cfg')

// 告警测试
describe('3、告警接口测试', function () {
    it('should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`rules/alarms/${util.unitTestParams.deviceIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it(`POST should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.post(`rules/alarms/${util.unitTestParams.deviceId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .send([
                {
                    "Key": "温度",
                    "IndeterminateRules": {
                        "min": "10",
                        "max": "20"
                    },
                    "WarningRules": {
                        "min": "30",
                        "max": "40"
                    }
                }
            ])
            .expect(200, done);
    })

    it(`GET  should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.get(`rules/alarms/${util.unitTestParams.deviceId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            //.expect(200, done);
            .end(function (err, resp) {
                done();

                var Key = resp.body[0].Key;
                var IndeterminateRules = resp.body[0].IndeterminateRules;
                var WarningRules = resp.body[0].WarningRules;
                //console.info(IndeterminateRules);

                util.expect(Key).to.be.equal('温度');
                util.expect(IndeterminateRules.min).to.be.equal('10');
                util.expect(IndeterminateRules.max).to.be.equal('20');
                util.expect(WarningRules.min).to.be.equal('30');
                util.expect(WarningRules.max).to.be.equal('40');
            })
    })
});
