#!/bin/bash

# parameter for ros installation
if [ "$1" != '' ]
then
    ROS=$1
else
    echo "No ROS installation directory provided!"
    exit
fi

# parameter for workspace
if [ "$2" != '' ]
then
    WS=$2
else
    echo "No ROS workspace provided!"
    exit
fi

source $ROS/setup.bash
source $WS/devel/setup.bash
export ROSCONSOLE_FORMAT='[${node}:${line}]: ${message}'
cd $WS
catkin_make
roslaunch emergency_exit small_maze.launch visual:=true

