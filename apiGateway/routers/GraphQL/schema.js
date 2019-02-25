
//  定期监测报告
const axios = require('axios');
const util = require('../../util/utils');
var path = require('path'); //系统路径模块
var fs = require('fs'); //文件模块

const {
  GraphQLObjectType,
  GraphQLSchema,
  GraphQLInt,
  GraphQLID,
  GraphQLString,
  GraphQLList,
  GraphQLNonNull,
} = require('graphql');

function findExist(itemkey, sensorType, dataList){
  for (var i = 0; i < dataList.length; i++){
    if (dataList[i].monitorItem == itemkey && dataList[i].sensorType == sensorType){
      return i;
    }
  }

  return -1;
}

//  根据编码获取检测项索引值
function getMonitorItemIdxByCode(code, monitorItem){
  for (var i = 0; i < monitorItem.length; i++){
    if (monitorItem[i].code === code){
      return i;
    }
  }

  return -1;
}

// 获取监测项目及测点数据
async function getMonitorData(req, assetId){
  let data = [];
  var file = path.join('public/monitorItem.json'); 
  //读取json文件
  var monitorItem = fs.readFileSync(file, 'utf-8');
  var jsonMonitorItem = JSON.parse(monitorItem);

  let token = req.headers['x-authorization'];
  let getDeviceByAssetIdAPI = util.getAPI() + `currentUser/getDeviceByAssetId?assetId=${assetId.assetId}`;

  await axios.get(getDeviceByAssetIdAPI, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      let find = false;
      for (var i = 0; i < resp.data.length; i++) {
        let info = resp.data[i];
        // 遍历属性 查找moniteritem
        for (var j = 0; j < info.attributeKvList.length; j++){
          let attr = info.attributeKvList[j];
          if (attr.key === 'moniteritem' && attr.value != '') {
            // console.log('监测项:' + attr.value + ' ' + info.device.type);
            let code_idx = getMonitorItemIdxByCode(attr.value, jsonMonitorItem);
            
            if (-1 != code_idx){
              let monitorItemName = jsonMonitorItem[code_idx].name;
              let idx = findExist(monitorItemName, info.device.type, data);
              if (-1 != idx) {
                data[idx].measurePointCnt += 1;
              } else {
                let _dt = {monitorItem:monitorItemName, sensorType:info.device.type, unit:'度', measurePointCnt:1};
                data.push(_dt);
              }
            }
          }
        }
      }
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });

    return data;
}



// 获取数据
async function getCableForceAnalysisStatsData(req, assetId){
  let data = [
    {no:1, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'8',  designValue:'5', threshold:'10', perOverThreshold:'20%'},
    {no:2, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'20', designValue:'10', threshold:'20', perOverThreshold:'30%'},
    {no:3, occurrenceTime:'索力数据量统计分析测试数据 底层数据API还未实现', measureValue:'30', designValue:'15', threshold:'30', perOverThreshold:'50%'}
  ];

  return data;
}


