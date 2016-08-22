/**
 * Copyright (C) 2012-2016 Thales Services SAS.
 *
 * This file is part of AuthZForce CE.
 *
 * AuthZForce CE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AuthZForce CE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AuthZForce CE.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ow2.authzforce.core.pdp.impl.func;

import java.util.Arrays;
import java.util.List;

import org.ow2.authzforce.core.pdp.api.EvaluationContext;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.StatusHelper;
import org.ow2.authzforce.core.pdp.api.expression.Expression;
import org.ow2.authzforce.core.pdp.api.expression.Expressions;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionSignature;
import org.ow2.authzforce.core.pdp.api.func.SingleParameterTypedFirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.value.AttributeValue;
import org.ow2.authzforce.core.pdp.api.value.BooleanValue;
import org.ow2.authzforce.core.pdp.api.value.Datatype;
import org.ow2.authzforce.core.pdp.api.value.StandardDatatypes;

/**
 * A class that implements the logical functions "or"
 * <p>
 * From XACML core specification of function 'urn:oasis:names:tc:xacml:1.0:function:or': This function SHALL return
 * "False" if it has no arguments and SHALL return "True" if at least one of its arguments evaluates to "True". The
 * order of evaluation SHALL be from first argument to last. The evaluation SHALL stop with a result of "True" if any
 * argument evaluates to "True", leaving the rest of the arguments unevaluated.
 *
 * 
 * @version $Id: $
 */
final class LogicalOrFunction extends SingleParameterTypedFirstOrderFunction<BooleanValue, BooleanValue>
{

	private static final class Call extends FirstOrderFunctionCall<BooleanValue>
	{
		private final String indeterminateArgMsgPrefix;
		private final String invalidArgTypeMsgPrefix;

		private final List<Expression<?>> checkedArgExpressions;

		private Call(final FirstOrderFunctionSignature<BooleanValue> functionSig,
				final List<Expression<?>> argExpressions, final Datatype<?>[] remainingArgTypes)
				throws IllegalArgumentException
		{
			super(functionSig, argExpressions, remainingArgTypes);
			this.checkedArgExpressions = argExpressions;
			indeterminateArgMsgPrefix = "Function " + functionSig.getName() + ": Indeterminate arg #";
			invalidArgTypeMsgPrefix = "Function " + functionSig.getName() + ": Invalid type (expected = "
					+ StandardDatatypes.BOOLEAN_FACTORY.getDatatype() + ") of arg#";
		}

		@Override
		public BooleanValue evaluate(final EvaluationContext context, final AttributeValue... checkedRemainingArgs)
				throws IndeterminateEvaluationException
		{
			/**
			 * TODO: optimize this function call by checking the following:
			 * <ol>
			 * <li>If any argument expression is constant BooleanAttributeValue True, return always true.</li>
			 * <li>Else If all argument expressions are constant BooleanAttributeValue False, return always false.</li>
			 * <li>Else If any argument expression is constant BooleanAttributeValue False, remove it from the
			 * arguments, as it has no effect on the final result. Indeed, or function is commutative and or(false, x,
			 * y...) = or(x, y...).</li>
			 * </ol>
			 * The first two optimizations can be achieved by pre-evaluating the function call with context = null and
			 * check the result if no IndeterminateEvaluationException is thrown.
			 */

			IndeterminateEvaluationException indeterminateException = null;
			int argIndex = 0;
			for (final Expression<?> arg : checkedArgExpressions)
			{
				// Evaluate the argument
				final BooleanValue attrVal;
				try
				{
					attrVal = Expressions.eval(arg, context, StandardDatatypes.BOOLEAN_FACTORY.getDatatype());
					if (attrVal.getUnderlyingValue())
					{
						return BooleanValue.TRUE;
					}
				}
				catch (final IndeterminateEvaluationException e)
				{
					// save the indeterminate to throw later only if there was not any TRUE in remaining
					// args
					indeterminateException = new IndeterminateEvaluationException(indeterminateArgMsgPrefix + argIndex,
							StatusHelper.STATUS_PROCESSING_ERROR, e);
				}

				argIndex++;
			}

			// do the same with remaining arg values
			if (checkedRemainingArgs != null)
			{

				for (final AttributeValue arg : checkedRemainingArgs)
				{
					// Evaluate the argument
					final BooleanValue attrVal;
					try
					{
						attrVal = BooleanValue.class.cast(arg);
					}
					catch (final ClassCastException e)
					{
						throw new IndeterminateEvaluationException(
								invalidArgTypeMsgPrefix + argIndex + ": " + arg.getClass().getName(),
								StatusHelper.STATUS_PROCESSING_ERROR, e);
					}

					if (attrVal.getUnderlyingValue())
					{
						return BooleanValue.TRUE;
					}

					argIndex++;
				}
			}

			if (indeterminateException != null)
			{
				// there was at least one indeterminate arg that could have been TRUE or FALSE ->
				// indeterminate result
				throw indeterminateException;
			}

			return BooleanValue.FALSE;
		}
	}

	LogicalOrFunction(final String functionId)
	{
		super(functionId, StandardDatatypes.BOOLEAN_FACTORY.getDatatype(), true,
				Arrays.asList(StandardDatatypes.BOOLEAN_FACTORY.getDatatype()));
	}

	/** {@inheritDoc} */
	@Override
	public FirstOrderFunctionCall<BooleanValue> newCall(final List<Expression<?>> argExpressions,
			final Datatype<?>... remainingArgTypes)
	{
		return new Call(functionSignature, argExpressions, remainingArgTypes);
	}

}
