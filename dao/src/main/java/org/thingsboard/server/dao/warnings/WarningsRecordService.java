package org.thingsboard.server.dao.warnings;

import org.thingsboard.server.common.data.warnings.WarningsRecord;

public interface WarningsRecordService {
	WarningsRecord save(WarningsRecord warningsRecord);
}
