package it.uniroma2.art.semanticturkey.properties;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import it.uniroma2.art.semanticturkey.constraints.MsgInterpolationVariables;
import it.uniroma2.art.semanticturkey.properties.STProperties.BasicPropertiesConstraints;

public class STPropertiesChecker {

	private static class AlwaysPassingValidator<A extends Annotation, T>
			implements ConstraintValidator<A, T> {

		@Override
		public void initialize(A constraintAnnotation) {
			// nothing to do
		}

		@Override
		public boolean isValid(T value, ConstraintValidatorContext context) {
			return true;
		}

	}

	private class AutowiredValidatorFilteringFactory implements ConstraintValidatorFactory {

		@Override
		public void releaseInstance(ConstraintValidator<?, ?> instance) {
			// nothing to do
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
			boolean autowired = FieldUtils.getFieldsWithAnnotation(key, Autowired.class).length != 0
					|| MethodUtils.getMethodsWithAnnotation(key, Autowired.class).length != 0;

			T validator;
			if (autowired) {
				validator = (T) new AlwaysPassingValidator();
			} else {
				try {
					validator = (T) ConstructorUtils.invokeConstructor(key);
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
						| InstantiationException e) {
					throw new RuntimeException(e);
				}
			}

			if (validator instanceof STProperties.BasicPropertyConstraintValidator) {
				((STProperties.BasicPropertyConstraintValidator)validator).setAllowIncomplete(allowIncomplete);
			}

			return validator;
		}
	};

	private STProperties props;
	private boolean allowIncomplete;
	private String errorMsg;

	private STPropertiesChecker(STProperties conf) {
		this.props = conf;
	}

	public static STPropertiesChecker getModelConfigurationChecker(STProperties conf) {
		return new STPropertiesChecker(conf);
	}

	public STPropertiesChecker allowIncomplete(boolean allowIncomplete) {
		this.allowIncomplete = allowIncomplete;
		return this;
	}

	/**
	 * Tells if the handed {@link STProperties} are valid. The concept of validity is currently limited to
	 * checking that the values for all required properties have been set. Further constraints could be added
	 * in the future.<br/>
	 * 
	 * @return <code>true</code> if the handed property set is valid.
	 */
	public boolean isValid() {
		StringBuilder msgBuilder = new StringBuilder();

		LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
		validatorFactoryBean.setConstraintValidatorFactory(new AutowiredValidatorFilteringFactory());
		validatorFactoryBean.afterPropertiesSet();

		Validator validator = validatorFactoryBean.getValidator();
		Set<ConstraintViolation<STProperties>> constraintViolations = validator.validate(props);

		for (ConstraintViolation<STProperties> violation : constraintViolations) {
			String path = violation.getPropertyPath().toString();

			// skips the violation reports for BasicPropertiesConstraints associated with the root path
			// since we are interested in the other more specific violation reports
			if (violation.getConstraintDescriptor().getAnnotation()
					.annotationType() == BasicPropertiesConstraints.class
					&& path.isEmpty()) {
				continue;
			}
			if (msgBuilder.length() != 0) {
				msgBuilder.append("\n");
			}
			if (!path.isEmpty()) {
				msgBuilder.append(path).append(": ");
			}
			msgBuilder.append(
					violation.getMessage().replace(MsgInterpolationVariables.invalidParamValuePlaceHolder,
							"" + violation.getInvalidValue()));
		}

		if (!constraintViolations.isEmpty()) {
			setErrorMessage(msgBuilder.toString());
			return false;
		}
		return true;
	}

	private void setErrorMessage(String msg) {
		errorMsg = msg;
	}

	public String getErrorMessage() {
		return errorMsg;
	}

}
