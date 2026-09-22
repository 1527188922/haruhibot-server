#!/bin/bash
java -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="${1:-6005}" haruhibotServer.jar
