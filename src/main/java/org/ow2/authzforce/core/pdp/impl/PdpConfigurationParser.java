/**
 * Copyright (C) 2011-2015 Thales Services SAS.
 *
 * This file is part of AuthZForce.
 *
 * AuthZForce is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * AuthZForce is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with AuthZForce. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.authzforce.core.pdp.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.ow2.authzforce.core.pdp.api.CombiningAlg;
import org.ow2.authzforce.core.pdp.api.CombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.api.Datatype;
import org.ow2.authzforce.core.pdp.api.DatatypeFactory;
import org.ow2.authzforce.core.pdp.api.DatatypeFactoryRegistry;
import org.ow2.authzforce.core.pdp.api.DecisionResultFilter;
import org.ow2.authzforce.core.pdp.api.EnvironmentProperties;
import org.ow2.authzforce.core.pdp.api.EnvironmentPropertyName;
import org.ow2.authzforce.core.pdp.api.FirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.Function;
import org.ow2.authzforce.core.pdp.api.FunctionSet;
import org.ow2.authzforce.core.pdp.impl.combining.BaseCombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.impl.combining.StandardCombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.impl.func.FunctionRegistry;
import org.ow2.authzforce.core.pdp.impl.func.StandardFunctionRegistry;
import org.ow2.authzforce.core.pdp.impl.value.BaseDatatypeFactoryRegistry;
import org.ow2.authzforce.core.pdp.impl.value.StandardDatatypeFactoryRegistry;
import org.ow2.authzforce.core.xmlns.pdp.Pdp;
import org.ow2.authzforce.xacml.identifiers.XACMLDatatypeId;
import org.ow2.authzforce.xmlns.pdp.ext.AbstractDecisionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

/**
 * XML-based PDP Configuration parser
 * 
 */
public class PdpConfigurationParser
{
	private static final IllegalArgumentException NULL_PDP_MODEL_HANDLER_ARGUMENT_EXCEPTION = new IllegalArgumentException(
			"Undefined PDP configuration model handler");
	private final static Logger LOGGER = LoggerFactory.getLogger(PdpConfigurationParser.class);

	/**
	 * Create PDP instance.
	 * 
	 * @param confLocation
	 *            location of PDP configuration XML file, compliant with the PDP XML schema (pdp.xsd). This location may be any resource string supported by
	 *            Spring ResourceLoader. For example: classpath:com/myapp/aaa.xsd, file:///data/bbb.xsd, http://myserver/ccc.xsd... More info:
	 *            http://docs.spring.io/spring/docs/current/spring-framework-reference/html/resources.html
	 * @return PDP instance
	 * 
	 * @throws IOException
	 *             I/O error reading from {@code confLocation}
	 * @throws IllegalArgumentException
	 *             Invalid PDP configuration at {@code confLocation}
	 * 
	 */
	public static PDPImpl getPDP(String confLocation) throws IOException, IllegalArgumentException
	{
		return getPDP(confLocation, null, null);
	}

