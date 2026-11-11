#!/bin/sh

set -e

APIGRAB_VERSION=1.0.0

# 镜像名称与仓库，可通过环境变量覆盖
IMAGE_NAME=${IMAGE_NAME:-apigrab}
IMAGE_TAG=${IMAGE_TAG:-${APIGRAB_VERSION}}
DOCKER_REGISTRY=${DOCKER_REGISTRY:-registry.cn-hangzhou.aliyuncs.com}
DOCKER_NAMESPACE=${DOCKER_NAMESPACE:-inrgihc}

BUILD_DOCKER_DIR="$( cd "$( dirname "$0"  )" && pwd  )"
PROJECT_ROOT_DIR=$( dirname "$BUILD_DOCKER_DIR")
DOCKER_APIGRAB_DIR=$BUILD_DOCKER_DIR/apigrab

# 1. 构建项目（产出 target/apigrab-release-<version>.tar.gz）
cd $PROJECT_ROOT_DIR && sh docker-maven-build.sh && cd -

# 2. 从发行包中取运行依赖 jar 与外部 JDBC 驱动（国产库）放入镜像构建目录
cd $BUILD_DOCKER_DIR \
 && tar zxvf $PROJECT_ROOT_DIR/target/apigrab-release-${APIGRAB_VERSION}.tar.gz -C /tmp \
 && cp /tmp/apigrab-release-${APIGRAB_VERSION}/lib/* ${BUILD_DOCKER_DIR}/apigrab/apigrab-release/lib/ \
 && mkdir -p ${BUILD_DOCKER_DIR}/apigrab/apigrab-release/drivers \
 && cp -r /tmp/apigrab-release-${APIGRAB_VERSION}/drivers/. ${BUILD_DOCKER_DIR}/apigrab/apigrab-release/drivers/ \
 && rm -rf /tmp/apigrab-release-*

# 3. 构建镜像
cd ${DOCKER_APIGRAB_DIR} \
  && tar zcvf apigrab-release.tar.gz apigrab-release/ \
  && docker build -t ${IMAGE_NAME}:${IMAGE_TAG} . \
  && rm -f apigrab-release.tar.gz \
  && rm -f apigrab-release/lib/*.jar \
  && rm -rf apigrab-release/drivers

# 4. 清理项目
cd $PROJECT_ROOT_DIR && sh docker-maven-clean.sh && cd -

# 5. 推送镜像（版本号 + latest）
docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:${IMAGE_TAG}
docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:latest
docker push ${DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${IMAGE_NAME}:latest
