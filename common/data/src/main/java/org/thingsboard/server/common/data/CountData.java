package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回值数量统计值。
 * Created by ztao at 2019/4/22 10:52.
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class CountData {

    private Long count;
}
