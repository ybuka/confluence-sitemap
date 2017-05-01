package org.ybuka.confluence.plugins.sitemap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ybuka.confluence.plugins.sitemap.SiteMapComponent.TaskStatus;

import com.opensymphony.webwork.ServletActionContext;

public class SiteMapStatusAction extends SiteMapAdminAction {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(SiteMapStatusAction.class);

	public SiteMapStatusAction() {
	}

	@Override
	public String execute() throws Exception {
		
		TaskStatus ts =  getLastTaskStatus();
		if(ts !=null && org.apache.commons.lang3.StringUtils.isNoneBlank(ts.getErrorMssg())){
			addActionError("Last generation of sitemap is failed. " + ts.getErrorMssg());
		}		
		return SUCCESS;
	}

	public TaskStatus getLastTaskStatus() {
		return siteMapGenerator.getLastFinishedStatus();
	}

	public TaskStatus getStatus() {
		return siteMapGenerator.getStatus();
	}
	
	public String download(){
		setHeaders();
		return SUCCESS;
	}

	public InputStream getInputStream() {
		File f = new File(siteMapGenerator.retrieveOutputFileLocation());
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			log.error(e.toString(), e);
		}
		return inputStream;
	}
	
	public boolean isSitemapExist(){
		File f = new File(siteMapGenerator.retrieveOutputFileLocation());
		return f.isFile() & f.exists();
	}
	
	public String getScheduledJobPageURI(){
		return this.getGlobalSettings().getBaseUrl() + "/admin/scheduledjobs/viewscheduledjobs.action#siteMapGeneratorJobId";
	}
	
	protected void setHeaders() {
	    final HttpServletResponse response = ServletActionContext.getResponse();	
	    //The Content-Disposition could not be set using atlassian xwork, so -  set it here
	    response.addHeader("Content-Disposition", "attachment; filename=sitemap.xml");
	  }
}
