package org.hypertrace.core.attribute.service.projection;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.LiteralValue;

abstract class AbstractAttributeProjection<R> implements AttributeProjection {

  private final AttributeKind resultKind;
  private final List<AttributeKind> argumentKinds;

  protected AbstractAttributeProjection(
      AttributeKind resultKind, List<AttributeKind> argumentKinds) {
    this.resultKind = resultKind;
    this.argumentKinds = argumentKinds;
  }

  @Override
  public LiteralValue project(List<LiteralValue> arguments) {
    Preconditions.checkArgument(arguments.size() == argumentKinds.size());
    List<Object> unwrappedArguments = new ArrayList<>(argumentKinds.size());
    for (int index = 0; index < arguments.size(); index++) {
      int argumentIndex = index;
      LiteralValue argumentLiteral = arguments.get(argumentIndex);
      AttributeKind attributeKind = this.argumentKinds.get(argumentIndex);
      Object unwrappedArgument =
          ValueCoercer.fromLiteral(argumentLiteral, attributeKind)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          String.format(
                              "Projection argument %s at index %d could not be converted to expected type %s",
                              argumentLiteral, argumentIndex, attributeKind)));

      unwrappedArguments.add(argumentIndex, unwrappedArgument);
    }
    Object unwrappedResult = this.doUnwrappedProjection(unwrappedArguments);
    return ValueCoercer.toLiteral(unwrappedResult, this.resultKind)
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    String.format(
                        "Projection result %s could not be converted to expected type %s",
                        unwrappedResult, this.resultKind)));
  }

  protected abstract R doUnwrappedProjection(List<Object> arguments);
}
