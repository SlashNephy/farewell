# farewell

👋 A tiny tool to notify when you are unfollowed or blocked by your followers on Twitter

[![Kotlin](https://img.shields.io/badge/Kotlin-1.4.30-blue)](https://kotlinlang.org)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/SlashNephy/farewell)](https://github.com/SlashNephy/farewell/releases)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/SlashNephy/farewell/Docker)](https://hub.docker.com/r/slashnephy/farewell)
[![Docker Image Size (tag)](https://img.shields.io/docker/image-size/slashnephy/farewell/latest)](https://hub.docker.com/r/slashnephy/farewell)
[![Docker Pulls](https://img.shields.io/docker/pulls/slashnephy/farewell)](https://hub.docker.com/r/slashnephy/farewell)
[![license](https://img.shields.io/github/license/SlashNephy/farewell)](https://github.com/SlashNephy/farewell/blob/master/LICENSE)
[![issues](https://img.shields.io/github/issues/SlashNephy/farewell)](https://github.com/SlashNephy/farewell/issues)
[![pull requests](https://img.shields.io/github/issues-pr/SlashNephy/farewell)](https://github.com/SlashNephy/farewell/pulls)

## Requirements

- Java 8 or later

## Get Started

### Docker

There are some image tags.

- `slashnephy/farewell:latest`  
  Automatically published every push to `master` branch.
- `slashnephy/farewell:dev`  
  Automatically published every push to `dev` branch.
- `slashnephy/farewell:<version>`  
  Coresponding to release tags on GitHub.

`docker-compose.yml`

```yaml
version: '3.8'

services:
  farewell:
    container_name: farewell
    image: slashnephy/farewell:latest
    restart: always
    environment:
      # Twitter の資格情報 (必須)
      TWITTER_CK: xxx
      TWITTER_CS: xxx
      TWITTER_AT: xxx
      TWITTER_ATS: xxx
      
      # チェック間隔
      INTERVAL_SECONDS: 60
      # Discord Webhook URL (必須)
      DISCORD_WEBHOOK_URL: https://xxx
```
