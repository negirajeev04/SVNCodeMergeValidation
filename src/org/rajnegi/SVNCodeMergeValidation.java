package org.rajnegi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.rajnegi.handler.SaxHandler;
import org.rajnegi.vo.FilePath;
import org.rajnegi.vo.Revision;
import org.rajnegi.vo.SVNTag;
import org.xml.sax.InputSource;

public class SVNCodeMergeValidation {

	private static Properties props = new Properties();
	private static final String NEWLINE = "\n";
	private static List<SVNTag> tagsToValidate = new ArrayList();
	private static String svnLogCommand = "svn log --xml --stop-on-copy <<URL>> -v -r {FROM}:{TO}";
	private static String svnDiffCommand = "svn diff <<FILE1>> <<FILE2>>";
	private static String svnInfoCommand = "svn info <<URL>>";
	private static String asofDate = "";
	private static String toDate = "";
	private static String svnURL = "";
	private static String exemptFileList = "";
	private static String exemptProjectList = "";
	public static String ignoreKeysFromName = "";
	private static StringBuilder errMsg = new StringBuilder();
	private static boolean sendEmail = false;
	private static boolean validateURL = false;

	private static String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static String parentXMLElement = "Discrepancies";
	private static String discrepantXMLElement = "<Discrepancy><Tag>#TAG#</Tag><Revision>#REVISION#</Revision><Author>#AUTH#</Author><Modified_Date>#DATE#</Modified_Date><Discrepant_Files>#FILES#</Discrepant_Files><CheckIn_Comments>#COMMENTS#</CheckIn_Comments><Discrepant_Tags>#DISC#</Discrepant_Tags></Discrepancy>";
	private static String outputFile = "Discrepancies.xml";

