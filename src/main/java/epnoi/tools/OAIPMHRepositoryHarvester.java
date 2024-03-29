package epnoi.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import epnoi.tools.logging.EpnoiToolsLogger;

import ORG.oclc.oai.harvester2.verb.ListRecords;

public class OAIPMHRepositoryHarvester extends CommandLineTool {
	public static final String PARAMETER_COMMAND = "-command";
	public static final String PARAMETER_NAME = "-name";
	public static final String PARAMETER_URL = "-URL";
	public static final String PARAMETER_OUT = "-out";
	public static final String PARAMETER_IN = "-in";
	public static final String PARAMETER_FROM = "-from";
	public static final String PARAMETER_TO = "-to";
	public static final String PARAMETER_COMMAND_INIT = "init";
	public static final String PARAMETER_COMMAND_HARVEST = "harvest";
	private static final Logger logger = Logger
			.getLogger(OAIPMHRepositoryHarvester.class.getName());

	// ---------------------------------------------------------------------------------------------------------------------------------------

	/*
	 * -command init -out /JUNK2 -URL http://export.arxiv.org/oai2 -name arxive
	 * -command harvest -in /JUNK2/OAIPMH/harvests/arxive -from 2012-04-10 -to
	 * 2012-05-10
	 */

	public static void main(String[] args) {

		try {

			HashMap<String, String> options = getOptions(args);

			OutputStream out = System.out;

			String command = (String) options.get(PARAMETER_COMMAND);
			String in = (String) options.get(PARAMETER_IN);
			String fileName = new SimpleDateFormat("MM-dd-hh-mmSSS-yyyy")
					.format(new Date());

		//	EpnoiLogger.setup(in + "/logs/" + fileName);
			if (command != null) {
				if (command.equals(PARAMETER_COMMAND_INIT)) {
					// Add arguments checking here
					_initRepositoryHarvest(options);
				} else if (command.equals(PARAMETER_COMMAND_HARVEST)) {
					_updateRepositoryHarvest(options);
				}
			}

			if (out != System.out)
				out.close();
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------------------------------

	private static void _updateRepositoryHarvest(HashMap<String, String> options) {
		String in = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_IN);
		String from = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_FROM);
		String to = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_TO);

		System.out
				.println("Updating the repository harvest with the following paraneters: -in "
						+ in + " -from " + from + " -to " + to);

		logger.info("Updating the repository harvest with the following paraneters: -in "
				+ in + " -from " + from + " -to " + to);

		Manifest manifest = ManifestHandler.read(in + "/" + "manifest.xml");

		if (manifest != null) {
			System.out.println("Updating the harvest of the repository "
					+ manifest.getRepository() + " in the URL "
					+ manifest.getURL());
			logger.fine("Updating the harvest of the repository "
					+ manifest.getRepository() + " in the URL "
					+ manifest.getURL());

			File harvestDirectory = new File(in + "/" + "harvest");
			if (!harvestDirectory.exists()) {
				harvestDirectory.mkdir();
			}

			String metadataPrefix = (String) options.get("-metadataPrefix");
			if (metadataPrefix == null)
				metadataPrefix = "oai_dc";

			String setSpec = (String) options.get("-setSpec");

			// out = new FileOutputStream(outFileName, true);
			Locale locale = Locale.ENGLISH;
			DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd",
					locale);
			// The initial date is the from parameter date
			System.out.println(">from " + from);
			Date fromDate = null;
			try {
				fromDate = simpleDateFormat.parse(from);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println(">fromDate>" + fromDate.toString());

			Calendar c = Calendar.getInstance();
			c.setTime(fromDate);

			Date untilDate = null;
			try {
				untilDate = simpleDateFormat.parse(to);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println(">until Date>" + untilDate.toString());
			Date auxDate = null;
			String repositoryDirectoryName = in;
			File repositoryDirectory = new File(repositoryDirectoryName);

			while (fromDate.before(untilDate)) {

				c.add(Calendar.DATE, 1);
				auxDate = c.getTime();
				logger.info("Attempting to harvest the day "
						+ simpleDateFormat.format(fromDate));
				String outputFileName = repositoryDirectoryName + "/harvest/"
						+ simpleDateFormat.format(fromDate) + ".xml";
				OutputStream outputFile = null;

				if (!new File(outputFileName).isFile()) {
					try {
						outputFile = new FileOutputStream(outputFileName, true);
					} catch (FileNotFoundException e) {
						logger.severe(e.getMessage());
						e.printStackTrace();
					}

					System.out.println("harvest(" + manifest.getURL() + ","
							+ simpleDateFormat.format(fromDate) + ","
							+ simpleDateFormat.format(auxDate) + ", "
							+ metadataPrefix + ", " + setSpec + ", "
							+ outputFileName);

					logger.info("harvest(" + manifest.getURL() + ","
							+ simpleDateFormat.format(fromDate) + ","
							+ simpleDateFormat.format(auxDate) + ", "
							+ metadataPrefix + ", " + setSpec + ", "
							+ outputFileName);

					try {
						harvest(manifest.getURL(),
								simpleDateFormat.format(fromDate),
								simpleDateFormat.format(auxDate),
								metadataPrefix, setSpec, outputFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					} catch (ParserConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					} catch (TransformerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					} catch (NoSuchFieldException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					}

					try {
						outputFile.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						logger.severe(e.getMessage());
					}
				} else {
					logger.info("The harvested file "
							+ outputFileName
							+ " already exists and therefore this day is skipped");
				}

				fromDate = auxDate;

			}

		}

	}

	// ---------------------------------------------------------------------------------------------------------------------------------------

	private static void _initRepositoryHarvest(HashMap<String, String> options) {
		String name = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_NAME);
		String URL = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_URL);
		String out = (String) options
				.get(OAIPMHRepositoryHarvester.PARAMETER_OUT);
		System.out
				.println("Initializing repository harvest with the following paraneters: -name "
						+ name + " -URL " + URL + " -out " + out);
		logger.info("Initializing repository harvest with the following paraneters: -name "
				+ name + " -URL " + URL + " -out " + out);

