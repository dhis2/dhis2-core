FROM ubuntu:24.04 AS devcontainer-ubuntu

ARG TZ
ENV TZ="$TZ"
ENV DEBIAN_FRONTEND=noninteractive

# Create non-root user
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID

# Check if this user exists to satisfy both Ubuntu and Debian images
RUN apt-get update && apt-get install -y --no-install-recommends locales sudo zsh \
    && locale-gen en_US.UTF-8 \
    && EXISTING_USER=$(getent passwd "$USER_UID" | cut -d: -f1) \
    && if [ -n "$EXISTING_USER" ]; then \
           if [ "$EXISTING_USER" != "$USERNAME" ]; then \
               usermod  -l "$USERNAME" "$EXISTING_USER"; \
               usermod  -d "/home/$USERNAME" -m "$USERNAME"; \
               groupmod -n "$USERNAME" "$EXISTING_USER"; \
           fi; \
           usermod -s /usr/bin/zsh "$USERNAME"; \
       else \
           groupadd --gid "$USER_GID" "$USERNAME"; \
           useradd --uid "$USER_UID" --gid "$USER_GID" -m -s /usr/bin/zsh "$USERNAME"; \
       fi \
    && echo "$USERNAME ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

RUN apt-get install -y --no-install-recommends \
    aggregate \
    bash \
    bash-completion \
    build-essential \
    ca-certificates \
    curl \
    diffutils \
    dnsutils \
    eslint \
    fd-find \
    file \
    fzf \
    gawk \
    gh \
    git \
    git-delta \
    gnupg2 \
    iproute2 \
    ipset \
    iptables \
    isort \
    jq \
    less \
    lsof \
    make \
    man-db \
    postgresql \
    postgresql-contrib \
    postgresql-postgis \
    postgresql-postgis-scripts \
    procps \
    ripgrep \
    sed \
    shellcheck \
    socat \
    strace \
    tree \
    unzip \
    vim \
    wget \
    yamllint \
    zip \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install opengrep (semgrep OSS fork). Pin version + SHA256 — the image build
# fails if the binary ever changes upstream. Single static binary, no runtime deps.
ARG OPENGREP_VERSION=1.19.0
ARG OPENGREP_SHA256=1d69a41beb88e8e7917f26cc6a16c1edf298f31402807e6d1afbb5d8684c3590
RUN curl -fsSL "https://github.com/opengrep/opengrep/releases/download/v${OPENGREP_VERSION}/opengrep_manylinux_x86" -o /usr/local/bin/opengrep \
    && echo "${OPENGREP_SHA256}  /usr/local/bin/opengrep" | sha256sum -c - \
    && chmod +x /usr/local/bin/opengrep

# Set `DEVCONTAINER` environment variable to help with orientation
ENV DEVCONTAINER=true
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Fix shell for NVM
RUN ln -sf bash /usr/bin/sh

# Create workspace and config directories and set permissions
RUN mkdir -p /workspaces /home/$USERNAME/.claude /home/$USERNAME/.config
RUN echo 'export PATH="$HOME/.local/bin:$PATH"' >> /home/$USERNAME/.bashrc
RUN echo 'export PATH="$HOME/.local/bin:$PATH"' >> /home/$USERNAME/.zshrc
RUN chown -R $USERNAME:$USERNAME /workspaces /home/$USERNAME/.claude /home/$USERNAME/.config

WORKDIR /workspaces

# Set up non-root user
USER $USERNAME