	public static void main(String[] args) {

		if ((errMsg != null) && (errMsg.toString().length() > 0)) {
			System.out.println(errMsg);
			// sendEmail(errMsg.toString());
			System.exit(1);
		}

		asofDate = props.getProperty("AS_OF_DT") != null ? props.getProperty("AS_OF_DT").trim() : "";

		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		if ("".equalsIgnoreCase(asofDate)) {
			Calendar cal = Calendar.getInstance();
			cal.add(5, -1);
			asofDate = sdf.format(cal.getTime());
		}

		toDate = props.getProperty("TO_DATE") != null ? props.getProperty("TO_DATE").trim() : "";

		if ("".equalsIgnoreCase(toDate)) {
			Calendar cal = Calendar.getInstance();
			toDate = sdf.format(cal.getTime());
		}

		String tags = props.getProperty("TAGS") != null ? props.getProperty("TAGS").trim() : "";

		for (String str : tags.split(",")) {
			str = str.trim();
			SVNTag svntag = new SVNTag();
			svntag.setRepository(str);
			svntag.setDependency(props.getProperty(str) != null ? props.getProperty(str).trim() : "");
			svntag.setUrl(props.getProperty(str + "_URL") != null ? props.getProperty(str + "_URL").trim() : "");
			tagsToValidate.add(svntag);
		}

		String[] svnResult = (String[]) null;
		for (SVNTag svntag : tagsToValidate) {
			System.out.println(svntag);
			String svnLogCMD = svnLogCommand.replaceAll("<<URL>>", encloseInDoubleQuotes(svntag.getUrl()));
			svnLogCMD = svnLogCMD.replaceAll("FROM", asofDate);
			svnLogCMD = svnLogCMD.replaceAll("TO", toDate);
			System.out.println(
					" Checking revisions in " + svntag.getRepository() + " from " + asofDate + " to " + toDate);
			System.out.println(svnLogCMD);

			try {

				svnResult = (String[]) executeSVN(svnLogCMD, false);
				if (Boolean.parseBoolean(svnResult[0])) {

					System.out.println("Error while executing svn log command");
					errMsg.append(svnResult[1]);

				} else {
					List<Revision> revisionList = parseSVNLogResult(svnResult[1]);
					svntag.setRevisionList(revisionList);

					if ((revisionList != null) && (!revisionList.isEmpty())) {
						int m=0, k=0;
						Iterator<Revision> localIterator2 = revisionList.iterator();

						while (localIterator2.hasNext() && k < m) {
							{
								Revision rev = (Revision) localIterator2.next();
								System.out.println(rev);
								String dependency = svntag.getDependency();
								String[] arrayOfString2;
								m = (arrayOfString2 = dependency.split(",")).length;
								k = 0;
								String rep = arrayOfString2[k];
								rep = rep.trim();
								String url = props.getProperty(rep + "_URL") != null
										? props.getProperty(rep + "_URL").trim()
										: "";

								for (FilePath file : rev.getFilePathList()) {
									String fileName = file.getFilePath();

									if (isExempted(fileName)) {
										System.out.println("File " + fileName + " is ignored !!!");
									} else {
										String svnDiffCmd = svnDiffCommand.replaceAll("<<FILE1>>",
												encloseInDoubleQuotes(svntag.getUrl() + fileName));
										svnDiffCmd = svnDiffCmd.replaceAll("<<FILE2>>",
												encloseInDoubleQuotes(url + fileName));
										System.out.println(
												fileName + " comparing " + svntag.getRepository() + " and " + rep);
										System.out.println(svnDiffCmd);
										String[] diffResult = (String[]) executeSVN(svnDiffCmd, false);

										if ((diffResult != null) && (!Boolean.parseBoolean(diffResult[0]))
												&& (diffResult[1] != null) && (diffResult[1].trim().length() != 0)) {
											System.out.println("Diff found");
											file.setDiscrepant(true);
											rev.setDiscrepant(true);
											svntag.setDiscrepant(true);
											if ((rev.getDiscrepantTags() != null)
													&& (rev.getDiscrepantTags().indexOf(rep) == -1)) {
												rev.setDiscrepantTags(rev.getDiscrepantTags() + ", " + rep);
											} else {
												rev.setDiscrepantTags(rep);
											}
										} else if (Boolean.parseBoolean(diffResult[0])) {
											errMsg.append(diffResult[1]);
											System.out.println("Error while executing svn diff - " + diffResult[1]);
										} else {
											System.out.println("No Diff");
										}
									}
								}
								k++;
							}

						}

					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		String discrepancies = processDiscrepancies();
		System.out.println();
		System.out.println("Error Message == >");
		System.out.println(errMsg.toString());
		System.out.println();

		if ((discrepancies != null) && (discrepancies.trim().length() > 0)) {
			writeToFile(discrepancies);
		}
	}

	private static Object executeSVN(String command, boolean getBufferedReader) {

		BufferedReader bri = null;
		StringBuilder result = new StringBuilder(2000);
		String[] resultArr = new String[2];
		char[] buf = new char['Ѐ'];

		try {

			Process p = Runtime.getRuntime().exec(command);
			bri = new BufferedReader(new InputStreamReader(p.getInputStream()));

			if (getBufferedReader) {
				BufferedReader localBufferedReader1 = bri;
				return localBufferedReader1;
			}
			int numRead = 0;
			while ((numRead = bri.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				result.append(readData);
				buf = new char['Ѐ'];
				resultArr[0] = "false";
			}

			if (result.toString().trim().length() == 0) {
				bri = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				while ((numRead = bri.read(buf)) != -1) {
					String readData = String.valueOf(buf, 0, numRead);
					result.append(readData);
					buf = new char['Ѐ'];
					resultArr[0] = "true";
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();

			if ((!getBufferedReader) && (bri != null)) {
				try {
					bri.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} finally {
			if ((!getBufferedReader) && (bri != null)) {
				try {
					bri.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		resultArr[1] = result.toString();

		return resultArr;
	}

	private static List<Revision> parseSVNLogResult(String svnResult) {
		List<Revision> listRev = new ArrayList();
		SAXParserFactory factory = null;
		SAXParser saxParser = null;
		SaxHandler saxhandler = new SaxHandler();
		try {
			factory = SAXParserFactory.newInstance();
			saxParser = factory.newSAXParser();
			saxParser.parse(new InputSource(new StringReader(svnResult)), saxhandler);
			listRev = saxhandler.getRevisionList();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return listRev;
	}

	private static String processDiscrepancies() {
		StringBuilder msgXML = new StringBuilder(xmlHeader);
		StringBuilder filesForXML = new StringBuilder();
		String xmlResult = "";

		StringBuilder message = new StringBuilder();
		message.append("------------------------------- DISCREPANCIES -------------------------------").append("\n");

		msgXML.append("\n").append("<" + parentXMLElement + ">");
		Revision rev;
		for (SVNTag svntag : tagsToValidate) {
			message.append("TAG : " + svntag.getRepository()).append("\n");

			if (!svntag.isDiscrepant()) {
				message.append("No discrepancies observed !!").append("\n");
			} else {
				for (Iterator localIterator2 = svntag.getRevisionList().iterator(); localIterator2.hasNext();) {
					rev = (Revision) localIterator2.next();
					if (rev.isDiscrepant()) {

						filesForXML = new StringBuilder();
						msgXML.append("\n").append(discrepantXMLElement.replaceAll("#TAG#", svntag.getRepository())
								.replaceAll("#REVISION#", rev.getRevision()).replaceAll("#AUTH#", rev.getAuthor())
								.replaceAll("#DATE#", rev.getModifiedDate()).replace("#COMMENTS#", rev.getComments())
								.replaceAll("#DISC#", rev.getDiscrepantTags()));

						message.append("-------------------------------------------------------------").append("\n");
						message.append("Revision : " + rev.getRevision());
						message.append("\n");
						message.append("Author : " + rev.getAuthor());
						message.append("\n");
						message.append("Date Modified : " + rev.getModifiedDate());
						message.append("\n");
						message.append("Path : ");
						message.append("\n");
						for (FilePath path : rev.getFilePathList()) {
							if (path.isDiscrepant()) {
								filesForXML.append(path).append("\n");
								message.append(path);
								message.append("\n");
							}
						}
						message.append("Comments : " + rev.getComments());
						message.append("\n");
						message.append("Discrepant tags : " + rev.getDiscrepantTags()).append("\n");

						msgXML = new StringBuilder(msgXML.toString().replaceAll("#FILES#", filesForXML.toString()));

						xmlResult = msgXML.toString();
					}
				}
			}
		}

		xmlResult = xmlResult + "</" + parentXMLElement + ">";
		message.append("------------------------------- DISCREPANCIES END-------------------------------");

		StringBuilder guidelines = new StringBuilder();
		guidelines.append("Code Merge Guidelines - ").append("\n");
		for (SVNTag svntag : tagsToValidate) {
			guidelines.append(props.getProperty("GUIDELINES_TEXT").replaceAll("<<MAINTAG>>", svntag.getRepository())
					.replaceAll("<<DEPENDENTTAGS>>", svntag.getDependency())).append("\n");
		}

		message.append("\n").append("\n").append("\n").append(guidelines.toString());

		return xmlResult;
	}

	private static boolean isExempted(String fileName) {
		if (exemptFileList.length() != 0) {
			for (String key : exemptFileList.split(",")) {
				key = key.trim();
				if (fileName.indexOf(key) != -1) {
					return true;
				}
			}
		}

		if (exemptProjectList.length() != 0) {
			for (String key : exemptProjectList.split(",")) {
				key = key.trim();

				if ((fileName.startsWith(key)) || (fileName.startsWith("/" + key))) {
					return true;
				}
			}
		}

		return false;
	}

	private static void loadAndValidateProperties() {
		System.out.println("Loading and validating properties...");

		try {
			props.load(new FileInputStream(new File("Guidelines.properties")));

			String temp = props.getProperty("SEND_EMAIL") != null ? props.getProperty("SEND_EMAIL").trim() : "";
			sendEmail = "Y".equalsIgnoreCase(temp);

			temp = props.getProperty("VALIDATE_URL") != null ? props.getProperty("VALIDATE_URL").trim() : "";
			validateURL = "Y".equalsIgnoreCase(temp);

			svnURL = props.getProperty("SVN_URL") != null ? props.getProperty("SVN_URL").trim() : "";
			asofDate = props.getProperty("AS_OF_DT") != null ? props.getProperty("AS_OF_DT").trim() : "";
			exemptFileList = props.getProperty("EXEMPT_FILES_KEYWORDS") != null
					? props.getProperty("EXEMPT_FILES_KEYWORDS").trim()
					: "";
			exemptProjectList = props.getProperty("EXEMPT_PROJECTS") != null
					? props.getProperty("EXEMPT_PROJECTS").trim()
					: "";
			ignoreKeysFromName = props.getProperty("IGNORE_KEYS_FROM_NAME") != null
					? props.getProperty("IGNORE_KEYS_FROM_NAME").trim()
					: "";

			String userName = props.getProperty("SVN_USER_NAME") != null ? props.getProperty("SVN_USER_NAME").trim()
					: "";
			String password = props.getProperty("SVN_PASS") != null ? props.getProperty("SVN_PASS") : "";

			temp = props.getProperty("IGNORE_SPACES_EOL") != null ? props.getProperty("IGNORE_SPACES_EOL").trim() : "";
			boolean ignoreSpaces = "Y".equalsIgnoreCase(temp);

			if (userName.length() != 0) {
				svnLogCommand = svnLogCommand.replaceAll("svn",
						"svn --username=" + userName + " --password=" + password);
				svnDiffCommand = svnDiffCommand.replaceAll("svn",
						"svn --username=" + userName + " --password=" + password);
				svnInfoCommand = svnInfoCommand.replaceAll("svn",
						"svn --username=" + userName + " --password=" + password);
			}

			if (ignoreSpaces) {
				svnDiffCommand = svnDiffCommand.replaceAll("diff", "diff --diff-cmd diff -x -ewbB");
			}

			String tags = props.getProperty("TAGS") != null ? props.getProperty("TAGS").trim() : "";

			if ((tags == null) || (tags.trim().length() == 0)) {
				errMsg.append("Missing key/value - TAGS");
				return;
			}

			for (String tag : tags.split(",")) {
				tag = tag.trim();
				String depends = props.getProperty(tag);
				if ((depends == null) || (depends.trim().length() == 0)) {
					errMsg.append("\n");
					errMsg.append("Missing dependencies for " + tag + ". Missing key - " + tag);
				} else {
					for (String dep : depends.split(",")) {
						dep = dep.trim();
						String depUrl = props.getProperty(dep + "_URL");
						if ((depUrl == null) || (depUrl.trim().length() == 0)) {
							errMsg.append("\n");
							errMsg.append("Missing key/value for " + dep + "_URL" + ". Please specify svn URL.");
						} else if (!isURLValid(depUrl)) {
							if (errMsg.toString().indexOf("E170001") != -1) {
								return;
							}

							errMsg.append("\n");
							errMsg.append("Unable to connect. Please check the SVN URL");
							errMsg.append("\n");
							errMsg.append(dep + "_URL = " + depUrl);
						}
					}
				}
				String url = props.getProperty(tag + "_URL");
				if ((url == null) || (url.trim().length() == 0)) {
					errMsg.append("\n");
					errMsg.append("Missing key/value for " + tag + "_URL" + ". Please specify svn URL.");
				} else if (!isURLValid(url)) {
					if (errMsg.toString().indexOf("E170001") != -1) {
						return;
					}

					errMsg.append("\n");
					errMsg.append("Unable to connect. Please check the SVN URL");
					errMsg.append("\n");
					errMsg.append(tag + "_URL = " + url);
				}
			}

			if ((svnURL == null) || (svnURL.trim().length() == 0)) {
				errMsg.append("\n");
				errMsg.append("Missing key/value - SVN_URL.");

			}

		} catch (IOException ex) {

			ex.printStackTrace();
			errMsg.append("Unable to load properties - " + ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			errMsg.append("Error while reading properties - " + ex.getMessage());
		}
	}

	public static String getIgnoreKeysFromName() {
		return ignoreKeysFromName;
	}

	private static boolean isURLValid(String url) {
		if (!validateURL) {
			return true;
		}

		String svnInfoCmd = svnInfoCommand.replaceAll("<<URL>>", encloseInDoubleQuotes(url));
		String[] svnInfoRes = (String[]) executeSVN(svnInfoCmd, false);

		if ((svnInfoRes != null) && (Boolean.parseBoolean(svnInfoRes[0])) && (svnInfoRes[1] != null)
				&& (svnInfoRes[1].indexOf("Repository UUID") == -1)) {
			errMsg.append(svnInfoRes[1]);
			return false;
		}

		return true;
	}

	private static String encloseInDoubleQuotes(String input) {
		return "\"" + input + "\"";
	}

	private static void writeToFile(String content) {
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(outputFile);
			bw = new BufferedWriter(fw);
			bw.write(content);
			System.out.println("Done writing to " + outputFile);
		} catch (IOException e) {
			e.printStackTrace();
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}
