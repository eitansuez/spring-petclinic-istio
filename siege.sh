#!/bin/sh

siege --concurrent=3 --delay=3 --file=./urls.txt
