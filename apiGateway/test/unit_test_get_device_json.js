var util = require('./unit_test_cfg')

// 获取转换后的JSON
describe('7、获取转换后的device.json接口测试', function () {
    it('获取转换后的device.json GET should return a 200 response', function (done) {
        // this.timeout(20000);
        util.API.get(`getConvertDeviceJson/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.sysAdminToken)
            //.expect(200, done);
            .end(function (err, resp) {
                var props = resp.body[0];
                //console.log(props);
                (props.client_attrib != undefined).should.be.true;
                (props.share_attrib  != undefined).should.be.true;
                done();
            });
    })
});