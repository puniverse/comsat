#!/bin/bash
start=$(date +%s)
echo -e "Current repo: $TRAVIS_REPO_SLUG Commit: $TRAVIS_COMMIT\n"

function error_exit
{
        echo -e "\e[01;31m$1\e[00m" 1>&2
        exit 1
}

if type -p java; then
    echo Found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo Found java executable in JAVA_HOME
    _java="$JAVA_HOME/bin/java"
else
    echo "No java found!"
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" -ge "1.8" ]]; then
        echo "Java 1.8 found"
        _java18parts=true
    else
        echo "Java < 1.8, not building Java 1.8+ parts"
    fi
fi

if [[ "$_java18parts" ]]; then
    echo -e "Building 'comsat-ring-jetty9' (Clojure)..."
    cd comsat-ring-jetty9
    lein test || error_exit "Error building 'comsat-ring-jetty9'"
    cd ..
fi

echo -e "Building 'comsat-httpkit' (Clojure)..."
cd comsat-httpkit
lein test || error_exit "Error building 'comsat-clj-http'"
cd ..

if [[ "$_java18parts" ]]; then
    echo -e "Building 'comsat-redis'..."
    cd comsat-redis
    make test || error_exit "Error building 'comsat-redis'"
    cd ..
fi

end=$(date +%s)
elapsed=$(( $end - $start ))
minutes=$(( $elapsed / 60 ))
seconds=$(( $elapsed % 60 ))
echo "Buid-clj process finished in $minutes minute(s) and $seconds seconds"
