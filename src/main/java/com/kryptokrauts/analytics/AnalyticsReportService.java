package com.kryptokrauts.analytics;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.analyticsreporting.v4.AnalyticsReporting;
import com.google.api.services.analyticsreporting.v4.AnalyticsReportingScopes;
import com.google.api.services.analyticsreporting.v4.model.DateRange;
import com.google.api.services.analyticsreporting.v4.model.DateRangeValues;
import com.google.api.services.analyticsreporting.v4.model.Dimension;
import com.google.api.services.analyticsreporting.v4.model.GetReportsRequest;
import com.google.api.services.analyticsreporting.v4.model.GetReportsResponse;
import com.google.api.services.analyticsreporting.v4.model.Metric;
import com.google.api.services.analyticsreporting.v4.model.Report;
import com.google.api.services.analyticsreporting.v4.model.ReportRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AnalyticsReportService {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  @ConfigProperty(name = "analytics.application.name")
  String ANALYTICS_APPLICATION_NAME;

  @ConfigProperty(name = "analytics.view.id")
  String ANALYTICS_VIEW_ID;

  private AnalyticsReporting analyticsReporting;

  /**
   * constructor that initializes an analytics reporting api v4 service object
   *
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public AnalyticsReportService(
      @ConfigProperty(name = "analytics.key.file.location") String keyFileLocation)
      throws GeneralSecurityException, IOException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    GoogleCredential credential =
        GoogleCredential.fromStream(new FileInputStream(keyFileLocation))
            .createScoped(AnalyticsReportingScopes.all());

    // Construct the Analytics Reporting service object.
    analyticsReporting =
        new AnalyticsReporting.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(ANALYTICS_APPLICATION_NAME)
            .build();
  }

  /**
   * Queries the Analytics Reporting API V4 and generates a report for Discord.
   *
   * @return String content of the report
   * @throws IOException
   */
  public String generateReport(ReportType reportType) throws IOException {
    ArrayList<ReportRequest> requests = new ArrayList<ReportRequest>();
    switch (reportType) {
      case YESTERDAY:
        requests.add(visitors("yesterday", "yesterday"));
        break;
      case LAST_7_DAYS:
        requests.add(devices("7DaysAgo", "yesterday"));
        requests.add(browsers("7DaysAgo", "yesterday"));
        requests.add(countries("7DaysAgo", "yesterday"));
        requests.add(topPages("7DaysAgo", "yesterday"));
        requests.add(trafficSources("7DaysAgo", "yesterday"));
        break;
    }
    GetReportsRequest getReport = new GetReportsRequest().setReportRequests(requests);
    GetReportsResponse response = analyticsReporting.reports().batchGet(getReport).execute();
    return ReportType.YESTERDAY.equals(reportType)
        ? createYesterdayMessage(response)
        : createLast7DaysMessage(response);
  }

  private String createYesterdayMessage(GetReportsResponse response) {
    DateRangeValues dateRangeValues =
        response.getReports().stream().findFirst().get().getData().getTotals().stream()
            .findFirst()
            .get();
    StringBuilder builder = new StringBuilder();
    builder.append("**MAINNET | Yesterday metrics**");
    builder.append(System.lineSeparator());
    builder.append(
        "    - Average session duration: "
            + dateRangeValues.getValues().get(2)
            + " seconds / "
            + Double.valueOf(dateRangeValues.getValues().get(2)) / 60
            + " minutes");
    builder.append(System.lineSeparator());
    builder.append("    - Unique sessions: " + dateRangeValues.getValues().get(0));
    builder.append(System.lineSeparator());
    builder.append("    - Bounce rate: " + dateRangeValues.getValues().get(1));
    builder.append(System.lineSeparator());
    return builder.toString();
  }

  private String createLast7DaysMessage(GetReportsResponse response) {
    StringBuilder builder = new StringBuilder();
    builder.append("**MAINNET | Last 7 days metrics**");
    builder.append(System.lineSeparator());
    builder.append("Devices");
    builder.append(System.lineSeparator());
    Report deviceReport = response.getReports().get(0);
    appendReportData(builder, deviceReport);
    builder.append("Browsers");
    builder.append(System.lineSeparator());
    Report browserReport = response.getReports().get(1);
    appendReportData(builder, browserReport);
    builder.append("Countries");
    builder.append(System.lineSeparator());
    Report countryReport = response.getReports().get(2);
    appendReportData(builder, countryReport);
    builder.append("Top Page Views");
    builder.append(System.lineSeparator());
    Report topPagesReport = response.getReports().get(3);
    appendReportData(builder, topPagesReport);
    builder.append("Traffic Sources");
    builder.append(System.lineSeparator());
    Report trafficSourcesReport = response.getReports().get(4);
    appendReportData(builder, trafficSourcesReport);
    return builder.toString();
  }

  private void appendReportData(StringBuilder builder, Report report) {
    report
        .getData()
        .getRows()
        .forEach(
            reportRow -> {
              builder.append(
                  "    - "
                      + reportRow.getDimensions().stream().findFirst().get()
                      + ": "
                      + reportRow.getMetrics().stream().findFirst().get().getValues().stream()
                          .findFirst()
                          .get());
              builder.append(System.lineSeparator());
            });
    builder.append("    -------");
    builder.append(System.lineSeparator());
    builder.append("    Total: " + report.getData().getTotals().get(0).getValues().get(0));
    builder.append(System.lineSeparator());
  }

  private ReportRequest visitors(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric sessions = new Metric().setExpression("ga:sessions").setAlias("sessions");
    Metric bounceRate = new Metric().setExpression("ga:users").setAlias("bounceRate");
    Metric avgSessionDuration =
        new Metric().setExpression("ga:avgSessionDuration").setAlias("avgSessionDuration");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(Arrays.asList(sessions, bounceRate, avgSessionDuration));
  }

  private ReportRequest devices(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric sessions = new Metric().setExpression("ga:sessions").setAlias("sessions");
    Dimension deviceCategory = new Dimension().setName("ga:deviceCategory");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(Arrays.asList(sessions))
        .setDimensions(Arrays.asList(deviceCategory));
  }

  private ReportRequest browsers(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric sessions = new Metric().setExpression("ga:sessions").setAlias("sessions");
    Dimension browser = new Dimension().setName("ga:browser");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(Arrays.asList(sessions))
        .setDimensions(Arrays.asList(browser));
  }

  private ReportRequest countries(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric sessions = new Metric().setExpression("ga:sessions").setAlias("sessions");
    Dimension country = new Dimension().setName("ga:country");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(Arrays.asList(sessions))
        .setDimensions(Arrays.asList(country));
  }

  private ReportRequest topPages(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric pageviews = new Metric().setExpression("ga:pageviews").setAlias("pageviews");
    Dimension pagePath = new Dimension().setName("ga:pagePath");
    Dimension pageTitle = new Dimension().setName("ga:pageTitle");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(Arrays.asList(pageviews))
        .setDimensions(Arrays.asList(pagePath, pageTitle));
  }

  private ReportRequest trafficSources(String start, String end) {
    DateRange dateRange = new DateRange();
    dateRange.setStartDate(start);
    dateRange.setEndDate(end);

    Metric sessions = new Metric().setExpression("ga:sessions").setAlias("sessions");
    Metric pageviews = new Metric().setExpression("ga:pageviews").setAlias("pageviews");
    Metric users = new Metric().setExpression("ga:users").setAlias("users");
    Metric pageviewsPerSession =
        new Metric().setExpression("ga:pageviewsPerSession").setAlias("pageviewsPerSession");
    Metric avgSessionDuration =
        new Metric().setExpression("ga:avgSessionDuration").setAlias("avgSessionDuration");
    Metric exits = new Metric().setExpression("ga:exits").setAlias("exits");

    Dimension source = new Dimension().setName("ga:source");
    Dimension hasSocialSourceReferral = new Dimension().setName("ga:hasSocialSourceReferral");

    return new ReportRequest()
        .setViewId(ANALYTICS_VIEW_ID)
        .setDateRanges(Arrays.asList(dateRange))
        .setMetrics(
            Arrays.asList(
                sessions, pageviews, users, pageviewsPerSession, avgSessionDuration, exits))
        .setDimensions(Arrays.asList(source, hasSocialSourceReferral));
  }
}
