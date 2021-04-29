package com.kryptokrauts.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.entity.RestChannel;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DiscordPostingService {

  public static final Snowflake CHANNEL_ID_STATISTICS_AENALYTICS =
      Snowflake.of(702871548872228994l);

  GatewayDiscordClient gateway;

  public DiscordPostingService(@ConfigProperty(name = "discord.token") String token) {
    DiscordClient client = DiscordClient.create(token);
    this.gateway = client.login().block();
  }

  public void sendMessage(Channel channel, String message) {
    RestChannel restChannel =
        this.gateway.getChannelById(channel.getChannelId()).block().getRestChannel();
    MessageData messageData = restChannel.createMessage(message).block();
  }
}
