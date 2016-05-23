#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG Commit: $TRAVIS_COMMIT\n"

function error_exit
{
        echo -e "\e[01;31m$1\e[00m" 1>&2
        exit 1
}

echo -e "Building Clojure parts..."

echo -e "Building 'comsat-ring-jetty9'..."
cd comsat-ring-jetty9
lein test || error_exit "Error building 'comsat-ring-jetty9'"
cd ..

echo -e "Building 'comsat-httpkit'..."
cd comsat-httpkit
lein test || error_exit "Error building 'comsat-clj-http'"
cd ..

echo -e "Building 'comsat-redis'..."
cd comsat-redis
make test || error_exit "Error building 'comsat-redis'"
cd ..

end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Buid-clj process finished in $minutes minute(s) and $seconds seconds"
