/**
 * Copyright (C) 2011-2015 Thales Services SAS.
 *
 * This file is part of AuthZForce.
 *
 * AuthZForce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthZForce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthZForce.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package com.thalesgroup.authzforce.core.test.func;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.thalesgroup.authzforce.core.attr.AttributeValue;
import com.thalesgroup.authzforce.core.attr.StringAttributeValue;
import com.thalesgroup.authzforce.core.eval.Expression;
import com.thalesgroup.authzforce.core.eval.ExpressionResult;
import com.thalesgroup.authzforce.core.eval.PrimitiveResult;

@RunWith(Parameterized.class)
public class StringConversionFunctionsTest extends GeneralFunctionTest
{

	private static final String NAME_STRING_NORMALIZE_SPACE = "urn:oasis:names:tc:xacml:1.0:function:string-normalize-space";
	private static final String NAME_STRING_NORMALIZE_TO_LOWER_CASE = "urn:oasis:names:tc:xacml:1.0:function:string-normalize-to-lower-case";

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> params() throws Exception
	{
		return Arrays.asList(
		// urn:oasis:names:tc:xacml:1.0:function:string-normalize-space
				new Object[] { NAME_STRING_NORMALIZE_SPACE, Arrays.asList(new StringAttributeValue("test")), new PrimitiveResult<>(new StringAttributeValue("test"), StringAttributeValue.TYPE) },//
				new Object[] { NAME_STRING_NORMALIZE_SPACE, Arrays.asList(new StringAttributeValue("   test   ")), new PrimitiveResult<>(new StringAttributeValue("test"), StringAttributeValue.TYPE) },

				// urn:oasis:names:tc:xacml:1.0:function:string-normalize-to-lower-case
				new Object[] { NAME_STRING_NORMALIZE_TO_LOWER_CASE, Arrays.asList(new StringAttributeValue("test")), new PrimitiveResult<>(new StringAttributeValue("test"), StringAttributeValue.TYPE) },//
				new Object[] { NAME_STRING_NORMALIZE_TO_LOWER_CASE, Arrays.asList(new StringAttributeValue("TeST")), new PrimitiveResult<>(new StringAttributeValue("test"), StringAttributeValue.TYPE) });
	}

	protected StringConversionFunctionsTest(String functionName, List<Expression<? extends ExpressionResult<? extends AttributeValue>>> inputs, ExpressionResult<? extends AttributeValue> expectedResult)
	{
		super(functionName, inputs, expectedResult);
	}

}