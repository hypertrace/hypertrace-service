package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import java.util.function.Function;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

class UnaryAttributeProjection<T, R> extends AbstractAttributeProjection<R> {
  private final Function<T, R> projectionImplementation;

  UnaryAttributeProjection(
      AttributeKind resultKind,
      AttributeKind argumentKind,
      Function<T, R> projectionImplementation) {
    super(resultKind, List.of(argumentKind));
    this.projectionImplementation = projectionImplementation;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected R doUnwrappedProjection(List<Object> arguments) {
    return this.projectionImplementation.apply((T) arguments.get(0));
  }
}
