package com.thalesgroup.authzforce.core.func;

import java.util.List;

import com.sun.xacml.attr.DNSNameAttributeValue;
import com.sun.xacml.attr.IPAddressAttributeValue;
import com.thalesgroup.authzforce.core.attr.AnyURIAttributeValue;
import com.thalesgroup.authzforce.core.attr.AttributeValue;
import com.thalesgroup.authzforce.core.attr.Base64BinaryAttributeValue;
import com.thalesgroup.authzforce.core.attr.BooleanAttributeValue;
import com.thalesgroup.authzforce.core.attr.DateAttributeValue;
import com.thalesgroup.authzforce.core.attr.DateTimeAttributeValue;
import com.thalesgroup.authzforce.core.attr.DayTimeDurationAttributeValue;
import com.thalesgroup.authzforce.core.attr.DoubleAttributeValue;
import com.thalesgroup.authzforce.core.attr.HexBinaryAttributeValue;
import com.thalesgroup.authzforce.core.attr.IntegerAttributeValue;
import com.thalesgroup.authzforce.core.attr.PrimitiveAttributeValue;
import com.thalesgroup.authzforce.core.attr.RFC822NameAttributeValue;
import com.thalesgroup.authzforce.core.attr.StringAttributeValue;
import com.thalesgroup.authzforce.core.attr.TimeAttributeValue;
import com.thalesgroup.authzforce.core.attr.X500NameAttributeValue;
import com.thalesgroup.authzforce.core.attr.YearMonthDurationAttributeValue;
import com.thalesgroup.authzforce.core.eval.DatatypeDef;
import com.thalesgroup.authzforce.core.eval.Expression;
import com.thalesgroup.authzforce.core.eval.ExpressionResult;
import com.thalesgroup.authzforce.core.eval.IndeterminateEvaluationException;
import com.thalesgroup.authzforce.core.eval.PrimitiveResult;

/**
 * Implements generic match functions taking parameters of same/equal type, i.e. standard (A.3.1)
 * Equality predicates and special match function x500Name-match
 * 
 * @param <T>
 *            type of compared parameters
 */
public abstract class EqualTypeMatchFunction<T extends AttributeValue> extends BaseFunction<PrimitiveResult<BooleanAttributeValue>>
{

	/**
	 * Standard identifier for the string-equal function.
	 */
	public static final String NAME_STRING_EQUAL = FUNCTION_NS_1 + "string-equal";

	/**
	 * Standard identifier for the boolean-equal function.
	 */
	public static final String NAME_BOOLEAN_EQUAL = FUNCTION_NS_1 + "boolean-equal";

	/**
	 * Standard identifier for the integer-equal function.
	 */
	public static final String NAME_INTEGER_EQUAL = FUNCTION_NS_1 + "integer-equal";

	/**
	 * Standard identifier for the double-equal function.
	 */
	public static final String NAME_DOUBLE_EQUAL = FUNCTION_NS_1 + "double-equal";

	/**
	 * Standard identifier for the date-equal function.
	 */
	public static final String NAME_DATE_EQUAL = FUNCTION_NS_1 + "date-equal";

	/**
	 * Standard identifier for the time-equal function.
	 */
	public static final String NAME_TIME_EQUAL = FUNCTION_NS_1 + "time-equal";

	/**
	 * Standard identifier for the dateTime-equal function.
	 */
	public static final String NAME_DATETIME_EQUAL = FUNCTION_NS_1 + "dateTime-equal";

	/**
	 * Standard identifier for the dayTimeDuration-equal function.
	 */
	public static final String NAME_DAYTIME_DURATION_EQUAL = FUNCTION_NS_3 + "dayTimeDuration-equal";

	/**
	 * Standard identifier for the yearMonthDuration-equal function.
	 */
	public static final String NAME_YEARMONTH_DURATION_EQUAL = FUNCTION_NS_3 + "yearMonthDuration-equal";

	/**
	 * Standard identifier for the anyURI-equal function.
	 */
	public static final String NAME_ANYURI_EQUAL = FUNCTION_NS_1 + "anyURI-equal";

