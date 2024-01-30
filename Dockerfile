FROM steelhousedev/slim-run-11:1.0

ENV APPLICATION smart-pixel-config-service
COPY ./build/distributions/smart-pixel-config-service*.zip ./
RUN rm smart-pixel-config-service-boot*.zip
RUN unzip smart-pixel-config-service*.zip && rm smart-pixel-config-service*.zip