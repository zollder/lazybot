package org.bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.stage.Stage;

import org.apache.commons.lang3.StringUtils;
import org.bot.configuration.SpringApplicationConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class LazyBot extends Application
{
	private static AnnotationConfigApplicationContext appContext;

	private DataFactory dataFactory;
	private List<String> keywords;
	private List<SiteInfo> siteData;
	private List<SiteInfo> siteInfoList;
	private static List<String> availableParsers = Arrays.asList("Google Web Search","Text Ask","Text Avg","Text Bing","Text Google","Text Lycos","Text Mail","Text Meta","Text Nigma","Text Ru Yahoo","Text Ukr","User Parser Text Rambler","Bing","Google","Qip","Rambler","Ru Ask","Ru Yahoo","Ukr","Yahoo");

	private static WebDriver driver;

	protected static final Logger logger = LoggerFactory.getLogger(LazyBot.class);

	// ----------------------------------------------------------------------------------------------------------------------
	public static void main(String[] args)
	{
		launch(args);
	}

	// ----------------------------------------------------------------------------------------------------------------------
	@Override
	public void start(Stage primaryStage)
	{
		appContext = new AnnotationConfigApplicationContext(SpringApplicationConfig.class);
		dataFactory = appContext.getBean(DataFactory.class);

		logger.debug("Application started.");

		// retrieve keywords
		keywords = dataFactory.getData(dataFactory.getKeywordsResource());
		Collections.shuffle(keywords);
		logger.debug("Keywords: " + keywords.size());
		int startIndex = 0;
		int endIndex = 99;
		int range = 100;

		// retrieve site data
		List<String> inputData = dataFactory.getData(dataFactory.getInputDataResource());
		siteData = dataFactory.getSiteData(inputData);


		// 10 lines max now
		siteInfoList = new ArrayList<>();
		for (SiteInfo siteInfo : siteData)
		{
			logger.info(getOutDataString(siteInfo));
			siteInfo.setKeysAsString(getDomainKeys(keywords.subList(startIndex, endIndex)));
//			List<String> keys = keywords.subList(startIndex, endIndex-1);
			startIndex = startIndex + range;
			endIndex = endIndex + range;
//			SiteInfo updatedSiteInfo = create(siteInfo);
			// TODO: move to SiteInfo
//			updatedSiteInfo.setSiteLink(getSiteLink(updatedSiteInfo.getDomain()));
//
//			siteInfoList.add(updatedSiteInfo);
//			dataFactory.saveSiteData(updatedSiteInfo);
		}

        stop(primaryStage);
	}

	// ----------------------------------------------------------------------------------------------------------------------
    public void stop(Stage primaryStage)
    {
    	primaryStage.close();

        if (appContext != null)
            appContext.close();

        try { super.stop(); }
        catch (Exception ex)
        { logger.error("Failed to close context. Message: " + ex.getLocalizedMessage()); }
    }

	// ----------------------------------------------------------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------------------------------------------------------
	private static SiteInfo create(SiteInfo siteInfo)
	{
		String domain = siteInfo.getDomain();
		String adminBaseUrl = "http://" + domain + "/wp-admin";
		String siteName = siteInfo.getSiteName();
		siteInfo.setDbName(getDbName(domain));

		driver = new FirefoxDriver();
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		driver.manage().window().maximize();

		// Configure WordPress
		driver.get(adminBaseUrl);
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.findElement(By.cssSelector(".button.button-large")).click();
		driver.findElement(By.id("dbname")).clear();
		driver.findElement(By.id("dbname")).sendKeys(siteInfo.getDbName());
		driver.findElement(By.id("uname")).clear();
		driver.findElement(By.id("uname")).sendKeys(siteInfo.getDbUsername());
		driver.findElement(By.id("pwd")).clear();
		driver.findElement(By.id("pwd")).sendKeys(siteInfo.getDbPassword());
		driver.findElement(By.name("submit")).click();

		// start installation
		driver.findElement(By.cssSelector(".button.button-large")).click();

		// configure site
		driver.findElement(By.id("weblog_title")).clear();
		driver.findElement(By.id("weblog_title")).sendKeys(siteName);
		driver.findElement(By.id("user_login")).clear();
		driver.findElement(By.id("user_login")).sendKeys(siteInfo.getAdminUsername());
		delay(5);
		String sitePassword = driver.findElement(By.id("pass1")).getAttribute("data-pw");
		siteInfo.setAdminPassword(sitePassword);
		logger.debug("Site password: " + sitePassword);
		driver.findElement(By.id("admin_email")).clear();
		driver.findElement(By.id("admin_email")).sendKeys(siteInfo.getAdminEmail());
		driver.findElement(By.id("submit")).click();

		// finalize configuration
		driver.findElement(By.cssSelector(".button.button-large")).click();

		// Login and start configuration
		driver.get(adminBaseUrl);

		// Login
		driver.findElement(By.id("user_login")).sendKeys(siteInfo.getAdminUsername());
//		driver.findElement(By.id("user_pass")).sendKeys(")@N3@9eDj04VfmY3AT");
		driver.findElement(By.id("user_pass")).sendKeys(siteInfo.getAdminPassword());
		driver.findElement(By.id("wp-submit")).click();

		// Activate plugins, enable cache
		driver.findElement(By.id("menu-plugins")).click();
		int size = driver.findElements(By.className("activate")).size();
		for (int i = 0; i < size; i++)
			driver.findElement(By.className("activate")).click();

		// close annoying pop-up, if present
		if (driver.findElement(By.cssSelector(".wp-pointer-content")).isDisplayed())
			driver.findElement(By.className("close")).click();

		// Activate cache
		driver.get(adminBaseUrl + "/options-general.php?page=wpsupercache");
		driver.findElement(By.name("wp_cache_easy_on")).click();
		driver.findElement(By.className("button-primary")).click();

		// Delete default post
		driver.get(adminBaseUrl + "/post.php?post=1&action=edit");
		driver.findElement(By.id("delete-action")).click();

		// Modify category
		driver.get(adminBaseUrl + "/edit-tags.php?action=edit&taxonomy=category&tag_ID=1&post_type=post");
		driver.findElement(By.id("name")).clear();
		driver.findElement(By.id("name")).sendKeys(siteInfo.getCategory());
		driver.findElement(By.id("slug")).clear();
		driver.findElement(By.id("slug")).sendKeys(siteInfo.getCategory());
		driver.findElement(By.id("term_meta_post_pattern-html")).click();
		driver.findElement(By.id("term_meta_post_pattern")).clear();
		driver.findElement(By.id("term_meta_post_pattern")).sendKeys(getPostTemplate());
		driver.findElement(By.id("submit")).click();

		// Appearance - activate "ReFresh" theme
		driver.get(adminBaseUrl + "/themes.php");
		List<WebElement> activateButtons = driver.findElements(By.cssSelector(".button.button-secondary.activate"));;
		for (int i = 0; i < activateButtons.size(); i++) {
			WebElement element = activateButtons.get(i);
			if (element.getAttribute("href").contains("action=activate&stylesheet=refresh")) {
				element.click();
				break;
			}
		}

		// Remove meta widget
		driver.get(adminBaseUrl + "/widgets.php");
		WebElement meta = driver.findElement(By.id("sidebar-1")).findElements(By.className("widget")).get(5);
		meta.click();
		delay(3);
		meta.findElement(By.className("widget-control-remove")).click();

		// Modify header.php
		driver.get(adminBaseUrl + "/theme-editor.php");
		driver.findElement(By.partialLinkText("header.php")).click();
		String originalContent = driver.findElement(By.id("newcontent")).getText();

		String modifiedContent = null;
		if (!StringUtils.contains(originalContent, "credit.actionpay.ru"))
			modifiedContent = StringUtils.replaceOnce(originalContent, "</head>", getHeaderScript(domain));
		if (!StringUtils.contains(originalContent, "controllerName"))
			modifiedContent = StringUtils.replaceOnce(modifiedContent, "<div class=\"wrapper\">", getBodyScript());

		if (StringUtils.isNotBlank(modifiedContent)) {
			driver.findElement(By.id("newcontent")).clear();
			driver.findElement(By.id("newcontent")).sendKeys(modifiedContent);
			driver.findElement(By.id("submit")).click();
		}

		// Options - general settings
		driver.get(adminBaseUrl + "/options-general.php");
		driver.findElement(By.id("blogname")).clear();
		driver.findElement(By.id("blogname")).sendKeys(siteName);
		driver.findElement(By.id("blogdescription")).clear();
		driver.findElement(By.id("blogdescription")).sendKeys(siteInfo.getTagline());
		driver.findElement(By.id("submit")).click();

		// Options - reading settings
		driver.get(adminBaseUrl + "/options-reading.php");
		driver.findElement(By.id("posts_per_page")).clear();
		driver.findElement(By.id("posts_per_page")).sendKeys("5");
		driver.findElement(By.id("posts_per_rss")).clear();
		driver.findElement(By.id("posts_per_rss")).sendKeys("10");
		driver.findElement(By.id("submit")).click();

		// Options - discussion settings
		driver.get(adminBaseUrl + "/options-discussion.php");
		updateChackbox(true, driver.findElement(By.id("default_pingback_flag")));
		updateChackbox(true, driver.findElement(By.id("default_ping_status")));
		updateChackbox(false, driver.findElement(By.id("default_comment_status")));
		updateChackbox(true, driver.findElement(By.id("require_name_email")));
		updateChackbox(true, driver.findElement(By.id("comment_registration")));
		updateChackbox(true, driver.findElement(By.id("close_comments_for_old_posts")));
		updateChackbox(true, driver.findElement(By.id("thread_comments")));
		updateChackbox(true, driver.findElement(By.id("page_comments")));
		updateChackbox(false, driver.findElement(By.id("comments_notify")));
		updateChackbox(false, driver.findElement(By.id("moderation_notify")));
		updateChackbox(true, driver.findElement(By.id("comment_moderation")));
		updateChackbox(true, driver.findElement(By.id("comment_whitelist")));
		driver.findElement(By.id("submit")).click();

		// Options - permalink settings
		driver.get(adminBaseUrl + "/options-permalink.php");
		WebElement commonSettingsTable = driver.findElement(By.cssSelector(".form-table.permalink-structure"));
		List<WebElement> settings = commonSettingsTable.findElements(By.tagName("label"));
		settings.stream()
			.filter(s -> s.getText().contains("Post name") || s.getText().contains("Название записи")).findFirst().get().click();
		driver.findElement(By.id("submit")).click();

		// WordpreSED - cron URL
		delay(2);
		driver.get(adminBaseUrl + "/admin.php?page=wordpresed_admin");
		driver.findElement(By.cssSelector("button.btn.btn-primary.has-spinner.btnChangeSecretWord.btnChange")).click();
		delay(2);
		siteInfo.setNewCronSecret(driver.findElement(By.id("spanChangeSecretWord")).getText());
		siteInfo.setCronUrl(getCronUrl(siteInfo.getNewCronSecret(), siteInfo.getDomain()));
//		logger.debug(siteInfo.getNewCronSecret());
		logger.debug(siteInfo.getCronUrl());

		delay(2);
		driver.get(adminBaseUrl + "/admin.php?page=wordpresed_keywords");
		driver.findElement(By.id("addNewKeywordsButton")).click();
		driver.findElement(By.id("addFromTextField")).clear();
		driver.findElement(By.id("addFromTextField")).sendKeys(siteInfo.getKeysAsString());
		driver.findElement(By.cssSelector(".btn.dropdown-toggle.selectpicker.btn-default")).click();
		driver.findElement(By.cssSelector(".dropdown-menu.inner.selectpicker")).findElement(By.className("level-0")).click();
		driver.findElement(By.id("createKeysOK")).click();
		driver.findElement(By.id("createKeysOK")).click();
		delay(2);
		if (driver.findElement(By.cssSelector(".modal-dialog")).isDisplayed())
			driver.findElement(By.id("btn-ok")).click();


		/*driver.findElement(By.cssSelector(".btn.btn-success.btn-sm.pull-right.text-uppercase.checkAllParsers")).click();*/
		/*Float parserTime = Float.valueOf(StringUtils.trimToEmpty(parserElements.get(3).getText()).substring(0, 4));*/
		driver.get(adminBaseUrl + "/admin.php?page=wordpresed_parsers");
		delay(3);
		// must be improved
		List<WebElement> textParsers = driver.findElement(By.id("text-parsers-table")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
		for (WebElement parser : textParsers) {
			List<WebElement> parserElements = parser.findElements(By.tagName("td"));
			String parserName = StringUtils.trimToEmpty(parserElements.get(1).getText());
			parserElements.get(0).click();
			if (availableParsers.contains(parserName))
				parserElements.get(0).click();
/*			String parserLang = StringUtils.trimToEmpty(parserElements.get(2).getText().toLowerCase());
			if (parserName.contains("Text") && (parserLang.toLowerCase().contains("ru") || parserLang.toLowerCase().contains("ua")))
				parserElements.get(0).click();
			if (parserName.equals("Google")||
					parserName.equals("Bing") ||
					parserName.equals("Lycos") ||
					parserName.equals("Qip") ||
					parserName.equals("Ru Ask") ||
					parserName.equals("Ru Yahoo") ||
					parserName.equals("Rambler"))
*/
		}
		driver.findElement(By.id("saveSelectionsForPars")).click();
		delay(2);

		List<WebElement> tabs = driver.findElements(By.cssSelector("ul.nav.nav-tabs > li > a"));
		for (WebElement tab : tabs) {
			if ((tab.getAttribute("href") != null) && tab.getAttribute("href").contains("images"))
				tab.click();
		}
		List<WebElement> imagesParsers = driver.findElement(By.id("images-parsers-table")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));
		for (WebElement parser : imagesParsers) {
			List<WebElement> parserElements = parser.findElements(By.tagName("td"));
			String parserName = StringUtils.trimToEmpty(parserElements.get(1).getText());
			if (parserName.contains("Images Google") || parserName.contains("Images Yahoo") || parserName.contains("Images Yandex Class"))
				parserElements.get(0).click();
		}
		driver.findElement(By.id("saveSelectionsForPars")).click();

		// configure options
/*		driver.get(adminBaseUrl + "/admin.php?page=wordpresed_options");
		List<WebElement> generalOptions = driver.findElement(By.id("collapse-posts-general")).findElements(By.cssSelector(".row.bottom-buffer"));
		generalOptions.get(0).click();
		delay(1);
		generalOptions.get(3).click();
		delay(1);
		generalOptions.get(5).click();

		List<WebElement> commentsOptions = driver.findElement(By.id("collapse-posts-comments")).findElements(By.cssSelector(".row.bottom-buffer"));
		delay(1);
		commentsOptions.get(0).click();

		delay(1);
		driver.findElement(By.cssSelector("button.btn.btn-lg.btn-success.btnSaveAll.pull-right")).click();

		List<WebElement> leftTabOptions = driver.findElements(By.cssSelector(".nav.nav-tabs.tabs-left > li > a"));
		for (WebElement leftTabOption : leftTabOptions) {
			if ((leftTabOption.getAttribute("href") != null) && leftTabOption.getAttribute("href").contains("curl"))
				leftTabOption.click();
		}*/

		driver.close();
		return siteInfo;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getDbName(String domain)
	{
		String dbName = "admin_" + StringUtils.replaceChars(domain, ".", "_");
		logger.debug("Database name: " + dbName);
		return dbName;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getPostTemplate()
	{
		StringBuffer template = new StringBuffer();
		template.append("[paragraph sentences=8 withkey=1]\n");
		template.append("[more]\n");
		template.append("[paragraph sentences=8 withkey=1]\n");
		template.append("[more]\n");
		template.append("[paragraph sentences=8 withkey=1]\n");
		template.append("[more]\n");
		template.append("[paragraph sentences=8 withkey=1]");
		return template.toString();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getHeaderScript(String domain)
	{
		String modifiedDomain = StringUtils.replace(domain, ".", "_");
		StringBuffer headerScript = new StringBuffer();
		headerScript.append("<script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js\"></script>\n");
		headerScript.append("<script src=\"//credit.actionpay.ru/feed/finance/apFinance.js\"></script>\n");
		headerScript.append("<script>\n");
		headerScript.append("$(document).ready(function() {\n");
		headerScript.append("$('#controllerName').apFinance({type:'code',referal:'NzI2MzEzOTY2ODkw&subaccount=").append(modifiedDomain).append("'});\n");
		headerScript.append("});\n");
		headerScript.append("</script>\n");
		headerScript.append("</head>");
		return headerScript.toString();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getBodyScript()
	{
		StringBuffer headerScript = new StringBuffer();
		headerScript.append("<div id=\"controllerName\"></div>\n");
		headerScript.append("<div class=\"wrapper\">");
		return headerScript.toString();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static void updateChackbox(Boolean select, WebElement checkbox) {
		String attribute = checkbox.getAttribute("type");
		if ((attribute != null) && !attribute.equals("checkbox")) {
			logger.debug("Specified WebElement is not a checkbox.");
			return;
		}

		if (select && StringUtils.isBlank(checkbox.getAttribute("checked")))
			checkbox.click();
		if (!select && StringUtils.isNotBlank(checkbox.getAttribute("checked")))
			checkbox.click();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getCronUrl(String secret, String domain) {
		StringBuffer url = new StringBuffer();
		url.append("wget \"http://");
		url.append(domain);
		url.append("/wp-admin/admin-ajax.php?action=wpsed_cron_execution&secret=");
		url.append(secret);
		url.append("\" -O cron.txt /dev/null");
		return url.toString();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getDomainKeys(List<String> selectedKeys) {
		logger.debug("Selected keys size: " + selectedKeys.size());
		StringBuffer keys = new StringBuffer();
		for (String key : selectedKeys) {
			keys.append(key);
			keys.append("\n");
		}
		return keys.toString();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	/*	private static List<String> partitionList(List<String> selectedKeys)
	{
		List<String> partitionedList = new ArrayList<String>();
		for (List<String> sublist : Lists.partition(selectedKeys, 500))
		{
			StringBuffer keys = new StringBuffer();
			for (String key : sublist)
			{
				keys.append(key);
				keys.append("\n");
			}
			partitionedList.add(keys.toString());
		}

		return partitionedList;
	}*/

	// ----------------------------------------------------------------------------------------------------------------------
	private static void delay(int delay)
	{
		try {
			Thread.sleep(delay*1000);
		} catch (InterruptedException e) {
			logger.error("Delay interrupted.", e.getMessage());
			e.printStackTrace();
		}
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private static String getSiteLink(String domain)
	{
		StringBuffer siteLink = new StringBuffer();
		siteLink.append("<a href=\"http://www.");
		siteLink.append(domain);
		siteLink.append("\">HTML</a>");
		return siteLink.toString();
	}

	private static String getOutDataString(SiteInfo siteInfo)
	{
		StringBuffer outString = new StringBuffer();
		outString.append(siteInfo.getDomain()).append(";");
		outString.append(siteInfo.getSiteName()).append(";");
		outString.append(siteInfo.getCategory()).append(";");
		outString.append(siteInfo.getTagline());
		return outString.toString();
	}
}