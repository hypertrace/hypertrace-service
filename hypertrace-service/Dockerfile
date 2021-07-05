ARG HYPERTRACE_UI_VERSION
FROM hypertrace/hypertrace-ui:${HYPERTRACE_UI_VERSION} AS ui

FROM razorpay/java:11
LABEL maintainer="Hypertrace 'https://www.hypertrace.org/'"
WORKDIR /app
COPY build/docker/libs libs/
COPY build/docker/resources resources/
COPY build/docker/classes classes/
COPY --from=ui /usr/share/nginx/html resources/hypertrace-ui
COPY docker_entrypoint.sh docker_entrypoint.sh
ENTRYPOINT ["/app/docker_entrypoint.sh"]
EXPOSE 9001 9002 2020
HEALTHCHECK --interval=2s --start-period=15s --timeout=2s CMD wget -qO- http://127.0.0.1:9002/health &> /dev/null || exit 1
ENV SERVICE_NAME=hypertrace-service JAVA_OPTS=""