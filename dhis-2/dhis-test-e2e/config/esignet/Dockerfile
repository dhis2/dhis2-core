# AI Assisted
# @author Morten Svanæs <msvanaes@dhis2.org>
# v2.0.0 plus ONLY the reviewed external-RSA JWKS metadata correction.
# Retain the released runtime image and configuration; do not replace the IdP with a stub.
FROM golang:1.26.0-bookworm AS build
WORKDIR /src
ADD --checksum=sha256:9c49f38087288654a7a4e107eff4d4da7b7cd2198e6578a98724e0d30fefc4fa https://codeload.github.com/mosip/esignet/tar.gz/db528423dac7898ad546cb7b2f5f867a949676a7 /tmp/esignet.tar.gz
RUN tar -xzf /tmp/esignet.tar.gz --strip-components=1
COPY external-rsa-jwks.patch /tmp/external-rsa-jwks.patch
RUN git apply /tmp/external-rsa-jwks.patch
WORKDIR /src/esignet-service
RUN CGO_ENABLED=0 go build -trimpath -ldflags='-s -w' -o /out/esignet.exe ./cmd/esignet

FROM mosipid/esignet:2.0.0
LABEL org.opencontainers.image.description="eSignet v2.0.0 + external RSA JWKS alg correction (DHIS2 E2E)"
COPY --from=build /out/esignet.exe /home/mosip/esignet.exe
