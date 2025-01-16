#!/system/bin/sh

$MODULE_DIR/bin/daemonize sh -c "$MODULE_DIR/bin/uplink_watchdog 2>&1 | $MODULE_DIR/bin/logrotate --max-size 1024000 --backup-files-count 1 --output $DATA_DIR/restart.log"
