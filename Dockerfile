FROM hseeberger/scala-sbt
ADD . /root
RUN  sbt stage
CMD /root/target/universal/stage/bin/ocs-api
