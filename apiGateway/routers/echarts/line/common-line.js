const axios = require('axios');
const node_echarts = require('node-echarts');
const util  = require('../../../util/utils');
const logger = require('../../../util/logger');


function SendPngResponse(option, params, res){
    if (!option)
    {
        util.responData(404, '访问资源不存在。', res);
        return;  
    }

    let config = {
        width: params.chartWidth   ? params.chartWidth  * 100 : 500, // Image width, type is number.
        height: params.chartHeight ? params.chartHeight * 100 : 400, // Image height, type is number.
        option: option, // Echarts configuration, type is Object.
        //If the path  is not set, return the Buffer of image.
        // path:  '', // Path is filepath of the image which will be created.
        enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
    }
    let bytes = node_echarts(config);
    if (bytes) {
        let data = 'data:image/png;base64,' + bytes.toString('base64');
        util.responData(200, data, res);
    }
}

function processData(option, params, allData, maxCnt, res){
    // 检查数据有效性
    let dataValid = false;
    let chooseIdx = 0;
    let dataLen = 0;
    for (let i = 0; i < allData.length; i++){
        dataLen = allData[i].length;
        if (dataLen > 0) {
            dataValid = true;
            chooseIdx = i;
            break;
        }
    }

    if (dataValid){
        for (let i = 0; i < dataLen; i++) {                    
            for (let idx = 0; idx < maxCnt; idx++) {
                if (allData[idx] && allData[idx][i]) {
                    val = Number.parseFloat(allData[idx][i].value);
                    option.series[idx].data.push(val);
                }
            }

            var dat = util.getDateString(allData[chooseIdx][i].ts, params.interval);
            option.xAxis[0].data.push(dat);        
        }
    }

    SendPngResponse(option, params, res);
}

function getData(plotCfg, option, params, token, res){    
    if (plotCfg)
    {
        let diff = params.endTime - params.startTime;
        let interval = Number.parseFloat(params.interval) * 1000;

        let keyValue = plotCfg.keys;
        let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
        + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&agg=AVG`;
        api = encodeURI(api);
        //console.log(api);

        axios.get(api, {
            headers: { "X-Authorization": token }
        }).then(response => {
            //console.log('idx:' + idx + ' return:' + retCnt);
            let keys = plotCfg.keys.split(',');
            let allData = [];
            for (let i = 0; i < plotCfg.maxCnt; i++){
                allData[i] = (response.data[keys[i]]) ? response.data[keys[i]] : [];
            }
            //logger.log('info',JSON.stringify(response.body));
            processData(option, params, allData, plotCfg.maxCnt, res);
        }).catch(err => {
            logger.log('error',`err:`+err);
            processData(option, res, params, plotCfg.maxCnt, res);
        });   
    }else{
        logger.log('info','no plotcfg indicate!!');
    }
}

function resetPreData(option, maxCnt){
    option.xAxis[0].data = [];
    for (let idx = 0; idx < maxCnt; idx++) {
        option.series[idx].data = [];
    }
}

exports.resetPreData = resetPreData;
exports.getData = getData;