/**
 * Copyright 2012-2017 Thales Services SAS.
 *
 * This file is part of AuthzForce CE.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ow2.authzforce.core.pdp.testutil;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.Attributes;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.Request;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.Response;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.Result;

import org.ow2.authzforce.core.pdp.api.JaxbXACMLUtils;
import org.ow2.authzforce.core.pdp.api.XMLUtils.NamespaceFilteringParser;
import org.ow2.authzforce.core.pdp.impl.BasePdpEngine;
import org.ow2.authzforce.core.pdp.impl.DefaultEnvironmentProperties;
import org.ow2.authzforce.core.pdp.testutil.ext.xmlns.TestAttributeProvider;
import org.ow2.authzforce.core.xmlns.pdp.Pdp;
import org.ow2.authzforce.core.xmlns.pdp.StaticRefPolicyProvider;
import org.ow2.authzforce.core.xmlns.pdp.StaticRootPolicyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

public class TestUtils
{

	/**
	 * JAXB context for (un)marshalling TestAttributeProvider configuration
	 */
	public static final JAXBContext TEST_ATTRIBUTE_PROVIDER_JAXB_CONTEXT;
	static
	{
		try
		{
			TEST_ATTRIBUTE_PROVIDER_JAXB_CONTEXT = JAXBContext.newInstance(TestAttributeProvider.class);
		}
		catch (final JAXBException e)
		{
			throw new RuntimeException("Error instantiating JAXB context for unmarshalling TestAttributeProvider configurations", e);
		}
	}

	/**
	 * the logger we'll use for all messages
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);

	/**
	 * This creates the XACML request from file on classpath
	 * 
	 * @param requestFileLocation
	 *            file path (with Spring-supported URL prefixes: 'classpath:', etc.) path to the request file, relative to classpath
	 * @param unmarshaller
	 *            XACML unmarshaller
	 * @return the XML/JAXB Request or null if any error
	 * @throws JAXBException
	 *             error reading XACML 3.0 Request from the file at {@code requestFileLocation}
	 * @throws FileNotFoundException
	 *             no file found at {@code requestFileLocation}
	 */
	public static Request createRequest(final String requestFileLocation, final NamespaceFilteringParser unmarshaller) throws JAXBException, FileNotFoundException
	{
		/**
		 * Get absolute path/URL to request file in a portable way, using current class loader. As per javadoc, the name of the resource passed to ClassLoader.getResource() is a '/'-separated path
		 * name that identifies the resource. So let's build it. Note: do not use File.separator as path separator, as it will be turned into backslash "\\" on Windows, and will be URL-encoded (%5c)
		 * by the getResource() method (not considered path separator by this method), and file will not be found as a result.
		 */
		final URL requestFileURL = ResourceUtils.getURL(requestFileLocation);
		if (requestFileURL == null)
		{
			throw new FileNotFoundException("No XACML Request file found at location: 'classpath:" + requestFileLocation + "'");
		}

		LOGGER.debug("Request file to read: {}", requestFileURL);
		final Request request = (Request) unmarshaller.parse(requestFileURL);
		return request;
	}

	/**
	 * This creates the XACML response from file on classpath
	 * 
	 * @param responseFileLocation
	 *            path to the response file (with Spring-supported URL prefixes: 'classpath:', etc.)
	 * @param unmarshaller
	 *            XACML unmarshaller
	 * @return the XML/JAXB Response or null if any error
	 * @throws JAXBException
	 *             error reading XACML 3.0 Request from the file at {@code responseFileLocation}
	 * @throws FileNotFoundException
	 *             no file found at {@code responseFileLocation}
	 */
	public static Response createResponse(final String responseFileLocation, final NamespaceFilteringParser unmarshaller) throws JAXBException, FileNotFoundException
	{
		/**
		 * Get absolute path/URL to response file in a portable way, using current class loader. As per javadoc, the name of the resource passed to ClassLoader.getResource() is a '/'-separated path
		 * name that identifies the resource. So let's build it. Note: do not use File.separator as path separator, as it will be turned into backslash "\\" on Windows, and will be URL-encoded (%5c)
		 * by the getResource() method (not considered path separator by this method), and file will not be found as a result.
		 */
		final URL responseFileURL = ResourceUtils.getURL(responseFileLocation);
		LOGGER.debug("Response file to read: {}", responseFileLocation);
		final Response response = (Response) unmarshaller.parse(responseFileURL);
		return response;
	}

	public static String printResponse(final Response response)
	{
		final StringWriter writer = new StringWriter();
		try
		{
			final Marshaller marshaller = JaxbXACMLUtils.createXacml3Marshaller();
			marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			marshaller.marshal(response, writer);
		}
		catch (final Exception e)
		{
			LOGGER.error("Error marshalling Response", e);
		}

		return writer.toString();
	}

	/**
	 * Normalize a XACML response for comparison with another normalized one. In particular, it removes every Result's status as we choose to ignore the Status. Indeed, a PDP implementation might
	 * return a perfectly XACML-compliant response but with extra StatusCode/Message/Detail that we would not expect.
	 * 
	 * @param response
	 *            input XACML Response
	 * @return normalized response
	 */
	public static Response normalizeForComparison(final Response response)
	{
		final List<Result> results = new ArrayList<>();
		/*
		 * We iterate over all results, because for each results, we don't compare everything. In particular, we choose to ignore the Status. Indeed, a PDP implementation might return a perfectly
		 * XACML-compliant response but with extra StatusCode/Message/Detail that we would not expect.
		 */
		for (final Result result : response.getResults())
		{
			// We ignore the status, so set it to null in both expected and tested response to avoid
			// Status comparison
			results.add(new Result(result.getDecision(), null, result.getObligations(), result.getAssociatedAdvice(), normalizeAttributeCategories(result.getAttributes()), result
					.getPolicyIdentifierList()));
		}

		return new Response(results);
	}

	private static final Comparator<Attributes> ATTRIBUTES_COMPARATOR = new Comparator<Attributes>()
	{

		@Override
		public int compare(final Attributes arg0, final Attributes arg1)
		{
			if (arg0 == null || arg1 == null)
			{
				throw new IllegalArgumentException("Invalid Attribtues args for comparator");
			}

			return arg0.getCategory().compareTo(arg1.getCategory());
		}

	};

	private static List<Attributes> normalizeAttributeCategories(final List<Attributes> attributesList)
	{
		// Attributes categories may be in different order than expected although it is still compliant (order does not matter to the spec)
		// always use the same order (lexicographical here)
		final SortedSet<Attributes> sortedSet = new TreeSet<>(ATTRIBUTES_COMPARATOR);
		for (final Attributes attributes : attributesList)
		{
			sortedSet.add(attributes);
		}

		return new ArrayList<>(sortedSet);
	}

	/**
	 * Creates PDP from root policy file
	 * 
	 * @param rootPolicyLocation
	 *            root XACML policy location (with Spring-supported URL prefixes: 'classpath:', etc.)
	 * @param refPoliciesDirectoryLocation
	 *            (optional) directory containing files of XACML Policy(Set) that can be referred to from root policy at {@code policyLocation} via Policy(Set)IdReference; required only if there is
	 *            any Policy(Set)IdReference in {@code rootPolicyLocation} to resolve. If file not found, support for Policy(Set)IdReference is disabled, i.e. any presence of such reference is
	 *            considered invalid.
	 * @param enableXPath
	 *            Enable support for AttributeSelectors and xpathExpression datatype. Reminder: AttributeSelector and xpathExpression datatype support are marked as optional in XACML 3.0 core
	 *            specification, so set this to false if you are testing mandatory features only.
	 * @param attributeProviderConfLocation
	 *            (optional) {@link TestAttributeProvider} XML configuration location
	 * @param requestFilterId
	 *            RequestFilter ID
	 * @return PDP instance
	 * @throws IllegalArgumentException
	 *             invalid XACML policy located at {@code policyLocation}
	 * @throws IOException
	 *             if error closing some resources used by the PDP after {@link IllegalArgumentException} occurred
	 * @throws URISyntaxException
	 * @throws JAXBException
	 */
	public static BasePdpEngine getPDPNewInstance(final String rootPolicyLocation, final String refPoliciesDirectoryLocation, final boolean enableXPath, final String attributeProviderConfLocation,
			final String requestFilterId) throws IllegalArgumentException, IOException, URISyntaxException, JAXBException
	{
		final Pdp jaxbPDP = new Pdp();
		jaxbPDP.setEnableXPath(enableXPath);

		/**
		 * Get absolute path/URL to PolicySet file and, if any, the directory of referenceable sub-PolicySets, in a portable way, using current class loader. As per javadoc, the name of the resource
		 * passed to ClassLoader.getResource() is a '/'-separated path name that identifies the resource. So let's build it. Note: do not use File.separator as path separator, as it will be turned
		 * into backslash "\\" on Windows, and will be URL-encoded (%5c) by the getResource() method (not considered path separator by this method), and file will not be found as a result.
		 */
		if (refPoliciesDirectoryLocation != null)
		{
			URL refPoliciesDirectoryURL = null;
			try
			{
				refPoliciesDirectoryURL = ResourceUtils.getURL(refPoliciesDirectoryLocation);
			}
			catch (final FileNotFoundException e)
			{
				LOGGER.info("No refPolicies directory: {} -> Policy(Set)IdReference(s) not supported for this test.", refPoliciesDirectoryLocation);
			}

			if (refPoliciesDirectoryURL != null)
			{
				final StaticRefPolicyProvider jaxbRefPolicyProvider = new StaticRefPolicyProvider();
				jaxbRefPolicyProvider.setId("refPolicyProvider");
				final List<String> jaxbRefPolicyProviderPolicyLocations = jaxbRefPolicyProvider.getPolicyLocations();
				final Path refPoliciesDirectoryPath = Paths.get(refPoliciesDirectoryURL.toURI());
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(refPoliciesDirectoryPath))
				{
					for (final Path path : stream)
					{
						if (Files.isRegularFile(path))
						{
							jaxbRefPolicyProviderPolicyLocations.add(path.toString());
						}
					}
				}
				catch (final DirectoryIteratorException ex)
				{
					// I/O error encounted during the iteration, the cause is an IOException
					throw ex.getCause();
				}

				// set max PolicySet reference depth to max possible depth automatically
				if (!jaxbRefPolicyProviderPolicyLocations.isEmpty())
				{
					jaxbPDP.setMaxPolicyRefDepth(BigInteger.valueOf(jaxbRefPolicyProviderPolicyLocations.size()));
					jaxbPDP.setRefPolicyProvider(jaxbRefPolicyProvider);
				}
			}
		}

		final URL rootPolicyFileURL = ResourceUtils.getURL(rootPolicyLocation);
		final StaticRootPolicyProvider jaxbRootPolicyProvider = new StaticRootPolicyProvider();
		jaxbRootPolicyProvider.setId("rootPolicyProvider");
		jaxbRootPolicyProvider.setPolicyLocation(rootPolicyFileURL.toString());
		jaxbPDP.setRootPolicyProvider(jaxbRootPolicyProvider);

		// test attribute provider
		if (attributeProviderConfLocation != null)
		{
			try
			{
				final URL testAttrProviderURL = ResourceUtils.getURL(attributeProviderConfLocation);
				final Unmarshaller unmarshaller = TEST_ATTRIBUTE_PROVIDER_JAXB_CONTEXT.createUnmarshaller();
				final JAXBElement<TestAttributeProvider> testAttributeProviderElt = (JAXBElement<TestAttributeProvider>) unmarshaller.unmarshal(testAttrProviderURL);
				jaxbPDP.getAttributeProviders().add(testAttributeProviderElt.getValue());
			}
			catch (final FileNotFoundException e)
			{
				LOGGER.info("No test attribute provider configuration found at: {} -> TestAttributeProvider not supported for this test.", attributeProviderConfLocation);
			}
		}

		// request filter
		if (requestFilterId != null)
		{
			jaxbPDP.setRequestFilter(requestFilterId);
		}

		return BasePdpEngine.getInstance(jaxbPDP, new DefaultEnvironmentProperties());
	}

	/**
	 * assertEquals() for XACML responses (handles normalization of the responses)
	 * 
	 * @param testId
	 *            test identifier
	 * @param expectedResponse
	 *            expected response
	 * @param actualResponseFromPDP
	 *            actual response
	 * @throws JAXBException
	 */
	public static void assertNormalizedEquals(final String testId, final Response expectedResponse, final Response actualResponseFromPDP) throws JAXBException
	{
		if (testId == null)
		{
			throw new IllegalArgumentException("Undefined test ID");
		}

		if (expectedResponse == null)
		{
			throw new IllegalArgumentException("Undefined expected response for response equality check");
		}

		if (actualResponseFromPDP == null)
		{
			throw new IllegalArgumentException("Undefined actual response  for response equality check");
		}

		// normalize responses for comparison
		final Response normalizedExpectedResponse = TestUtils.normalizeForComparison(expectedResponse);
		final Response normalizedActualResponse = TestUtils.normalizeForComparison(actualResponseFromPDP);
		assertEquals("Test '" + testId + "' (Status elements removed/ignored for comparison): ", normalizedExpectedResponse, normalizedActualResponse);
	}
}
