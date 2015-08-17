package com.thalesgroup.authzforce.core.func;

import java.util.List;

import com.sun.xacml.ctx.Status;
import com.thalesgroup.authzforce.core.attr.AttributeValue;
import com.thalesgroup.authzforce.core.attr.BooleanAttributeValue;
import com.thalesgroup.authzforce.core.eval.DatatypeDef;
import com.thalesgroup.authzforce.core.eval.EvaluationContext;
import com.thalesgroup.authzforce.core.eval.Expression;
import com.thalesgroup.authzforce.core.eval.ExpressionResult;
import com.thalesgroup.authzforce.core.eval.IndeterminateEvaluationException;
import com.thalesgroup.authzforce.core.eval.PrimitiveResult;

/**
 * A class that implements the logical functions "or" and "and".
 * <p>
 * From XACML core specification of function 'urn:oasis:names:tc:xacml:1.0:function:or': This
 * function SHALL return "False" if it has no arguments and SHALL return "True" if at least one of
 * its arguments evaluates to "True". The order of evaluation SHALL be from first argument to last.
 * The evaluation SHALL stop with a result of "True" if any argument evaluates to "True", leaving
 * the rest of the arguments unevaluated.
 * 
 */
public class LogicalOrFunction extends BaseFunction<PrimitiveResult<BooleanAttributeValue>>
{
	/**
	 * XACML standard identifier for the "or" logical function
	 */
	public static final String NAME_OR = FUNCTION_NS_1 + "or";

	/**
	 * Instantiates the function
	 * 
	 */
	public LogicalOrFunction()
	{
		super(NAME_OR, BooleanAttributeValue.TYPE, true, BooleanAttributeValue.TYPE);
	}

	private static final String INDETERMINATE_ARG_MESSAGE_PREFIX = "Function " + NAME_OR + ": Indeterminate arg #";
	private static final String INVALID_ARG_TYPE_MESSAGE_PREFIX = "Function " + NAME_OR + ": Invalid type (expected = " + BooleanAttributeValue.class.getName() + ") of arg#";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.thalesgroup.authzforce.core.func.BaseFunction#getFunctionCall(java.util.List,
	 * com.thalesgroup.authzforce.core.eval.DatatypeDef[])
	 */
	@Override
	protected Call getFunctionCall(final List<Expression<? extends ExpressionResult<? extends AttributeValue>>> checkedArgExpressions, DatatypeDef[] checkedRemainingArgTypes)
	{
		/**
		 * TODO: optimize this function call by checking the following:
		 * <ol>
		 * <li>If any argument expression is constant BooleanAttributeValue True, return always
		 * true.</li>
		 * <li>Else If all argument expressions are constant BooleanAttributeValue False, return
		 * always false.</li>
		 * <li>
		 * Else If any argument expression is constant BooleanAttributeValue False, remove it from
		 * the arguments, as it has no effect on the final result. Indeed, or function is
		 * commutative and or(false, x, y...) = or(x, y...).</li>
		 * </ol>
		 * The first two optimizations can be achieved by pre-evaluating the function call with
		 * context = null and check the result if no IndeterminateEvaluationException is thrown.
		 */
		return new Call(checkedRemainingArgTypes)
		{

			@Override
			protected PrimitiveResult<BooleanAttributeValue> evaluate(EvaluationContext context, AttributeValue... remainingArgs) throws IndeterminateEvaluationException
			{
				return eval(context, checkedArgExpressions, remainingArgs);
			}
		};
	}

	/**
	 * Logical 'or' evaluation method.
	 * 
	 * @param context
	 * @param checkedArgExpressions
	 *            arg expression whose return type is assumed valid (already checked) for this
	 *            function
	 * @param checkedRemainingArgs
	 *            remaining arg values, whose datatype is assumed valid (already checked) for this
	 *            function
	 * @return true iff all checkedArgExpressions return True and all remainingArgs are True
	 * @throws IndeterminateEvaluationException
	 */
	public static PrimitiveResult<BooleanAttributeValue> eval(EvaluationContext context, List<Expression<? extends ExpressionResult<? extends AttributeValue>>> checkedArgExpressions, AttributeValue[] checkedRemainingArgs) throws IndeterminateEvaluationException
	{
		int argIndex = 0;
		for (final Expression<? extends ExpressionResult<? extends AttributeValue>> arg : checkedArgExpressions)
		{
			// Evaluate the argument
			final BooleanAttributeValue attrVal;
			try
			{
				attrVal = evalPrimitiveArg(arg, context, BooleanAttributeValue.class);
			} catch (IndeterminateEvaluationException e)
			{
				throw new IndeterminateEvaluationException(INDETERMINATE_ARG_MESSAGE_PREFIX + argIndex, Status.STATUS_PROCESSING_ERROR, e);
			}

			if (attrVal.getValue())
			{
				return PrimitiveResult.TRUE;
			}

			argIndex++;
		}

		// do the same with remaining arg values
		for (final AttributeValue arg : checkedRemainingArgs)
		{
			// Evaluate the argument
			final BooleanAttributeValue attrVal;
			try
			{
				attrVal = BooleanAttributeValue.class.cast(arg);
			} catch (ClassCastException e)
			{
				throw new IndeterminateEvaluationException(INVALID_ARG_TYPE_MESSAGE_PREFIX + argIndex + ": " + arg.getClass().getName(), Status.STATUS_PROCESSING_ERROR, e);
			}

			if (attrVal.getValue())
			{
				return PrimitiveResult.TRUE;
			}

			argIndex++;
		}

		return PrimitiveResult.FALSE;
	}
}