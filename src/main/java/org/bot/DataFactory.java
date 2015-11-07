/*
 * DataGenerator.java
 * Created by zollder.
 */
package org.bot;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DataFactory
{
	@Value("${input.keywords}")
	private Resource keywordsResource;

	@Value("${input.data}")
	private Resource inputDataResource;

	@Value("${output.data}")
	private Resource outputDataResource;

	@Value("${site.db.username}")
	private String siteDbUsername;

	@Value("${site.db.password}")
	private String siteDbPassword;

	@Value("${site.admin.username}")
	private String siteAdminUsername;

	@Value("${site.admin.email}")
	private String siteAdminEmail;

	@Value("${site.server.name}")
	private String siteServername;

	// ----------------------------------------------------------------------------------------------------------------------
	protected static Logger logger = LoggerFactory.getLogger(DataFactory.class);
	private Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	// ----------------------------------------------------------------------------------------------------------------------
	public Resource getKeywordsResource()
	{
		return keywordsResource;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public Resource getInputDataResource()
	{
		return inputDataResource;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public Resource getOutputDataResource()
	{
		return outputDataResource;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public String getSiteDbUsername()
	{
		return siteDbUsername;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public String getSiteDbPassword()
	{
		return siteDbPassword;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public String getSiteAdminUsername()
	{
		return siteAdminUsername;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public String getSiteAdminEmail()
	{
		return siteAdminEmail;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public String getSiteServerName()
	{
		return siteServername;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public void insertKeywords(List<String> keywords, String dbName) throws SQLException
	{
		Connection connection = ConnectionObject.getConnection(dbName);

		// clean the table before insert
		connection.setAutoCommit(false);
		Statement statement = connection.createStatement();
		logger.info(String.format("Cleaning up %s database", dbName));
		statement.execute("delete from wp_wpsed_keywords");
		connection.commit();

		String insertQuery = "insert into wp_wpsed_keywords values (?,?,?,?,?,?,?,?,?)";
		connection.setAutoCommit(false);
		PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
		Long id = Long.valueOf(0);
		for (String keyword : keywords)
		{
			preparedStatement.setLong(1, id++);
			preparedStatement.setString(2, keyword);
			preparedStatement.setInt(3, 1);
			preparedStatement.setInt(4, 0);
			preparedStatement.setInt(5, 0);
			preparedStatement.setInt(6, 0);
			preparedStatement.setInt(7, 0);
			preparedStatement.setInt(8, 1);
			preparedStatement.setString(9, "");
			preparedStatement.addBatch();
		}

		logger.info(String.format("Inserting %s keywords into %s database", keywords.size(), dbName));
		int[] result = preparedStatement.executeBatch();
		connection.commit();
		logger.info("Inserted keywords: " + result.length);

		if (preparedStatement != null)
			preparedStatement.close();

		if (connection != null)
			connection.close();
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public List<String> getData(Resource resource)
	{
		Set<String> lines = new TreeSet<>();
		List<String> linesAsList = new ArrayList<>();
        if ((resource != null) && resource.exists())
        {
            List<String> allLines = null;
            try { allLines = Files.readAllLines(Paths.get(resource.getURI()), DEFAULT_CHARSET); }
            catch (IOException e) { logger.warn("Error retrieving resource file path"); }

            if ((allLines != null) && !allLines.isEmpty())
            {
                for (String line : allLines)
                {
                    line = line.trim();
                    if ((line.length() == 0) || (line.charAt(0) == '#'))
                        continue;

                    if (!lines.add(String.valueOf(line)))
                        logger.warn("duplicate line found: [{}]", line);

                    linesAsList = new ArrayList<>();
                    linesAsList.addAll(lines);
                }
            }
        }

        return linesAsList;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public List<SiteInfo> getSiteData(List<String> dataLines)
	{
		List<SiteInfo> parsedData = new ArrayList<>();
		for (String line : dataLines)
		{
			String[] splittedLine = StringUtils.split(line, ";");
			SiteInfo info = new SiteInfo();

			// parsed properties
			info.setDomain(splittedLine[0]);
			info.setSiteName(splittedLine[1]);
			info.setCategory(splittedLine[2]);
			info.setTagline(splittedLine[3]);

			// static properties
			info.setDbName(buildDbName(info.getDomain()));
			info.setDbUsername(getSiteDbUsername());
			info.setDbPassword(getSiteDbPassword());
			info.setAdminUsername(getSiteAdminUsername());
			info.setAdminEmail(getSiteAdminEmail());
			info.setSiteLink(getSiteLink(info.getDomain()));

			parsedData.add(info);
		}
		return parsedData;
	}

	// ----------------------------------------------------------------------------------------------------------------------
	public void saveSiteData(SiteInfo siteInfo)
	{
		Resource resource = getOutputDataResource();
		logger.info("Saving (appending) data to: " + resource.getFilename());
		try
		{
			String pathToFile = resource.getURI().getRawPath();
			logger.debug("Start writing to:" + pathToFile);
			FileWriter writer = new FileWriter(pathToFile, true);
			writer.append(LocalDate.now().toString()).append(";");
			writer.append(siteInfo.getDomain()).append(";");
			writer.append(siteInfo.getAdminUsername()).append(";");
			writer.append(siteInfo.getAdminPassword()).append(";");
			writer.append(getSiteServerName()).append(";");
			writer.append(siteInfo.getDbName()).append(";");
			writer.append(siteInfo.getDbUsername()).append(";");
			writer.append(siteInfo.getDbPassword()).append(";");
			writer.append(";");
			writer.append(siteInfo.getCronUrl()).append(";");
			writer.append(siteInfo.getSiteLink()).append(";");
			writer.append("\n");
			writer.flush();
			writer.close();
			logger.debug("End writing");
		}
		catch (IOException e)
		{
			logger.debug("Error witing to file", e.getMessage());
		}
	}

	// ----------------------------------------------------------------------------------------------------------------------
	private String buildDbName(String domain)
	{
		String dbName = "admin_" + StringUtils.replaceChars(domain, ".", "_");
		return dbName;
	}


	// ----------------------------------------------------------------------------------------------------------------------
	private String getSiteLink(String domain)
	{
		StringBuffer siteLink = new StringBuffer();
		siteLink.append("<a href=\"http://www.");
		siteLink.append(domain);
		siteLink.append("\">HTML</a>");
		return siteLink.toString();
	}
}