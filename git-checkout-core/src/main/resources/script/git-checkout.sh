#!/bin/bash
bash_path=$(dirname $0)
credential_java_key='credential_java_path_'$BK_CI_BUILD_JOB_ID
credential_java_path=$(env | grep "^[a-zA-Z0-9_]*$credential_java_key=" | awk '{sub(/^[^=]+=/,"");print; exit;}')
if [[ -z $credential_java_path ]]; then
        exit 1
fi
credential_jar_key='credential_jar_path_'$BK_CI_BUILD_JOB_ID
credential_jar_path=$(env | grep "^[a-zA-Z0-9_]*$credential_jar_key=" | awk '{sub(/^[^=]+=/,"");print; exit;}')
if [[ -z $credential_jar_path ]]; then
        $credential_java_path -jar $bash_path/$credential_jar_path $@
elif [ -f $bash_path/git-checkout-credential.jar ]; then
        $credential_java_path -jar $bash_path/git-checkout-credential.jar $@
fi
