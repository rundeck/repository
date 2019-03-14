#!/usr/bin/env bash

if [ "$1" == "-refresh" ] ; then
    echo "Refreshing app"
    tar -xf build/distributions/repository-cli.tar
    exit 0
fi
#export REPOSITORY_CLI_OPTS="-Dstage=stage"
./repository-cli/bin/repository-cli $@
