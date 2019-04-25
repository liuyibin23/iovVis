package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.historyvideo.HistoryVideo;

@RestController
@RequestMapping("/api")
@Slf4j
public class HistoryVideoController extends BaseController {
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/historyVideo", method = RequestMethod.POST)
	@ResponseBody
	public HistoryVideo saveHistoryVideo(){


		return null;
	}
}
