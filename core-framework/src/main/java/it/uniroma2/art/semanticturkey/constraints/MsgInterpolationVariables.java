package it.uniroma2.art.semanticturkey.constraints;

public class MsgInterpolationVariables {

	// we still need this variable since Hibernate Validator 4.2 doesn't support ${validatedValue} natively
	public static final String invalidParamValuePlaceHolder = "${validatedValue}"; 
	
}
