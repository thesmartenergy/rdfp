#!/bin/bash

echo "\$1 is the website project name, \$2 is the website name"
echo ""
echo "deploy website project \"$1\" with website name \"$2\""
echo ""

if [ -z "$1" ] || [ -z "$2" ]; then
	   echo "\$1 is the website project name, \$2 is the website name";
	   exit  -1
fi

path=${PWD##*/}
echo "path is " $path
echo ""

websitepath="$1/target/$2.war"
echo "websitepath is " $websitepath
echo ""

echo "run maven"
mvn clean package -P docs
rc=$?
if [[ $rc -ne 0 ]] ; then
  echo 'could not run maven'; exit $rc
fi

echo ""
echo "delete existing $2.war on the server"
ssh ci.emse.fr "cd /var/www/glassfish-apps/ && ls && rm $2.war && echo  \"undeploying\""

echo ""
echo "send $2.war on the server"
scp ${websitepath} root@ci.emse.fr:/var/www/glassfish-apps/$2.war

echo ""
echo "deploy $2.war on the server"
ssh ci.emse.fr "/opt/glassfish4/glassfish/bin/asadmin undeploy $2 && /opt/glassfish4/glassfish/bin/asadmin deploy /var/www/glassfish-apps/$2.war"

echo "ok ?"
echo
