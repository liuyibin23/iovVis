// 裂缝计信息汇总表
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

// 获取数据
async function getCrackMeterInfoStatsData(req, assetId){
  let data = [
    {no:1, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注1',},
    {no:2, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥墩', comment:'备注2'},
    {no:3, deviceNo:'裂缝计信息汇总测试数据 底层数据API还未实现', installPosition:'桥面', comment:'备注3'}
  ];

  return data;
}

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

const queryObj = new GraphQLObjectType({
name: 'crackMeterInfoStatsQuery',
description: '裂缝计信息汇总',
fields: {
  crackMeterInfoStatsData: {
      type: new GraphQLList(crackMeterInfoStats),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getCrackMeterInfoStatsData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
