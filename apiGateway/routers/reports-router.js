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
const logger = require('../util/logger');

const MAX_RETRY_CNT = 3;   // 失败重传次数
const defaultUrl = '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';

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

  let api = util.getAPI() + `currentUser/page/reports?limit=${req.query.limit}`;
  if (assetID != 'ALL') {
    api += `&assetIdStr=${assetID}`;
  }

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
          "report_date": reportInfo.createTs,
          "assetId":reportInfo.assetId.id,
          "tenantId":reportInfo.tenantId.id,
          "customerId":reportInfo.customerId.id,
          "userId":reportInfo.userId.id,
          "userName":reportInfo.userName
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

function processFileUpload(assetID, req, res){
  let token = req.headers['x-authorization'];
  let fileName = req.files.report_file.path;
  let params = req.body;

  if (fileName && params) {
    uploadFileToServer(fileName, assetID, params, MAX_RETRY_CNT, 1, token, res);
  } else {
    res.responData(util.CST.ERR400, util.CST.MSG400, res);
  }
}

// POST
router.post('/:id', multipartMiddleware, async function (req, res) {
  let assetID = req.params.id;

  if (req.baseUrl == '/api/v1/reports/upload') {
    // 单独处理文件上传
    processFileUpload(assetID, req, res);
  } else {
    let token = req.headers['x-authorization'];
    let params = req.body;
    if (!params.fileId){
      util.responData(util.CST.ERR400, util.CST.MSG400, res);
      return;
    }
  
    // 下载文件到本地
    let downloadFileHost = util.getFSVR() + params.fileId;
    axios.get(downloadFileHost, {
      headers: {
        "X-Authorization": token
      },
      responseType: 'arraybuffer'
    }).then((resp) => {
      let query_time = {
        'startTs': params.startTime,
        'endTs': params.endTime
      };
      decodeFile(resp.data, query_time, token, req, res);
  
      let msg = '开始在后台处理报表生成';
      console.log(msg);
      logger.log('info',msg);
      util.responData(200, msg, res);
    }).catch((err) => {
      logger.log('info','POST error.');
      util.responErrorMsg(err, res);
    });
  }
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
  //console.log('Creating report (can take some time) ...');
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
        logger.log('info','--- try to axios ---', type, chart_name, devid,query_time.startTs,query_time.endTs,inerval);
        let data = [];
        let api = util.getAPI() + `v1/echarts/${type}?chart_name=${chart_name}&devid=${devid}&startTime=${query_time.startTs}&endTime=${query_time.endTs}&interval=${inerval}&chartWidth=${w_cm}&chartHeight=${h_cm}`;
        api = encodeURI(api);

        if((query_time.endTs - query_time.startTs)/(inerval*1000) > 700){
          data = defaultUrl
          flag = true;
          return { width: w_cm, height: h_cm, data, extension: '.png' };
        }

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
    logger.log('error',err);
  });

  //console.log('模板处理完成，生成报表中...');
  logger.log('info','模板处理完成，生成报表中...');
  generateReport(doc, req, res);
}

function deleteFile(fileName){
  fs.unlink(fileName, function(err){
    if (err) {
      logger.log('error',`${fileName} delete Failed,` + err.message);
    } else {
      logger.log('info',`${fileName} delete OK.`);
    }
  });
}

// 上传文件到文件服务器
function uploadFileToServer(fileName, assetID, params, maxRetryCnt, tryCnt, token, res) {
  var formData = {
    file: fs.createReadStream(fileName),
  };
  let host = util.getFSVR();
  let uploadFileHost = host + 'api/file/upload/';
  
  request.post({ url: uploadFileHost, formData: formData }, function (err, httpResponse, body) {
    if (err) {
      console.log('文件上传失败！');
      deleteFile(fileName);
      if (res) {
        util.responData(util.CST.MSG400, '文件上传失败', res);
      }
    }
    else {
      try {
        if (JSON.parse(body).success) {
          let msg = `第${tryCnt}次文件[${fileName}]上传成功, 保存报表信息到数据库...`;
          console.log(msg);
          logger.log('info', msg);
          deleteFile(fileName);

          let debugInfo = `类型:${params.report_type} 报表名字:${params.report_name} 操作者:${params.operator}`;
          console.log(debugInfo);
          logger.log('info', debugInfo);
          let bodyData = JSON.parse(body)
          let urlPath = host + bodyData.fileId;

          let data = {
            "userName": params.operator,
            "assetId": {
              "entityType": "ASSET",
              "id": assetID
            },
            "name": params.report_name,
            "type": params.report_type,
            "fileId": bodyData.fileId,
            "fileUrl": urlPath,
            "additionalInfo": null
          };
          saveToDB(data, token, res);
        }
        else {
          let msg = `第${tryCnt}次上传报表文件[${fileName}]失败。${body}`;
          console.log(msg);
          logger.log('info', msg);

          // 重试几次都失败，报错
          if (tryCnt == maxRetryCnt) {
            if (res) {
              deleteFile(fileName);
              util.responData(util.CST.ERR400, msg, res); 
            }              
          } else {
            // 重试
            tryCnt += 1;
            uploadFileToServer(fileName, assetID, params, maxRetryCnt, tryCnt, token, res);
          }
        }
      } catch (err) {
        let msg = `报表文件[${fileName}]上传失败。${err.message}`;
        console.log(msg);
        logger.log('info', msg);
        deleteFile(fileName);
        if (res) {
          util.responData(util.CST.ERR400, msg, res);
        }
      }
    }
  });
}

function generateReport(doc, req, res) {
  var fileId = req.body.fileId.split('/');
  var tmpFileName = 'tmp.docx';
  if (fileId[4]) {
    tmpFileName = `${req.body.report_type}_${req.body.report_name}_${fileId[4]}`;
  }
  
  // 创建一个bufferstream
  var bufferStream = new stream.PassThrough();
  bufferStream.end(doc);
  // 创建一个可以写入的流，写入到文件中
  var writerStream = fs.createWriteStream(tmpFileName);
  writerStream.write(doc, 'UTF8');
  writerStream.end();
  
  writerStream.on('finish', function () {
    let msg = `写入完成。开始上传报表文件。[${tmpFileName}]`;
    console.log(msg);
    logger.log('info', msg);
    
    let assetID = req.params.id;
    let params = req.body;
    let token = req.headers['x-authorization'];
    uploadFileToServer(tmpFileName, assetID, params, MAX_RETRY_CNT, 1, token, null);
  });

  writerStream.on('error', function (err) {
    logger.log('info','writerStream error' + err);
    console.log(err.stack);
  });
}

// 保存到数据库
function saveToDB(data, token, res) {
  let api = util.getAPI() + 'currentUser/report';
  axios.post(api, data, {
    headers: {
      "X-Authorization": token
    },
  }).then((resp) => {
    let msg = '数据库记录更新成功';
    console.log(msg);
    logger.log('info', msg);
    if (res) {
      util.responData(util.CST.OK200, msg, res);
    }
  }).catch((err) => {
    let msg = '数据库记录更新出错 ';
    if (err.response && err.response.data.message) {
      let errMsg = err.response.data.message;
      if (errMsg) {        
        logger.log('info', msg + errMsg);
        console.log(msg + errMsg);              
        if (res) {
          util.responData(util.CST.ERR400, errMsg, res);
        }
      }
    } else {
      if (res) {
        util.responData(util.CST.ERR400, msg, res);
      }
    }
  });
}

module.exports = router