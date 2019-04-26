const express = require('express');
const multipart = require('connect-multiparty');
const axios = require('axios');
const fs = require('fs');
const request = require('request');
const createReport = require('docx-templates');
const util = require('../util/utils');
const stream = require('stream');
const router = express.Router();
const multipartMiddleware = multipart();
require('isomorphic-fetch');

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Reports Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Reports Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})

// GET
router.get('/:assetId', async function (req, res) {
  let assetID = req.params.assetId;
  let token = req.headers['x-authorization'];

  let api = util.getAPI() + `currentUser/page/reports?assetIdStr=${assetID}&limit=${req.query.limit}`;

  if (req.query.startTs && req.query.endTs) {
    api += `&startTs=${req.query.startTs}&endTs=${req.query.endTs}`;
  }

  if (req.query.idOffset) {
    api += `&idOffset=${req.query.idOffset}`;
  }

  if (req.query.typeFilter) {
    api += `&typeFilter=${req.query.typeFilter}`;
  }

  axios.get(api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    let data = {
      data:[],
      hasNext:false,
      nextPageLink:null
    };
    if (resp.data && resp.data.data) {
      for (let i = 0; i < resp.data.data.length; i++){
        let reportInfo = resp.data.data[i];
        let _dt = {
          "report_name": reportInfo.name,
          "report_id": reportInfo.id.id,
          "report_fileId": reportInfo.fileId,
          "report_url": reportInfo.fileUrl,
          "report_type": reportInfo.type,
          "report_date": reportInfo.createTs
        };

        data.data.push(_dt);
      }

      data.hasNext = resp.data.hasNext;
      data.nextPageLink = resp.data.nextPageLink;
    }

    util.responData(200, data, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})
// POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let assetID = req.params.id;
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

    let msg = '开始在后台处理报表生成';
    console.log(msg);
    util.responData(200, msg, res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})

function deleteFile(fileId) {
  let host = util.getFSVR();
  let deleteFileHost = host + 'api/file/delete/';
  request.post({ url: deleteFileHost, form: { fileId: fileId } }, function (err, httpResponse, body) {
    if (err) {
      console.log(`${fileId} 文件删除失败`);
    }
    else {
      console.log(`${fileId} 文件删除成功`);
    }
  });
}

function deleteFileRecord(reportId, token, res) {
  let api = util.getAPI() + `currentUser/report/${reportId}`;
  axios.delete(api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    util.responData(200, "成功删除资产的报表。", res);
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
}

// DELETE
router.delete('/:id', async function (req, res) {
  // 查询属性
  let assetID = req.params.id;
  let token = req.headers['x-authorization'];

  // 查询是否有记录
  let api = util.getAPI() + `currentUser/count/reports?assetIdStr=${assetID}`;
  axios.get(api, {
    headers: {
      "X-Authorization": token
    }
  }).then((resp) => {
    if (resp.data.count > 0) {
      let fileId = req.query.fileId;
      let reportId = req.query.reportId;
      // 删除文件服务器数据
      deleteFile(fileId);

      // 删除记录
      deleteFileRecord(reportId, token, res);
    } else {
      util.responData(200, '此资产下无报表文件。', res);
    }
  }).catch((err) => {
    util.responErrorMsg(err, res);
  });
})



async function decodeFile(buffer, query_time, token, req, res) {
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

  console.log('模板处理完成，生成报表中...');
  generateReport(doc, req, res);
}

// 上传文件到文件服务器
function uploadFileToServer(fileName, req, res) {
  var formData = {
    file: fs.createReadStream(fileName),
  };
  let host = util.getFSVR();
  let uploadFileHost = host + 'api/file/upload/';
  request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      console.log('文件上传失败！');
    }
    else {
      try {
        if (JSON.parse(body).success) {
          console.log('文件上传成功, 保存报表信息到数据库...');
          console.log(`类型:${req.query.report_type} 报表名字:${req.query.report_name} 操作者:${req.query.operator}`);
          let bodyData = JSON.parse(body)
          let urlPath = host + bodyData.fileId;

          let data = {
            "userName": req.query.operator,
            "assetId": {
              "entityType": "ASSET",
              "id": req.params.id
            },
            "name": req.query.report_name,
            "type": req.query.report_type,
            "fileId": bodyData.fileId,
            "fileUrl": urlPath,
            "additionalInfo": null
          };
          let token = req.headers['x-authorization'];
          saveToDB(data, token);
        }
        else {
          console.log('报表文件上传失败。');
        }
      } catch (err) {
        console.log('报表文件上传失败。');
      }
    }
  });
}

function generateReport(doc, req, res) {
  var tmpFileName = 'tmp.docx';
  // 创建一个bufferstream
  var bufferStream = new stream.PassThrough();
  bufferStream.end(doc);
  // 创建一个可以写入的流，写入到文件中
  var writerStream = fs.createWriteStream(tmpFileName);
  writerStream.write(doc, 'UTF8');
  writerStream.end();

  writerStream.on('finish', function () {
    console.log("写入完成。开始上传报表文件。");
    uploadFileToServer(tmpFileName, req, res);
  });

  writerStream.on('error', function (err) {
    console.log(err.stack);
  });
}

// 保存到数据库
function saveToDB(data, token) {
  let api = util.getAPI() + 'currentUser/report';
  axios.post(api, data, {
    headers: {
      "X-Authorization": token
    },
  }).then((resp) => {
    console.log('数据库记录更新完成...');
  }).catch((err) => {
    console.log('数据库记录更新出错' + err);
  });
}

module.exports = router