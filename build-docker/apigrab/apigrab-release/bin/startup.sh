#!/bin/sh
#
# API Grab 容器启动脚本（前台运行）
#
#############################################
# !!!!!! 需要修改启动主类时改这里
APP_MAIN="com.gitee.apigrab.admin.ApigrabApplication"
#############################################

APP_HOME="${BASH_SOURCE-$0}"
APP_HOME="$(dirname "${APP_HOME}")"
APP_HOME="$(cd "${APP_HOME}"; pwd)"
APP_HOME="$(cd "$(dirname ${APP_HOME})"; pwd)"

APP_LIB_PATH=$APP_HOME/lib
APP_CONF_PATH=$APP_HOME/conf

# JVM 参数，可通过环境变量 JAVA_OPTS 覆盖
if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-server -Xms1024m -Xmx2048m -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
fi

if [ "$JAVA_HOME" != "" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

# conf 目录在最前，lib 下所有 jar 依次加入 classpath
CLASSPATH=$APP_CONF_PATH
for i in $APP_LIB_PATH/*.jar
do
	CLASSPATH="$i:$CLASSPATH"
done

# 前台运行；exec 让 java 成为 PID 1，便于接收 docker stop 的信号
exec $JAVA -cp "$CLASSPATH" $JAVA_OPTS $APP_MAIN --spring.config.additional-location=file:${APP_CONF_PATH}/
