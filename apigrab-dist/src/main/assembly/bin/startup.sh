#!/usr/bin/env bash
#
# API Grab 启动脚本
#
#############################################
# !!!!!! 需要修改启动主类时改这里
APP_MAIN="com.gitee.apigrab.admin.ApigrabApplication"
#############################################

APP_HOME="${BASH_SOURCE-$0}"
APP_HOME="$(dirname "${APP_HOME}")"
APP_HOME="$(cd "${APP_HOME}"; pwd)"
APP_HOME="$(cd "$(dirname ${APP_HOME})"; pwd)"

APP_BIN_PATH=$APP_HOME/bin
APP_LIB_PATH=$APP_HOME/lib
APP_CONF_PATH=$APP_HOME/conf

APP_PID_FILE="${APP_HOME}/run/${APP_MAIN}.pid"
APP_RUN_LOG="${APP_HOME}/logs/console.log"

[ -d "${APP_HOME}/run" ] || mkdir -p "${APP_HOME}/run"
[ -d "${APP_HOME}/logs" ] || mkdir -p "${APP_HOME}/logs"
cd ${APP_HOME}

# JVM 参数，可通过环境变量 JAVA_OPTS 覆盖
if [ -z "${JAVA_OPTS}" ]; then
  JAVA_OPTS="-server -Xms1024m -Xmx2048m -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
fi

# 优先使用 JAVA_HOME，其次依赖 PATH 上的 java（需 Java 8）
if [ "$JAVA_HOME" != "" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

# 把 conf 目录与 lib 下所有 jar 加入 classpath（conf 在最前，外置配置优先生效）
CLASSPATH=$APP_CONF_PATH
for i in $APP_LIB_PATH/*.jar
do
	CLASSPATH="$i:$CLASSPATH"
done

res=`ps aux | grep java | grep $APP_HOME | grep $APP_MAIN | grep -v grep | awk '{print $2}'`
if [ -n "$res" ]; then
  echo "$res program is already running"
  exit 1
fi

echo -n `date +'%Y-%m-%d %H:%M:%S'` >>${APP_RUN_LOG}
echo "---- Start service [${APP_MAIN}] process. ">>${APP_RUN_LOG}

nohup $JAVA -cp "$CLASSPATH" $JAVA_OPTS $APP_MAIN --spring.config.additional-location=file:${APP_CONF_PATH}/ >>${APP_RUN_LOG} 2>&1 &

RETVAL=$?
PID=$!
echo ${PID} >${APP_PID_FILE}
echo "Started [${APP_MAIN}] pid=${PID}, log=${APP_RUN_LOG}"
exit ${RETVAL}
