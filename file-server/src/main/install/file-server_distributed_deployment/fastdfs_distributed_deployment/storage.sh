#!/bin/sh
if [ ! -f "storage.conf.bak" ]
then
	cp /etc/fdfs/storage.conf storage.conf.bak
fi
sed "s/^.*tracker_server=.*$/tracker_server=$TRACKER_IP${TRACKER_IP1:+\ntracker_server=${TRACKER_IP1}}/" storage.conf.bak > storage.conf
sed "s/^.*group_name=.*$/group_name=$GROUP_NAME/" storage.conf > storage_.conf
cp storage_.conf /etc/fdfs/storage.conf
/data/fastdfs/storage/fdfs_storaged /etc/fdfs/storage.conf
if [ ! -f "mod_fastdfs.conf.bak" ]
then
	cp /etc/fdfs/mod_fastdfs.conf mod_fastdfs.conf.bak
fi
sed "s/^.*tracker_server=.*$/tracker_server=$TRACKER_IP${TRACKER_IP1:+\ntracker_server=${TRACKER_IP1}}/" mod_fastdfs.conf.bak > mod_fastdfs.conf
sed "s/^.*group_name=.*$/group_name=$GROUP_NAME/" mod_fastdfs.conf > mod_fastdfs_.conf
cp mod_fastdfs_.conf /etc/fdfs/mod_fastdfs.conf
/etc/nginx/sbin/nginx
tail -f /data/fast_data/logs/storaged.log
