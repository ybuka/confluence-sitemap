package org.ybuka.confluence.plugins.sitemap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.confluence.schedule.ScheduledJobKey;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.config.JobId;

public class SiteMapGeneratorJobRunner implements JobRunner {
	private static final Logger log = LoggerFactory.getLogger(SiteMapGeneratorJobRunner.class);

	public static final String JOB_ID = "org.ybuka.confluence.plugins.sitemap:siteMapGeneratorJob";
	private final SiteMapComponent siteMapGenerator;

	public static ScheduledJobKey getJobKey() {
		return ScheduledJobKey.valueOf(JobId.of(JOB_ID));
	}

	public SiteMapGeneratorJobRunner(SiteMapComponent siteMapGenerator) {
		this.siteMapGenerator = siteMapGenerator;
	}

	@Override
	public JobRunnerResponse runJob(JobRunnerRequest request) {
		log.info("Sitemap Job triggered");
		this.siteMapGenerator.generateSiteMap();
		return null;
	}

}
