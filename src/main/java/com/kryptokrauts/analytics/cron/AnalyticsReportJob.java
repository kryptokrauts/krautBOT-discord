package com.kryptokrauts.analytics.cron;

import com.google.common.base.Splitter;
import com.kryptokrauts.analytics.AnalyticsReportService;
import com.kryptokrauts.analytics.ReportType;
import com.kryptokrauts.discord.Channel;
import com.kryptokrauts.discord.DiscordPostingService;
import io.quarkus.scheduler.Scheduled;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AnalyticsReportJob {

  @Inject AnalyticsReportService analyticsReportService;
  @Inject DiscordPostingService discordPostingService;

  @Scheduled(cron = "{cron.expression}")
  void analyticsToDiscord() {
    System.out.println("Running analytics cronjob ...");
    try {
      String yesterdayStats = analyticsReportService.generateReport(ReportType.YESTERDAY);
      String last7DaysStats = analyticsReportService.generateReport(ReportType.LAST_7_DAYS);
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(yesterdayStats);
      stringBuilder.append(System.lineSeparator());
      stringBuilder.append(last7DaysStats);
      Iterable<String> messages = Splitter.fixedLength(2000).split(stringBuilder.toString());
      messages.forEach(
          msg -> discordPostingService.sendMessage(Channel.STATISTICS_AENALYTICS, msg));
    } catch (Exception e) {
      discordPostingService.sendMessage(
          Channel.STATISTICS_AENALYTICS, "Error generating stats report: " + e.getMessage());
    }
  }
}