		File repositoryDirectory = new File(out + "/OAIPMH/harvests/" + name);
		if (!repositoryDirectory.exists()) {
			boolean success = repositoryDirectory.mkdirs();
			if (success) {
				System.out.println("The directory " + out
						+ " has been successfully created!");
				logger.info("The directory " + out
						+ " has been successfully created!");

				Manifest manifest = new Manifest();
				manifest.setRepository(name);
				manifest.setURL(URL);
				ManifestHandler
						.marshallToFile(manifest,
								repositoryDirectory.getAbsolutePath()
										+ "/manifest.xml");
				// First we create the harvest directory
				File harvestDirectory = new File(
						repositoryDirectory.getAbsolutePath() + "/harvest");
				harvestDirectory.mkdir();

				// First we create the harvest directory
				File logDirectory = new File(
						repositoryDirectory.getAbsolutePath() + "/logs");
				logDirectory.mkdir();
				/*
				 * try { manifestFile.createNewFile(); } catch (IOException e) {
				 * // TODO Auto-generated catch block e.printStackTrace(); }
				 */
			} else {
				logger.severe("Something went wrong when creating the " + out
						+ "/OAIPMH/harvests/" + name);
				// System.out.println(":(");
			}
		} else {
			logger.severe("The directory "
					+ repositoryDirectory.getAbsolutePath()
					+ " already existed");
			throw new IllegalArgumentException("The directory "
					+ repositoryDirectory.getAbsolutePath()
					+ " already existed");
		}
	}

	// -----------------------------------------------------------------------------------------------

	public static void harvest2(String baseURL, String from, String until,
			String metadataPrefix, String setSpec, OutputStream out) {
		System.out.println("from> " + from + "until> " + until);
	}

	// -----------------------------------------------------------------------------------------------

	public static void harvest(String baseURL, String from, String until,
			String metadataPrefix, String setSpec, OutputStream out)
			throws IOException, ParserConfigurationException, SAXException,
			TransformerException, NoSuchFieldException {

		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				.getBytes("UTF-8"));
		out.write("<harvest>\n".getBytes("UTF-8"));
		out.write("\n".getBytes("UTF-8"));
		out.write("<timeStamp>".getBytes("UTF-8"));
		Date timeStamp = new Date(System.currentTimeMillis());
		out.write(timeStamp.toString().getBytes("UTF-8"));
		out.write("</timeStamp>".getBytes("UTF-8"));

		out.write("\n".getBytes("UTF-8"));

		ListRecords listRecords = new ListRecords(baseURL, from, until,
				setSpec, metadataPrefix);
		while (listRecords != null) {

			NodeList errors = listRecords.getErrors();
			if (errors != null && errors.getLength() > 0) {
				System.out.println("Found errors");
				int length = errors.getLength();
				for (int i = 0; i < length; ++i) {
					Node item = errors.item(i);
					System.out.println(item);
				}
				System.out.println("Error record: " + listRecords.toString());
				break;
			}

			out.write(listRecords.toString().getBytes("UTF-8"));
			out.write("\n".getBytes("UTF-8"));
			String resumptionToken = listRecords.getResumptionToken();

			if (resumptionToken == null || resumptionToken.length() == 0) {
				listRecords = null;
			} else {

				listRecords = new ListRecords(baseURL, resumptionToken);
			}

		}
		out.write("</harvest>\n".getBytes("UTF-8"));
	}

	// -----------------------------------------------------------------------------------------------

}
