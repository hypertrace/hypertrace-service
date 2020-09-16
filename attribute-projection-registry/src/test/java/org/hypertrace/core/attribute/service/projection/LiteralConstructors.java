package org.hypertrace.core.attribute.service.projection;

import javax.annotation.Nonnull;
import org.hypertrace.core.attribute.service.v1.LiteralValue;

class LiteralConstructors {
  static LiteralValue stringLiteral(@Nonnull String stringValue) {
    return LiteralValue.newBuilder().setStringValue(stringValue).build();
  }

  static LiteralValue longLiteral(long longValue) {
    return LiteralValue.newBuilder().setIntValue(longValue).build();
  }

  static LiteralValue doubleLiteral(double doubleValue) {
    return LiteralValue.newBuilder().setFloatValue(doubleValue).build();
  }

  static LiteralValue booleanLiteral(boolean booleanValue) {
    return LiteralValue.newBuilder().setBooleanValue(booleanValue).build();
  }
}
