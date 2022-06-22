# AdNav

## ScheduledExecutorService over Timer

Java Timer is unable to deal with client disconnection situations, so ScheduledExecutorService is used over Timer. 

## edge cases

This app deals with latitude edge cases perfectly, but not longitude. In cases where longitude >180 or <-180, Flightradar24 will return null result. In these cases, this app sets longitude [-180, +180]. This will return correct results, but it can obviously be optimised in terms of CPU efficiency. It should be good enough for now. 

## raw websocket config

To make it easier to test using Postman, this spring back-end uses raw websocket. So the users won't need to subcribe to any topic, which seems to be a pain in the ass in Postman by the way. 
