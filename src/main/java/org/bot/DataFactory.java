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
			info.setDbUsername(getSiteDbUsername());
			info.setDbPassword(getSiteDbPassword());
			info.setAdminUsername(getSiteAdminUsername());

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
			writer.append(siteInfo.getDomain());
			writer.append(";");
			writer.append(siteInfo.getAdminPassword());
			writer.append(";");
			writer.append(siteInfo.getCronUrl());
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
}