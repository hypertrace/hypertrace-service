package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import java.util.function.BiFunction;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

class BinaryAttributeProjection<T, U, R> extends AbstractAttributeProjection<R> {
  private final BiFunction<T, U, R> projectionImplementation;

  BinaryAttributeProjection(
      AttributeKind resultKind,
      AttributeKind firstArgumentKind,
      AttributeKind secondArgumentKind,
      BiFunction<T, U, R> projectionImplementation) {
    super(resultKind, List.of(firstArgumentKind, secondArgumentKind));
    this.projectionImplementation = projectionImplementation;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected R doUnwrappedProjection(List<Object> arguments) {
    return this.projectionImplementation.apply((T) arguments.get(0), (U) arguments.get(1));
  }
}
