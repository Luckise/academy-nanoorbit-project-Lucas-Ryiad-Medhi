package fr.efrei.nanooribt

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import kotlin.math.*

/**
 * Simplified orbital mechanics for the AR sky-tracking feature.
 *
 * This is NOT a precise SGP4 propagator — it's a Keplerian circular-orbit
 * approximation seeded deterministically from the satellite ID, sufficient
 * for an educational AR overlay. Real ground-station planning would use TLE
 * data + SGP4; here we only need plausible "where in the sky" answers.
 */
object OrbitalMechanics {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val GM_KM3_PER_S2 = 398600.4418
    private const val EARTH_ROTATION_RAD_PER_S = 7.2921159e-5

    data class SkyPosition(
        val azimuthDeg: Double,
        val elevationDeg: Double,
        val rangeKm: Double
    ) {
        val isAboveHorizon: Boolean get() = elevationDeg > 0.0
    }

    /**
     * Compute (az, el, range) from an observer at sea level to a satellite
     * on a circular orbit, at a given epoch.
     *
     * @param satelliteId stable id used to seed initial mean anomaly + RAAN
     * @param altitudeKm orbit altitude above earth surface
     * @param inclinationDeg orbit inclination
     * @param obsLatDeg observer latitude
     * @param obsLonDeg observer longitude
     * @param timeMs unix epoch in milliseconds
     */
    fun skyPosition(
        satelliteId: String,
        altitudeKm: Double,
        inclinationDeg: Double,
        obsLatDeg: Double,
        obsLonDeg: Double,
        timeMs: Long
    ): SkyPosition {
        val rOrbit = EARTH_RADIUS_KM + altitudeKm
        val periodSec = 2.0 * PI * sqrt(rOrbit.pow(3) / GM_KM3_PER_S2)
        val incl = Math.toRadians(inclinationDeg)

        val seed = stableSeed(satelliteId)
        val m0 = (seed % 360) * PI / 180.0
        val raan0 = (((seed / 360) % 360) * PI / 180.0)

        val tSec = timeMs / 1000.0
        val meanAnomaly = (m0 + 2.0 * PI * tSec / periodSec) % (2.0 * PI)

        // Position in orbital plane (perigee on +x, true anomaly = mean anomaly for circular)
        val xOp = rOrbit * cos(meanAnomaly)
        val yOp = rOrbit * sin(meanAnomaly)

        // Inclination rotation about x-axis
        val xI = xOp
        val yI = yOp * cos(incl)
        val zI = yOp * sin(incl)

        // RAAN drifts slowly westward; for our simulation we apply a fixed RAAN0
        // and rely on Earth's rotation to give the relative motion under the satellite.
        val raan = raan0
        val xEci = xI * cos(raan) - yI * sin(raan)
        val yEci = xI * sin(raan) + yI * cos(raan)
        val zEci = zI

        // Convert ECI -> ECEF using GMST. Approximation (Vallado simplified):
        val gmst = greenwichMeanSiderealTimeRad(timeMs)
        val xEcef = xEci * cos(gmst) + yEci * sin(gmst)
        val yEcef = -xEci * sin(gmst) + yEci * cos(gmst)
        val zEcef = zEci

        // Observer ECEF (sea level, spherical earth)
        val obsLat = Math.toRadians(obsLatDeg)
        val obsLon = Math.toRadians(obsLonDeg)
        val xObs = EARTH_RADIUS_KM * cos(obsLat) * cos(obsLon)
        val yObs = EARTH_RADIUS_KM * cos(obsLat) * sin(obsLon)
        val zObs = EARTH_RADIUS_KM * sin(obsLat)

        // Vector from observer to satellite (ECEF)
        val dx = xEcef - xObs
        val dy = yEcef - yObs
        val dz = zEcef - zObs

        // Rotate to local ENU
        val sinLat = sin(obsLat); val cosLat = cos(obsLat)
        val sinLon = sin(obsLon); val cosLon = cos(obsLon)
        val east = -sinLon * dx + cosLon * dy
        val north = -sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz
        val up = cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz

        val range = sqrt(dx * dx + dy * dy + dz * dz)
        val elevation = Math.toDegrees(asin(up / range))
        var azimuth = Math.toDegrees(atan2(east, north))
        if (azimuth < 0) azimuth += 360.0

        return SkyPosition(azimuth, elevation, range)
    }

    private fun greenwichMeanSiderealTimeRad(timeMs: Long): Double {
        // Julian Date from Unix epoch
        val jd = timeMs / 86400000.0 + 2440587.5
        val t = (jd - 2451545.0) / 36525.0
        // GMST in seconds (IAU 1982 simplified)
        var gmstSec = 67310.54841 +
                (876600.0 * 3600.0 + 8640184.812866) * t +
                0.093104 * t * t -
                6.2e-6 * t * t * t
        gmstSec %= 86400.0
        if (gmstSec < 0) gmstSec += 86400.0
        return gmstSec * 2.0 * PI / 86400.0
    }