// 获取数据
async function getDeflectometerMonitorInfoStatsData(req, assetId){
  let data = [
    {no:1, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'10', preMonitorValue:'10', NowMonitorValue:'15', changeValue:'1', rateOfChange:'10%', warningLevel:'蓝色'},
    {no:2, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'12', NowMonitorValue:'15', changeValue:'1', warningLevel:'12%', warningLevel:'黄色'},
    {no:3, monitorName:'主拱圈挠度自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'25', NowMonitorValue:'20', changeValue:'1', warningLevel:'20%', warningLevel:'红色'}
  ];

  return data;
}


// 获取数据
async function getDeflectometerData(req, assetId){
  let data = [
    {no:1, deviceNo:'主拱圈挠度计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注1',},
    {no:2, deviceNo:'主拱圈挠度计信息汇总测试数据 底层数据API还未实现', installPosition:'桥墩', comment:'备注2'},
    {no:3, deviceNo:'主拱圈挠度计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注3'}
  ];

  return data;
}


// 获取数据
async function getCrackMeterInfoStatsData(req, assetId){
  let data = [
    {no:1, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注1',},
    {no:2, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥墩', comment:'备注2'},
    {no:3, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注3'}
  ];

  return data;
}

// 获取数据
async function getVehicleLoadStatsData(req, assetId){
  let data = [
    {no:1, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'1车道', level:'1级'},
    {no:2, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'2车道', level:'1级'},
    {no:3, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'3车道', level:'1级'}
  ];

  return data;
}

// 获取数据
async function getCrackMonitoInfoStatsData(req, assetId){
  let data = [
    {no:1, monitorName:'裂缝发展自动监测统计表测试数据 底层数据API还未实现', initialValue:'10', preMonitorValue:'10', NowMonitorValue:'15', changeValue:'1', rateOfChange:'10%', warningLevel:'蓝色'},
    {no:2, monitorName:'裂缝发展自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'12', NowMonitorValue:'15', changeValue:'1', warningLevel:'12%', warningLevel:'黄色'},
    {no:3, monitorName:'裂缝发展自动监测统计表测试数据 底层数据API还未实现', initialValue:'20', preMonitorValue:'25', NowMonitorValue:'20', changeValue:'1', warningLevel:'20%', warningLevel:'红色'}
  ];

  return data;
}

// 监测项目及测点布置
 const MonitorItemAndItem = new GraphQLObjectType({
  name: 'monitorItemAndMesurePoint',
  fields: {
    // 监测项目
    monitorItem: {
      type: GraphQLString
    },
    // 传感器类型
    sensorType: {
      type: GraphQLString
    },
    // 单位
    unit: {
      type: GraphQLString
    },
    // 测点数量
    measurePointCnt:{
      type: GraphQLInt
    }
  }
});


 // 索力数据量统计分析
 const cableForceAnalysisStats = new GraphQLObjectType({
  name: 'cableForceAnalysisStats',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 发生时间
    occurrenceTime: {
      type: GraphQLString
    },
    // 测值
    measureValue: {
      type: GraphQLString
    },
    // 设计值
    designValue:{
      type: GraphQLString
    },
    // 阈值
    threshold:{
      type: GraphQLString
    },
    //超阈值百分比
    perOverThreshold:{
       type: GraphQLString
    }
  }
});

 // 车辆荷载数据量统计分析
 const vehicleLoadStatics = new GraphQLObjectType({
  name: 'vehicleLoadStatics',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 发生时间
    occurrenceTime: {
      type: GraphQLString
    },
    // 车速/车重
    vel: {
      type: GraphQLString
    },
    // 车重
    load:{
      type: GraphQLString
    },
    // 所在车道
    driveway:{
      type: GraphQLString
    },
    //超限等级
    level:{
       type: GraphQLString
    }
  }
});


 // 裂缝计信息汇总
 const crackMeterInfoStats = new GraphQLObjectType({
  name: 'crackMeterInfoStats',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 仪器编号
    deviceNo: {
      type: GraphQLString
    },
    // 安装位置
    installPosition: {
      type: GraphQLString
    },
    // 工作状态
    workingStat:{
      type: GraphQLString
    },
    // 备注
    comment:{
      type: GraphQLString
    }
  }
});

 // 裂缝发展自动监测统计表
 const crackMonitoInfoStats = new GraphQLObjectType({
  name: 'crackMonitoInfoStats',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 监测名称
    monitorName: {
      type: GraphQLString
    },
    // 初始值
    initialValue: {
      type: GraphQLString
    },
    // 上次监测值
    preMonitorValue:{
      type: GraphQLString
    },
    // 本次监测值
    NowMonitorValue:{
      type: GraphQLString
    },
     // 变化值
    changeValue:{
      type: GraphQLString
    },
     // 变化率
     rateOfChange:{
      type: GraphQLString
    },
    // 预警级别
    warningLevel:{
      type: GraphQLString
    },
  }
});

 // 主拱圈挠度自动监测统计
 const deflectometerInfo = new GraphQLObjectType({
  name: 'deflectometerInfo',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 仪器编号
    deviceNo: {
      type: GraphQLString
    },
    // 安装位置
    installPosition: {
      type: GraphQLString
    },
    // 工作状态
    workingStat:{
      type: GraphQLString
    },
    // 备注
    comment:{
      type: GraphQLString
    }
  }
});

 // 
 const deflectometerMonitorInfo = new GraphQLObjectType({
  name: 'deflectometerMonitorInfo',
  fields: {
    // 序号
    no: {
      type: GraphQLString
    },
    // 监测名称
    monitorName: {
      type: GraphQLString
    },
    // 初始值
    initialValue: {
      type: GraphQLString
    },
    // 上次监测值
    preMonitorValue:{
      type: GraphQLString
    },
    // 本次监测值
    NowMonitorValue:{
      type: GraphQLString
    },
     // 变化值
    changeValue:{
      type: GraphQLString
    },
     // 变化率
     rateOfChange:{
      type: GraphQLString
    },
    // 预警级别
    warningLevel:{
      type: GraphQLString
    },
  }
});

const queryObj = new GraphQLObjectType({
name: 'MonitorItemAndItemQuery',
description: '监测项目及测点布置',
fields: {
    monitorData: {
      type: new GraphQLList(MonitorItemAndItem),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getMonitorData(req, assetId);
      }
    },
    // 车辆荷载数据量统计
    vehicleLoadStaticsData: {
      type: new GraphQLList(vehicleLoadStatics),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getVehicleLoadStatsData(req, assetId);
      }
    },
    // 索力数据量统计分析
    cableForceAnalysisStatsData: {
      type: new GraphQLList(cableForceAnalysisStats),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getCableForceAnalysisStatsData(req, assetId);
      }
    },
    //  自动监测报表
    autoMonitorData: {
      type: new GraphQLList(MonitorItemAndItem),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getMonitorData(req, assetId);
      }
    },
    //裂缝计信息汇总
    crackMonitoInfoStatsData: {
      type: new GraphQLList(crackMonitoInfoStats),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getCrackMonitoInfoStatsData(req, assetId);
      }
    },
    crackMeterInfoStatsData: {
      type: new GraphQLList(crackMeterInfoStats),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getCrackMeterInfoStatsData(req, assetId);
      }
    },
    deflectometerInfoStatsData: {
      type: new GraphQLList(deflectometerInfo),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getDeflectometerData(req, assetId);
      }
    },
    deflectometerMonitorInfoStatsData: {
      type: new GraphQLList(deflectometerMonitorInfo),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getDeflectometerMonitorInfoStatsData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
