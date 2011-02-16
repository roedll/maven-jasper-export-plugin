package com.abiquo.jasper;

import static net.sf.jasperreports.engine.JRParameter.REPORT_FILE_RESOLVER;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.SimpleFileResolver;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Maven plugin to export jasper files to PDF, HTML or XMl using MySQL as data source.
 * 
 * @goal jasper-export
 * @phase generate-sources
 */
public class JasperExportMojo extends AbstractMojo
{
    /**
     * The directory to scan
     * 
     * @parameter
     */
    private File jasperDirectory;

    /**
     * The directory to with jrxml resources
     * 
     * @parameter
     */
    private File jrxmlDirectory;

    /**
     * The output directory
     * 
     * @parameter
     */
    private File outputDirectory;

    /**
     * The MySQL host
     * 
     * @parameter
     */
    private String mysqlHost;

    /**
     * The MySQL database
     * 
     * @parameter
     */
    private String mysqlDatabase;

    /**
     * The MySQL user
     * 
     * @parameter
     */
    private String mysqlUser;

    /**
     * The MySQL password
     * 
     * @parameter
     */
    private String mysqlPassword;

    /**
     * Valid export formats
     */
    private enum ExportFormat
    {
        PDF, HTML, XML
    };

    /**
     * The output format, PDF, HTML or XML
     * 
     * @parameter default-value = "PDF"
     */
    private String exportFormat;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().debug("jasperDirectory = " + jasperDirectory);
        getLog().debug("jrxmlDirectory = " + jrxmlDirectory);
        getLog().debug("outputDirectory = " + outputDirectory);
        getLog().debug("mysqlHost = " + mysqlHost);
        getLog().debug("mysqlDatabase = " + mysqlDatabase);
        getLog().debug("mysqlUser = " + mysqlUser);
        getLog().debug("mysqlPassword = " + mysqlPassword);

        checkNotNull(jasperDirectory, "jasperDirectory");
        checkNotNull(jrxmlDirectory, "jrxmlDirectory");
        checkNotNull(outputDirectory, "outputDirectory");
        checkNotNull(mysqlHost, "mysqlHost");
        checkNotNull(mysqlDatabase, "mysqlDatabase");
        checkNotNull(mysqlUser, "mysqlUser");
        checkNotNull(mysqlPassword, "mysqlPassword");

        checkDirectory(jasperDirectory, false);
        checkDirectory(jrxmlDirectory, false);
        checkDirectory(outputDirectory, true);

        ExportFormat format = checkValidFormat();

        Collection<String> jasperFiles = scanJasperFiles(jasperDirectory);

        if (jasperFiles.isEmpty())
        {
            getLog().info("Nothing to fill, no jasper files found.");
        }
        else
        {
            Connection connection = createConnection();

            getLog().info("Exporting " + jasperFiles.size() + " jasper files to " + format);

            for (String file : jasperFiles)
            {
                getLog().info("Exporting jasper file: " + file);
                fillAndExportReport(file, connection, format);
            }
        }
    }

    private Connection createConnection() throws MojoExecutionException
    {
        try
        {
            String url = String.format("jdbc:mysql://%s/%s", mysqlHost, mysqlDatabase);
            Connection connection = DriverManager.getConnection(url, mysqlUser, mysqlPassword);
            connection.setAutoCommit(false);

            return connection;
        }
        catch (SQLException e)
        {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillAndExportReport(String jasperFile, final Connection connection,
        final ExportFormat format) throws MojoExecutionException
    {
        try
        {
            String subdir =
                FilenameUtils.getPath(jasperFile.replace(jasperDirectory.getAbsolutePath(), ""));

            String jrxmldir = FilenameUtils.concat(jrxmlDirectory.getAbsolutePath(), subdir);
            String outdir = FilenameUtils.concat(outputDirectory.getAbsolutePath(), subdir);
            String outfile = FilenameUtils.concat(outdir, FilenameUtils.getBaseName(jasperFile));

            checkDirectory(new File(jrxmldir), false);
            checkDirectory(new File(outdir), true);

            List<File> parents = new ArrayList<File>();
            parents.add(new File(jrxmldir));
            parents.add(new File(FilenameUtils.getFullPath(jasperFile)));

            Map parameters = new HashMap();
            parameters.put(REPORT_FILE_RESOLVER, new SimpleFileResolver(parents));

            JasperReport report = (JasperReport) JRLoader.loadObject(jasperFile);
            JasperPrint print = JasperFillManager.fillReport(report, parameters, connection);

            switch (format)
            {
                case PDF:
                    JasperExportManager.exportReportToPdfFile(print, outfile + ".pdf");
                    break;

                case HTML:
                    JasperExportManager.exportReportToHtmlFile(print, outfile + ".html");
                    break;

                case XML:
                    JasperExportManager.exportReportToXmlFile(print, outfile + ".xml", true);
                    break;
            }
        }
        catch (JRException e)
        {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    private ExportFormat checkValidFormat() throws MojoExecutionException
    {
        ExportFormat format = ExportFormat.valueOf(exportFormat.toUpperCase());

        if (format == null)
        {
            throw new MojoExecutionException("Invalid export format " + exportFormat);
        }

        return format;
    }

    private void checkNotNull(Object object, String name) throws MojoExecutionException
    {
        if (object == null)
        {
            throw new MojoExecutionException("Missing required property " + name);
        }
    }

    private void checkDirectory(File directory, boolean writable) throws MojoExecutionException
    {
        if (directory.exists() && !directory.isDirectory())
        {
            throw new MojoExecutionException(directory + " is not a directory");
        }
        else if (!directory.exists() && writable && !directory.mkdirs())
        {
            throw new MojoExecutionException(directory + " could not be created");
        }

        if (writable && !directory.canWrite())
        {
            throw new MojoExecutionException(directory + " is not writable");
        }
    }

    private Collection<String> scanJasperFiles(final File folder)
    {
        Collection<String> files = new HashSet<String>();

        for (File file : folder.listFiles())
        {
            if (file.isDirectory())
            {
                files.addAll(scanJasperFiles(file));
            }
            else if (file.getName().endsWith(".jasper"))
            {
                files.add(file.getAbsolutePath());
            }
        }

        return files;
    }
}
