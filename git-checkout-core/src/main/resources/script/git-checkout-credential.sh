#!/bin/bash
credential_key='credential_java_path_'$BK_CI_BUILD_JOB_ID
credential_java_path=$(env | grep "$credential_key" | awk -F '=' '{print $2}')
if [[ -z $credential_java_path ]]; then
        exit 1
fi
$credential_java_path -jar $HOME/.checkout/git-checkout-credential.jar $@
