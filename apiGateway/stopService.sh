#!/bin/sh
sudo kill -9 $(ps aux | grep [n]ode | awk '{print $2}')