	/**
	 * Standard identifier for the x500Name-equal function.
	 */
	public static final String NAME_X500NAME_EQUAL = FUNCTION_NS_1 + "x500Name-equal";

	/**
	 * Standard identifier for the rfc822Name-equal function.
	 */
	public static final String NAME_RFC822NAME_EQUAL = FUNCTION_NS_1 + "rfc822Name-equal";

	/**
	 * Standard identifier for the hexBinary-equal function.
	 */
	public static final String NAME_HEXBINARY_EQUAL = FUNCTION_NS_1 + "hexBinary-equal";

	/**
	 * Standard identifier for the base64Binary-equal function.
	 */
	public static final String NAME_BASE64BINARY_EQUAL = FUNCTION_NS_1 + "base64Binary-equal";

	/**
	 * Standard identifier for the ipAddress-equal function.
	 */
	public static final String NAME_IPADDRESS_EQUAL = FUNCTION_NS_2 + "ipAddress-equal";

	/**
	 * Standard identifier for the dnsName-equal function.
	 */
	public static final String NAME_DNSNAME_EQUAL = FUNCTION_NS_2 + "dnsName-equal";

	/**
	 * Standard identifier for the string-equal-ignore-case function.
	 */
	private static final String NAME_STRING_EQUAL_IGNORE_CASE = FUNCTION_NS_3 + "string-equal-ignore-case";

	/**
	 * Standard identifier for the x500Name-match function (different from x500Name-regexp-match
	 * down below).
	 */
	public static final String NAME_X500NAME_MATCH = FUNCTION_NS_1 + "x500Name-match";

	// /**
	// * Logger used for all classes
	// */
	// private static final Logger LOGGER = LoggerFactory
	// .getLogger(EqualTypeMatchFunction.class);

	private final Class<T[]> parameterArrayClass;

	/**
	 * Function cluster
	 */
	public static final FunctionSet CLUSTER = new FunctionSet(FunctionSet.DEFAULT_ID_NAMESPACE + "equal-type-match",
	//
			new Equal<>(NAME_STRING_EQUAL, StringAttributeValue.identifier, StringAttributeValue[].class),
			//
			new Equal<>(NAME_BOOLEAN_EQUAL, BooleanAttributeValue.identifier, BooleanAttributeValue[].class),
			//
			new Equal<>(NAME_INTEGER_EQUAL, IntegerAttributeValue.identifier, IntegerAttributeValue[].class),
			//
			new Equal<>(NAME_DOUBLE_EQUAL, DoubleAttributeValue.identifier, DoubleAttributeValue[].class),
			//
			new Equal<>(NAME_DATE_EQUAL, DateAttributeValue.identifier, DateAttributeValue[].class),
			//
			new Equal<>(NAME_TIME_EQUAL, TimeAttributeValue.identifier, TimeAttributeValue[].class),
			//
			new Equal<>(NAME_DATETIME_EQUAL, DateTimeAttributeValue.identifier, DateTimeAttributeValue[].class),
			//
			new Equal<>(NAME_DAYTIME_DURATION_EQUAL, DayTimeDurationAttributeValue.identifier, DayTimeDurationAttributeValue[].class),
			//
			new Equal<>(NAME_YEARMONTH_DURATION_EQUAL, YearMonthDurationAttributeValue.identifier, YearMonthDurationAttributeValue[].class),
			//
			new Equal<>(NAME_ANYURI_EQUAL, AnyURIAttributeValue.identifier, AnyURIAttributeValue[].class),
			//
			new Equal<>(NAME_X500NAME_EQUAL, X500NameAttributeValue.identifier, X500NameAttributeValue[].class),
			//
			new Equal<>(NAME_RFC822NAME_EQUAL, RFC822NameAttributeValue.identifier, RFC822NameAttributeValue[].class),
			//
			new Equal<>(NAME_HEXBINARY_EQUAL, HexBinaryAttributeValue.identifier, HexBinaryAttributeValue[].class),
			//
			new Equal<>(NAME_BASE64BINARY_EQUAL, Base64BinaryAttributeValue.identifier, Base64BinaryAttributeValue[].class),
			//
			new Equal<>(NAME_IPADDRESS_EQUAL, IPAddressAttributeValue.identifier, IPAddressAttributeValue[].class),
			//
			new Equal<>(NAME_DNSNAME_EQUAL, DNSNameAttributeValue.identifier, DNSNameAttributeValue[].class),
			//
			new EqualIgnoreCase<>(NAME_STRING_EQUAL_IGNORE_CASE, StringAttributeValue.identifier, StringAttributeValue[].class),
			//
			new X500NameMatch());

