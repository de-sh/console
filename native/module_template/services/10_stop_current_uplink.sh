#!/system/bin/sh

pkill -x uplink_watchdog
pkill -x uplink

# check for 10s if uplink has stopped
# do a pkill -9 -x uplink if it's still running

KILLED=false
for i in $(seq 1 10); do
  if ! pgrep -x uplink; then
    echo uplink has stopped
    KILLED=true
    break
  fi
  sleep 1
done

if [ $KILLED = false ]; then
  echo uplink still running, sending sigkill
  pkill -9 -x uplink
fi


