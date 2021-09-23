#!/bin/bash
credential_java_path=`eval echo '$'credential_java_path_$BK_CI_JOB_ID`
if [[ -z $credential_java_path ]]; then
        exit 1
fi
$credential_java_path -jar $HOME/.checkout/git-checkout-credential.jar $@