package org.hypertrace.core.attribute.service.projection;

import static org.hypertrace.core.attribute.service.projection.LiteralConstructors.booleanLiteral;
import static org.hypertrace.core.attribute.service.projection.LiteralConstructors.doubleLiteral;
import static org.hypertrace.core.attribute.service.projection.LiteralConstructors.longLiteral;
import static org.hypertrace.core.attribute.service.projection.LiteralConstructors.stringLiteral;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.junit.jupiter.api.Test;

class BinaryAttributeProjectionTest {

  private final BinaryAttributeProjection<Long, Long, Long> sumProjection =
      new BinaryAttributeProjection<>(
          AttributeKind.TYPE_INT64, AttributeKind.TYPE_INT64, AttributeKind.TYPE_INT64, Long::sum);

  @Test
  void projectsForAnyConvertibleArgTypes() {
    assertEquals(
        longLiteral(5), sumProjection.project(List.of(longLiteral(4), stringLiteral("1"))));
    assertEquals(
        longLiteral(6), sumProjection.project(List.of(doubleLiteral(5.0d), longLiteral(1))));
    assertEquals(
        longLiteral(7), sumProjection.project(List.of(stringLiteral("3"), doubleLiteral(4.0f))));
  }

  @Test
  void throwsIfArgListIsOfUnexpectedLength() {
    assertThrows(IllegalArgumentException.class, () -> sumProjection.project(List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () -> sumProjection.project(List.of(longLiteral(1), longLiteral(2), longLiteral(3))));
  }

  @Test
  void throwsIfAnyArgIsNotConvertible() {
    assertThrows(
        IllegalArgumentException.class,
        () -> sumProjection.project(List.of(booleanLiteral(true), longLiteral(1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> sumProjection.project(List.of(longLiteral(1), booleanLiteral(true))));
  }

  @Test
  void throwsIfResultIsNotConvertibleToExpectedType() {
    BinaryAttributeProjection<Long, Long, Long> badProjection =
        new BinaryAttributeProjection<>(
            AttributeKind.TYPE_BOOL, AttributeKind.TYPE_INT64, AttributeKind.TYPE_INT64, Long::sum);

    assertThrows(
        UnsupportedOperationException.class,
        () -> badProjection.project(List.of(longLiteral(2), longLiteral(1))));
  }
}
