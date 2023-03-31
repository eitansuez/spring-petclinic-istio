#!/bin/sh

# siege --concurrent=6 --delay=2 --file=./urls.txt
siege --concurrent=6 --delay=2 --file=./frontend-urls.txt

# siege --concurrent=3 --delay=3 http://${LB_IP}/api/gateway/owners/6
