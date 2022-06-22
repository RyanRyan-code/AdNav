# AdNav

## how to run

> $ docker pull riponway2a/adnav
> $ docker run -d -p 8080:8080 riponway2a/adnav

## query format

(for Postman)
the endpoint is ws://localhost:8080/scan

post body should be like this:
{
"latitude": 89,
"longitude": 179,
"radius" : 2000000
}

unit for latitude and longitude is degree.
unit for radius is meters.

## ScheduledExecutorService over Timer

Java Timer is unable to deal with client disconnection situations, so ScheduledExecutorService is used over Timer. 

## edge cases

This app deals with latitude edge cases perfectly, but not longitude. In cases where longitude >180 or <-180, Flightradar24 will return null result. In these cases, this app sets longitude [-180, +180]. This will return correct results, but it can obviously be optimised in terms of CPU efficiency. It should be good enough for now. 

## raw websocket config

To make it easier to test using Postman, this spring back-end uses raw websocket. So the users won't need to subcribe to any topic, which seems to be a pain in the ass in Postman by the way. 
