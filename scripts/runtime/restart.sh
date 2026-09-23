#!/bin/bash
currentPath=$( cd "$( dirname "$0" )" && pwd )
echo "Current path is ${currentPath}"
xmlFile="${currentPath}/haruhibot_service.xml"
echo "XML file is ${xmlFile}"
SERVICE_NAME=$(sed -n 's/.*<id>\(.*\)<\/id>.*/\1/p' "${xmlFile}" | tr -d '\000-\037\177-\377' | sed 's/？//g')
echo "Service name is ${SERVICE_NAME}"
if systemctl is-active --quiet "${SERVICE_NAME}".service; then
ARGUMENTS=$(sed -n 's/.*<arguments>\(.*\)<\/arguments>.*/\1/p' "${xmlFile}" | sed 's/？//g')
echo "Arguments is ${ARGUMENTS}"
_JAVA_HOME="${1:-$JAVA_HOME}"
echo "JAVA_HOME is ${_JAVA_HOME}"
cat>/usr/lib/systemd/system/"${SERVICE_NAME}".service<<EOF
[Unit]  
Description=${SERVICE_NAME}
After=network.target
   
[Service]  
Type=simple  
WorkingDirectory=${currentPath}
ExecStart=${_JAVA_HOME}/bin/java ${ARGUMENTS}
Restart=always  
User=root
    
[Install]  
WantedBy=multi-user.target  
EOF
systemctl daemon-reload && systemctl restart "${SERVICE_NAME}".service
else
    echo "Service ${SERVICE_NAME} is not active."  
fi
