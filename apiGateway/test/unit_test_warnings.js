var util = require('./unit_test_cfg')

// 预警测试
describe('4、预警接口测试', function () {
    it('should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`warnings/${util.unitTestParams.assetIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it(`设置资产预警状态 POST should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.post(`warnings/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                asset_warning_level: 'BLUE'
            })
            .expect(200, done);
    })

    it(`获取资产预警状态 GET  should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.get(`warnings/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            //.expect(200, done);
            .end(function (err, resp) {
                done();
                //console.info(resp.body.asset_warning_level);
                var level = resp.body.asset_warning_level;
                util.expect(level).to.be.equal('BLUE');
            })
    })

    it(`设置资产预警规则 POST should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.post(`rules/warnings/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .send({
                "blueRules": [
                    {
                        "andRule": [
                            "123456"
                        ]
                    }
                ],
                "orangeRules": [
                    {
                        "andRule": [
                            "654321"
                        ]
                    }
                ]
            })
            .expect(200, done);
    })

    it(`获取资产预警规则 GET  should return a 200 response'`, function (done) {
        //this.timeout(20000);
        util.API.get(`rules/warnings/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            //.expect(200, done);
            .end(function (err, resp) {
                done();
                //console.info(resp.body.blueRules);
                //console.info(resp.body.orangeRules);
                var blueRules = resp.body.blueRules[0];
                var orangeRules = resp.body.orangeRules[0];
                //var level = resp.body.asset_warning_level;      
                util.expect(blueRules.andRule[0]).to.be.equal('123456');
                util.expect(orangeRules.andRule[0]).to.be.equal('654321');
            })
    })
});
