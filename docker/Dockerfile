FROM eclipse-temurin:25-jdk

ARG ANDROID_CLI_TOOLS=commandlinetools-linux-15859902_latest.zip
ARG GRADLE_VERSION=9.8.1

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates unzip wget \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && wget -q "https://dl.google.com/android/repository/$ANDROID_CLI_TOOLS" -O /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools" \
    && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses > /dev/null \
    && sdkmanager --sdk_root="$ANDROID_HOME" --install "platform-tools" "platforms;android-37.0" "build-tools;36.0.0" > /dev/null \
    && chmod -R a+rwX "$ANDROID_HOME"

RUN wget -q "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s "/opt/gradle-$GRADLE_VERSION/bin/gradle" /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

WORKDIR /workspace
ENTRYPOINT ["gradle", "--no-daemon"]
CMD ["assembleDebug"]
