FROM jenkins/jenkins:lts-jdk17

USER root

# docker CLI and AWS CLI (to log in to ECR)
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io awscli \
    && rm -rf /var/lib/apt/lists/*