    /**
     * Propagate sky position from real Keplerian orbital elements (CelesTrak OMM / TLE).
     *
     * This is a two-body Keplerian propagator (no J2/drag/SGP4) — accurate for short-term
     * AR display where the satellite epoch is recent. For LEO with eccentricity < 0.01
     * (ISS, Starlink, NOAA, etc.) the position error is well under the angular size of
     * a marker on screen.
     */
    fun skyPositionFromElements(
        epoch: OffsetDateTime,
        meanMotionRevPerDay: Double,
        eccentricity: Double,
        inclinationDeg: Double,
        raanDeg: Double,
        argPericenterDeg: Double,
        meanAnomalyAtEpochDeg: Double,
        obsLatDeg: Double,
        obsLonDeg: Double,
        timeMs: Long
    ): SkyPosition {
        val n = meanMotionRevPerDay * 2.0 * PI / 86400.0 // rad/s
        val a = (GM_KM3_PER_S2 / (n * n)).pow(1.0 / 3.0) // semi-major axis (km)

        val epochMs = epoch.toInstant().toEpochMilli()
        val dtSec = (timeMs - epochMs) / 1000.0
        val m0 = Math.toRadians(meanAnomalyAtEpochDeg)
        var meanAnomaly = (m0 + n * dtSec) % (2.0 * PI)
        if (meanAnomaly < 0) meanAnomaly += 2.0 * PI

        // Solve Kepler: M = E - e sin E (Newton-Raphson)
        var ecc = meanAnomaly
        repeat(8) {
            ecc -= (ecc - eccentricity * sin(ecc) - meanAnomaly) /
                    (1.0 - eccentricity * cos(ecc))
        }

        val cosE = cos(ecc); val sinE = sin(ecc)
        val sqrt1mE2 = sqrt(1.0 - eccentricity * eccentricity)
        // Position in orbital (perifocal) frame
        val xPf = a * (cosE - eccentricity)
        val yPf = a * sqrt1mE2 * sinE

        // 3-1-3 rotation (argPericenter, inclination, RAAN) → ECI
        val omega = Math.toRadians(argPericenterDeg)
        val incl = Math.toRadians(inclinationDeg)
        val raan = Math.toRadians(raanDeg)

        val cosO = cos(omega); val sinO = sin(omega)
        val cosI = cos(incl);  val sinI = sin(incl)
        val cosR = cos(raan);  val sinR = sin(raan)

        // Rotate by ω about Z
        val x1 = xPf * cosO - yPf * sinO
        val y1 = xPf * sinO + yPf * cosO
        // Rotate by i about X
        val x2 = x1
        val y2 = y1 * cosI
        val z2 = y1 * sinI
        // Rotate by Ω about Z → ECI
        val xEci = x2 * cosR - y2 * sinR
        val yEci = x2 * sinR + y2 * cosR
        val zEci = z2

        // ECI -> ECEF via GMST
        val gmst = greenwichMeanSiderealTimeRad(timeMs)
        val xEcef = xEci * cos(gmst) + yEci * sin(gmst)
        val yEcef = -xEci * sin(gmst) + yEci * cos(gmst)
        val zEcef = zEci

        return ecefToSkyPos(xEcef, yEcef, zEcef, obsLatDeg, obsLonDeg)
    }

    private fun ecefToSkyPos(
        xSat: Double, ySat: Double, zSat: Double,
        obsLatDeg: Double, obsLonDeg: Double
    ): SkyPosition {
        val obsLat = Math.toRadians(obsLatDeg)
        val obsLon = Math.toRadians(obsLonDeg)
        val xObs = EARTH_RADIUS_KM * cos(obsLat) * cos(obsLon)
        val yObs = EARTH_RADIUS_KM * cos(obsLat) * sin(obsLon)
        val zObs = EARTH_RADIUS_KM * sin(obsLat)

        val dx = xSat - xObs
        val dy = ySat - yObs
        val dz = zSat - zObs

        val sinLat = sin(obsLat); val cosLat = cos(obsLat)
        val sinLon = sin(obsLon); val cosLon = cos(obsLon)
        val east = -sinLon * dx + cosLon * dy
        val north = -sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz
        val up = cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz

        val range = sqrt(dx * dx + dy * dy + dz * dz)
        val elevation = Math.toDegrees(asin(up / range))
        var azimuth = Math.toDegrees(atan2(east, north))
        if (azimuth < 0) azimuth += 360.0
        return SkyPosition(azimuth, elevation, range)
    }

    /**
     * Parse a CelesTrak OMM EPOCH string. Tolerates trailing 'Z' or fractional
     * seconds being absent.
     */
    fun parseOmmEpoch(s: String): OffsetDateTime {
        val cleaned = s.removeSuffix("Z")
        val fmt = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .toFormatter()
        return OffsetDateTime.of(java.time.LocalDateTime.parse(cleaned, fmt), ZoneOffset.UTC)
    }

    private fun stableSeed(id: String): Long {
        var h = 1469598103934665603L
        for (c in id) {
            h = h xor c.code.toLong()
            h *= 1099511628211L
        }
        return (h and Long.MAX_VALUE)
    }
}
