package org.thingsboard.server.service.device;

import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface DeviceBaseAttributeService {
	DeviceAutoLogon saveDeviceAttribute(Device device, DeviceAutoLogon deviceAutoLogon) throws IOException, ThingsboardException;
	DeviceAutoLogon findDeviceAttribute(Device device) throws ExecutionException, InterruptedException, IOException;
}
