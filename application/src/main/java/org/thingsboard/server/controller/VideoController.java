package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.VideoInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.Authority;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Slf4j
public class VideoController extends BaseController{
	/**
	* @Description: 1.2.18.1 新增/修改视频信息
	* @Author: ShenJi
	* @Date: 2019/4/2
	* @Param: [groupType, groupId, videoInfo]
	* @return: org.thingsboard.server.common.data.VideoInfo
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/videoInfo", method = RequestMethod.POST)
	@ResponseBody
	VideoInfo saveVideoInfo(@RequestParam Authority groupType,
							@RequestParam String groupId,
							@RequestBody VideoInfo videoInfo) throws ThingsboardException {
		try {
			checkAuthority(groupType,groupId);

			videoInfo.setGroupId(UUID.fromString(groupId));
			return videoInfoService.saveVideoInfo(videoInfo);
		} catch (Exception e) {
			throw handleException(e);
		}

	}

	/**
	* @Description: 1.2.18.2 查询视频信息
	* @Author: ShenJi
	* @Date: 2019/4/2
	* @Param: [groupType, groupId]
	* @return: org.thingsboard.server.common.data.VideoInfo
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/videoInfo", method = RequestMethod.GET)

	VideoInfo getVideoInfo(@RequestParam Authority groupType,
							@RequestParam String groupId) throws ThingsboardException {
		try {
			checkAuthority(groupType,groupId);
			return videoInfoService.findVideoInfoByGroupId(UUID.fromString(groupId));
		} catch (Exception e) {
			throw handleException(e);
		}

	}


}
