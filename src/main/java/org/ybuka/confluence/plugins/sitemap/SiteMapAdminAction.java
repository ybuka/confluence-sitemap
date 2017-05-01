package org.ybuka.confluence.plugins.sitemap;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.schedule.ExecutionStatus;
import com.atlassian.confluence.schedule.ScheduledJobStatus;
import com.atlassian.confluence.schedule.managers.ScheduledJobManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;

public abstract class SiteMapAdminAction extends ConfluenceActionSupport {

	private static final long serialVersionUID = 1L;

	protected ScheduledJobManager scheduledJobManager;
	protected SiteMapComponent siteMapGenerator;

	@Override
	public boolean isPermitted() {
		return this.permissionManager.hasPermission(getAuthenticatedUser(), Permission.ADMINISTER, PermissionManager.TARGET_SYSTEM);
	}

	public boolean isJobRunning() {

		ScheduledJobStatus status = scheduledJobManager.getScheduledJob(SiteMapGeneratorJobRunner.getJobKey());
		if (status != null) {
			ExecutionStatus executionStatus = status.getStatus();
			return ExecutionStatus.RUNNING.equals(executionStatus);
		} else {
			return false;
		}
	}

	public void setScheduledJobManager(ScheduledJobManager scheduledJobManager) {
		this.scheduledJobManager = scheduledJobManager;
	}

	public void setSiteMapGenerator(SiteMapComponent siteMapGenerator) {
		this.siteMapGenerator = siteMapGenerator;
	}

}
