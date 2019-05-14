var util = require('./unit_test_cfg')

// 模板测试
describe('1、模板接口测试', function () {
    it('should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it('should return a 200 response', function (done) {
        //this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(200, done);
    })

    // it('/device api test', function(done) {
    //     api.post(`templates/${assetId}`)
    //     .set('Content-Type','application/json')
    //     .set('Authorization', token)
    //     .send({
    //         template_name:'UnitTestTemplate',
    //         template_file:'xxx'
    //     })
    //     .expect(200) //断言希望得到返回http状态码
    //     .end(function(err, res) {
    //         console.info(res.body);//得到返回我们可以用2.2中的断言对返回结果进行判断。
    //         done();
    //     });
    // })
});
