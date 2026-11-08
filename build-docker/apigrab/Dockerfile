FROM eclipse-temurin:8-jre-alpine

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ADD apigrab-release.tar.gz  /

EXPOSE      5880

USER        root

WORKDIR /apigrab-release

RUN chmod u+x /apigrab-release/bin/startup.sh

CMD  ["/apigrab-release/bin/startup.sh"]
