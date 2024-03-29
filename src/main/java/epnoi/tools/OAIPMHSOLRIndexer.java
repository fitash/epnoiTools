package epnoi.tools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// --------------------------------------------------------------------------------------------------------------------------

public class OAIPMHSOLRIndexer extends CommandLineTool {
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

	SolrServer server;

	/*
	 * OAIPMHIndexer -in where-oaipmh-harvest-dir -repository name
	 * 
	 * -name arxiv -in /JUNK (/JUNK/OAIPMH/harvests/arxive/harvest should exist
	 * )
	 */
	public static void main(String[] args) throws Exception {

		HashMap<String, String> options = getOptions(args);

		OutputStream out = System.out;

		String in = (String) options.get(PARAMETER_IN);

		String indexDir = in + "/OAIPMH/index";
		String name = (String) options.get(PARAMETER_NAME);

		String harvestDir = in + "/OAIPMH/harvests/" + name + "/harvest";

		System.out
				.println("Updating the repository harvest with the following paraneters: -in "
						+ in);

		logger.info("Updating the repository harvest with the following paraneters: -in "
				+ in);

		long start = new Date().getTime();
		String SOLRServerURL = "http://localhost:8983/solr";
		OAIPMHSOLRIndexer indexer = new OAIPMHSOLRIndexer(SOLRServerURL);
		int numIndexed = 0;

		File folder = new File(harvestDir);
		File[] listOfFiles = folder.listFiles();
		System.out.println("Indexing the directory/repository "
				+ folder.getAbsolutePath());

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String filePath = "file://" + listOfFiles[i].getAbsolutePath();
				System.out.println("Found the file: " + filePath);
				if (filePath.endsWith(".xml")) {
					indexer.indexHarvestFile(filePath);

				}
				// ----------------------------------------------------indexer.getServer().commit();
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
			}
		}

		indexer.close();
		long end = new Date().getTime();

		System.out.println("Indexing " + numIndexed + " files took "
				+ (end - start) + " milliseconds");
	}

	public OAIPMHSOLRIndexer(String indexDir) {
		
		this.server = new HttpSolrServer(indexDir);
	}

	// --------------------------------------------------------------------------------------------------------------------------

	public void indexHarvestFile(String filepath) throws Exception {

		System.out.println("Indexing the file " + filepath);
		logger.info("Indexing the file " + filepath);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(false);
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		org.w3c.dom.Document harvestDocument = builder.parse(filepath);
		XPath xpath = XPathFactory.newInstance().newXPath();
		// XPath Query for showing all nodes value
		XPathExpression expr = xpath.compile("//record");

		Object result = expr.evaluate(harvestDocument, XPathConstants.NODESET);
		NodeList nodes = (NodeList) result;

		for (int i = 0; i < nodes.getLength(); i++) {

			Element recordElement = (Element) nodes.item(i);

			SolrInputDocument document = _indexRecord(recordElement,
					simpleDateFormat);

			/*
			 * String documentURI =
			 * document.get(ExternalResourceLucenHelper.URI);
			 * writer.updateDocument(new Term(ExternalResourceLucenHelper.URI,
			 * documentURI), document); System.out.println("Indexing " +
			 * (document.getValues(ExternalResourceLucenHelper.URI)[0]));
			 */
			System.out
					.println("Indexing document--------------------------------------");
			System.out.println(document);
			this.server.add(document);

		}
		this.server.commit();

		// Document doc = getDocument(URL);
		// writer.addDocument(doc);
	}

	/*
	 * <header> identifier-> datestamp2012-04-11</datestamp> setSpec-> uno
	 * 
	 * <metadata> title-> solo uno setspec -> solo uno
	 */

	private SolrInputDocument _indexRecord(Element recordElement,
			SimpleDateFormat simpleDateFormat) {

		SolrInputDocument newDocument = new SolrInputDocument();

		// newDocument.add(new Field("contents", handler.toString(),
		// Field.Store.NO, Field.Index.ANALYZED));

		Element headerElement = (Element) recordElement.getElementsByTagName(
				"header").item(0);

		// Identifier
		NodeList identifierNodeList = headerElement
				.getElementsByTagName("identifier");

		if ((identifierNodeList != null)
				&& (identifierNodeList.item(0) != null)) {
			String identifier = identifierNodeList.item(0).getTextContent();

			newDocument.setField("id", identifier);
		}
		// setSpec
		NodeList setSpecNodeList = recordElement
				.getElementsByTagName("setSpec");
		if ((setSpecNodeList != null) && (setSpecNodeList.item(0) != null)) {
			String setSpec = setSpecNodeList.item(0).getTextContent();

			newDocument.setField("setSpec", setSpec);
		}

		NodeList newnodes = recordElement.getElementsByTagName("dc:title");

		for (int j = 0; j < newnodes.getLength(); j++) {

			newDocument.setField("title", newnodes.item(j).getTextContent());

		}

		newnodes = recordElement.getElementsByTagName("dc:identifier");

		for (int j = 0; j < newnodes.getLength(); j++) {
			String identifier = newnodes.item(j).getTextContent();
			// System.out.println("Identifier----> " + identifier);
			if (identifier.startsWith("http://")) {
				// System.out.println("Este es el URL " + identifier);

			}
		}

		newnodes = recordElement.getElementsByTagName("dc:creator");

		for (int j = 0; j < newnodes.getLength(); j++) {
			System.out.println("-" + newnodes.item(j).getTextContent());
			newDocument.addField("creator", newnodes.item(j).getTextContent());

		}

		newnodes = recordElement.getElementsByTagName("dc:subject");

		for (int j = 0; j < newnodes.getLength(); j++) {
			newDocument.addField("subject", newnodes.item(j).getTextContent());

		}

		newnodes = recordElement.getElementsByTagName("dc:date");

		for (int j = 0; j < newnodes.getLength(); j++) {

			try {

				SimpleDateFormat formatoDeFecha = new SimpleDateFormat(
						"yyyy-MM-dd");
				Date date = formatoDeFecha.parse(newnodes.item(j)
						.getTextContent());
				String indexFormatDate = simpleDateFormat.format(date);
				newDocument.addField("date", indexFormatDate);

			} catch (DOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		newnodes = recordElement.getElementsByTagName("dc:description");
		for (int j = 0; j < newnodes.getLength(); j++) {
			newDocument.addField("description", newnodes.item(j)
					.getTextContent());
		}

		newnodes = recordElement.getElementsByTagName("dc:type");

		for (int j = 0; j < newnodes.getLength(); j++) {
			newDocument.addField("type", newnodes.item(j).getTextContent());

		}

		return newDocument;

	}

	// --------------------------------------------------------------------------------------------------------------------------

	public void close() throws IOException {

	}

	// -----------------------------------------------------------------------------------------------

	public SolrServer getServer() {
		return server;
	}

	public void setServer(SolrServer server) {
		this.server = server;
	}

}