	/**
	 * Creates a new <code>EqualTypeMatchFunction</code> object.
	 * 
	 * @param functionName
	 *            the standard XACML name of the function to be handled by this object, including
	 *            the full namespace
	 * @param paramTypeURI
	 *            parameter type URI
	 * @param paramArrayType
	 *            parameter array type
	 */
	public EqualTypeMatchFunction(String functionName, String paramTypeURI, Class<T[]> paramArrayType)
	{
		super(functionName, BooleanAttributeValue.TYPE, false, new DatatypeDef(paramTypeURI), new DatatypeDef(paramTypeURI));
		this.parameterArrayClass = paramArrayType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.thalesgroup.authzforce.core.func.BaseFunction#getFunctionCall(java.util.List,
	 * com.thalesgroup.authzforce.core.eval.DatatypeDef[])
	 */
	@Override
	protected Call getFunctionCall(List<Expression<? extends ExpressionResult<? extends AttributeValue>>> checkedArgExpressions, DatatypeDef[] checkedRemainingArgTypes)
	{
		return new EagerPrimitiveEvalCall<T>(parameterArrayClass, checkedArgExpressions, checkedRemainingArgTypes)
		{
			@Override
			protected final PrimitiveResult<BooleanAttributeValue> evaluate(T[] args) throws IndeterminateEvaluationException
			{
				return PrimitiveResult.getInstance(match(args[0], args[1]));
			}

		};
	}

	protected abstract boolean match(T arg0, T arg1);

	/**
	 * *-equal function
	 * 
	 * @param <T>
	 *            parameter type
	 */
	private static class Equal<T extends AttributeValue> extends EqualTypeMatchFunction<T>
	{

		/**
		 * Instantiates *-equal function
		 * 
		 * @param functionName
		 *            function ID
		 * @param paramTypeURI
		 *            datatype URI of parameters
		 * @param paramArrayType
		 *            parameter array type
		 */
		public Equal(String functionName, String paramTypeURI, Class<T[]> paramArrayType)
		{
			super(functionName, paramTypeURI, paramArrayType);
		}

		@Override
		public final boolean match(T arg0, T arg1)
		{
			return arg0.equals(arg1);
		}
	}

	/**
	 * *-equal-ignore-case function
	 * 
	 * @param <T>
	 *            parameter type
	 */
	private static class EqualIgnoreCase<T extends PrimitiveAttributeValue<String>> extends EqualTypeMatchFunction<T>
	{

		/**
		 * Instantiates *-equal-ignore-case function
		 * 
		 * @param functionName
		 *            function ID
		 * @param paramTypeURI
		 *            datatype URI of parameters
		 * @param paramArrayType
		 *            parameter class
		 */
		public EqualIgnoreCase(String functionName, String paramTypeURI, Class<T[]> paramArrayType)
		{
			super(functionName, paramTypeURI, paramArrayType);
		}

		@Override
		public final boolean match(T arg0, T arg1)
		{
			return arg0.getValue().equalsIgnoreCase(arg1.getValue());
		}
	}

	/**
	 * x500Name-match function
	 * 
	 */
	private static class X500NameMatch extends EqualTypeMatchFunction<X500NameAttributeValue>
	{

		/**
		 * Instantiates the function
		 */
		public X500NameMatch()
		{
			super(NAME_X500NAME_MATCH, X500NameAttributeValue.identifier, X500NameAttributeValue[].class);
		}

		@Override
		public final boolean match(X500NameAttributeValue arg0, X500NameAttributeValue arg1)
		{
			return arg0.match(arg1);
		}
	}

}