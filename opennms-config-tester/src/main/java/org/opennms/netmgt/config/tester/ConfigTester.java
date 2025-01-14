/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.netmgt.config.tester;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.opennms.core.db.DataSourceFactory;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.utils.ConfigFileConstants;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.tester.checks.ConfigCheckValidationException;
import org.opennms.netmgt.config.tester.checks.PropertiesFileChecker;
import org.opennms.netmgt.config.tester.checks.XMLFileChecker;
import org.opennms.netmgt.filter.FilterDaoFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ConfigTester implements ApplicationContextAware {

	private Logger log = Logger.getLogger(ConfigTester.class.getName());

	private ApplicationContext m_context;
	private Map<String, String> m_configs;
	private final static Pattern DIRECTORY_AND_CLASS_PATTERN = Pattern.compile("^directory:(.+)$");

	public Map<String, String> getConfigs() {
		return m_configs;
	}

	public void setConfigs(Map<String, String> configs) {
		m_configs = configs;
	}

	public ApplicationContext getApplicationContext() {
		return m_context;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		m_context = context;
	}

	public void testConfig(final String name, final boolean ignoreUnknown) {
		checkConfigNameValid(name, ignoreUnknown);
		final String beanName = m_configs.get(name);
		final Matcher matcher = DIRECTORY_AND_CLASS_PATTERN.matcher(beanName);
		if (matcher.matches()) {
			final String xmlModelName = matcher.group(1);
			checkDirectoryForXmlFiles(name, xmlModelName);
		} else if("directory".equalsIgnoreCase(beanName)){
			checkDirectoryForUnKnownFiles(name);
		} else if ("unknown".equalsIgnoreCase(beanName)){
			checkFileForSyntax(name);
		} else {
			m_context.getBean(beanName);
		}
	}

	private void checkDirectoryForXmlFiles(final String name, final String xmlModelName) {
		final Path directory = Paths.get(ConfigFileConstants.getFilePathString(), name);

		if (!Files.exists(directory)) {
			return;
		}

		final Class clazz;
		try {
			clazz = Class.forName(xmlModelName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.xml")) {
			for (final Path entry : stream) {
				try {
					JaxbUtils.unmarshal(clazz, entry.toFile(), true);
				} catch (final Exception e) {
					throw new ConfigCheckValidationException("File " + entry.toString() + " not valid!", e);
				}
			}
		} catch (final IOException e) {
			throw new ConfigCheckValidationException(e);
		}
	}

	private void checkDirectoryForUnKnownFiles(String name) {
		Path directory = Paths.get(ConfigFileConstants.getFilePathString(), name);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{properties,xml}")) {
			for (Path entry : stream) {
				checkFileForSyntax(entry);
			}
		} catch (IOException e) {
			throw new ConfigCheckValidationException(e);
		}
	}

	private void checkFileForSyntax(String fileName) {
		File file;
		try {
			file = ConfigFileConstants.getConfigFileByName(fileName);
			checkFileForSyntax(file.toPath());
		} catch (IOException e) {
			throw new ConfigCheckValidationException(e);
		}
	}

	private void checkFileForSyntax(Path file) {
		if(file.toString().endsWith("properties")){
            PropertiesFileChecker.checkFile(file).forSyntax();
		} else if(file.toString().endsWith("xml")){
			XMLFileChecker.checkFile(file).forSyntax();
		} else {
			log.warning("No FileChecker for file found: " + file.toAbsolutePath().toString());
		}
	}

	private void checkConfigNameValid(String name, boolean ignoreUnknown) {
		if (!m_configs.containsKey(name)) {
			if (ignoreUnknown) {
				System.err.println("Unknown configuration: " + name + "... skipping.");
			} else {
				throw new IllegalArgumentException("config '" + name + "' is not a known config name");
			}
		}
	}

	public static void main(String[] argv) {

        FilterDaoFactory.setInstance(new ConfigTesterFilterDao());
        DataSourceFactory.setInstance(new ConfigTesterDataSource());
		ConfigTester tester = BeanUtils.getBean("configTesterContext", "configTester", ConfigTester.class);

		final CommandLineParser parser = new PosixParser();

		final Options options = new Options();
		options.addOption("h", "help",           false, "print this help and exit");
		options.addOption("a", "all",         	 false, "check all supported configuration files");
		options.addOption("l", "list",   		 false, "list supported configuration files and exit");
		options.addOption("v", "verbose", 		 false, "list each configuration file as it is tested");
		options.addOption("i", "ignore-unknown", false, "ignore unknown configuration files and continue processing");

		final CommandLine line;
		try {
			line = parser.parse(options, argv, false);
		} catch (ParseException e) {
			System.err.println("Invalid usage: " + e.getMessage());
			System.err.println("Run 'config-tester -h' for help.");
			System.exit(1);
			
			return; // not reached; here to eliminate warning on line being uninitialized
		}

		final boolean ignoreUnknown = line.hasOption("i");

		if ((line.hasOption('l') || line.hasOption('h') || line.hasOption('a'))) {
			if (line.getArgList().size() > 0) {
				System.err.println("Invalid usage: No arguments allowed when using the '-a', '-h', or '-l' options.");
				System.err.println("Run 'config-tester -h' for help.");
				System.exit(1);
			}
		} else {
			if (line.getArgs().length == 0) {
				System.err.println("Invalid usage: too few arguments.  Use the '-h' option for help.");
				System.exit(1);
			}
		}
		
		boolean verbose = line.hasOption('v');

		DataSourceFactory.setInstance(new ConfigTesterDataSource());

		if (line.hasOption('l')) {
			System.out.println("Supported configuration files: ");
			for (String configFile : tester.getConfigs().keySet()) {
				System.out.println("    " + configFile);
			}
			System.out.println("Note: not all OpenNMS configuration files are currently supported.");
		} else if (line.hasOption('h')) {
			 final HelpFormatter formatter = new HelpFormatter();
			 formatter.printHelp("config-tester -a\nOR: config-tester [config files]\nOR: config-tester -l\nOR: config-tester -h", options);
		} else if (line.hasOption('a')) {
			for (String configFile : tester.getConfigs().keySet()) {
				tester.testConfig(configFile, verbose, ignoreUnknown);
			}
			System.out.println("Test is finished, found no problems.");
		} else {
			for (String configFile : line.getArgs()) {
				tester.testConfig(configFile, verbose, ignoreUnknown);
			}
			System.out.println("Test is finished, found no problems.");
		}
	}

	private void testConfig(String configFile, boolean verbose, boolean ignoreUnknown) {
		if (verbose) {
			System.out.print("Testing " + configFile + " ... ");
		}
		
		long start = System.currentTimeMillis();
		testConfig(configFile, ignoreUnknown);
		long end = System.currentTimeMillis();
		
		if (verbose) {
			System.out.println("OK (" + (((float) (end - start)) / 1000) + "s)");
		}
	}
}
