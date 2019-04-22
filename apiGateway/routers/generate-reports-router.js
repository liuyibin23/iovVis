const express = require('express');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
const util = require('../util/utils');
const createReport = require('docx-templates');
const router = express.Router();
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
    var fileName = `${req.query.report_type}_${req.query.report_name}_${req.query.report_date}.docx`;
    fileName = encodeURI(fileName);
    var stream = require('stream');
    // 创建一个bufferstream
    var bufferStream = new stream.PassThrough();
    //将Buffer写入
    bufferStream.end(filePath);

    res.writeHead(200, {
        'Content-Type': 'application/force-download',
        'Content-Disposition': 'attachment;filename=' + fileName
    });
    bufferStream.pipe(res);
}

//GET STAT
router.get('/:id', function (req, res) {
    let id = req.params.id;

    // 下载文件到本地
    let token = req.headers['x-authorization'];
    // download file
    let downloadFileHost = util.getFSVR() + req.query.fileId;
    axios.get(downloadFileHost, {
        headers: {
            "X-Authorization": token
        },
        responseType: 'arraybuffer'
    }).then((resp) => {
        let query_time = {
            'startTs': req.query.startTime,
            'endTs': req.query.endTime
        };
        decodeFile(resp.data, query_time, token, req, res);
    }).catch((err) => {
        util.responErrorMsg(err, res);
    });

})

module.exports = router