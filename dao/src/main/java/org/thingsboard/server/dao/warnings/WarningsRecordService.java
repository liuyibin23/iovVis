package org.thingsboard.server.dao.warnings;

import org.thingsboard.server.common.data.warnings.WarningsRecord;

import java.util.List;
import java.util.UUID;

public interface WarningsRecordService {
	WarningsRecord save(WarningsRecord warningsRecord);
	List<WarningsRecord> findWarningByAssetId(UUID assetId);
}
