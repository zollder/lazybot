package org.bot;

import java.util.List;

public class SiteInfo
{
	private String domain;
	private String cronUrl;
	private String newCronSecret;
	private String siteName;
	private String category;
	private String tagline;
	private List<String> keys;
	private String keysAsString;
	private String dbName;
	private String siteLink;

	private String dbUsername;
	private String dbPassword;
	private String adminUsername;
	private String adminPassword;
	private String adminEmail;

	public String getCategory()
	{
		return category;
	}
	public void setCategory(String category)
	{
		this.category = category;
	}

	public String getTagline()
	{
		return tagline;
	}
	public void setTagline(String tagline)
	{
		this.tagline = tagline;
	}

	public List<String> getKeys()
	{
		return keys;
	}
	public void setKeys(List<String> keys)
	{
		this.keys = keys;
	}

	public String getDomain()
	{
		return domain != null ? domain : "";
	}
	public void setDomain(String domain)
	{
		this.domain = domain;
	}

	public String getCronUrl()
	{
		return cronUrl != null ? cronUrl : "";
	}
	public void setCronUrl(String cronUrl)
	{
		this.cronUrl = cronUrl;
	}

	public String getNewCronSecret()
	{
		return newCronSecret != null ? newCronSecret : "";
	}
	public void setNewCronSecret(String newCronSecret)
	{
		this.newCronSecret = newCronSecret;
	}

	public String getSiteName()
	{
		return siteName;
	}
	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}

	public String getKeysAsString()
	{
		return keysAsString;
	}
	public void setKeysAsString(String keysAsString)
	{
		this.keysAsString = keysAsString;
	}

	public String getDbName()
	{
		return dbName;
	}
	public void setDbName(String dbName)
	{
		this.dbName = dbName;
	}

	public String getSiteLink()
	{
		return siteLink;
	}
	public void setSiteLink(String siteLink)
	{
		this.siteLink = siteLink;
	}

	public String getDbUsername()
	{
		return dbUsername;
	}
	public void setDbUsername(String dbUsername)
	{
		this.dbUsername = dbUsername;
	}

	public String getDbPassword()
	{
		return dbPassword;
	}
	public void setDbPassword(String dbPassword)
	{
		this.dbPassword = dbPassword;
	}

	public String getAdminUsername()
	{
		return adminUsername;
	}
	public void setAdminUsername(String adminUsername)
	{
		this.adminUsername = adminUsername;
	}

	public String getAdminEmail()
	{
		return adminEmail;
	}
	public void setAdminEmail(String adminEmail)
	{
		this.adminEmail = adminEmail;
	}


	public String getAdminPassword()
	{
		return adminPassword != null ? adminPassword : "";
	}
	public void setAdminPassword(String password)
	{
		this.adminPassword = password;
	}
}