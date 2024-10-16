#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

url=$url
credentials=$credentials

APP_IDS=('92b75fd0-34cc-451c-942f-3dd0f283bcbd' 'a4cd3827-e717-4e09-965d-ab05df2591e5')

getLatestAvailableAppVersion() {
  app_hub_id=$1
  core_version=$(curl -u $credentials $url/api/system/info | jq -r '.version' | grep -o -E '2.[0-9]+')
  latest_compatible_version_response=$(curl -u $credentials $url/api/appHub/v2/apps/$app_hub_id/versions?minDhisVersion=lte:$core_version | jq -r '.result[0] // empty')
  echo "$latest_compatible_version_response"
}

installOrUpdate() {
  app_hub_id=$1
  app_response=$(curl -u $credentials $url/api/apps | jq --arg app_hub_id $app_hub_id -c '.[] | select(.app_hub_id==$app_hub_id)')
  app_version=$(echo "$app_response" | jq -r .version)

  latest_compatible_version_response="$(getLatestAvailableAppVersion $app_hub_id)"
  latest_compatible_version=$(echo $latest_compatible_version_response | jq -r '.version // empty')
  app_name=$(echo $latest_compatible_version_response | jq -r .slug)

  if [[ -z "$latest_compatible_version" ]]; then 
    echo "App $app_hub_id is not compatible with this instance. Skipping install."
    return
  fi 

  if [ -z "$app_version" ] || [[ $app_version != $latest_compatible_version  ]];then 
    echo "Installing $app_name app version $latest_compatible_version"
    download_url=$(echo $latest_compatible_version_response | jq -r .downloadUrl )
    download_name="$app_name.$latest_compatible_version.zip"
    downloadApp $download_url $download_name
    importApp $download_name

  else
    echo "$app_name app is up-to-date"
  fi  
}

downloadApp () {
    download_name=$2
    download_url=$1

    curl -s -L $download_url --output $download_name

}
importApp() {
  download_name=$1
  status=$(curl -o /dev/null -u $credentials -F file=@$download_name $url/api/apps -w "%{http_code}")

  if [ $status != 204 ];then
    echo 'App install failed!'
  fi
}


for i in "${APP_IDS[@]}";
do
  installOrUpdate $i
done
