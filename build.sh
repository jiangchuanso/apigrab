#!/bin/sh
#
# API Grab 一键构建：产出 target/apigrab-release-<version>.tar.gz
#
mvn clean package -Dmaven.test.skip=true
