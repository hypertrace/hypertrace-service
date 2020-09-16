package org.hypertrace.core.attribute.service.projection;

import static org.hypertrace.core.attribute.service.v1.ProjectionOperator.PROJECTION_OPERATOR_CONCAT;
import static org.hypertrace.core.attribute.service.v1.ProjectionOperator.PROJECTION_OPERATOR_HASH;

import java.util.Map;
import java.util.Optional;
import org.hypertrace.core.attribute.service.projection.functions.Concatenate;
import org.hypertrace.core.attribute.service.projection.functions.Hash;
import org.hypertrace.core.attribute.service.v1.AttributeKind;
import org.hypertrace.core.attribute.service.v1.ProjectionOperator;

public class AttributeProjectionRegistry {

  private static final Map<ProjectionOperator, AttributeProjection> PROJECTION_MAP =
      Map.of(
          PROJECTION_OPERATOR_CONCAT,
          new BinaryAttributeProjection<>(
              AttributeKind.TYPE_STRING,
              AttributeKind.TYPE_STRING,
              AttributeKind.TYPE_STRING,
              Concatenate::concatenate),
          PROJECTION_OPERATOR_HASH,
          new UnaryAttributeProjection<>(
              AttributeKind.TYPE_STRING, AttributeKind.TYPE_STRING, Hash::hash));

  public Optional<AttributeProjection> getProjection(ProjectionOperator projectionOperator) {
    return Optional.ofNullable(PROJECTION_MAP.get(projectionOperator));
  }
}
