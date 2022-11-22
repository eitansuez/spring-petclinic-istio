#!/bin/sh

#siege --concurrent=3 --delay=3 --file=./urls.txt

siege --concurrent=3 --delay=3 http://localhost/#!/owners/details/6
