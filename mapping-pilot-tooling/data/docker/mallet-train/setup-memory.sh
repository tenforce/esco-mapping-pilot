#!/bin/bash

cd /usr/src/mallet

if [ -n "$MEMORY" ]
then
   echo "Setting memory to $MEMORY"
   sed -i "s/^MEMORY=.*\$/MEMORY=$MEMORY/" bin/mallet
fi
