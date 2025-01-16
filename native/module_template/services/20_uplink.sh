#!/system/bin/sh

$MODULE_DIR/bin/daemonize sh -c\
 "$MODULE_DIR/bin/uplink -a $DATA_DIR/device.json -c $MODULE_DIR/etc/uplink.config.toml -v 2>&1 | $MODULE_DIR/bin/logrotate --max-size 10000000 --backup-files-count 30 --output $DATA_DIR/out.log"