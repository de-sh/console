#!/system/bin/sh

set -x

export MODULE_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"
. $MODULE_DIR/env.sh
mkdir -p $DATA_DIR

cd $DATA_DIR || exit
for service in $MODULE_DIR/services/*; do
  if [ -x "$service" ]; then
    "$service"
  fi
done