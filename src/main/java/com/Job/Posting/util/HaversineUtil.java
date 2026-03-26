package com.Job.Posting.util;

import org.springframework.stereotype.Component;

@Component
public class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates distance between two coordinates using Haversine formula
     * Same formula used by Uber, Swiggy, Urban Company
     *
     * @return distance in KM, Double.MAX_VALUE if coordinates are null
     */
    public double calculateDistance(Double lat1, Double lng1,
                                    Double lat2, Double lng2) {
        // handle null coordinates gracefully
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return Double.MAX_VALUE;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    // round to 1 decimal place
    public double roundDistance(double distance) {
        return Math.round(distance * 10.0) / 10.0;
    }
}
