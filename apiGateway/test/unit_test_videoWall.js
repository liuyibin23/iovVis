var util = require('./unit_test_cfg')

// 视频墙
describe('5、视频墙接口测试', function () {
    it('获取视频墙信息 GET should return a 200 response', function (done) {
        // this.timeout(20000);
        util.API.get(`currentUser/wallResult/`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            //.expect(200, done);
            .end(function (err, resp) {
                var videoInfo = resp.body.videoInfo;
                //console.log(videoInfo);
                util.expect(videoInfo).to.be.equal('UnitTestVideoInfo');
                done();
            });
    })

    it('设置视频墙信息 POST should return a 200 response', function (done) {
        // this.timeout(20000);
        util.API.post(`currentUser/wallResult/`)
            .set('Accept', 'application/json')
            .set('X-Authorization', util.unitTestParams.token)
            .query({
                videoInfo: "UnitTestVideoInfo"
            })
            .expect(200, done);
    })
});