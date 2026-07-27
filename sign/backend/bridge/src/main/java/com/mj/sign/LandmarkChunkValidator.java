package com.mj.sign;

import com.mj.sign.protos.LandmarkProto.ClientStreamChunk;
import com.mj.sign.protos.LandmarkProto.LandmarkFrame;
import com.mj.sign.protos.LandmarkProto.Point3D;

import java.util.List;
import java.util.Optional;

final class LandmarkChunkValidator {
    private LandmarkChunkValidator() {
    }

    static Optional<ValidationError> validate(
            ClientStreamChunk chunk,
            int maxPointsPerGroup,
            float maxAbsoluteCoordinate
    ) {
        long previousTimestamp = Long.MIN_VALUE;
        for (LandmarkFrame frame : chunk.getFramesList()) {
            if (frame.getTimestampMs() < 0) {
                return error("invalid-timestamp", "Frame timestamp_ms must not be negative.");
            }
            if (frame.getTimestampMs() < previousTimestamp) {
                return error("timestamp-regression", "Frame timestamp_ms must be non-decreasing within a chunk.");
            }
            previousTimestamp = frame.getTimestampMs();

            for (NamedPoints group : List.of(
                    new NamedPoints("left_hand", frame.getLeftHandList()),
                    new NamedPoints("right_hand", frame.getRightHandList()),
                    new NamedPoints("pose", frame.getPoseList()),
                    new NamedPoints("face_contour", frame.getFaceContourList())
            )) {
                Optional<ValidationError> pointsError = validatePoints(
                        group.points(),
                        group.name(),
                        maxPointsPerGroup,
                        maxAbsoluteCoordinate
                );
                if (pointsError.isPresent()) {
                    return pointsError;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<ValidationError> validatePoints(
            List<Point3D> points,
            String field,
            int maxPointsPerGroup,
            float maxAbsoluteCoordinate
    ) {
        if (points.size() > maxPointsPerGroup) {
            return error(
                    "too-many-landmarks",
                    field + " exceeds the maximum of " + maxPointsPerGroup + " points per frame."
            );
        }
        for (Point3D point : points) {
            if (!Float.isFinite(point.getX())
                    || !Float.isFinite(point.getY())
                    || !Float.isFinite(point.getZ())) {
                return error("invalid-coordinate", field + " contains a non-finite coordinate.");
            }
            if (Math.abs(point.getX()) > maxAbsoluteCoordinate
                    || Math.abs(point.getY()) > maxAbsoluteCoordinate
                    || Math.abs(point.getZ()) > maxAbsoluteCoordinate) {
                return error(
                        "coordinate-out-of-range",
                        field + " coordinates must be within ±" + maxAbsoluteCoordinate + "."
                );
            }
        }
        return Optional.empty();
    }

    private static Optional<ValidationError> error(String code, String message) {
        return Optional.of(new ValidationError(code, message));
    }

    private record NamedPoints(String name, List<Point3D> points) {
    }

    record ValidationError(String code, String message) {
    }
}
