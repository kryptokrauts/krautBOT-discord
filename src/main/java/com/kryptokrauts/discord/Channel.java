package com.kryptokrauts.discord;

import discord4j.common.util.Snowflake;

public enum Channel {
  STATISTICS_AENALYTICS(Snowflake.of(702871548872228994l)),
  STATISTICS_KRYPTOKRAUTS(Snowflake.of(702871493654347877l));

  private Snowflake channelId;

  Channel(Snowflake channelId) {
    this.channelId = channelId;
  }

  Snowflake getChannelId() {
    return this.channelId;
  }
}
