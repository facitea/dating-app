package com.dating.backend.matching;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Approximate straight-line distance between registered administrative-area centroids, not GPS. */
@Component
public class RegionDistance {
    private record Point(double lat, double lon) {}
    private final Map<String,Point> centers = new HashMap<>();
    public RegionDistance() {
        try(var reader=new BufferedReader(new InputStreamReader(new ClassPathResource("matching/region-centers.csv").getInputStream(),StandardCharsets.UTF_8))) {
            reader.lines().filter(line->!line.isBlank()&&!line.startsWith("#")).forEach(line->{
                var parts=line.split(",");centers.put(parts[0],new Point(Double.parseDouble(parts[1]),Double.parseDouble(parts[2])));
            });
        } catch(IOException e) { throw new IllegalStateException("Cannot load region centers",e); }
    }
    private String key(String region) { return region==null?"":region.trim().replaceAll("\\s+"," "); }
    public boolean known(String region) { return centers.containsKey(key(region)); }
    public boolean within(String a,String b,int radiusKm) {
        var first=centers.get(key(a));var second=centers.get(key(b));
        if(first==null||second==null)return false;
        double lat=Math.toRadians(second.lat-first.lat),lon=Math.toRadians(second.lon-first.lon);
        double h=Math.pow(Math.sin(lat/2),2)+Math.cos(Math.toRadians(first.lat))*Math.cos(Math.toRadians(second.lat))*Math.pow(Math.sin(lon/2),2);
        double distance=6371.0088*2*Math.asin(Math.sqrt(Math.min(1,h)));
        return distance<=radiusKm;
    }
}
