package org.ybuka.confluence.plugins.sitemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ybuka.confluence.plugins.sitemap.SiteMapComponent.TaskStatus;

public class SiteMapStatusAction extends SiteMapAdminAction {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SiteMapStatusAction.class);

	public SiteMapStatusAction() {
	}

	@Override
	public String execute() throws Exception {

		return SUCCESS;
	}

	public TaskStatus getLastTaskStatus() {
		return siteMapGenerator.getLastFinishedStatus();
	}

	public TaskStatus getStatus() {
		return siteMapGenerator.getStatus();
	}
	
	public String download(){
		return SUCCESS;
	}

	public InputStream getInputStream() {
		File f = new File(siteMapGenerator.retrieveAbsoluteFileLocation());
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			log.error(e.toString(), e);
		}
		return inputStream;
	}
	
	public boolean isSitemapExist(){
		File f = new File(siteMapGenerator.retrieveAbsoluteFileLocation());
		return f.isFile() & f.exists();
	}
	
	public String getScheduledJobPageURI(){
		return this.getGlobalSettings().getBaseUrl() + "/admin/scheduledjobs/viewscheduledjobs.action";
	}

}
