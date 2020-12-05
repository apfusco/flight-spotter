package com.example.blank;

public class AircraftInfo {

    private int icao24;
    private String callsign;
    private String originCountry;
    private String estDepartureAirport;
    private String estArrivalAirport;

    public AircraftInfo(int icao24, String callsign, String originCountry) {
        this.icao24 = icao24;
        this.callsign = callsign;
        this.originCountry = originCountry;
    }

    public AircraftInfo(int icao24, String callsign, String originCountry,
                        String estDepartureAirport, String estArrivalAirport) {
        this.icao24 = icao24;
        this.callsign = callsign;
        this.originCountry = originCountry;
        this.estDepartureAirport = estDepartureAirport;
        this.estArrivalAirport = estArrivalAirport;
    }

    public int getIcao24() { return this.icao24; }

    public String getCallsign() { return this.callsign; }

    public String getOriginCountry() { return this.callsign; }

    public String getEstDepartureAirport() { return this.estDepartureAirport; }

    public String getEstArrivalAirport() { return this.estArrivalAirport; }

    public void setCallsign(String callsign) { this.callsign = callsign; }

    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public void setEstDepartureAirport(String estDepartureAirport) {
        this.estDepartureAirport = estDepartureAirport;
    }

    public void setEstArrivalAirport(String estArrivalAirport) {
        this.estArrivalAirport = estArrivalAirport;
    }
}
