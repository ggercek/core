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
package com.thalesgroup.authzforce.core.eval;

import javax.xml.bind.JAXBElement;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.ExpressionType;

import com.thalesgroup.authzforce.core.attr.AttributeValue;

/**
 * Super interface of the internal model's equivalents of the JAXB types derived from
 * ExpressionType, and that are "Evaluatable", i.e. evaluation depends on the Evaluation Context:
 * <ul>
 * <li>AttributeValue</li>
 * <li>Apply</li>
 * <li>AttributeSelector</li>
 * <li>VariableReference</li>
 * <li>AttributeDesignator</li>
 * <li>Function</li>
 * </ul>
 * 
 * @param <JAXB_T>
 *            XACML-schema derived type of the XACML/JAXB object/element bound to this expression
 * 
 * @param <T>
 *            type of result, i.e single-value or bag of values
 */

public interface JAXBBoundExpression<JAXB_T extends ExpressionType, T extends ExpressionResult<? extends AttributeValue>> extends Expression<T>
{
	/**
	 * Gets the instance of the Java representation of the XACML-schema-defined Expression
	 * bound/equivalent to this expression
	 * 
	 * @return JAXB element equivalent
	 */
	JAXBElement<JAXB_T> getJAXBElement();

	/**
	 * Gets the expected return type of the expression if evaluated.
	 * 
	 * @return expression evaluation's return type
	 */
	@Override
	DatatypeDef getReturnType();

	/**
	 * Evaluates the expression using the given context.
	 * 
	 * @param context
	 *            the representation of the request
	 * 
	 * @return the result of evaluation that may be a single value T (e.g. function result,
	 *         AttributeValue, Condition, Match...) or bag of values (e.g. AttributeDesignator,
	 *         AttributeSelector)
	 * @throws IndeterminateEvaluationException
	 *             if evaluation "Indeterminate" (see XACML core specification)
	 */
	@Override
	T evaluate(EvaluationContext context) throws IndeterminateEvaluationException;

	/**
	 * Tells whether this expression is actually a static value, i.e. independent from the
	 * evaluation context (e.g. AttributeValue, VariableReference to AttributeValue...). This
	 * enables expression consumers to do optimizations, e.g. functions may pre-compile/pre-evaluate
	 * parts of their inputs knowing some are constant values.
	 * 
	 * @return true iff a static/fixed/constant value
	 */
	@Override
	boolean isStatic();

}