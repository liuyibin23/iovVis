//车辆荷载数据量统计分析
const axios = require('axios');
const util = require('../../../../util/utils');
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

// 获取数据
async function getVehicleLoadStatsData(req, assetId){
  let data = [
    {no:1, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'1车道', level:'1级'},
    {no:2, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'2车道', level:'1级'},
    {no:3, occurrenceTime:'车辆荷载数据量统计分析测试数据 底层数据API还未实现', vel:'5 m/s', load:'20T', driveway:'3车道', level:'1级'}
  ];

  return data;
}

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

const queryObj = new GraphQLObjectType({
name: 'vehicleLoadStaticsQuery',
description: '车辆荷载数据量统计',
fields: {
    vehicleLoadStaticsData: {
      type: new GraphQLList(vehicleLoadStatics),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getVehicleLoadStatsData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
