#!/bin/bash

# parameter for workspace
if [ "$1" != '' ]
then
    WS=$1
else
    echo "No ROS workspace provided!"
    exit
fi

# detect ros version
ROS=$(rosversion -d)

source /opt/ros/$ROS/setup.bash
source $WS/devel/setup.bash
export ROSCONSOLE_FORMAT='[${node}:${line}]: ${message}'
cd $WS
catkin_make
roslaunch emergency_exit stadium.launch visual:=true

