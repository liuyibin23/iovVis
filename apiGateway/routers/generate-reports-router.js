const express = require('express');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
const util = require('../util/utils');
const createReport = require('docx-templates');
const router = express.Router();
const stream = require('stream');

require('isomorphic-fetch');

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Reports Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
    res.send('Generate Reports Api home page')
})
// define the about route
router.get('/about', function (req, res) {
    res.send('About Generate reports')
})

async function decodeFile(buffer, query_time, token,req, res) {
    console.log('Creating report (can take some time) ...');
    let api = util.getAPI() + `v1/tables?template=%E5%AE%9A%E6%9C%9F%E7%9B%91%E6%B5%8B%E6%8A%A5%E5%91%8A&startTime=${query_time.startTs}&endTime=${query_time.endTs}&graphQL=`;
    const doc = await createReport({
        template: buffer,
        data: query =>
            fetch(api + query, {
                method: 'GET',
                headers: {
                    Accept: 'application/json',
                    "X-Authorization": token,
                    'Content-Type': 'application/json',
                }
            })
                .then(res => res.json())
                .then(res => res.data),
        additionalJsContext: {
            genIMG: async (type, chart_name, devid, inerval, w_cm, h_cm) => {
                console.log('--- try to axios ---', type, chart_name, devid);
                let data = [];
                let api = util.getAPI() + `v1/echarts/${type}?chart_name=${chart_name}&devid=${devid}&startTime=${query_time.startTs}&endTime=${query_time.endTs}&interval=${inerval}&chartWidth=${w_cm}&chartHeight=${h_cm}`;
                api = encodeURI(api);

                await axios.get(api, {
                    headers: { "X-Authorization": token }
                }).then(resp => {
                    const dataUrl = resp.data;
                    data = dataUrl.slice('data:image/png;base64,'.length);
                    flag = true;
                }).catch(err => {
                    console.log(err);
                    flag = false;
                });
                return { width: w_cm, height: h_cm, data, extension: '.png' };
            }
        }
    }).catch(err => {
        console.log(err);
    });

    console.log('完成，发送文件中...');
    ResponFile(doc, req, res);
    console.log('发送完成');
}

function ResponFile(filePath, req, res) {
if (1) {
   var tmpFileName = 'tmp.docx';
   // 创建一个bufferstream
   var bufferStream = new stream.PassThrough();
    //将Buffer写入
    bufferStream.end(filePath);
    // 创建一个可以写入的流，写入到文件 output.txt 中
    var writerStream = fs.createWriteStream(tmpFileName);
    writerStream.write(filePath,'UTF8');
    writerStream.end();

    writerStream.on('finish', function() {
        console.log("写入完成。");

        // 上传文件到文件服务器
        var formData = {
            file: fs.createReadStream(tmpFileName),
        };
        let host = util.getFSVR();
        let uploadFileHost = host + 'api/file/upload/';
        request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
            if (err) {
                return console.error('upload failed:', err);
            }
            else {
                try {
                    if (JSON.parse(body).success) {
                        console.log('Upload successful!  Server responded with:', body);
                        let bodyData = JSON.parse(body)
                        let urlPath = host + bodyData.fileId;

                        let data = {
                            "userName": req.query.operator, 
                            "assetId": { 
                                "entityType": "ASSET",
                                "id": req.params.id
                            },
                            "name": req.query.report_name,
                            "type": 'DAY',
                            "fileId": bodyData.fileId,
                            "fileUrl": urlPath,
                            "additionalInfo": null
                        };
                        let token = req.headers['x-authorization'];
                        saveToDB(data, token);
                    }
                    else {
                        util.responData(501, '报表文件上传失败。', res);
                    }
                } catch(err) {
                    util.responData(501, '报表文件上传失败。', res);
                }
            }
        });
    });

    writerStream.on('error', function(err){
        console.log(err.stack);
    });
}
}

// 保存到数据库
function saveToDB(data, token){
    let api = util.getAPI() + 'currentUser/report';
    axios.post(api, data, {
        headers: {
            "X-Authorization": token
        },
    }).then((resp) => {
        console.log(resp);
    }).catch((err) =>{
        console.log(err);
    });
}

router.get('/:id', function (req, res) {
    let id = req.params.id;
    util.responData(200, "不支持此接口", res);

    // // 下载文件到本地
    // let token = req.headers['x-authorization'];
    // // download file
    // let downloadFileHost = util.getFSVR() + req.query.fileId;
    // axios.get(downloadFileHost, {
    //     headers: {
    //         "X-Authorization": token
    //     },
    //     responseType: 'arraybuffer'
    // }).then((resp) => {
    //     let query_time = {
    //         'startTs': req.query.startTime,
    //         'endTs': req.query.endTime
    //     };
    //     decodeFile(resp.data, query_time, token, req, res);

    //     console.log("process finish");
    //     util.responData(200, "开始在后台处理报表生成", res);
    // }).catch((err) => {
    //     util.responErrorMsg(err, res);
    // });
})

module.exports = router