	/**
	 * Create PDP instance. Locations here may be any resource string supported by Spring ResourceLoader. More info:
	 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/resources.html
	 * 
	 * For example: classpath:com/myapp/aaa.xsd, file:///data/bbb.xsd, http://myserver/ccc.xsd...
	 * 
	 * @param confLocation
	 *            location of PDP configuration XML file, compliant with the PDP XML schema (pdp.xsd)
	 * 
	 * @param extensionXsdLocation
	 *            location of user-defined extension XSD (may be null if no extension to load), if exists; in such XSD, there must be a XSD import for each
	 *            extension, where the 'schemaLocation' attribute value must be ${fully_qualidifed_jaxb_class_bound_to_extension_XML_type}.xsd, for example:
	 * 
	 *            <pre>
	 * {@literal
	 * 		  <?xml version="1.0" encoding="UTF-8"?> 
	 * 		  <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
	 *            targetNamespace="http://thalesgroup.com/authzforce/model/3.0"
	 *            xmlns:tns="http://thalesgroup.com/authzforce/model/3.0"
	 *            elementFormDefault="qualified" attributeFormDefault="unqualified">
	 * 
	 *            <xs:import
	 *            namespace="http://thalesgroup.com/authzforce/model/3.0/Provider/attribute/rest"
	 *            schemaLocation=
	 *            "com.thalesgroup.authzforce.model._3_0.Provider.attribute.rest.RESTfulAttributeProvider.xsd"
	 *            />
	 * 
	 *            </xs:schema>
	 * 			}
	 * </pre>
	 * 
	 *            In this example, 'com.thalesgroup.authzforce.model._3_0.Provider.attribute.rest.RESTfulAttributeFinde r ' is the JAXB-annotated class bound to
	 *            XML type 'RESTfulAttributeProvider'. We assume that this XML type is an extension of one the PDP extension base types,
	 *            'AbstractAttributeProvider' (that extends 'AbstractPdpExtension' like all other extension base types) in this case.
	 * 
	 * @param catalogLocation
	 *            location of XML catalog for resolving XSDs imported by the pdp.xsd (PDP configuration schema) and the extension XSD specified as
	 *            'extensionXsdLocation' argument (may be null)
	 * @return PDP instance
	 * @throws IOException
	 *             I/O error reading from {@code confLocation}
	 * @throws IllegalArgumentException
	 *             Invalid PDP configuration at {@code confLocation}
	 * 
	 */
	public static PDPImpl getPDP(String confLocation, String catalogLocation, String extensionXsdLocation) throws IOException, IllegalArgumentException
	{
		return getPDP(confLocation, new PdpModelHandler(catalogLocation, extensionXsdLocation));
	}

	/**
	 * Create PDP instance. Locations here can be any resource string supported by Spring ResourceLoader. More info:
	 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/resources.html
	 * 
	 * For example: classpath:com/myapp/aaa.xsd, file:///data/bbb.xsd, http://myserver/ccc.xsd...
	 * 
	 * @param confFile
	 *            PDP configuration XML file, compliant with the PDP XML schema (pdp.xsd)
	 * 
	 * @param extensionXsdLocation
	 *            location of user-defined extension XSD (may be null if no extension to load), if exists; in such XSD, there must be a XSD import for each
	 *            extension, where the 'schemaLocation' attribute value must be ${fully_qualidifed_jaxb_class_bound_to_extension_XML_type}.xsd, for example:
	 * 
	 *            <pre>
	 * {@literal
	 * 		  <?xml version="1.0" encoding="UTF-8"?> 
	 * 		  <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
	 *            targetNamespace="http://thalesgroup.com/authzforce/model/3.0"
	 *            xmlns:tns="http://thalesgroup.com/authzforce/model/3.0"
	 *            elementFormDefault="qualified" attributeFormDefault="unqualified">
	 * 
	 *            <xs:import
	 *            namespace="http://thalesgroup.com/authzforce/model/3.0/Provider/attribute/rest"
	 *            schemaLocation=
	 *            "com.thalesgroup.authzforce.model._3_0.Provider.attribute.rest.RESTfulAttributeProvider.xsd"
	 *            />
	 * 
	 *            </xs:schema>
	 * 			}
	 * </pre>
	 * 
	 *            In this example, 'com.thalesgroup.authzforce.model._3_0.Provider.attribute.rest.RESTfulAttributeFinde r ' is the JAXB-annotated class bound to
	 *            XML type 'RESTfulAttributeProvider'. We assume that this XML type is an extension of one the PDP extension base types,
	 *            'AbstractAttributeProvider' (that extends 'AbstractPdpExtension' like all other extension base types) in this case.
	 * 
	 * @param catalogLocation
	 *            location of XML catalog for resolving XSDs imported by the pdp.xsd (PDP configuration schema) and the extension XSD specified as
	 *            'extensionXsdLocation' argument (may be null)
	 * @return PDP instance
	 * @throws IOException
	 *             I/O error reading from {@code confLocation}
	 * @throws IllegalArgumentException
	 *             Invalid PDP configuration at {@code confLocation}
	 * 
	 */
	public static PDPImpl getPDP(File confFile, String catalogLocation, String extensionXsdLocation) throws IOException, IllegalArgumentException
	{
		return getPDP(confFile, new PdpModelHandler(catalogLocation, extensionXsdLocation));
	}

