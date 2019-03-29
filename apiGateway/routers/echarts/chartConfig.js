var path = require('path'); //系统路径模块
var fs = require('fs'); //文件模块
const logger = require('../../util/logger');

var gCfg = null;

function loadChartCfg(filePath){
    let data = [];
    var file = path.join(filePath); 

    try {
        //读取json文件
        var cfg = fs.readFileSync(file, 'utf-8');
        var cfgInfo = JSON.parse(cfg);

        if (cfgInfo.PlotParams) {
            gCfg = cfgInfo.PlotParams;
            return true;
        }
    } catch(err){
        console.log(err.message);
        logger.log('error', err.message);
        return false;
    }
}

function getCfgParams(chartName, type){
    let allCfg = [];
    if (type === 'LINE') {
        allCfg = gCfg.Line;
    }
    else if (type == 'Bar') {
        allCfg = gCfg.Bar;
    }
    else if (type == 'Pie') {
        allCfg = gCfg.Pie;
    }
    else if (type == 'Radar'){
        allCfg = gCfg.Radar;
    }
    
    return allCfg[chartName];
}

exports.loadChartCfg = loadChartCfg;
exports.getCfgParams = getCfgParams;