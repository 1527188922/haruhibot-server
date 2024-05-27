#!/bin/bash
currentPath=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
echo "Current path is ${currentPath}"
xmlFile="${currentPath}/haruhibot_service.xml"
echo "XML file is ${xmlFile}"
SERVICE_NAME=$(sed -n 's/.*<id>\(.*\)<\/id>.*/\1/p' "${xmlFile}" | tr -d '\000-\037\177-\377' | sed 's/？//g')
echo "Service name is ${SERVICE_NAME}"
systemctl stop "${SERVICE_NAME}"
systemctl disable "${SERVICE_NAME}".service
rm -f /usr/lib/systemd/system/"${SERVICE_NAME}".service
systemctl daemon-reload
