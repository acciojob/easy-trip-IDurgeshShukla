package com.driver.controllers;


import com.driver.model.Airport;
import com.driver.model.City;
import com.driver.model.Flight;
import com.driver.model.Passenger;
import io.swagger.models.auth.In;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class AirportController {
    public TreeMap<String ,Airport> Airports = new TreeMap<>();
    public TreeMap<Integer ,Passenger> Passengers = new TreeMap<>();
    public HashMap<Integer,Flight> Flights = new HashMap<>();
    public HashMap<Integer,List<Flight>> flightBookings = new HashMap<>();
    public HashMap<Integer,List<Passenger>> passangersBooking = new HashMap<>();
    HashMap<Integer, Integer> cancelled = new HashMap<>();
    @PostMapping("/add_airport")
    public String addAirport(@RequestBody Airport airport){
        //Simply add airport details to your database
        Airports.put(airport.getAirportName(),airport);
        //Return a String message "SUCCESS"
        return "SUCCESS";
    }

    @GetMapping("/get-largest-aiport")
    public String getLargestAirportName(){
        String largest = "";
        int max = Integer.MIN_VALUE;
        //Largest airport is in terms of terminals. 3 terminal airport is larger than 2 terminal airport
        for (String name : Airports.keySet()){
            int terminals = Airports.get(name).getNoOfTerminals();
            if( terminals > max){
                max = terminals;
                largest = name;
            }
        }
        //Incase of a tie return the Lexicographically smallest airportName

       return largest;
    }

    @GetMapping("/get-shortest-time-travel-between-cities")
    public double getShortestDurationOfPossibleBetweenTwoCities(@RequestParam("fromCity") City fromCity, @RequestParam("toCity")City toCity){
        //Find the duration by finding the shortest flight that connects these 2 cities directly
        for (int id : Flights.keySet()){
            if (Flights.get(id).getFromCity().equals(fromCity) && Flights.get(id).getToCity().equals(toCity)){
                return Flights.get(id).getDuration();
            }
        }
        //If there is no direct flight between 2 cities return -1.

       return -1;
    }

    @GetMapping("/get-number-of-people-on-airport-on/{date}")
    public int getNumberOfPeopleOn(@PathVariable("date") Date date,@RequestParam("airportName")String airportName){
        int count =0;
        //Calculate the total number of people who have flights on that day on a particular airport
        for (int key:Flights.keySet()) {
            if(Flights.get(key).getFlightDate().equals(date)){
                if ((Flights.get(key).getFromCity().name().equals(airportName) || Flights.get(key).getToCity().name().equals(airportName)))
                count++;
            }
        }
        //This includes both the people who have come for a flight and who have landed on an airport after their flight

        return count;
    }

    @GetMapping("/calculate-fare")
    public int calculateFlightFare(@RequestParam("flightId")Integer flightId){

        int peoples = 0;
        //Calculation of flight prices is a function of number of people who have booked the flight already.
        try {
            peoples = passangersBooking.get(flightId).size();
            //Price for any flight will be : 3000 + noOfPeopleWhoHaveAlreadyBooked*50

            //Suppose if 2 people have booked the flight already : the price of flight for the third person will be 3000 + 2*50 = 3100
            //This will not include the current person who is trying to book, he might also be just checking price

            return 3000 + peoples * 50;
        } catch (Exception e){
            return -1;
        }

    }


    @PostMapping("/book-a-ticket")
    public String bookATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the numberOfPassengers who have booked the flight is greater than : maxCapacity, in that case :
        //return a String "FAILURE"
        if (passangersBooking.getOrDefault(flightId,new ArrayList<>()).size() < Flights.get(flightId).getMaxCapacity()){
        List<Passenger> passengers = passangersBooking.getOrDefault(flightId, new ArrayList<>());
        passengers.add(passengers.get(passengerId));
        passangersBooking.put(flightId, passengers);
    }
        try {
            if (flightBookings.size() >= Flights.get(flightId).getMaxCapacity()) return "FAILURE";
            List<Flight> list = flightBookings.getOrDefault(passengerId, new ArrayList<>());
            Passenger passenger = Passengers.get(passengerId);
            //Also if the passenger has already booked a flight then also return "FAILURE".
            for (Flight  flight : list){
                if (flight.getFlightId() == flightId) return "FAILURE";
            }
        }catch (Exception e){
            return "FAILURE";
        }
        //else if you are able to book a ticket then return "SUCCESS"
        List<Flight> list = flightBookings.getOrDefault(passengerId,new ArrayList<>());
        list.add(Flights.get(flightId));
        flightBookings.put(passengerId,list);
        return "SUCCESS";
    }

    @PutMapping("/cancel-a-ticket")
    public String cancelATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the passenger has not booked a ticket for that flight or the flightId is invalid or in any other failure case
        int cancel =0;
        if (passangersBooking.containsKey(flightId)){
            List<Passenger> list = passangersBooking.get(flightId);
            for (int i = 0;i < list.size(); i++){
                if (list.get(i).getPassengerId() == passengerId){
                    list.remove(i);
                    cancel++;
                    passangersBooking.put(flightId,list);
                    cancelled.put(flightId,cancel);
                    break;
                }
            }
        }
       try {
           List<Flight> flights = flightBookings.get(passengerId);
           for (int i = 0; i< flights.size(); i++){
               if (flights.get(i).getFlightId() == flightId){
                   flights.remove(i);
                   flightBookings.put(passengerId,flights);
                   return "SUCCESS";
               }
           }
       } catch (Exception e){
           return "FAILURE";
       }
        // then return a "FAILURE" message
        // Otherwise return a "SUCCESS" message
        // and also cancel the ticket that passenger had booked earlier on the given flightId
        return "FAILURE";
    }


    @GetMapping("/get-count-of-bookings-done-by-a-passenger/{passengerId}")
    public int countOfBookingsDoneByPassengerAllCombined(@PathVariable("passengerId")Integer passengerId){
        try {
            return flightBookings.get(passengerId).size();
        } catch (Exception e){
            return -1;
        }
        //Tell the count of flight bookings done by a passenger: This will tell the total count of flight bookings done by a passenger :
    }

    @PostMapping("/add-flight")
    public String addFlight(@RequestBody Flight flight){
        try {
            Flights.put(flight.getFlightId(), flight);
            return "SUCCESS";
        }
        catch (Exception e){
            return "FAILURE";
        }
        //Return a "SUCCESS" message string after adding a flight.
    }


    @GetMapping("/get-aiportName-from-flight-takeoff/{flightId}")
    public String getAirportNameFromFlightId(@PathVariable("flightId")Integer flightId){

        //We need to get the starting airportName from where the flight will be taking off (Hint think of City variable if that can be of some use)

        //return null incase the flightId is invalid or you are not able to find the airportName
        try {
            return Flights.get(flightId).getFromCity().name();
        }catch (Exception e){
            return null;
        }
    }


    @GetMapping("/calculate-revenue-collected/{flightId}")
    public int calculateRevenueOfAFlight(@PathVariable("flightId")Integer flightId){
        int total = 0;
        Flight flight = Flights.get(flightId);
        //Calculate the total revenue that a flight could have
        //That is of all the passengers that have booked a flight till now and then calculate the revenue
        //Revenue will also decrease if some passenger cancels the flight
        int totals = calculateFlightFare(flightId);
        int cancel = cancelled.getOrDefault(flightId,0);
        if (cancel == 0) return total - 50;
        return totals - cancelled.get(flightId);
    }


    @PostMapping("/add-passenger")
    public String addPassenger(@RequestBody Passenger passenger){

        //Add a passenger to the database
        Passengers.put(passenger.getPassengerId(),passenger);
        //And return a "SUCCESS" message if the passenger has been added successfully.

       return "SUCCESS";
    }


}
