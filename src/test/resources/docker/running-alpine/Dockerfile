# Image with infinite running alpine
FROM alpine:3.9
COPY infinite-loop.sh .
RUN ["chmod","777","infinite-loop.sh"]
ENTRYPOINT ["sh", "infinite-loop.sh"]