	/**
	 * Create PDP instance. Locations here can be any resource string supported by Spring ResourceLoader. More info:
	 * http://docs.spring.io/spring/docs/current/spring-framework-reference/html/resources.html.
	 * <p>
	 * To allow using file paths relative to the parent folder of the configuration file (located at confLocation) anywhere in this configuration file
	 * (including in PDP extensions'), we define a property 'PARENT_DIR', so that the placeholder ${PARENT_DIR} can be used as prefix for file paths in the
	 * configuration file. E.g. if confLocation = 'file:///path/to/configurationfile', then ${PARENT_DIR} will be replaced by 'file:///path/to'. If confLocation
	 * is not a file on the filesystem, then ${PARENT_DIR} is undefined.
	 * 
	 * @param confLocation
	 *            location of PDP configuration file
	 * @param modelHandler
	 *            PDP configuration model handler
	 * @return PDP instance
	 * @throws IOException
	 *             I/O error reading from {@code confLocation}
	 * @throws IllegalArgumentException
	 *             Invalid PDP configuration at {@code confLocation}
	 */
	public static PDPImpl getPDP(String confLocation, PdpModelHandler modelHandler) throws IOException, IllegalArgumentException
	{
		File confFile = null;
		try
		{
			confFile = ResourceUtils.getFile(confLocation);
		} catch (FileNotFoundException e)
		{
			throw new IllegalArgumentException("Invalid PDP configuration location: " + confLocation, e);
		}

		return getPDP(confFile, modelHandler);
	}

	/**
	 * Create PDP instance
	 * <p>
	 * To allow using file paths relative to the parent folder of the configuration file (located at confLocation) anywhere in this configuration file
	 * (including in PDP extensions'), we define a property 'PARENT_DIR', so that the placeholder ${PARENT_DIR} can be used as prefix for file paths in the
	 * configuration file. E.g. if confLocation = 'file:///path/to/configurationfile', then ${PARENT_DIR} will be replaced by 'file:///path/to'. If confLocation
	 * is not a file on the filesystem, then ${PARENT_DIR} is undefined.
	 * 
	 * @param confFile
	 *            PDP configuration file
	 * @param modelHandler
	 *            PDP configuration model handler
	 * @return PDP instance
	 * @throws IOException
	 *             I/O error reading from {@code confFile}
	 * @throws IllegalArgumentException
	 *             Invalid PDP configuration in {@code confFile}
	 */
	public static PDPImpl getPDP(File confFile, PdpModelHandler modelHandler) throws IOException, IllegalArgumentException
	{
		if (confFile == null || !confFile.exists())
		{
			// no property replacement of PARENT_DIR
			throw new IllegalArgumentException("Invalid configuration file location: No file exists at: " + confFile);
		}

		if (modelHandler == null)
		{
			throw NULL_PDP_MODEL_HANDLER_ARGUMENT_EXCEPTION;
		}

		// configuration file exists
		final Pdp pdpJaxbConf;
		try
		{
			pdpJaxbConf = modelHandler.unmarshal(new StreamSource(confFile), Pdp.class);
		} catch (JAXBException e)
		{
			throw new IllegalArgumentException("Invalid PDP configuration file", e);
		}

		// Set property PARENT_DIR in environment properties for future replacement in configuration strings by PDP extensions using file paths
		final String propVal = confFile.getParentFile().toURI().toString();
		LOGGER.debug("Property {} = {}", EnvironmentPropertyName.PARENT_DIR, propVal);
		final EnvironmentProperties envProps = new DefaultEnvironmentProperties(Collections.singletonMap(EnvironmentPropertyName.PARENT_DIR, propVal));
		return getPDP(pdpJaxbConf, envProps);
	}

