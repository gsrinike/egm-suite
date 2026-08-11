package eu.egm.srv.iidm.transformer.api;

import java.util.List;
import java.util.Map;

public record IidmGridViewMapDataResponse(
        String importId,
        String networkId,
        String tsoName,
        String businessDay,
        String businessTime,
        String timeFrame,
        GridViewBounds bounds,
        List<GridViewPoint> points,
        List<GridViewLine> lines,
        List<String> diagnostics) {
    public IidmGridViewMapDataResponse {
        points = points == null ? List.of() : List.copyOf(points);
        lines = lines == null ? List.of() : List.copyOf(lines);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public record GridViewBounds(
            double minLatitude,
            double maxLatitude,
            double minLongitude,
            double maxLongitude) {
    }

    public record GridViewPoint(
            String id,
            String label,
            double latitude,
            double longitude,
            Map<String, Object> details) {
        public GridViewPoint {
            details = details == null ? Map.of() : details;
        }
    }

    public record GridViewLine(
            String id,
            String label,
            List<GridViewPoint> points,
            Map<String, Object> details) {
        public GridViewLine {
            points = points == null ? List.of() : List.copyOf(points);
            details = details == null ? Map.of() : details;
        }
    }
}
