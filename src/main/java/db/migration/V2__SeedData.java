package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

/**
 * Seeds reference routes and a rolling set of schedules for the next two days.
 * Implemented as a Java migration so date arithmetic is portable across MySQL and H2.
 */
public class V2__SeedData extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        long kandyRoute = insertRoute(connection,
                "COL-KAN", "Colombo-Kandy Express", "Colombo", "Kandy",
                new BigDecimal("1200.00"), 115);
        long galleRoute = insertRoute(connection,
                "COL-GAL", "Colombo-Galle Coastal", "Colombo", "Galle",
                new BigDecimal("900.00"), 120);

        LocalDate today = LocalDate.now();
        insertSchedule(connection, kandyRoute, "EX-1001",
                today.plusDays(1), "08:30", "12:15", 60);
        insertSchedule(connection, kandyRoute, "EX-1002",
                today.plusDays(2), "15:45", "19:30", 60);
        insertSchedule(connection, galleRoute, "EX-2001",
                today.plusDays(1), "09:00", "11:40", 80);
        insertSchedule(connection, galleRoute, "EX-2002",
                today.plusDays(2), "13:20", "16:00", 80);
    }

    private long insertRoute(Connection connection, String code, String name,
                             String origin, String destination,
                             BigDecimal fare, int distanceKm) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO train_routes (code, name, origin, destination, base_fare, distance_km) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, origin);
            ps.setString(4, destination);
            ps.setBigDecimal(5, fare);
            ps.setInt(6, distanceKm);
            ps.executeUpdate();
            java.sql.ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            return queryRouteId(connection, code);
        }
    }

    private long queryRouteId(Connection connection, String code) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id FROM train_routes WHERE code = ?")) {
            ps.setString(1, code);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new IllegalStateException("Route not created: " + code);
    }

    private void insertSchedule(Connection connection, long routeId, String trainCode,
                                LocalDate serviceDate, String departure, String arrival,
                                int capacity) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO train_schedules "
                        + "(route_id, train_code, service_date, departure_time, arrival_time, "
                        + "capacity, available_seats, fare) "
                        + "SELECT ?, ?, ?, ?, ?, ?, ?, r.base_fare FROM train_routes r WHERE r.id = ?")) {
            ps.setLong(1, routeId);
            ps.setString(2, trainCode);
            ps.setDate(3, java.sql.Date.valueOf(serviceDate));
            ps.setTime(4, java.sql.Time.valueOf(departure + ":00"));
            ps.setTime(5, java.sql.Time.valueOf(arrival + ":00"));
            ps.setInt(6, capacity);
            ps.setInt(7, capacity);
            ps.setLong(8, routeId);
            ps.executeUpdate();
        }
    }
}
