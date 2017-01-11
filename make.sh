#!/bin/bash

websitepath="rdfp-website/target/rdfp.war"
echo "websitepath is " $websitepath
echo ""

echo "run maven"
mvn clean package -P docs
rc=$?
if [[ $rc -ne 0 ]] ; then
  echo 'could not run maven'; exit $rc
fi

echo ""
echo "delete existing rdfp.war on the server"
ssh ci.emse.fr "cd /var/www/glassfish-apps/ 
rm rdfp-old.war"

ssh ci.emse.fr "cd /var/www/glassfish-apps/ 
mv rdfp.war rdfp-old.war"

echo "send rdfp.war on the server"
scp ${websitepath} "root@ci.emse.fr:/var/www/glassfish-apps/rdfp.war"

echo ""
echo "deploy rdfp.war on the server"
ssh ci.emse.fr "/opt/glassfish4/glassfish/bin/asadmin undeploy rdfp && /opt/glassfish4/glassfish/bin/asadmin deploy /var/www/glassfish-apps/rdfp.war"

echo "ok"
echo
