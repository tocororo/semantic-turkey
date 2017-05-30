package it.uniroma2.art.semanticturkey.converters;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringToMapGenericConverter
		implements ConditionalGenericConverter {

	@Autowired
	private ApplicationContext applicationContext;

	private ConversionService conversionService;

	private final ObjectMapper mapper;
	private final String conversionServiceId;

	public StringToMapGenericConverter(String conversionServiceId) {
		this.mapper = new ObjectMapper();
		this.conversionServiceId = conversionServiceId;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Map.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		conversionService = (ConversionService) applicationContext.getBean(conversionServiceId);
		return (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class),
				targetType.getMapKeyTypeDescriptor())
				&& this.conversionService.canConvert(TypeDescriptor.valueOf(String.class),
						targetType.getMapValueTypeDescriptor()));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Map<String, String> rawMap;
		try {
			rawMap = mapper.readValue((String) source, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		Map<?, ?> map = rawMap.entrySet().stream().collect(Collectors.toMap(
				entry -> conversionService.convert(entry.getKey(), TypeDescriptor.valueOf(String.class),
						targetType.getMapKeyTypeDescriptor()),
				entry -> conversionService.convert(entry.getValue(),
						TypeDescriptor.valueOf(
								entry.getValue() != null ? entry.getValue().getClass() : Object.class),
						targetType.getMapValueTypeDescriptor())));

		return map;
	}

}
