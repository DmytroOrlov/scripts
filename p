#!/bin/bash

MACPORTS_PREFIX='/opt/local'

export PATH="/opt/local/bin:/opt/local/sbin:$PATH"

command=$1

shift

exec $command $*
