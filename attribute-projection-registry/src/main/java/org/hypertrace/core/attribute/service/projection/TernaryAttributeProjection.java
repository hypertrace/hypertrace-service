package org.hypertrace.core.attribute.service.projection;

import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;

public class TernaryAttributeProjection <T, U, V, R> extends AbstractAttributeProjection<R> {
  private final TriFunction<T, U, V, R> projectionImplementation;

  TernaryAttributeProjection(
      AttributeKind resultKind,
      AttributeKind firstArgumentKind,
      AttributeKind secondArgumentKind,
      AttributeKind thirdArgumentKind,
      TriFunction<T, U, V, R> projectionImplementation) {
    super(resultKind, List.of(firstArgumentKind, secondArgumentKind, thirdArgumentKind));
    this.projectionImplementation = projectionImplementation;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected R doUnwrappedProjection(List<Object> arguments) {
    return this.projectionImplementation.apply((T) arguments.get(0), (U) arguments.get(1), (V) arguments.get(2));
  }
}
