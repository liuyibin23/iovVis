var util = require('./unit_test_cfg')

var deleteParams = {
    assetId:"",
    reportId:"",
    fileId:""
};

// 报表测试
describe('2、报表接口测试', function () {
    it('GET 获取报表文件 错误的ID should return a 500 response', function (done) {
        // this.timeout(20000);
        util.API.get(`reports/${util.unitTestParams.assetIdError}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(500, done);
    })

    it(`GET 获取报表文件 正确的ID should return a 200 response`, function (done) {
        this.timeout(30000);
        util.API.get(`reports/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 20
            })
            //.set('limit', 20)
            .expect(200, done);
    })

    it('GET 获取所有报表文件  reports/ALL', function (done) {
        this.timeout(20000);
        util.API.get(`reports/ALL`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 200
            })
            .expect(200, done);
    })

    // it('POST 生成报表文件 xxxxxxxxx  - should return a 400 response', function (done) {
    //     //this.timeout(20000);
    //     util.API.post(`reports/${util.unitTestParams.assetId}`)
    //         .set('Accept', 'application/json')
    //         .set('X-Authorization', util.unitTestParams.token)
    //         .send({
    //             fileId: "xxxxxxxxx",
    //             report_name:"UnitTestReport",
    //             report_type:"DAY",
    //             operator:"柳强东",
    //             startTime:"1551369600000",
    //             endTime:"1556640000000"
    //         })
    //         .expect(400, done);
    // })
    
    var tmplateFileId = "";
    it('生成报表文件前获取模板文件 GET should return a 200 response', function (done) {
        //this.timeout(20000);
        util.API.get(`templates/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .expect(200)
            .end(function(err, resp){
                done();
                tmplateFileId = resp.body[0].fileId;
                //console.log(resp.body);
                //console.log(tmplateFileId);
            });
    })

    it('POST 生成报表文件 should return a 200 response', function (done) {
        //this.timeout(20000);
        //console.log(`post use: ${tmplateFileId}`);
        util.API.post(`reports/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .send({
                fileId: tmplateFileId,
                report_name:"UnitTestReport",
                report_type:"DAY",
                operator:"柳强东",
                startTime:"1551369600000",
                endTime:"1556640000000"
            })
            .expect(200, done);
    })

    var manualUploadReportName = 'UnitTestManualUploadReport';
    it('POST 手动上传报表文件 should return a 200 response', function (done) {
        this.timeout(50000);
        util.API.post(`reports/upload/${util.unitTestParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .field('report_name', manualUploadReportName)
            .field('report_type', 'DAY')
            .field('operator', '柳强东')
            .attach('report_file', '20190508.docx')
            .expect(200, done);
    })

    it('GET 删除报表前获取数据  reports/ALL', function (done) {
        this.timeout(20000);
        util.API.get(`reports/ALL`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                limit: 200
            })
            //.set('limit', 20)
            .end(function (err, resp) {
                done();

                var reports = resp.body.data;
                console.log(reports.length);
                var find = false;
                for (var i = 0; i < reports.length; i++) {
                    let _dt = reports[i];

                    if (_dt.report_name === manualUploadReportName) {
                        find = true;

                        deleteParams.assetId   = _dt.assetId;
                        deleteParams.report_id = _dt.report_id;
                        deleteParams.report_fileId = _dt.report_fileId;
                        break;
                    }
                }
                
                util.expect(find).to.be.equal(true);
            });
            //.expect(200, done);
    })

    it('DELETE 删除报表', function (done) {
        //console.log(`delete params: ${deleteParams.assetId} ${deleteParams.report_id} ${deleteParams.report_fileId}`);
        this.timeout(20000);
        // 临时屏蔽
        util.API.delete(`reports/${deleteParams.assetId}`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                reportId: deleteParams.report_id,
                fileId: deleteParams.report_fileId
            })
            .expect(200, done);
    })
});