	/**
	 * Get PDP instance
	 * 
	 * @param pdpJaxbConf
	 *            (JAXB-bound) PDP configuration
	 * @param envProps
	 *            PDP configuration environment properties (e.g. PARENT_DIR)
	 * @return PDP instance
	 * @throws IllegalArgumentException
	 *             invalid PDP configuration
	 * @throws IOException
	 *             if any error occurred closing already created {@link Closeable} modules (policy Providers, attribute Providers, decision cache)
	 */
	public static PDPImpl getPDP(Pdp pdpJaxbConf, EnvironmentProperties envProps) throws IllegalArgumentException, IOException
	{
		/*
		 * Initialize all parameters of ExpressionFactoryImpl: attribute datatype factories, functions, etc.
		 */

		final boolean enableXPath = pdpJaxbConf.isEnableXPath();

		// Attribute datatypes
		final DatatypeFactoryRegistry attributeFactory = new BaseDatatypeFactoryRegistry(
				pdpJaxbConf.isUseStandardDatatypes() ? (enableXPath ? StandardDatatypeFactoryRegistry.ALL_DATATYPES
						: StandardDatatypeFactoryRegistry.MANDATORY_DATATYPES) : null);
		for (final String attrDatatypeURI : pdpJaxbConf.getAttributeDatatypes())
		{
			final DatatypeFactory<?> datatypeFactory = PdpExtensionLoader.getExtension(DatatypeFactory.class, attrDatatypeURI);
			attributeFactory.addExtension(datatypeFactory);
		}

		// Functions
		final FunctionRegistry functionRegistry = new FunctionRegistry(pdpJaxbConf.isUseStandardFunctions() ? StandardFunctionRegistry.getInstance(enableXPath)
				: null);
		for (final String funcId : pdpJaxbConf.getFunctions())
		{
			final Function<?> function = PdpExtensionLoader.getExtension(Function.class, funcId);
			if (!enableXPath && isXpathBased(function))
			{
				throw new IllegalArgumentException("XPath-based function not allowed (because configuration parameter 'enableXPath' = false): " + function);
			}

			functionRegistry.addFunction(function);
		}

		for (final String funcSetId : pdpJaxbConf.getFunctionSets())
		{
			final FunctionSet functionSet = PdpExtensionLoader.getExtension(FunctionSet.class, funcSetId);
			for (final Function<?> function : functionSet.getSupportedFunctions())
			{
				if (!enableXPath && isXpathBased(function))
				{
					throw new IllegalArgumentException("XPath-based function not allowed (because configuration parameter 'enableXPath' = false): " + function);
				}

				functionRegistry.addFunction(function);
			}
		}

		// Combining Algorithms
		final CombiningAlgRegistry combiningAlgRegistry = new BaseCombiningAlgRegistry(
				pdpJaxbConf.isUseStandardCombiningAlgorithms() ? StandardCombiningAlgRegistry.INSTANCE : null);
		for (final String algId : pdpJaxbConf.getCombiningAlgorithms())
		{
			final CombiningAlg<?> alg = PdpExtensionLoader.getExtension(CombiningAlg.class, algId);
			combiningAlgRegistry.addExtension(alg);
		}

		// Decision combiner
		final String resultFilterId = pdpJaxbConf.getResultFilter();
		final DecisionResultFilter decisionResultFilter = resultFilterId == null ? null : PdpExtensionLoader.getExtension(DecisionResultFilter.class,
				resultFilterId);

		// decision cache
		final AbstractDecisionCache jaxbDecisionCache = pdpJaxbConf.getDecisionCache();

		return new PDPImpl(attributeFactory, functionRegistry, pdpJaxbConf.getAttributeProviders(), pdpJaxbConf.getMaxVariableRefDepth(), enableXPath,
				combiningAlgRegistry, pdpJaxbConf.getRootPolicyProvider(), pdpJaxbConf.getRefPolicyProvider(), pdpJaxbConf.getMaxPolicyRefDepth(),
				pdpJaxbConf.getRequestFilter(), pdpJaxbConf.isStrictAttributeIssuerMatch(), decisionResultFilter, jaxbDecisionCache, envProps);
	}

	private static boolean isXpathBased(Function<?> function)
	{
		/*
		 * A function is said "XPath-based" iff it takes at least one XPathExpression parameter. Regarding higher-order function, as of now, we only provide
		 * higher-order functions defined in the XACML (3.0) Core specification, which are not XPath-based, or if a higher-order function happens to take a
		 * XPathExpression parameter, it is actually a parameter to the first-order sub-function. Plus it is not possible to add extensions that are
		 * higher-order functions in this PDP implementation. Therefore, it is enough to check first-order functions (class FirstOrderFunction) only. (Remember
		 * that such functions may be used as parameter to a higher-order function.)
		 */
		if (function instanceof FirstOrderFunction)
		{
			final List<? extends Datatype<?>> paramTypes = ((FirstOrderFunction<?>) function).getParameterTypes();
			for (final Datatype<?> paramType : paramTypes)
			{
				if (paramType.getId().equals(XACMLDatatypeId.XPATH_EXPRESSION.value()))
				{
					return true;
				}
			}
		}

		return false;
	}

}
