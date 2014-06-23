package com.pmease.commons.wicket.editable;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;

import com.pmease.commons.editable.PropertyDescriptor;
import com.pmease.commons.loader.AppLoader;

@SuppressWarnings("serial")
public abstract class PropertyEditor<T> extends ValueEditor<T> {

	private final PropertyDescriptor propertyDescriptor;
	
	public PropertyEditor(String id, PropertyDescriptor propertyDescriptor, IModel<T> propertyModel) {
		super(id, propertyModel);
		
		this.propertyDescriptor = propertyDescriptor;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		if (findParent(BeanEditor.class) == null) {
			add(new INullAcceptingValidator<T>() {

				@Override
				public void validate(IValidatable<T> validatable) {
					Validator validator = AppLoader.getInstance(Validator.class);
					Set<?> violations = validator.validateValue(
							propertyDescriptor.getBeanClass(), 
							propertyDescriptor.getPropertyName(), 
							validatable.getValue());
					
					for (Object each: violations) {
						ConstraintViolation<?> violation = (ConstraintViolation<?>) each;
						addError(violation.getMessage());
					}
				}
				
			});
		}
		
		add(new AttributeAppender("class", " property editor editable"));
	}
	
	public PropertyDescriptor getPropertyDescriptor() {
		return propertyDescriptor;
	}

}
