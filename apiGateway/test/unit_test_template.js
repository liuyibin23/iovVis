var util = require('./unit_test_cfg')
var FormData = require('form-data')

// 模板测试
describe('1、模板接口测试', function () {
    it('GET should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it('GET should return a 200 response', function (done) {
        //this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(200, done);
    })

    var deleteParams = {
        assetId:"",
        template_name:""
    };

    it('GET 删除模板前获取数据', function (done) {
        //this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 200
            })
            .end(function (err, resp) {
                done();

                var reports = resp.body;
                console.log(reports.length);
                if (reports.length > 0) {
                    let _dt = reports[reports.length - 1];
                    deleteParams.assetId   = util.unitTestParams.assetId;
                    deleteParams.template_name = _dt.template_name;

                    //console.log(`delete params: ${deleteParams.assetId} ${deleteParams.report_id} ${deleteParams.report_fileId}`);
                }
            });
            //.expect(200, done);
    })

    it('DELETE 删除模板', function (done) {
        console.log(`delete params: ${deleteParams.assetId} ${deleteParams.template_name}`);
        //this.timeout(20000);

        // 临时屏蔽
        util.API.delete(`templates/${deleteParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                templateName : 'UnitTestTemplate'
            })
            .expect(200, done);
    })

    it('POST 上传模板', function(done){
        console.log("start up 模板");
        this.timeout(50000);
        util.API.post(`templates/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .set('Content-Type', 'multipart/form-data')
            .field('template_name', 'UnitTestTemplate')
            .attach('template_file', '20190508.docx')
            .end(function (err, resp) {
                done();
            });
    });
});
