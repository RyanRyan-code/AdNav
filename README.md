# AdNav

## how to run

> $ docker pull riponway2a/adnav
> 
> $ docker run -d -p 8080:8080 riponway2a/adnav

In mac, if you want to launch from terminal, you will need to have your Desktop Docker open at the same time. 

## query format

the endpoint is <br>**ws://localhost:8080/scan**

post body should be like this:
{<br>"latitude": 89,<br>"longitude": 179,<br>"radius" : 2000000<br>}

unit for latitude and longitude is degree.
unit for radius is meters.

## algorithm

First I tried to come up with a range of latitudes and longitudes. It should be big enough to include potential planes, but also small enough so that it won't cost too much time to calculate.

Then I used geoTools to accurately calculate the distance between two points on earth, and removed the planes that are outside the r-ring. 

## ScheduledExecutorService over Timer

Java Timer is unable to deal with client disconnection situations, so ScheduledExecutorService is used over Timer. 

## edge cases

This app deals with latitude edge cases perfectly, but not longitude. In cases where longitude >180 or <-180, Flightradar24 will return null result. In these cases, this app sets longitude [-180, +180]. This will return correct results, but it can obviously be optimised in terms of CPU efficiency. It should be good enough for now. 

## raw websocket config

To make it easier to test using Postman, this spring back-end uses raw websocket. So the users won't need to subcribe to any topic, which seems to be a pain in the ass in Postman by the way. 
